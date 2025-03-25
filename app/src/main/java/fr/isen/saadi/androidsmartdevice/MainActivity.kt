package fr.isen.saadi.androidsmartdevice

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.isen.saadi.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AndroidSmartDeviceTheme {
                // Définition du contenu
                MainScreen(
                    onBluetoothClick = {
                        // Lancement de l'activité ScanActivity lorsque le bouton est cliqué
                        val intent = Intent(this, ScanActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(onBluetoothClick: () -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Image du logo
            Image(
                painter = painterResource(id = R.drawable.bluetooth_icon), // Assurez-vous que le fichier bluetooth_icon.xml existe
                contentDescription = "Bluetooth Icon",
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "SmartDevice", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Cette application sert à gérer le Bluetooth")
            Spacer(modifier = Modifier.height(16.dp))
            // Bouton cliquable qui lance l'activité ScanActivity
            Button(onClick = onBluetoothClick) {
                Text("Ouvrir Scanner")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    AndroidSmartDeviceTheme {
        MainScreen(onBluetoothClick = {})
    }
}
