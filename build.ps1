# 一键构建脚本。用法：
#   .\build.ps1              出正式版 APK（签名过的，可以直接装手机）
#   .\build.ps1 test         跑单元测试 + 重新生成界面截图
#   .\build.ps1 install      构建并直接装到已用 USB 连上的手机
#   .\build.ps1 screenshots  只重新生成界面截图
param(
    [ValidateSet('apk', 'test', 'install', 'screenshots')]
    [string]$Task = 'apk'
)

$ErrorActionPreference = 'Stop'

# ---- 找 JDK ----
# AGP 8.x 需要 JDK 17~21，太高（比如 22+）反而不支持，所以优先挑 17/21。
function Find-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        return $env:JAVA_HOME
    }
    $roots = @(
        'C:\Program Files\Java',
        (Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'),
        (Join-Path $env:LOCALAPPDATA 'Programs\Android Studio\jbr'),
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
        (Join-Path $env:ProgramFiles 'Microsoft')
    )
    $found = @()
    foreach ($root in $roots) {
        if (-not (Test-Path $root)) { continue }
        if (Test-Path (Join-Path $root 'bin\java.exe')) { $found += $root }
        # 有些安装器会多套一层目录（例如 Program Files\Java\latest\jdk-21），所以往下找两层
        $found += (Get-ChildItem $root -Directory -Recurse -Depth 1 -ErrorAction SilentlyContinue |
            Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } |
            ForEach-Object { $_.FullName })
    }
    # 注意用 @() 包一层：只有一个候选时管道返回的是字符串本身，
    # 直接 [0] 取到的会是第一个字符（'C'），而不是整个路径
    $preferred = @($found | Where-Object { $_ -match 'jdk-?(17|21)\b|jbr' } | Sort-Object)
    if ($preferred.Count -gt 0) { return $preferred[0] }
    $rest = @($found)
    if ($rest.Count -gt 0) { return $rest[0] }
    return $null
}

# ---- 找 Android SDK ----
# 顺序：环境变量 -> local.properties -> 默认安装位置
function Find-AndroidSdk {
    foreach ($candidate in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)) {
        if ($candidate -and (Test-Path $candidate)) { return $candidate }
    }
    $localProps = Join-Path $PSScriptRoot 'local.properties'
    if (Test-Path $localProps) {
        $line = Get-Content $localProps |
            Where-Object { $_ -match '^\s*sdk\.dir\s*=' } |
            Select-Object -First 1
        if ($line) {
            $path = ($line -replace '^\s*sdk\.dir\s*=\s*', '').Trim()
            $path = $path.Replace('\\', '\').Replace('\:', ':')
            if (Test-Path $path) { return $path }
        }
    }
    $fallback = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path $fallback) { return $fallback }
    return $null
}

if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $env:JAVA_HOME = Find-JavaHome
}
$sdk = Find-AndroidSdk

if (-not $env:JAVA_HOME) {
    throw '找不到 JDK。请安装 JDK 17 或 21，或先设置 JAVA_HOME 环境变量。'
}
if (-not $sdk) {
    throw '找不到 Android SDK。请设置 ANDROID_HOME 环境变量，或在 local.properties 里写 sdk.dir。'
}

$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk

Write-Host "JDK       : $env:JAVA_HOME"
Write-Host "Android SDK: $env:ANDROID_HOME"
Write-Host ''

$gradlew = Join-Path $PSScriptRoot 'gradlew.bat'

switch ($Task) {
    'apk' { & $gradlew assembleRelease --console=plain }
    'test' { & $gradlew testDebugUnitTest --console=plain }
    'install' { & $gradlew installRelease --console=plain }
    'screenshots' { & $gradlew testDebugUnitTest --console=plain --rerun-tasks }
}

if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($Task -in @('apk', 'install')) {
    Write-Host ''
    Write-Host 'APK: app\build\outputs\apk\release\app-release.apk'
}
if ($Task -in @('test', 'screenshots')) {
    Write-Host ''
    Write-Host '截图: app\build\screenshots\'
}
