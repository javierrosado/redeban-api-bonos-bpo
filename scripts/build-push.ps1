param()
. "$PSScriptRoot/common.ps1"

try {
  Write-Log INFO "===== Iniciando build-push.ps1 ====="
  $P = Load-Params
  Validate-Params -ParamsMap $P
  Log-ParamsOverview -ParamsMap $P

  $sourceRoot = Get-SourceRoot -ParamsMap $P
  Write-Log INFO "Directorio de fuentes: $sourceRoot"

  $imageTag = "$($P.REGISTRY)/$($P.NAMESPACE)/$($P.APP_NAME):$($P.VERSION)"
  Write-Log INFO "Imagen objetivo: $imageTag"

  Write-Log INFO "Ejecutando compilacion Fast-JAR ..."
  & "$PSScriptRoot/compile.ps1" -ParamsMap $P -SourceRoot $sourceRoot
  if ($LASTEXITCODE -ne 0) { throw "Compilacion fallida" }

  $fastJar = Join-Path $sourceRoot "target/quarkus-app/quarkus-run.jar"
  if (-not (Test-Path $fastJar)) {
    throw "No se encontro el artefacto Fast-JAR: $fastJar"
  }
  Write-Log INFO "Artefacto detectado: $fastJar"

  Ensure-OcLogin -ParamsMap $P
  Ensure-OcProjectAccess -ParamsMap $P
  $registryToken = (& oc whoami -t)
  if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($registryToken)) {
    throw "No se pudo obtener el token de OpenShift para el registry"
  }
  Write-Log INFO "Autenticando en registry $($P.REGISTRY)..."
  & docker login $P.REGISTRY -u kubeadmin -p $registryToken | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "docker login al registry fallo" }

  $baseRaw = if ($P.ContainsKey("BASE_IMAGE") -and -not [string]::IsNullOrWhiteSpace($P.BASE_IMAGE)) { $P.BASE_IMAGE } else { Write-Log WARN "BASE_IMAGE no definido en parametros.conf. Usando default corporativo."; "/redeban-base-image/java21-runtime:1.0.0" }
  $baseImage = if ($baseRaw.StartsWith("/")) { "$($P.REGISTRY)$baseRaw" } else { $baseRaw }
  Write-Log INFO "Validando existencia de imagen base: $baseImage"

  $baseExists = (& docker images $baseImage -q)
  if (-not $baseExists) {
    Write-Log WARN "Imagen base no encontrada localmente. Descargando..."
    & docker pull $baseImage
    if ($LASTEXITCODE -ne 0) { throw "No se pudo descargar la imagen base $baseImage" }
  } else {
    Write-Log INFO "Imagen base presente localmente."
  }

  $env:DOCKER_BUILDKIT = "1"
  $buildArgs = @(
    'buildx','build','--load',
    '-t',$imageTag,
    '--build-arg',"BASE_IMAGE=$baseImage",
    '.'
  )

  $locationPushed = $false
  try {
    Push-Location -Path $sourceRoot
    $locationPushed = $true

    Write-Log INFO "Ejecutando: docker $($buildArgs -join ' ')"
    & docker @buildArgs
    if ($LASTEXITCODE -ne 0) {
      throw "Construccion de la imagen fallo"
    }

    Write-Log INFO "Publicando imagen: $imageTag"
    & docker push $imageTag
    if ($LASTEXITCODE -ne 0) { throw "docker push fallo" }
  }
  finally {
    if ($locationPushed) { Pop-Location }
  }

  Write-Log INFO "Build & Push OK -> $imageTag"
  exit 0
}
catch {
  Write-Log ERROR $_
  exit 1
}
