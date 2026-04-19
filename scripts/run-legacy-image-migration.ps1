param(
    [ValidateSet("inventory", "migrate", "verify")]
    [string]$Mode = "inventory",
    [switch]$DryRun,
    [string]$Profile = "prod",
    [int]$BatchSize = 200,
    [int]$SampleSize = 50,
    [int]$MaxRetries = 3,
    [int]$MaxImageSizeMB = 10,
    [switch]$IncludeFileRecordUrl,
    [switch]$SkipVerifyAfterMigrate,
    [string]$ReportDir = "migration-reports"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$envFile = Join-Path $projectRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') {
            return
        }
        $name, $value = $_ -split '=', 2
        if ($null -eq $name -or $null -eq $value) {
            return
        }
        $normalizedName = $name.Trim()
        $normalizedValue = $value.Trim().Trim('"').Trim("'")
        [Environment]::SetEnvironmentVariable($normalizedName, $normalizedValue, "Process")
    }
}

[Environment]::SetEnvironmentVariable("SPRING_PROFILES_ACTIVE", $Profile, "Process")
[Environment]::SetEnvironmentVariable("SPRING_MAIN_WEB_APPLICATION_TYPE", "none", "Process")
[Environment]::SetEnvironmentVariable("MIGRATION_LEGACY_IMAGES_ENABLED", "true", "Process")
[Environment]::SetEnvironmentVariable("MIGRATION_LEGACY_IMAGES_MODE", $Mode, "Process")
[Environment]::SetEnvironmentVariable("MIGRATION_LEGACY_IMAGES_DRY_RUN", $DryRun.IsPresent.ToString().ToLower(), "Process")
[Environment]::SetEnvironmentVariable("MIGRATION_LEGACY_IMAGES_BATCH_SIZE", $BatchSize.ToString(), "Process")
[Environment]::SetEnvironmentVariable("MIGRATION_LEGACY_IMAGES_SAMPLE_SIZE", $SampleSize.ToString(), "Process")
[Environment]::SetEnvironmentVariable("MIGRATION_LEGACY_IMAGES_MAX_RETRIES", $MaxRetries.ToString(), "Process")
[Environment]::SetEnvironmentVariable("MIGRATION_LEGACY_IMAGES_MAX_IMAGE_SIZE_BYTES", ($MaxImageSizeMB * 1024 * 1024).ToString(), "Process")
[Environment]::SetEnvironmentVariable("MIGRATION_LEGACY_IMAGES_INCLUDE_FILE_RECORD_URL", $IncludeFileRecordUrl.IsPresent.ToString().ToLower(), "Process")
[Environment]::SetEnvironmentVariable("MIGRATION_LEGACY_IMAGES_VERIFY_AFTER_MIGRATE", (-not $SkipVerifyAfterMigrate.IsPresent).ToString().ToLower(), "Process")
[Environment]::SetEnvironmentVariable("MIGRATION_LEGACY_IMAGES_REPORT_DIR", $ReportDir, "Process")

Write-Host "Running legacy image migration..."
Write-Host "  Mode: $Mode"
Write-Host "  DryRun: $($DryRun.IsPresent)"
Write-Host "  Profile: $Profile"
Write-Host "  BatchSize: $BatchSize"
Write-Host "  SampleSize: $SampleSize"
Write-Host "  MaxRetries: $MaxRetries"
Write-Host "  MaxImageSizeMB: $MaxImageSizeMB"
Write-Host "  ReportDir: $ReportDir"

mvn --% spring-boot:run
