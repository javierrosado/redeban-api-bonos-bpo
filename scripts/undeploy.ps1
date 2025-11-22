param(
  [string]$ParamsFile = (Join-Path $PSScriptRoot "parametros.conf")
)
. "$PSScriptRoot/common.ps1"
$P = Load-Params -File $ParamsFile
Validate-Params -ParamsMap $P
Write-Log INFO "===== Iniciando undeploy.ps1 ====="
Log-ParamsOverview -ParamsMap $P

try {
  $sourceRoot = Get-SourceRoot -ParamsMap $P
  $null = Get-EnvironmentManifestPath -ParamsMap $P -SourceRoot $sourceRoot
  $dir = Join-Path $sourceRoot "target/ocp"
  if (!(Test-Path $dir)) {
    Write-Log WARN "No existe $dir, renderizando desde deploy/$($P.ENVIRONMENT)..."
    $dir = Render-Manifests -ParamsMap $P -SourceRoot $sourceRoot
  }

  Ensure-OcLogin -ParamsMap $P
  Ensure-OcProjectAccess -ParamsMap $P

  Write-Log WARN "Eliminando recursos en namespace $($P.NAMESPACE)"
  & oc -n $P.NAMESPACE delete -f $dir --ignore-not-found=true
  if ($LASTEXITCODE -ne 0) { throw "oc delete fallo para $dir" }

  Write-Log INFO "Undeploy completado."
}
catch {
  Write-Log ERROR $_
  exit 1
}
