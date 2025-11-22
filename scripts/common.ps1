param()

Set-StrictMode -Version Latest

$script:ToolRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$script:ResolvedSourceRoot = $null
$script:OcLoggedIn = $false

function Write-Log {
  param([string]$Level, [string]$Message)
  $ts = Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"
  switch ($Level) {
    "INFO"    { $color = "Cyan" }
    "DEBUG"   { $color = "Gray" }
    "WARN"    { $color = "Yellow" }
    "ERROR"   { $color = "Red" }
    default   { $color = "White" }
  }
  Write-Host "[$ts][$Level] $Message" -ForegroundColor $color
}

function Add-GitCredentialsToUrl {
  param(
    [string]$Url,
    [string]$User,
    [string]$Password
  )

  if ([string]::IsNullOrWhiteSpace($Url)) { return $Url }
  $cleanUrl = $Url.TrimEnd("/")
  if ([string]::IsNullOrWhiteSpace($User)) { return $cleanUrl }
  if (-not [Uri]::IsWellFormedUriString($cleanUrl, [UriKind]::Absolute)) { return $cleanUrl }

  $builder = [UriBuilder]$cleanUrl
  if ($builder.Scheme -ne "http" -and $builder.Scheme -ne "https") { return $cleanUrl }

  $builder.UserName = $User
  $builder.Password = if ($Password) { $Password } else { "" }
  return $builder.Uri.AbsoluteUri.TrimEnd("/")
}

function Load-Params {
  param([string]$File = (Join-Path $PSScriptRoot "parametros.conf"))
  if (!(Test-Path $File)) { throw "No existe $File" }
  $map = @{}
  Get-Content $File | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $kv = $line -split "=", 2
    if ($kv.Length -eq 2) {
      $map[$kv[0]] = $kv[1]
    }
  }
  return $map
}

function Get-SourceRoot {
  param([hashtable]$ParamsMap)

  if ($script:ResolvedSourceRoot) { return $script:ResolvedSourceRoot }

  $mode = if ($ParamsMap.ContainsKey("GIT_SERVER_URL")) { $ParamsMap.GIT_SERVER_URL } else { "LOCAL" }
  if ([string]::IsNullOrWhiteSpace($mode) -or $mode.ToUpperInvariant() -eq "LOCAL") {
    $script:ResolvedSourceRoot = $script:ToolRoot
    Write-Log INFO "GIT_SERVER_URL=LOCAL -> se usaran las fuentes locales: $script:ToolRoot"
    return $script:ResolvedSourceRoot
  }

  if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "git no esta disponible en el PATH y es requerido para clonar el repositorio."
  }

  if (-not $ParamsMap.ContainsKey("GIT_PROJECT") -or [string]::IsNullOrWhiteSpace($ParamsMap.GIT_PROJECT)) {
    throw "GIT_PROJECT es obligatorio cuando se usa un repositorio remoto."
  }
  if (-not $ParamsMap.ContainsKey("GIT_BRANCH") -or [string]::IsNullOrWhiteSpace($ParamsMap.GIT_BRANCH)) {
    throw "GIT_BRANCH es obligatorio cuando se usa un repositorio remoto."
  }

  $server = $mode.TrimEnd("/")
  if (-not [Uri]::IsWellFormedUriString($server, [UriKind]::Absolute)) {
    throw "GIT_SERVER_URL no tiene un formato valido: $server"
  }

  $gitUser = if ($ParamsMap.ContainsKey("GIT_USERNAME")) { $ParamsMap.GIT_USERNAME } else { "" }
  $gitPass = if ($ParamsMap.ContainsKey("GIT_PASSWORD")) { $ParamsMap.GIT_PASSWORD } else { "" }
  $serverWithCreds = Add-GitCredentialsToUrl -Url $server -User $gitUser -Password $gitPass
  if ([string]::IsNullOrWhiteSpace($serverWithCreds)) {
    $serverWithCreds = $server
  }

  $project = $ParamsMap.GIT_PROJECT.TrimStart("/")
  $repoUrl = if ([Uri]::IsWellFormedUriString($project, [UriKind]::Absolute)) {
    Add-GitCredentialsToUrl -Url $project -User $gitUser -Password $gitPass
  } else {
    "$serverWithCreds/$project"
  }
  if (-not $repoUrl.EndsWith(".git")) {
    $repoUrl = "$repoUrl.git"
  }

  $dest = Join-Path $script:ToolRoot "target/git-src"
  if (Test-Path $dest) {
    Write-Log DEBUG "Limpiando clonado previo: $dest"
    Remove-Item $dest -Recurse -Force
  }
  $destParent = Split-Path $dest -Parent
  if (-not (Test-Path $destParent)) {
    New-Item -ItemType Directory -Path $destParent -Force | Out-Null
  }

  $branch = $ParamsMap.GIT_BRANCH
  Write-Log INFO "Clonando $repoUrl (branch $branch) ..."
  & git clone --depth 1 --branch $branch --single-branch $repoUrl $dest
  if ($LASTEXITCODE -ne 0) {
    throw "git clone fallo (repo: $repoUrl, branch: $branch)"
  }

  $script:ResolvedSourceRoot = (Resolve-Path $dest).Path
  Write-Log INFO "Fuentes disponibles en $script:ResolvedSourceRoot"
  return $script:ResolvedSourceRoot
}

