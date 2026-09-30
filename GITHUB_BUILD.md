# GitHub APK build

## Fix for the npm lock-file error

The workflow intentionally does **not** use `actions/setup-node` npm caching. This project generates `package-lock.json` during the `npm install` step in `scripts/bundle-esptool.sh`, so GitHub Actions does not require a lock file before that step.

## Build

1. Upload the contents of this directory to a new GitHub repository.
2. Push to `main`/`master`, or open **Actions → Build APK → Run workflow**.
3. Download the `ESP-Flasher-debug` artifact from the completed workflow.

The workflow uses the current `setup-java@v5` and `setup-node@v5` actions.
