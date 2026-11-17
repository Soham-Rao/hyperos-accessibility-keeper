param(
    [Parameter(Mandatory=$true)][string]$Serial,
    [string]$Adb = (Join-Path $PSScriptRoot '..\AndroidSdk\platform-tools\adb.exe'),
    [string]$Apk = (Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk')
)
$ErrorActionPreference = 'Stop'
$package = 'io.github.soham.accessibilitykeeper'
function Invoke-Device {
    & $Adb -s $Serial @args
    if ($LASTEXITCODE -ne 0) { throw 'Device command failed. Check the phone for an approval prompt.' }
}
if (!(Test-Path -LiteralPath $Apk)) { throw 'Build the debug APK first.' }
Invoke-Device get-state
Invoke-Device install -r $Apk
foreach ($permission in @('WRITE_SECURE_SETTINGS', 'DUMP', 'POST_NOTIFICATIONS')) {
    Invoke-Device shell pm grant $package "android.permission.$permission"
}
Invoke-Device shell dumpsys deviceidle whitelist "+$package"
Invoke-Device shell am start -n "$package/io.github.haku4130.noscrollguard.ui.MainActivity"
Write-Output 'Installed. Enable protection for the apps you want, then allow Accessibility Keeper in HyperOS Background autostart.'
