package fr.isen.saadi.androidsmartdevice

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
import fr.isen.saadi.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme

class ScanActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private var isBluetoothEnabled by mutableStateOf(false)
    private var isScanning by mutableStateOf(false)

    // Liste des appareils BLE détectés
    private val devices = mutableStateListOf<BluetoothDevice>()

    // Gestion des permissions
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.BLUETOOTH_SCAN] == true &&
                permissions[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                startScan()  // Démarrer le scan si toutes les permissions sont accordées
            } else {
                Toast.makeText(this, "Permissions nécessaires non accordées", Toast.LENGTH_SHORT).show()
            }
        }

    // Callback pour gérer les résultats du scan BLE
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                // Ajouter l'appareil à la liste uniquement s'il n'est pas déjà présent et qu'il a un nom
                addDeviceIfNotExist(devices, device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(this@ScanActivity, "Scan échoué avec le code $errorCode", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidSmartDeviceTheme {
                ScanScreen(
                    isScanning = isScanning,
                    devices = devices,
                    onScanClick = { checkPermissionsAndStartScan() },
                    onStopScanClick = { stopScan() },
                    onBluetoothStateChange = { state -> isBluetoothEnabled = state }
                )
            }
        }

        // Initialiser Bluetooth
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        checkBluetoothState()
    }

    private fun checkBluetoothState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non disponible sur cet appareil.", Toast.LENGTH_SHORT).show()
        } else {
            isBluetoothEnabled = bluetoothAdapter.isEnabled
            if (!isBluetoothEnabled) {
                Toast.makeText(this, "Bluetooth n'est pas activé.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionsAndStartScan() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+) → Besoin de BLUETOOTH_SCAN, BLUETOOTH_CONNECT et ACCESS_FINE_LOCATION
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.addAll(
                    listOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (API 29+) → Besoin de BLUETOOTH_SCAN et ACCESS_FINE_LOCATION
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            // Android < 10 → Seulement ACCESS_FINE_LOCATION requis
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissionsToRequest.isEmpty()) {
            // Toutes les permissions sont déjà accordées → Démarrer directement le scan
            startScan()
        } else {
            // Demander les permissions puis relancer le scan une fois accordées
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (bluetoothAdapter.isEnabled) {
            isScanning = true
            Toast.makeText(this, "Scan en cours...", Toast.LENGTH_SHORT).show()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
            } else {
                Toast.makeText(this, "Le Bluetooth Low Energy (BLE) n'est pas supporté sur cette version Android.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Activez le Bluetooth pour scanner.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        stopScan()
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        isScanning = false
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        Toast.makeText(this, "Scan arrêté.", Toast.LENGTH_SHORT).show()
    }

    // Fonction pour ajouter un appareil à la liste si l'appareil n'existe pas déjà
    private fun addDeviceIfNotExist(devices: MutableList<BluetoothDevice>, device: BluetoothDevice) {
        if (!devices.any { it.address == device.address } && !device.name.isNullOrEmpty()) {
            devices.add(device)
        }
    }
}

@Composable
fun ScanScreen(
    isScanning: Boolean,
    devices: List<BluetoothDevice>,
    onScanClick: () -> Unit,
    onStopScanClick: () -> Unit,
    onBluetoothStateChange: (Boolean) -> Unit
) {
    // Context de l'activité
    val context = LocalContext.current

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Scan BLE", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onScanClick, modifier = Modifier.fillMaxWidth()) {
                Text("Scanner")
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isScanning) {
                Button(onClick = onStopScanClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Arrêter")
                }
                CircularProgressIndicator()
            }
            devicesList(devices, context)
        }
    }
}

@Composable
fun devicesList(devices: List<BluetoothDevice>, context: Context) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (devices.isEmpty()) {
            Text("Aucun appareil trouvé")
        } else {
            devices.forEach { device ->
                DeviceButton(device, context)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceButton(device: BluetoothDevice, context: Context) {
    Button(
        onClick = {
            val intent = Intent(context, DeviceDetailsActivity::class.java)
            intent.putExtra("deviceName", device)
            context.startActivity(intent)
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
    ) {
        Text(text = device.name)
    }
}
