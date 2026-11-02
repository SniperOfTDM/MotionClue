# scripts/publish_gradual.ps1
# Pushes commits gradually to simulate an active, authentic development process,
# publishes the pre-built APK to GitHub Releases, and optionally shuts down the PC.

[CmdletBinding()]
param(
    [int]$MinMinutes = 20,
    [int]$MaxMinutes = 45,
    [int]$StepSize = 1,
    [switch]$ShutdownOnFinish,
    [switch]$SkipBuild,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "       MotionClue Gradual Push & Release Automation       " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Build and Verify APK
$apkPath = "app/build/outputs/apk/debug/app-debug.apk"
if (-not $SkipBuild) {
    Write-Host "`n[1/4] Building APK via Gradle assembleDebug..." -ForegroundColor Yellow
    if (-not $DryRun) {
        & .\gradlew.bat assembleDebug
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Gradle build failed with exit code $LASTEXITCODE"
            exit $LASTEXITCODE
        }
    }
}

if (-not (Test-Path $apkPath)) {
    Write-Error "APK file not found at $apkPath. Run with .\gradlew assembleDebug first."
    exit 1
}

$apkInfo = Get-Item $apkPath
$apkSizeMb = [math]::Round($apkInfo.Length / 1MB, 2)
Write-Host "[OK] Verified APK: $apkPath ($apkSizeMb MB)" -ForegroundColor Green

# 2. Collect Unpushed Commits
Write-Host "`n[2/4] Analyzing unpushed commits..." -ForegroundColor Yellow
$commits = @(git rev-list --reverse origin/main..HEAD)
$totalCommits = $commits.Count

if ($totalCommits -eq 0) {
    Write-Host "All commits are already pushed to origin/main." -ForegroundColor Green
} else {
    Write-Host "Found $totalCommits unpushed commit(s) ahead of origin/main." -ForegroundColor Cyan
    for ($i = 0; $i -lt $totalCommits; $i++) {
        $hash = $commits[$i]
        $summary = git log -1 --format="%h - %s" $hash
        Write-Host "  [$($i + 1)/$totalCommits] $summary" -ForegroundColor DarkGray
    }

    # 3. Gradual Push Loop
    Write-Host "`n[3/4] Starting gradual push (StepSize: $StepSize, Delay: ${MinMinutes}-${MaxMinutes} min)..." -ForegroundColor Yellow

    $pushTargets = @()
    for ($i = $StepSize - 1; $i -lt $totalCommits; $i += $StepSize) {
        $pushTargets += $commits[$i]
    }
    if ($pushTargets[-1] -ne $commits[-1]) {
        $pushTargets += $commits[-1]
    }

    $stepNum = 0
    foreach ($target in $pushTargets) {
        $stepNum++
        $commitInfo = git log -1 --format="%h %s" $target
        Write-Host "`n--> [$stepNum/$($pushTargets.Count)] Target commit: $commitInfo" -ForegroundColor Magenta

        if (-not $DryRun) {
            git push origin "$($target):refs/heads/main"
            if ($LASTEXITCODE -ne 0) {
                Write-Error "Git push failed at commit $target"
                exit $LASTEXITCODE
            }
            Write-Host "[OK] Push succeeded!" -ForegroundColor Green
        } else {
            Write-Host "[DRY RUN] Would execute: git push origin $($target):refs/heads/main" -ForegroundColor DarkCyan
        }

        # If not the last push target, wait
        if ($target -ne $pushTargets[-1]) {
            $delayMinutes = Get-Random -Minimum $MinMinutes -Maximum ($MaxMinutes + 1)
            $delaySeconds = $delayMinutes * 60
            $resumeTime = (Get-Date).AddMinutes($delayMinutes).ToString("HH:mm:ss")
            Write-Host "Sleeping for $delayMinutes minutes (resumes at $resumeTime)..." -ForegroundColor DarkYellow

            if (-not $DryRun) {
                while ($delaySeconds -gt 0) {
                    $minsLeft = [math]::Floor($delaySeconds / 60)
                    $secsLeft = $delaySeconds % 60
                    Write-Progress -Activity "Gradual push delay" -Status "$minsLeft min $secsLeft sec remaining until next push" -PercentComplete ((($delayMinutes * 60 - $delaySeconds) / ($delayMinutes * 60)) * 100)
                    Start-Sleep -Seconds 10
                    $delaySeconds -= 10
                }
                Write-Progress -Activity "Gradual push delay" -Completed
            } else {
                Write-Host "[DRY RUN] Simulating sleep ($delayMinutes minutes)" -ForegroundColor DarkCyan
            }
        }
    }
}

# 4. Publish Release with APK
Write-Host "`n[4/4] Creating GitHub Release and attaching APK..." -ForegroundColor Yellow
$releaseTag = "v0.1.0"
$releaseTitle = "MotionClue v0.1.0"
$releaseNotes = "Initial open-source release of MotionClue. Reduces motion sickness with native C++ vehicle motion cues, GPS speed flow, transport presets, and adaptive brightness."

if (-not $DryRun) {
    $existing = gh release view $releaseTag 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Release $releaseTag already exists. Uploading asset..." -ForegroundColor Yellow
        gh release upload $releaseTag "$apkPath#MotionClue-v0.1.0.apk" --clobber
    } else {
        Write-Host "Creating release $releaseTag..." -ForegroundColor Cyan
        gh release create $releaseTag "$apkPath#MotionClue-v0.1.0.apk" --title "$releaseTitle" --notes "$releaseNotes"
    }
    Write-Host "[OK] Release created and APK uploaded successfully!" -ForegroundColor Green
} else {
    Write-Host "[DRY RUN] Would execute: gh release create $releaseTag $apkPath#MotionClue-v0.1.0.apk" -ForegroundColor DarkCyan
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "                  ALL TASKS COMPLETE!                     " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green

# 5. Shutdown if requested
if ($ShutdownOnFinish) {
    Write-Host "`nShutting down PC in 120 seconds..." -ForegroundColor Red
    Write-Host "To cancel shutdown, run: shutdown /a" -ForegroundColor Yellow
    if (-not $DryRun) {
        shutdown.exe /s /t 120 /c "MotionClue push and APK release complete. Shutting down in 2 minutes. Type shutdown /a to abort."
    } else {
        Write-Host "[DRY RUN] Would run: shutdown.exe /s /t 120" -ForegroundColor DarkRed
    }
}
