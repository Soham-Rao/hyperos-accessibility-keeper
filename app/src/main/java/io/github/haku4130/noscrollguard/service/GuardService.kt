package io.github.haku4130.noscrollguard.service

import android.Manifest
import android.app.Service
import android.content.*
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.*
import android.provider.Settings
import io.github.haku4130.noscrollguard.*
import io.github.haku4130.noscrollguard.repair.*
import io.github.haku4130.noscrollguard.settings.*
import io.github.haku4130.noscrollguard.state.TargetRuntime
import io.github.haku4130.noscrollguard.work.HealthWorker
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GuardService : Service() {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private lateinit var observer: ContentObserver
    private lateinit var receiver: BroadcastReceiver

    override fun onCreate() {
        super.onCreate()
        GuardNotifications.ensureChannels(this)
        startForeground(GuardNotifications.ID_ONGOING, GuardNotifications.ongoing(this))
        if (!Consent.accepted(this)) { stopSelf(); return }
        HealthWorker.schedule(this)
        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { enqueue("settings changed") }
        }
        listOf(SecureKeys.ACCESSIBILITY_ENABLED, SecureKeys.ENABLED_SERVICES).forEach {
            contentResolver.registerContentObserver(Settings.Secure.getUriFor(it), false, observer)
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { enqueue("screen/unlock") }
        }
        registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }, Context.RECEIVER_NOT_EXPORTED)
        enqueue("startup")
        // A crash can leave settings untouched. Check while the phone is in use;
        // WorkManager provides a less frequent backstop while asleep.
        executor.scheduleWithFixedDelay({
            if (getSystemService(PowerManager::class.java).isInteractive) checkAndRepair(this, "health check")
        }, 30, 30, TimeUnit.SECONDS)
    }

    private fun enqueue(source: String) { executor.execute { checkAndRepair(this, source) } }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Consent.accepted(this)) { stopSelf(); return START_NOT_STICKY }
        executor.execute { checkAndRepair(this, "requested check", intent?.getBooleanExtra("manual", false) == true) }
        return START_STICKY
    }
    override fun onDestroy() {
        if (::observer.isInitialized) contentResolver.unregisterContentObserver(observer)
        if (::receiver.isInitialized) unregisterReceiver(receiver)
        // Do not interrupt a rebind between removing and restoring its target.
        executor.shutdown()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private val checking = AtomicBoolean(false)
        private val lastAttempt = mutableMapOf<String, Long>()
        private val bindingSince = mutableMapOf<String, Long>()
        fun start(context: Context, manual: Boolean = false) {
            if (!Consent.accepted(context)) return
            context.startForegroundService(Intent(context, GuardService::class.java).putExtra("manual", manual))
        }
        fun checkAndRepair(context: Context, source: String, manual: Boolean = false) {
            if (!checking.compareAndSet(false, true)) return
            try {
                if (!Consent.accepted(context)) return
                SelectionStore.reconcile(context)
                if (GuardApp.pauseState(context).isPaused()) return
                if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) return
                val catalog = Targets.installed(context)
                val selected = catalog.filter { Targets.protected(context, it) }
                if (selected.isEmpty()) return
                val settings = AndroidSecureSettings(context.contentResolver)
                val dump = TargetRuntime.read()
                for (target in selected) {
                    if (GuardApp.pauseState(context).isPaused() || !Consent.accepted(context)) break
                    if (!Targets.protected(context, target)) continue
                    val runtime = TargetRuntime.parse(dump, target.component, Targets.runtimeLabel(catalog, target))
                    val configured = settings.getInt(SecureKeys.ACCESSIBILITY_ENABLED, 0) == 1 &&
                        serviceList(settings.getString(SecureKeys.ENABLED_SERVICES)).any {
                            canonicalService(it) == canonicalService(target.component)
                        }
                    val now = SystemClock.elapsedRealtime()
                    val bindingFor = if (runtime?.binding == true) now - bindingSince.getOrPut(target.component) { now }
                        else { bindingSince.remove(target.component); 0L }
                    val decision = recoveryDecision(configured, runtime, RecoveryStatus.needsOpen(context, target), bindingFor, manual)
                    if (decision == RecoveryDecision.HEALTHY) {
                        lastAttempt.remove(target.component)
                        bindingSince.remove(target.component)
                        RecoveryStatus.setNeedsOpen(context, target, false)
                        continue
                    }
                    if (decision != RecoveryDecision.REPAIR) continue
                    // Avoid rapid cycles if a target repeatedly crashes or cannot bind.
                    if (!manual && lastAttempt[target.component]?.let { now - it < 60_000 } == true) continue
                    lastAttempt[target.component] = now
                    val log = GuardApp.eventLog(context)
                    log.append(System.currentTimeMillis(), "[$source] ${target.name}: recovery started (configured=$configured, runtime=$runtime)", target.component)
                    val result = TargetRepairer(settings).repair(target.component)
                    var message = when (result) {
                        RepairResult.Success -> {
                            var state = TargetRuntime.parse(TargetRuntime.read(), target.component, Targets.runtimeLabel(catalog, target))
                            repeat(4) {
                                if (state?.bound != true || state?.crashed == true) {
                                    Thread.sleep(1000)
                                    state = TargetRuntime.parse(TargetRuntime.read(), target.component, Targets.runtimeLabel(catalog, target))
                                }
                            }
                            RecoveryStatus.setNeedsOpen(context, target, state != null && (state?.bound != true || state?.crashed == true))
                            when {
                                state == null -> "settings restored; runtime could not be verified"
                                state?.bound == true && state?.crashed == false -> "recovered and bound"
                                else -> "open the app to finish connecting — accessibility settings are restored"
                            }
                        }
                        RepairResult.NoPermission -> "permission missing — reconnect for setup"
                        is RepairResult.Failed -> "recovery failed: ${result.reason}"
                    }
                    message = "${target.name}: $message"
                    log.append(System.currentTimeMillis(), "[$source] $message", target.component)
                    GuardNotifications.notifyRepair(context, message, if (result is RepairResult.Success && RecoveryStatus.needsOpen(context, target)) target.packageName else null)
                }
            } catch (e: Exception) {
                GuardApp.eventLog(context).append(System.currentTimeMillis(), "[$source] check failed: ${e.javaClass.simpleName}")
            } finally { checking.set(false) }
        }
    }
}
