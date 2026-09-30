package com.example.espflasher

import android.app.*
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.util.concurrent.atomic.AtomicBoolean
import android.util.Base64

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var usbManager: UsbManager
    private var driver: UsbSerialDriver? = null
    private var port: UsbSerialPort? = null
    private var connection: android.hardware.usb.UsbDeviceConnection? = null
    private var selected: UsbDevice? = null
    private var permissionPending = false
    private val ACTION_USB_PERMISSION = "com.example.espflasher.USB_PERMISSION"

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            permissionPending = false
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            if (device != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                openDevice(device)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usbManager = getSystemService(USB_SERVICE) as UsbManager
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(permissionReceiver, IntentFilter(ACTION_USB_PERMISSION), RECEIVER_NOT_EXPORTED) else @Suppress("DEPRECATION") run { registerReceiver(permissionReceiver, IntentFilter(ACTION_USB_PERMISSION)) }

        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = true
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(UsbBridge(), "AndroidUSB")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    override fun onDestroy() {
        try { unregisterReceiver(permissionReceiver) } catch (_: Exception) {}
        closePort()
        web.destroy()
        super.onDestroy()
    }

    private fun supportedDrivers(): List<UsbSerialDriver> =
        UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

    private fun requestFirstDevice(): String {
        val drivers = supportedDrivers()
        if (drivers.isEmpty()) return "NO_DEVICE"
        val d = drivers.first()
        selected = d.device
        driver = d
        if (!usbManager.hasPermission(d.device)) {
            permissionPending = true
            val pi = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION).setPackage(packageName), PendingIntent.FLAG_IMMUTABLE)
            usbManager.requestPermission(d.device, pi)
            return "PENDING"
        }
        openDevice(d.device)
        return "OPEN"
    }

    private fun openDevice(device: UsbDevice) {
        try {
            val d = driver ?: UsbSerialProber.getDefaultProber().probeDevice(device) ?: return
            driver = d
            connection?.close()
            connection = usbManager.openDevice(device)
            port = d.ports.firstOrNull() ?: return
            port!!.open(connection)
            // Important for ESP boards: release the bootloader UART from reset/flow-control state.
            try { port!!.dtr = false } catch (_: Exception) {}
            try { port!!.rts = false } catch (_: Exception) {}
            port!!.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            selected = device
        } catch (_: Exception) {
            closePort()
        }
    }

    private fun closePort() {
        try { port?.close() } catch (_: Exception) {}
        port = null
        try { connection?.close() } catch (_: Exception) {}
        connection = null
        driver = null
        selected = null
    }

    inner class UsbBridge {
        @JavascriptInterface fun requestPort(): String = requestFirstDevice()
        @JavascriptInterface fun portReady(): Boolean = port != null
        @JavascriptInterface fun close(): Boolean { closePort(); return true }
        @JavascriptInterface fun getInfo(): String {
            val d = selected ?: return "{}"
            return "{\"usbVendorId\":${d.vendorId},\"usbProductId\":${d.productId}}"
        }
        @JavascriptInterface fun open(baud: Int): Boolean {
            return try {
                port?.setParameters(baud, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                true
            } catch (_: Exception) { false }
        }
        @JavascriptInterface fun write(data: String): Int {
            val p = port ?: return -1
            return try {
                val bytes = Base64.decode(data, Base64.NO_WRAP)
                p.write(bytes, 5000)
                bytes.size
            } catch (_: Exception) { -1 }
        }
        @JavascriptInterface fun read(timeout: Int): String {
            val p = port ?: return ""
            return try {
                val buf = ByteArray(4096)
                val n = p.read(buf, timeout.coerceIn(1, 5000))
                if (n <= 0) "" else Base64.encodeToString(buf.copyOf(n), Base64.NO_WRAP)
            } catch (_: Exception) { "" }
        }
        @JavascriptInterface fun setSignals(dtr: Boolean, rts: Boolean): Boolean {
            val p = port ?: return false
            return try {
                p.dtr = dtr
                p.rts = rts
                true
            } catch (_: Exception) { false }
        }
        @JavascriptInterface fun listDevices(): String {
            val sb = StringBuilder("[")
            supportedDrivers().forEachIndexed { i, d ->
                if (i > 0) sb.append(',')
                val dev = d.device
                sb.append("{\"vid\":${dev.vendorId},\"pid\":${dev.productId},\"name\":\"")
                sb.append(dev.productName?.replace("\\", "\\\\")?.replace("\"", "\\\"") ?: "USB Serial")
                sb.append("\"}")
            }
            sb.append(']')
            return sb.toString()
        }
    }
}