function Ensure-OcLogin {
  param([hashtable]$ParamsMap)

  if ($script:OcLoggedIn) { return }

  if (-not $ParamsMap.ContainsKey("OCP_API_URL") -or [string]::IsNullOrWhiteSpace($ParamsMap.OCP_API_URL)) {
    throw "OCP_API_URL es obligatorio para operar con OpenShift."
  }
  $server = $ParamsMap.OCP_API_URL
  if (-not [Uri]::IsWellFormedUriString($server, [UriKind]::Absolute)) {
    throw "OCP_API_URL no es una URL valida: $server"
  }

  $token = if ($ParamsMap.ContainsKey("OCP_TOKEN")) { $ParamsMap.OCP_TOKEN } else { "" }
  $user  = if ($ParamsMap.ContainsKey("OCP_USER")) { $ParamsMap.OCP_USER } else { "" }
  $pass  = if ($ParamsMap.ContainsKey("OCP_PASSWORD")) { $ParamsMap.OCP_PASSWORD } else { "" }

  if (-not [string]::IsNullOrWhiteSpace($token)) {
    Write-Log INFO "Autenticando en OpenShift mediante token..."
    & oc login $server --token=$token --insecure-skip-tls-verify
  }
  elseif (-not [string]::IsNullOrWhiteSpace($user) -and -not [string]::IsNullOrWhiteSpace($pass)) {
    Write-Log INFO "Autenticando en OpenShift con usuario/password..."
    & oc login $server -u $user -p $pass --insecure-skip-tls-verify
  }
  else {
    throw "Se requiere OCP_TOKEN o el par OCP_USER/OCP_PASSWORD para iniciar sesion en OpenShift."
  }

  if ($LASTEXITCODE -ne 0) {
    throw "oc login fallo contra $server"
  }

  $script:OcLoggedIn = $true
  Write-Log INFO "Sesion OCP activa contra $server"
}

function Ensure-OcProjectAccess {
  param([hashtable]$ParamsMap)

  if (-not $ParamsMap.ContainsKey("NAMESPACE") -or [string]::IsNullOrWhiteSpace($ParamsMap.NAMESPACE)) {
    throw "NAMESPACE es obligatorio para validar acceso en OpenShift."
  }

  $namespace = $ParamsMap.NAMESPACE
  Write-Log INFO "Validando acceso al proyecto '$namespace'..."
  & oc get project $namespace > $null 2>&1
  if ($LASTEXITCODE -ne 0) {
    throw "No existe el proyecto '$namespace' o no tienes permisos para accederlo."
  }
  Write-Log INFO "Acceso confirmado para proyecto '$namespace'."
}

