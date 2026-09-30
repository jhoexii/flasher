# ESP8266 / ESP32 Flasher for Android

Android USB-OTG version of the uploaded **ESP8266 / ESP32 Flasher** page.

## Features

- ESP8266 / ESP32 bootloader connection
- Android USB Host / OTG
- CH340 / CH341
- CP210x
- FTDI
- ESP32 USB-Serial/JTAG devices supported by the serial library
- `.bin` firmware picker
- Flash address
- Baud rate
- Flash mode / frequency / size
- Erase-all option
- Full flash erase
- Progress and live console
- Automatic reset through esptool-js

## Build on GitHub

1. Create a new empty GitHub repository.
2. Upload this entire project, preserving the folder structure.
3. Push to `main` (or `master`).
4. Open **Actions → Build APK**.
5. After the workflow finishes, open the workflow run and download **ESP-Flasher-debug** from Artifacts.

You can also use **Actions → Build APK → Run workflow** without pushing another commit.

## Build locally

### Linux / macOS

```bash
./scripts/bundle-esptool.sh
gradle :app:assembleDebug
```

### Windows PowerShell

```powershell
./scripts/bundle-esptool.ps1
gradle :app:assembleDebug
```

The APK will be at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Using the APK

1. Connect the ESP board to the Android phone using a USB-OTG adapter/cable.
2. Put the board in download/bootloader mode if automatic reset does not work: hold **BOOT/FLASH**, tap **RESET**, then release **BOOT/FLASH**.
3. Tap **CONNECT DEVICE**.
4. Grant Android USB permission.
5. Select the `.bin` file.
6. Set the flash address/options and tap **FLASH**.

For a single merged ESP32 image, use the appropriate address (commonly `0x00000`). For separate bootloader/partition/application binaries, the original single-file UI is not a multi-partition flasher; merge them first or adapt the UI for multiple images.

## Architecture

The HTML UI from the supplied project remains the front end. Android exposes a small Web Serial-compatible bridge named `AndroidUSB` backed by `usb-serial-for-android`. The esptool-js bundle runs locally inside the WebView.

This avoids depending on Android Chrome's Web Serial implementation.

## Notes

The build downloads `esptool-js@0.7.0` during the build and bundles it into the APK. This is the same esptool-js version used by the supplied HTML.
