$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
npm install --no-audit --no-fund
New-Item -ItemType Directory -Force app/src/main/assets | Out-Null
npx esbuild node_modules/esptool-js/lib/index.js --bundle --format=iife --global-name=esptool --platform=browser --target=es2020 --outfile=app/src/main/assets/esptool.bundle.js
