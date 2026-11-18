package io.github.haku4130.noscrollguard

import android.app.Instrumentation
import android.app.Activity
import android.os.Bundle
import android.provider.Settings
import io.github.haku4130.noscrollguard.repair.canonicalService
import io.github.haku4130.noscrollguard.repair.serviceList
import io.github.haku4130.noscrollguard.service.GuardService
import io.github.haku4130.noscrollguard.state.TargetRuntime

/** Explicit device checks against a disposable fixture, never the user's apps. */
class VerificationInstrumentation : Instrumentation() {
    private lateinit var arguments: Bundle
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); this.arguments = arguments ?: Bundle(); start() }
    override fun onStart() {
        val result = Bundle()
        try {
            check(targetContext.packageName.endsWith(".verification")) { "Use -PverificationBuild=true; normal app must not be tested destructively" }
            val context = targetContext
            val fixture = GuardTarget("Keeper test fixture", "com.example.keeperfixture/com.example.keeperfixture.FixtureService")
            check(context.getSharedPreferences("targets", 0).all.none { (key, value) -> value == true && canonicalService(key) != fixture.component }) {
                "Verification copy has non-fixture selections; refusing to modify them"
            }
            fun configured() = serviceList(Settings.Secure.getString(context.contentResolver, "enabled_accessibility_services")).any { canonicalService(it) == fixture.component }
            fun disableFixture() {
                val others = serviceList(Settings.Secure.getString(context.contentResolver, "enabled_accessibility_services")).filter { canonicalService(it) != fixture.component }
                check(Settings.Secure.putString(context.contentResolver, "enabled_accessibility_services", others.joinToString(":")))
            }
            fun repair() = GuardService.checkAndRepair(context, "device verification", true)
            val case = arguments.getString("case") ?: "controls"
            when (case) {
                "approve-fixture-install" -> {
                    // Host has just requested installation of our built fixture.
                    // Never accept arbitrary installers or persist a device choice.
                    val automation = getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
                    var approved = false
                    repeat(40) {
                        if (!approved) {
                            val root = automation.rootInActiveWindow
                            if (root != null && root.packageName?.toString() in setOf("com.miui.securitycenter", "com.android.packageinstaller")) {
                                val labels = root.findAccessibilityNodeInfosByText("Keeper test fixture with a deliberately long service name for accessibility layout testing")
                                val title = root.findAccessibilityNodeInfosByText("Install via USB")
                                if (labels.isNotEmpty() && title.isNotEmpty()) {
                                    val install = root.findAccessibilityNodeInfosByText("Install").firstOrNull { it.text?.toString() == "Install" && it.isEnabled }
                                    approved = install?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true
                                }
                            }
                            if (!approved) Thread.sleep(100)
                        }
                    }
                    check(approved) { "Known fixture installation prompt was not available" }
                }
                "ui-audit" -> UiAudit(this).run()
                "controls" -> {
                    check(!SelectionStore.retain(context)) { "Retention must default off" }
                    context.getSharedPreferences("consent", 0).edit().clear().commit()
                    Targets.setProtected(context, fixture, true)
                    disableFixture(); repair(); check(!configured()) { "Repair ran before consent" }
                    Consent.accept(context); repair(); check(configured()) { "Consented recovery failed" }
                    Targets.setProtected(context, fixture, false)
                    check(configured()) { "Protection off changed Android setting" }
                    disableFixture(); repair(); check(!configured()) { "Deliberate disable was undone with protection off" }
                    Targets.setProtected(context, fixture, true)
                    GuardApp.pauseState(context).pauseFor(60_000)
                    repair(); check(!configured()) { "Paused recovery ran" }
                    GuardApp.pauseState(context).resume(); repair(); check(configured()) { "Resume did not recover" }
                    disableFixture()
                    GuardApp.pauseState(context).pauseFor(1)
                    Thread.sleep(20)
                    check(!GuardApp.pauseState(context).isPaused())
                    repair(); check(configured()) { "Expired pause blocked recovery" }
                }
                "dump-revoked" -> { check(TargetRuntime.read().let { TargetRuntime.parse(it, fixture.component, fixture.name) } == null) { "DUMP revocation did not produce unknown status" } }
                "write-revoked" -> {
                    check(!configured()) { "Fixture should be disabled by shell before write-revoked test" }
                    repair(); check(!configured()) { "Repair ran without write grant" }
                }
                "select-off-retention" -> { SelectionStore.setRetain(context, false); Targets.setProtected(context, fixture, true); check(Targets.protected(context, fixture)) }
                "select-on-retention" -> { SelectionStore.setRetain(context, true); Targets.setProtected(context, fixture, true); check(SelectionStore.retain(context)); check(Targets.protected(context, fixture)) }
                "expect-cleared" -> { check(!Targets.protected(context, fixture)) { "Reinstalled app retained choice with setting off" } }
                "expect-retained" -> { check(Targets.protected(context, fixture)) { "Same-signer reinstall lost opted-in choice" }; repair(); check(configured()) }
                "expect-new-component-off" -> {
                    val installed = Targets.installed(context).single { it.packageName == fixture.packageName }
                    check(installed.component != fixture.component)
                    check(!Targets.protected(context, installed)) { "A renamed service inherited the old service choice" }
                    val before = Settings.Secure.getString(context.contentResolver, "enabled_accessibility_services")
                    repair()
                    check(Settings.Secure.getString(context.contentResolver, "enabled_accessibility_services") == before) { "Missing selected component caused a settings write" }
                }
                else -> error("Unknown case")
            }
            // Instrumentation.finish terminates without a normal Activity stop, which
            // normally flushes SharedPreferences.apply. Wait for pending writes here.
            listOf("selection_identity", "targets", "consent", "guard", "recovery").forEach {
                check(context.getSharedPreferences(it, 0).edit().commit())
            }
            result.putString("stream", "PASS: $case\n")
            finish(Activity.RESULT_OK, result)
        } catch (failure: Throwable) {
            result.putString("stream", "FAIL: ${failure.javaClass.simpleName}: ${failure.message}\n")
            finish(Activity.RESULT_CANCELED, result)
        }
    }
}
