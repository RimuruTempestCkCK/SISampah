package com.example.sisampah.ui.screens.masyarakat

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.ResultSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasyarakatDashboard(onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SI-Sampah Masyarakat") },
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
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Beranda") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AddAPhoto, null) },
                    label = { Text("Lapor") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, null) },
                    label = { Text("Status") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Payments, null) },
                    label = { Text("Tagihan") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> HomeTab()
                1 -> ReportTab()
                2 -> StatusTab()
                3 -> PaymentTab()
            }
        }
    }
}

@Composable
fun HomeTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Selamat Datang,", fontSize = 20.sp)
        Text("Warga Peduli Lingkungan", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Jadwal Pengangkutan Terdekat", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Hari ini: 08:00 - 10:00 WIB")
                Text("Area: Perumahan Indah Permai")
            }
        }
    }
}

@Composable
fun ReportTab() {
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Kirim Laporan Sampah", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Lokasi Sampah") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Deskripsi Kondisi") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        } else {
            Button(
                onClick = {
                    if (location.isEmpty() || description.isEmpty()) {
                        Toast.makeText(context, "Isi semua bidang!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            val conn = MySqlHelper.getConnection()
                            if (conn != null) {
                                val query = "INSERT INTO trash_reports (reporterName, location, description, status, timestamp) VALUES (?, ?, ?, ?, NOW())"
                                val stmt = conn.prepareStatement(query)
                                stmt.setString(1, "Warga") // Idealnya ambil dari session user
                                stmt.setString(2, location)
                                stmt.setString(3, description)
                                stmt.setString(4, "Menunggu")
                                
                                val rows = stmt.executeUpdate()
                                withContext(Dispatchers.Main) {
                                    if (rows > 0) {
                                        Toast.makeText(context, "Laporan Terkirim!", Toast.LENGTH_SHORT).show()
                                        location = ""
                                        description = ""
                                    } else {
                                        Toast.makeText(context, "Gagal mengirim laporan", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                conn.close()
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Gagal terhubung database", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                        }
                    }
                }, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kirim Laporan")
            }
        }
    }
}

@Composable
fun StatusTab() {
    var reports by remember { mutableStateOf(listOf<Map<String, String>>()) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val conn = MySqlHelper.getConnection()
            if (conn != null) {
                try {
                    val query = "SELECT * FROM trash_reports ORDER BY timestamp DESC"
                    val rs = conn.createStatement().executeQuery(query)
                    val list = mutableListOf<Map<String, String>>()
                    while (rs.next()) {
                        list.add(mapOf(
                            "location" to (rs.getString("location") ?: ""),
                            "status" to (rs.getString("status") ?: ""),
                            "id" to (rs.getString("id") ?: "")
                        ))
                    }
                    withContext(Dispatchers.Main) {
                        reports = list
                    }
                    conn.close()
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Riwayat Laporan", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(reports) { report ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Laporan #${report["id"]}") },
                    supportingContent = { Text("Lokasi: ${report["location"]}") },
                    trailingContent = { 
                        AssistChip(onClick = {}, label = { Text(report["status"] ?: "") })
                    }
                )
            }
        }
    }
}

@Composable
fun PaymentTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tagihan Pengangkutan", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Bulan: Mei 2024", fontWeight = FontWeight.Bold)
                Text("Jumlah: Rp 25.000")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { /* Pay logic */ }) {
                    Text("Bayar Sekarang")
                }
            }
        }
    }
}