function Log-ParamsOverview {
  param([hashtable]$ParamsMap)

  $keys = @(
    "ENVIRONMENT","NAMESPACE","VERSION","APP_NAME",
    "REGISTRY","REGISTRY_PULL","ROUTE_HOST","BASE_IMAGE",
    "GIT_SERVER_URL","GIT_PROJECT","GIT_BRANCH"
  )
  $summary = @()
  foreach ($k in $keys) {
    if ($ParamsMap.ContainsKey($k)) {
      $value = if ([string]::IsNullOrWhiteSpace($ParamsMap[$k])) { "<vacio>" } else { $ParamsMap[$k] }
      $summary += "$k=$value"
    }
  }
  if ($summary.Count -gt 0) {
    Write-Log INFO ("Parametros: " + ($summary -join ", "))
  }
}

function Render-Manifests {
  param(
    [hashtable]$ParamsMap,
    [string]$SourceRoot
  )

  if ([string]::IsNullOrWhiteSpace($SourceRoot)) {
    $SourceRoot = $script:ToolRoot
  }

  $envDir = Get-EnvironmentManifestPath -ParamsMap $ParamsMap -SourceRoot $SourceRoot
  Write-Log INFO "Renderizando manifiestos desde $envDir"
  $out = Join-Path $SourceRoot "target/ocp"
  if (Test-Path $out) { Remove-Item $out -Recurse -Force }
  New-Item -ItemType Directory -Path $out -Force | Out-Null
  Write-Log INFO "Directorio temporal para manifiestos: $out"

  $registryPush = $ParamsMap.REGISTRY
  $registryPull = if ($ParamsMap.ContainsKey("REGISTRY_PULL") -and -not [string]::IsNullOrWhiteSpace($ParamsMap.REGISTRY_PULL)) {
    $ParamsMap.REGISTRY_PULL
  } else {
    $registryPush
  }

  Get-ChildItem $envDir -File | Where-Object { -not $_.PSIsContainer } | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $content = $content.Replace("__NAMESPACE__", $ParamsMap.NAMESPACE)
    $content = $content.Replace("__VERSION__", $ParamsMap.VERSION)
    $content = $content.Replace("__REGISTRY__", $registryPush)
    $content = $content.Replace("__REGISTRY_PULL__", $registryPull)
    $content = $content.Replace("__ROUTE_HOST__", $ParamsMap.ROUTE_HOST)
    $content = $content.Replace("__APP_NAME__", $ParamsMap.APP_NAME)
    $content = $content.Replace("__APP_CONTEXT__", $ParamsMap.APP_CONTEXT)
    $dest = Join-Path $out $_.Name
    Set-Content -Path $dest -Value $content -Encoding UTF8
  }
  Write-Log INFO "Manifiestos renderizados en $out"
  return $out
}

function Get-EnvironmentManifestPath {
  param(
    [hashtable]$ParamsMap,
    [string]$SourceRoot
  )

  if (-not $ParamsMap.ContainsKey("ENVIRONMENT") -or [string]::IsNullOrWhiteSpace($ParamsMap.ENVIRONMENT)) {
    throw "ENVIRONMENT no definido en parametros.conf"
  }

  if ([string]::IsNullOrWhiteSpace($SourceRoot)) {
    $SourceRoot = $script:ToolRoot
  }

  $envName = $ParamsMap.ENVIRONMENT.ToLowerInvariant()
  $validEnvs = @("desa","certi","prod")
  if ($validEnvs -notcontains $envName) {
    throw "ENVIRONMENT debe ser uno de: $($validEnvs -join ', ')"
  }

  $deployDir = Join-Path $SourceRoot "deploy"
  if (-not (Test-Path $deployDir)) {
    throw "No existe la carpeta deploy en $SourceRoot"
  }

  $envDir = Join-Path $deployDir $envName
  if (-not (Test-Path $envDir)) {
    throw "No existe la carpeta deploy/$envName en $SourceRoot"
  }

  return $envDir
}


