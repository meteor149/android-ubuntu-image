#!/usr/bin/env bash
set -euo pipefail
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
project_root="$(cd -- "$script_dir/../.." && pwd)"
source "$project_root/runtime/versions.env"
mkdir -p "$project_root/runtime/dist"
docker buildx build --platform linux/arm64 --file "$script_dir/Containerfile" \
  --build-arg "UBUNTU_IMAGE=$UBUNTU_IMAGE" --target artifact \
  --output "type=local,dest=$project_root/runtime/dist" "$script_dir"
