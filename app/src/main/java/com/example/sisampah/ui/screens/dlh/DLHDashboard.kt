package com.example.sisampah.ui.screens.dlh

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
fun DLHDashboard(onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SI-Sampah DLH") },
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
                    icon = { Icon(Icons.Default.Analytics, null) },
                    label = { Text("Monitor") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.BarChart, null) },
                    label = { Text("Rekap") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text("Lokasi") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> PetugasMonitorTab()
                1 -> RekapLaporanTab()
                2 -> LokasiPengangkutanTab()
            }
        }
    }
}

@Composable
fun PetugasMonitorTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Monitoring Aktivitas Petugas", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(5) { index ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    ListItem(
                        headlineContent = { Text("Petugas: Agus Setiawan") },
                        supportingContent = { Text("Status: Sedang Mengangkut (Blok B)") },
                        trailingContent = {
                            Icon(Icons.Default.Circle, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RekapLaporanTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Rekap Laporan Masyarakat", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Laporan Bulan Ini: 156")
                Text("Laporan Selesai: 140")
                Text("Laporan Dalam Proses: 16")
                LinearProgressIndicator(
                    progress = 0.9f,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun LokasiPengangkutanTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Peta Lokasi Pengangkutan", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        // Placeholder for map view
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Peta Lokasi (Google Maps Placeholder)")
            }
        }
    }
}
