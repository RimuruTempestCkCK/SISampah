package com.example.sisampah.ui.screens.petugas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetugasDashboard(onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SI-Sampah Petugas") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Schedule, null) },
                    label = { Text("Jadwal") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ListAlt, null) },
                    label = { Text("Laporan") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Payment, null) },
                    label = { Text("Bayar") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> PetugasJadwalTab()
                1 -> PetugasLaporanTab()
                2 -> PetugasPembayaranTab()
            }
        }
    }
}

@Composable
fun PetugasJadwalTab() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Jadwal Pengangkutan Hari Ini", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(3) { index ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                ListItem(
                    headlineContent = { Text("Area $index") },
                    supportingContent = { Text("Jam: 09:00 - 11:00 WIB") },
                    trailingContent = {
                        Button(onClick = {}) { Text("Update") }
                    }
                )
            }
        }
    }
}

@Composable
fun PetugasLaporanTab() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Laporan Masyarakat", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(5) { index ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                ListItem(
                    headlineContent = { Text("Laporan Sampah Menumpuk") },
                    supportingContent = { Text("Lokasi: Blok C$index") },
                    trailingContent = {
                        IconButton(onClick = {}) { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }
                    }
                )
            }
        }
    }
}

@Composable
fun PetugasPembayaranTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Input Pembayaran Iuran", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("Nama Warga / ID") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("Jumlah Pembayaran") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text("Simpan Pembayaran")
        }
    }
}
