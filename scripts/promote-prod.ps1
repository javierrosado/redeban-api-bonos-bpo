param(
  [ValidateSet("promote","deploy","undeploy","promoteAndDeploy")]
  [string]$Action = "promote",
  [string]$PromoteParamsFile = (Join-Path $PSScriptRoot "parametros-promote.conf"),
  [string]$DeployParamsFile = (Join-Path $PSScriptRoot "parametros-prod.conf")
)

. "$PSScriptRoot/common.ps1"

function Validate-PromoteParams {
  param(
    [hashtable]$ParamsMap,
    [string]$FileLabel
  )
  $errors = @()
  $required = @(
    "SOURCE_REGISTRY","SOURCE_NAMESPACE","SOURCE_IMAGE","SOURCE_TAG",
    "TARGET_REGISTRY","TARGET_NAMESPACE","TARGET_IMAGE","TARGET_TAG",
    "SOURCE_OCP_API_URL","TARGET_OCP_API_URL"
  )
  foreach ($key in $required) {
    if (-not $ParamsMap.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($ParamsMap[$key])) {
      $errors += "$key no definido en $FileLabel"
    }
  }

  foreach ($prefix in @("SOURCE","TARGET")) {
    $apiKey = "${prefix}_OCP_API_URL"
    if (-not [Uri]::IsWellFormedUriString($ParamsMap[$apiKey], [UriKind]::Absolute)) {
      $errors += "$apiKey no es una URL valida: $($ParamsMap[$apiKey])"
    }
    $tokenKey = "${prefix}_OCP_TOKEN"
    $userKey  = "${prefix}_OCP_USER"
    $passKey  = "${prefix}_OCP_PASSWORD"
    $token = if ($ParamsMap.ContainsKey($tokenKey)) { $ParamsMap[$tokenKey] } else { "" }
    $user  = if ($ParamsMap.ContainsKey($userKey)) { $ParamsMap[$userKey] } else { "" }
    $pass  = if ($ParamsMap.ContainsKey($passKey)) { $ParamsMap[$passKey] } else { "" }
    if ([string]::IsNullOrWhiteSpace($token) -and ([string]::IsNullOrWhiteSpace($user) -or [string]::IsNullOrWhiteSpace($pass))) {
      $errors += "Se requiere ${prefix}_OCP_TOKEN o el par ${prefix}_OCP_USER/${prefix}_OCP_PASSWORD"
    }
  }

  if ($errors.Count -gt 0) {
    foreach ($e in $errors) { Write-Log ERROR $e }
    throw "Parametros invalidos en $FileLabel"
  }
}

function Invoke-OcLoginForPrefix {
  param(
    [hashtable]$ParamsMap,
    [string]$Prefix
  )

  $api = $ParamsMap["${Prefix}_OCP_API_URL"]
  $token = if ($ParamsMap.ContainsKey("${Prefix}_OCP_TOKEN")) { $ParamsMap["${Prefix}_OCP_TOKEN"] } else { "" }
  $user  = if ($ParamsMap.ContainsKey("${Prefix}_OCP_USER")) { $ParamsMap["${Prefix}_OCP_USER"] } else { "" }
  $pass  = if ($ParamsMap.ContainsKey("${Prefix}_OCP_PASSWORD")) { $ParamsMap["${Prefix}_OCP_PASSWORD"] } else { "" }

  Write-Log INFO "Autenticando en cluster $Prefix ($api)..."
  if (-not [string]::IsNullOrWhiteSpace($token)) {
    & oc login $api --token=$token --insecure-skip-tls-verify
  }
  elseif (-not [string]::IsNullOrWhiteSpace($user) -and -not [string]::IsNullOrWhiteSpace($pass)) {
    & oc login $api -u $user -p $pass --insecure-skip-tls-verify
  }
  if ($LASTEXITCODE -ne 0) { throw "oc login fallo contra $api para $Prefix" }

  $whoami = (& oc whoami)
  if ($LASTEXITCODE -ne 0) { throw "No se pudo obtener usuario actual tras login $Prefix" }
  $clusterToken = (& oc whoami -t)
  if ($LASTEXITCODE -ne 0) { throw "No se pudo obtener token tras login $Prefix" }

  Write-Log INFO "Sesion $Prefix activa como $whoami"
  return @{
    Api = $api
    User = $whoami
    Token = $clusterToken
  }
}

function Login-Registry {
  param(
    [string]$Registry,
    [string]$User,
    [string]$Password,
    [string]$Label
  )
  Write-Log INFO "Autenticando en registry $Label ($Registry) con usuario $User..."
  & docker login $Registry -u $User -p $Password | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "docker login fallo para $Registry ($Label)" }
}

