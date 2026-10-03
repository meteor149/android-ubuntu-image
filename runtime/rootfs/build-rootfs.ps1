$ErrorActionPreference = 'Stop'
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$UbuntuImage = (Get-Content (Join-Path $ProjectRoot 'runtime/versions.env') | Where-Object { $_ -like 'UBUNTU_IMAGE=*' }).Substring(13)
$Dist = Join-Path $ProjectRoot 'runtime/dist'
New-Item -ItemType Directory -Force -Path $Dist | Out-Null
docker buildx build --platform linux/arm64 --file (Join-Path $PSScriptRoot 'Containerfile') --build-arg "UBUNTU_IMAGE=$UbuntuImage" --target artifact --output "type=local,dest=$Dist" $PSScriptRoot
if ($LASTEXITCODE -ne 0) { throw 'Ubuntu image build failed' }