function Validate-Params {
  param([hashtable]$ParamsMap)
  $errors = @()

  if (-not $ParamsMap.ContainsKey("NAMESPACE") -or [string]::IsNullOrWhiteSpace($ParamsMap.NAMESPACE)) {
    $errors += "NAMESPACE no definido en parametros.conf"
  }
  if (-not $ParamsMap.ContainsKey("REGISTRY") -or [string]::IsNullOrWhiteSpace($ParamsMap.REGISTRY)) {
    $errors += "REGISTRY no definido en parametros.conf"
  }
  if (-not $ParamsMap.ContainsKey("VERSION") -or [string]::IsNullOrWhiteSpace($ParamsMap.VERSION)) {
    $errors += "VERSION no definido en parametros.conf"
  }

  if (-not $ParamsMap.ContainsKey("APP_NAME") -or [string]::IsNullOrWhiteSpace($ParamsMap.APP_NAME)) {
    $errors += "APP_NAME no definido en parametros.conf"
  }
  if (-not $ParamsMap.ContainsKey("APP_CONTEXT") -or [string]::IsNullOrWhiteSpace($ParamsMap.APP_CONTEXT)) {
    $errors += "APP_CONTEXT no definido en parametros.conf"
  } elseif (-not $ParamsMap.APP_CONTEXT.StartsWith("/")) {
    $errors += "APP_CONTEXT debe iniciar con '/'. Valor actual: '$($ParamsMap.APP_CONTEXT)'"
  }

  if (-not $ParamsMap.ContainsKey("GIT_SERVER_URL") -or [string]::IsNullOrWhiteSpace($ParamsMap.GIT_SERVER_URL)) {
    $errors += "GIT_SERVER_URL no definido en parametros.conf"
  } elseif ($ParamsMap.GIT_SERVER_URL.ToUpperInvariant() -ne "LOCAL" -and -not [Uri]::IsWellFormedUriString($ParamsMap.GIT_SERVER_URL, [UriKind]::Absolute)) {
    $errors += "GIT_SERVER_URL debe ser 'LOCAL' o una URL absoluta. Valor actual: '$($ParamsMap.GIT_SERVER_URL)'"
  }

  if ($ParamsMap.ContainsKey("GIT_SERVER_URL") -and $ParamsMap.GIT_SERVER_URL.ToUpperInvariant() -ne "LOCAL") {
    if (-not $ParamsMap.ContainsKey("GIT_PROJECT") -or [string]::IsNullOrWhiteSpace($ParamsMap.GIT_PROJECT)) {
      $errors += "GIT_PROJECT es obligatorio cuando se usa un servidor Git remoto"
    }
    if (-not $ParamsMap.ContainsKey("GIT_BRANCH") -or [string]::IsNullOrWhiteSpace($ParamsMap.GIT_BRANCH)) {
      $errors += "GIT_BRANCH es obligatorio cuando se usa un servidor Git remoto"
    }
    $serverUri = if ([Uri]::IsWellFormedUriString($ParamsMap.GIT_SERVER_URL, [UriKind]::Absolute)) { [Uri]$ParamsMap.GIT_SERVER_URL } else { $null }
    if ($serverUri -and ($serverUri.Scheme -eq "http" -or $serverUri.Scheme -eq "https")) {
      if (-not $ParamsMap.ContainsKey("GIT_USERNAME") -or [string]::IsNullOrWhiteSpace($ParamsMap.GIT_USERNAME)) {
        $errors += "GIT_USERNAME es obligatorio cuando GIT_SERVER_URL usa http/https"
      }
      if (-not $ParamsMap.ContainsKey("GIT_PASSWORD") -or [string]::IsNullOrWhiteSpace($ParamsMap.GIT_PASSWORD)) {
        $errors += "GIT_PASSWORD es obligatorio cuando GIT_SERVER_URL usa http/https"
      }
    }
  }

  if (-not $ParamsMap.ContainsKey("ENVIRONMENT") -or [string]::IsNullOrWhiteSpace($ParamsMap.ENVIRONMENT)) {
    $errors += "ENVIRONMENT no definido en parametros.conf"
  }
  else {
    $validEnvs = @("desa","certi","prod")
    if ($validEnvs -notcontains $ParamsMap.ENVIRONMENT.ToLowerInvariant()) {
      $errors += "ENVIRONMENT debe ser uno de: $($validEnvs -join ', '). Valor actual: '$($ParamsMap.ENVIRONMENT)'"
    }
  }

  if ($errors.Count -gt 0) {
    foreach ($e in $errors) { Write-Log ERROR $e }
    throw "Parametros invalidos. Corrige parametros.conf y vuelve a intentar."
  }
}
