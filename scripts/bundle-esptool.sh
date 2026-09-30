#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
npm install --no-audit --no-fund
mkdir -p app/src/main/assets
npx esbuild node_modules/esptool-js/lib/index.js --bundle --format=iife --global-name=esptool --platform=browser --target=es2020 --outfile=app/src/main/assets/esptool.bundle.js
