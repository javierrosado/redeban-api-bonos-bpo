param(
  [string]$ParamsFile = (Join-Path $PSScriptRoot "parametros.conf")
)
. "$PSScriptRoot/common.ps1"

$P = Load-Params -File $ParamsFile
Validate-Params -ParamsMap $P
Write-Log INFO "===== Iniciando deploy.ps1 ====="
Log-ParamsOverview -ParamsMap $P
$imgRegistry = if ($P.ContainsKey("REGISTRY_PULL") -and -not [string]::IsNullOrWhiteSpace($P.REGISTRY_PULL)) {
  $P.REGISTRY_PULL
}
else {
  $P.REGISTRY
}
$img = "$imgRegistry/$($P.NAMESPACE)/$($P.APP_NAME):$($P.VERSION)"

try {
  $sourceRoot = Get-SourceRoot -ParamsMap $P
  Write-Log INFO "Directorio de fuentes: $sourceRoot"

  Ensure-OcLogin -ParamsMap $P

  Write-Log INFO "Renderizando manifiestos para $($P.APP_NAME)..."
  $dir = Render-Manifests -ParamsMap $P -SourceRoot $sourceRoot

  $manifests = @(
    @{ label = "ConfigMap"; file = "configmap.yaml" },
    @{ label = "Secret"; file = "secret.yaml" },
    @{ label = "Deployment"; file = "deployment.yaml" },
    @{ label = "Service"; file = "service.yaml" },
    @{ label = "Route"; file = "route.yaml" },
    @{ label = "HPA"; file = "hpa.yaml" }
  )

  foreach ($m in $manifests) {
    $path = Join-Path $dir $m.file
    Write-Log INFO "Aplicando $($m.label) ($path) en namespace $($P.NAMESPACE)"
    & oc -n $P.NAMESPACE apply -f $path
    if ($LASTEXITCODE -ne 0) { throw "oc apply fallo para $($m.file)" }
  }

  Write-Log INFO "Forzando reinicio del Deployment $($P.APP_NAME)..."
  & oc -n $P.NAMESPACE rollout restart ("deploy/$($P.APP_NAME)")
  if ($LASTEXITCODE -ne 0) { throw "oc rollout restart fallo para $($P.APP_NAME)" }

  Write-Log INFO "Esperando rollout del Deployment $($P.APP_NAME)..."
  & oc -n $P.NAMESPACE rollout status ("deploy/$($P.APP_NAME)")
  if ($LASTEXITCODE -ne 0) { throw "El rollout del deployment $($P.APP_NAME) no fue exitoso" }

  $routeHost = (& oc -n $P.NAMESPACE get route $P.APP_NAME -o jsonpath="{.spec.host}")
  if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($routeHost)) {
    Write-Log WARN "No se pudo obtener el host de la route $($P.APP_NAME)"
  }
  else {
    $base  = "https://$routeHost$($P.APP_CONTEXT)"
    $swagger = "$base/q/swagger-ui"
    $openapi = "$base/q/openapi?format=json"
    $health  = "$base/q/health"
    Write-Log INFO "Imagen desplegada: $img"
    Write-Log INFO "Rutas expuestas:"
    Write-Host "  Swagger UI : $swagger" -ForegroundColor Green
    Write-Host "  OPENAPI    : $openapi" -ForegroundColor Green
    Write-Host "  Health     : $health"  -ForegroundColor Green
  }

  exit 0
}
catch {
  Write-Log ERROR $_
  exit 1
}