function Invoke-ImagePromotion {
  param([string]$ParamsFile)

  Write-Log INFO "Ejecutando promocion de imagen usando $ParamsFile"
  $promoteParams = Load-Params -File $ParamsFile
  Validate-PromoteParams -ParamsMap $promoteParams -FileLabel $ParamsFile

  $summary = @(
    "origen=$($promoteParams.SOURCE_REGISTRY)/$($promoteParams.SOURCE_NAMESPACE)/$($promoteParams.SOURCE_IMAGE):$($promoteParams.SOURCE_TAG)",
    "destino=$($promoteParams.TARGET_REGISTRY)/$($promoteParams.TARGET_NAMESPACE)/$($promoteParams.TARGET_IMAGE):$($promoteParams.TARGET_TAG)"
  )
  Write-Log INFO ("Promocion: " + ($summary -join " -> "))

  $sourceAuth = Invoke-OcLoginForPrefix -ParamsMap $promoteParams -Prefix "SOURCE"
  Login-Registry -Registry $promoteParams.SOURCE_REGISTRY -User $sourceAuth.User -Password $sourceAuth.Token -Label "origen"

  $targetAuth = Invoke-OcLoginForPrefix -ParamsMap $promoteParams -Prefix "TARGET"
  Login-Registry -Registry $promoteParams.TARGET_REGISTRY -User $targetAuth.User -Password $targetAuth.Token -Label "destino"

  Write-Log INFO "Validando acceso al namespace destino $($promoteParams.TARGET_NAMESPACE)..."
  & oc get project $promoteParams.TARGET_NAMESPACE > $null 2>&1
  if ($LASTEXITCODE -ne 0) { throw "No existe el proyecto destino $($promoteParams.TARGET_NAMESPACE) o no tienes permisos" }

  $sourceImage = "$($promoteParams.SOURCE_REGISTRY)/$($promoteParams.SOURCE_NAMESPACE)/$($promoteParams.SOURCE_IMAGE):$($promoteParams.SOURCE_TAG)"
  $targetImage = "$($promoteParams.TARGET_REGISTRY)/$($promoteParams.TARGET_NAMESPACE)/$($promoteParams.TARGET_IMAGE):$($promoteParams.TARGET_TAG)"

  Write-Log INFO "Descargando imagen origen: $sourceImage"
  & docker pull $sourceImage
  if ($LASTEXITCODE -ne 0) { throw "docker pull fallo para $sourceImage" }

  Write-Log INFO "Etiquetando imagen como $targetImage"
  & docker tag $sourceImage $targetImage
  if ($LASTEXITCODE -ne 0) { throw "docker tag fallo" }

  Write-Log INFO "Publicando imagen en destino..."
  & docker push $targetImage
  if ($LASTEXITCODE -ne 0) { throw "docker push fallo para $targetImage" }

  Write-Log INFO "Promocion completada. Imagen disponible en $targetImage"
}

function Invoke-DeployScript {
  param(
    [string]$ScriptName,
    [string]$ParamsFile
  )

  $path = Join-Path $PSScriptRoot $ScriptName
  if (-not (Test-Path $path)) {
    throw "No se encontro el script $ScriptName en $PSScriptRoot"
  }

  Write-Log INFO "Ejecutando $ScriptName con parametros $ParamsFile..."
  & $path -ParamsFile $ParamsFile
  if ($LASTEXITCODE -ne 0) { throw "$ScriptName fallo para $ParamsFile" }
}

function Invoke-ProdDeploy { param([string]$ParamsFile) Invoke-DeployScript -ScriptName "deploy.ps1" -ParamsFile $ParamsFile }
function Invoke-ProdUndeploy { param([string]$ParamsFile) Invoke-DeployScript -ScriptName "undeploy.ps1" -ParamsFile $ParamsFile }

try {
  Write-Log INFO "===== Iniciando promote-prod.ps1 (accion: $Action) ====="
  switch ($Action.ToLowerInvariant()) {
    "promote"            { Invoke-ImagePromotion -ParamsFile $PromoteParamsFile }
    "deploy"             { Invoke-ProdDeploy -ParamsFile $DeployParamsFile }
    "undeploy"           { Invoke-ProdUndeploy -ParamsFile $DeployParamsFile }
    "promoteanddeploy"   {
      Invoke-ImagePromotion -ParamsFile $PromoteParamsFile
      Invoke-ProdDeploy -ParamsFile $DeployParamsFile
    }
    default              { throw "Accion no soportada: $Action" }
  }
  Write-Log INFO "Accion '$Action' completada."
  exit 0
}
catch {
  Write-Log ERROR $_
  exit 1
}
