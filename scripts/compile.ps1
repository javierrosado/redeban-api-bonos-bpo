param(
  [hashtable]$ParamsMap,
  [string]$SourceRoot
)

if (-not (Get-Command -Name Write-Log -ErrorAction SilentlyContinue)) {
  . "$PSScriptRoot/common.ps1"
}

Write-Log INFO "===== Iniciando compile.ps1 ====="

if (-not $ParamsMap) {
  $ParamsMap = Load-Params
  Validate-Params -ParamsMap $ParamsMap
  Log-ParamsOverview -ParamsMap $ParamsMap
} else {
  Log-ParamsOverview -ParamsMap $ParamsMap
}
if (-not $SourceRoot) {
  $SourceRoot = Get-SourceRoot -ParamsMap $ParamsMap
}

$locationPushed = $false

try {
  Write-Log INFO "Preparando compilacion Fast-JAR (sin tests) en $SourceRoot..."

  Push-Location -Path $SourceRoot
  $locationPushed = $true

  $cmd = 'mvn -q -DskipTests clean package'
  Write-Log INFO "Ejecutando: $cmd"
  & mvn -q -DskipTests clean package
  if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion Maven" }

  $fastJar = Join-Path $SourceRoot "target/quarkus-app/quarkus-run.jar"
  if (-not (Test-Path $fastJar)) { throw "No se encontro fast-jar: $fastJar" }
  Write-Log INFO "Artefacto construido correctamente: $(Split-Path $fastJar -Leaf)"
  Write-Log INFO "Contenido quarkus-app disponible en $(Join-Path $SourceRoot 'target/quarkus-app')"
}
catch {
  Write-Log ERROR $_
  exit 1
}
finally {
  if ($locationPushed) { Pop-Location }
}
