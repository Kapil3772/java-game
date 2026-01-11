# PowerShell script to build a fat JAR for your Java game

# Paths
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$binFolder = Join-Path $projectRoot "bin"
$fatJar = Join-Path $binFolder "Game-1.0.0.jar"
$manifestFile = Join-Path $binFolder "manifest.txt"
$gsonJar = Join-Path $binFolder "gson.jar"

# Ensure manifest exists
if (-Not (Test-Path $manifestFile)) {
    Write-Host "Creating manifest.txt..."
    Set-Content $manifestFile "Main-Class: App`r`n"
}

# Go to bin folder
Push-Location $binFolder

# Extract gson.jar into bin temporarily
Write-Host "Extracting gson.jar..."
jar xf gson.jar

# Create fat JAR
Write-Host "Creating MyGame-Fat.jar..."
jar cfm $fatJar manifest.txt *

# Clean up extracted gson classes
Write-Host "Cleaning up temporary gson files..."
Remove-Item -Recurse -Force META-INF/com/google

# Done
Pop-Location
Write-Host "Fat JAR created at: $fatJar"
