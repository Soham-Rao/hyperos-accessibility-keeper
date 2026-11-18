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
        private val tracker = RecoveryTracker { SystemClock.elapsedRealtime() }
        fun start(context: Context, manual: Boolean = false): Boolean {
            if (!Consent.accepted(context)) return false
            return runCatching {
                context.startForegroundService(Intent(context, GuardService::class.java).putExtra("manual", manual))
                true
            }.getOrElse {
                GuardApp.eventLog(context).append(System.currentTimeMillis(), "[monitor] background service start was blocked; reopen Keeper and check background settings")
                false
            }
        }
        fun checkAndRepair(context: Context, source: String, manual: Boolean = false) {
            if (!checking.compareAndSet(false, true)) return
            try {
                GuardNotifications.refreshOngoing(context)
                if (!Consent.accepted(context)) return
                SelectionStore.reconcile(context)
                if (GuardApp.pauseState(context).isPaused()) return
                if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) return
                val catalog = Targets.installed(context)
                val selected = catalog.filter { Targets.protected(context, it) }
                tracker.retain(selected.map { it.component }.toSet())
                if (selected.isEmpty()) return
                val settings = AndroidSecureSettings(context.contentResolver)
                var dump = TargetRuntime.read()
                for (target in selected) {
                    if (GuardApp.pauseState(context).isPaused() || !Consent.accepted(context)) break
                    if (!Targets.protected(context, target)) continue
                    val runtime = TargetRuntime.forCurrentProcess(dump, target.component, Targets.runtimeLabel(catalog, target))
                    val configured = settings.getInt(SecureKeys.ACCESSIBILITY_ENABLED, 0) == 1 &&
                        serviceList(settings.getString(SecureKeys.ENABLED_SERVICES)).any {
                            canonicalService(it) == canonicalService(target.component)
                        }
                    val decision = tracker.decide(target.component, configured, runtime, RecoveryStatus.needsOpen(context, target), manual)
                    if (decision == RecoveryDecision.HEALTHY) {
                        RecoveryStatus.setNeedsOpen(context, target, false)
                        continue
                    }
                    if (decision != RecoveryDecision.REPAIR) continue
                    val installation = SelectionStore.installation(context, target) ?: continue
                    val log = GuardApp.eventLog(context)
                    log.append(System.currentTimeMillis(), "[$source] ${target.name}: recovery started (configured=$configured, runtime=$runtime)", target.component)
                    val result = TargetRepairer(settings).repair(target.component,
                        canStart = { Consent.accepted(context) && !GuardApp.pauseState(context).isPaused() && Targets.protected(context, target) },
                        canRestore = { SelectionStore.installation(context, target) == installation })
                    var message = when (result) {
                        RepairResult.Success -> {
                            dump = TargetRuntime.read()
                            var state = TargetRuntime.forCurrentProcess(dump, target.component, Targets.runtimeLabel(catalog, target))
                            repeat(4) {
                                if (state?.bound != true || state?.crashed == true) {
                                    Thread.sleep(1000)
                                    dump = TargetRuntime.read()
                                    state = TargetRuntime.forCurrentProcess(dump, target.component, Targets.runtimeLabel(catalog, target))
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
                        RepairResult.Cancelled -> "recovery cancelled — protection control or app installation changed"
                        is RepairResult.Failed -> "recovery failed: ${result.reason}"
                    }
                    // A repair can change the state of other services. Never reuse
                    // the pre-repair snapshot for later targets in this pass.
                    if (result !is RepairResult.Success) dump = TargetRuntime.read()
                    message = "${target.name}: $message"
                    log.append(System.currentTimeMillis(), "[$source] $message", target.component)
                    if (result != RepairResult.Cancelled) GuardNotifications.notifyRepair(context, message, if (result is RepairResult.Success && RecoveryStatus.needsOpen(context, target)) target.packageName else null)
                }
            } catch (e: Exception) {
                GuardApp.eventLog(context).append(System.currentTimeMillis(), "[$source] check failed: ${e.javaClass.simpleName}")
            } finally { checking.set(false) }
        }
    }
}

