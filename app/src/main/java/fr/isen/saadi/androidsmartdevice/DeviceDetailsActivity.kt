package fr.isen.saadi.androidsmartdevice

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.UUID

class DeviceDetailsActivity : ComponentActivity() {
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothDevice: BluetoothDevice? = null
    var stock1: UUID? = null // UUID du bouton 1
    var stock2: UUID? = null // UUID du bouton 3

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val device = intent.getParcelableExtra<BluetoothDevice>("deviceName")

        setContent {
            val context = LocalContext.current

            // États pour les LEDs et la connexion
            var led1State by remember { mutableStateOf(false) }
            var led2State by remember { mutableStateOf(false) }
            var led3State by remember { mutableStateOf(false) }
            var isConnected by remember { mutableStateOf(false) }

            // États pour les compteurs des boutons physiques
            var button1Count by remember { mutableStateOf("") }
            var button3Count by remember { mutableStateOf("") }

            // État pour la gestion des notifications
            var notificationsEnabledButton1 by remember { mutableStateOf(false) }
            var notificationsEnabledButton3 by remember { mutableStateOf(false) }




            LaunchedEffect(device) {
                device?.let { bluetoothDevice = it }
            }

            DeviceDetailsScreen(
                deviceName = device?.name ?: "Appareil inconnu",
                led1State = led1State,
                led2State = led2State,
                led3State = led3State,
                button1Count = button1Count,
                button3Count = button3Count,
                onLedToggle = { ledId ->
                    when (ledId) {
                        1 -> led1State = !led1State
                        2 -> led2State = !led2State
                        3 -> led3State = !led3State
                    }
                    sendLedCommand(ledId, listOf(led1State, led2State, led3State)[ledId - 1])
                },
                onConnectClick = {
                    bluetoothDevice?.let { device ->
                        if (!isConnected) {
                            connectToDevice(device, context, { value, isButton1 ->
                                if (isButton1) {
                                    button1Count = value
                                } else {
                                    button3Count = value
                                }
                            })
                            isConnected = true
                        }
                    }
                },
                onDisconnectClick = {
                    bluetoothGatt?.close()
                    isConnected = false
                    Toast.makeText(context, "Déconnecté de l'appareil", Toast.LENGTH_SHORT).show()
                },
                isConnected = isConnected,
                notificationsEnabledButton1 = notificationsEnabledButton1,
                notificationsEnabledButton3 = notificationsEnabledButton3,
                onNotificationsToggleButton1 = { notificationsEnabledButton1 = !notificationsEnabledButton1 },
                onNotificationsToggleButton3 = { notificationsEnabledButton3 = !notificationsEnabledButton3 }
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(
        device: BluetoothDevice,
        context: Context,
        updateCounts: (String, Boolean) -> Unit
    ) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Le Bluetooth est désactivé", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothGatt = device.connectGatt(this, true, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                runOnUiThread {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Toast.makeText(context, "Connecté à ${device.name}", Toast.LENGTH_SHORT).show()
                        gatt?.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Toast.makeText(context, "Déconnecté", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Service 3, Caractéristique 2 (Bouton principal)
                    val service3 = gatt?.services?.getOrNull(2)
                    service3?.characteristics?.getOrNull(1)?.let { characteristic ->
                        // Stocker l'UUID du bouton principal dans stock1
                        stock1 = characteristic.uuid

                        gatt.setCharacteristicNotification(characteristic, true)
                        characteristic.descriptors?.getOrNull(0)?.let { descriptor ->
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }
                    }

                    // Service 2, Caractéristique 1 (Troisième bouton)
                    val service2 = gatt?.services?.getOrNull(1)
                    service2?.characteristics?.getOrNull(0)?.let { characteristic ->
                        // Stocker l'UUID du troisième bouton dans stock2
                        stock2 = characteristic.uuid

                        gatt.setCharacteristicNotification(characteristic, true)
                        characteristic.descriptors?.getOrNull(0)?.let { descriptor ->
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                }
            }



            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                super.onCharacteristicChanged(gatt, characteristic, value)
                charact(characteristic, value, updateCounts)
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                super.onCharacteristicChanged(gatt, characteristic)
                charact(characteristic, characteristic?.value, updateCounts)
            }

        })
    }

    private fun charact(
        characteristic: BluetoothGattCharacteristic?,
        value: ByteArray?,
        updateCounts: (String, Boolean) -> Unit
    ) {
        if (characteristic != null && value != null) {
            updateCounts(value[0].toInt().toString(), characteristic.uuid == stock2)

        }

    }



@SuppressLint("MissingPermission")
    private fun sendLedCommand(ledId: Int, state: Boolean) {
        bluetoothGatt?.let { gatt ->
            val service = gatt.services.getOrNull(2)
            service?.characteristics?.getOrNull(0)?.let {
                val ledCommand = if (state) ledId.toByte() else 0x00
                it.value = byteArrayOf(ledCommand)
                gatt.writeCharacteristic(it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close()
    }
}

@Composable
fun DeviceDetailsScreen(
    deviceName: String,
    led1State: Boolean,
    led2State: Boolean,
    led3State: Boolean,
    button1Count: String,
    button3Count: String,
    onLedToggle: (Int) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    isConnected: Boolean,
    notificationsEnabledButton1: Boolean,
    notificationsEnabledButton3: Boolean,
    onNotificationsToggleButton1: () -> Unit,
    onNotificationsToggleButton3: () -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Détails de l'appareil", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Nom de l'appareil : $deviceName", style = MaterialTheme.typography.bodyLarge)

            Button(
                onClick = {
                    if (isConnected) {
                        onDisconnectClick()
                    } else {
                        onConnectClick()
                    }
                },
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Text(text = if (isConnected) "Se déconnecter" else "Se connecter à l'appareil")
            }

            Spacer(modifier = Modifier.height(24.dp))

            LedButton(ledId = 1, isOn = led1State, onLedToggle = onLedToggle)
            LedButton(ledId = 2, isOn = led2State, onLedToggle = onLedToggle)
            LedButton(ledId = 3, isOn = led3State, onLedToggle = onLedToggle)

            Spacer(modifier = Modifier.height(24.dp))

            // Afficher les compteurs uniquement si les notifications sont activées pour chaque bouton
            if (notificationsEnabledButton1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(text = "Bouton Principal : $button1Count clics")
                    Button(
                        onClick = { onNotificationsToggleButton1() },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text("Désactiver Notification Bouton 1")
                    }
                }
            }

            if (notificationsEnabledButton3) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(text = "Troisième Bouton : $button3Count clics")
                    Button(
                        onClick = { onNotificationsToggleButton3() },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text("Désactiver Notification Bouton 3")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Ajout du switch pour activer/désactiver les notifications pour chaque bouton
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Notifications Bouton 1")
                Switch(checked = notificationsEnabledButton1, onCheckedChange = { onNotificationsToggleButton1() })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Notifications Bouton 3")
                Switch(checked = notificationsEnabledButton3, onCheckedChange = { onNotificationsToggleButton3() })
            }
        }
    }
}

@Composable
fun LedButton(ledId: Int, isOn: Boolean, onLedToggle: (Int) -> Unit) {
    Button(
        onClick = { onLedToggle(ledId) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOn) Color.Green else Color.Red
        ),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Text(text = "LED $ledId - ${if (isOn) "ON" else "OFF"}")
    }
}
