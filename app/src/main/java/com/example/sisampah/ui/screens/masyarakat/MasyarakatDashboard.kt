package com.example.sisampah.ui.screens.masyarakat

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── Warna tema (konsisten dengan Admin) ───────────────────────────────────────
private val GreenPrimary   = Color(0xFF2E7D32)
private val GreenLight     = Color(0xFF4CAF50)
private val GreenSurface   = Color(0xFFE8F5E9)
private val AmberAccent    = Color(0xFFFFC107)
private val RedAccent      = Color(0xFFE53935)
private val BlueStat       = Color(0xFF1565C0)

// ═══════════════════════════════════════════════════════════════════════════════
//  ROOT DASHBOARD
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasyarakatDashboard(onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Recycling, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Lapor-Sampah", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Masyarakat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = RedAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val tabs = listOf(
                    Triple("Beranda", Icons.Default.Home, 0),
                    Triple("Lapor",   Icons.Default.AddAPhoto, 1),
                    Triple("Status",  Icons.Default.History, 2),
                    Triple("Tagihan", Icons.Default.Payments, 3)
                )
                tabs.forEach { (label, icon, idx) ->
                    NavigationBarItem(
                        selected = selectedTab == idx,
                        onClick  = { selectedTab = idx },
                        icon     = { Icon(icon, null) },
                        label    = { Text(label, fontSize = 11.sp) },
                        colors   = NavigationBarItemDefaults.colors(
                            indicatorColor = GreenSurface,
                            selectedIconColor = GreenPrimary,
                            selectedTextColor = GreenPrimary,
                        )
                    )
                }
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

// ═══════════════════════════════════════════════════════════════════════════════
//  TAB 0 — BERANDA
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun HomeTab() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { -40 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(GreenPrimary, GreenLight)))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Selamat Datang,", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                            Text("Warga Lapor-Sampah", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Ayo jaga kebersihan lingkungan kita!", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.AccountCircle, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(56.dp))
                    }
                }
            }
        }

        item {
            Text("Ringkasan Info", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Laporan Saya", "5", Icons.Default.Description, BlueStat)
                StatCard(Modifier.weight(1f), "Status Aktif", "1", Icons.Default.Pending, AmberAccent)
            }
        }

        item {
            SectionCard(title = "Jadwal Pengangkutan Terdekat") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(GreenSurface),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Event, null, tint = GreenPrimary) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Senin & Kamis", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Pukul 08:00 - 10:00 WIB", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  TAB 1 — LAPOR
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun ReportTab() {
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Buat Laporan Baru", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            Text("Laporkan penumpukan sampah di sekitar Anda", fontSize = 13.sp, color = Color.Gray)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Lokasi Kejadian") },
                        leadingIcon = { Icon(Icons.Default.Place, null, tint = GreenPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi Kondisi") },
                        leadingIcon = { Icon(Icons.Default.Notes, null, tint = GreenPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    )

                    Button(
                        onClick = {
                            if (location.isBlank() || description.isBlank()) {
                                Toast.makeText(context, "Harap lengkapi data!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val conn = MySqlHelper.getConnection()
                                    if (conn != null) {
                                        val query = "INSERT INTO trash_reports (reporterName, location, description, status, timestamp) VALUES (?, ?, ?, ?, NOW())"
                                        val stmt = conn.prepareStatement(query)
                                        stmt.setString(1, "Warga") 
                                        stmt.setString(2, location)
                                        stmt.setString(3, description)
                                        stmt.setString(4, "Menunggu")
                                        stmt.executeUpdate()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Laporan Terkirim!", Toast.LENGTH_SHORT).show()
                                            location = ""; description = ""
                                        }
                                        conn.close()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                                } finally {
                                    withContext(Dispatchers.Main) { isLoading = false }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Kirim Laporan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  TAB 2 — STATUS (RIWAYAT)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun StatusTab() {
    var reports by remember { mutableStateOf(listOf<Map<String, String>>()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
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
                    withContext(Dispatchers.Main) { reports = list; isLoading = false }
                    conn.close()
                }
            } catch (e: Exception) { 
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Riwayat Laporan Saya", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            Spacer(Modifier.height(8.dp))
        }

        if (isLoading) {
            item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GreenPrimary) } }
        } else if (reports.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("Belum ada riwayat laporan", color = Color.Gray) } }
        } else {
            items(reports) { report ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("Laporan #${report["id"]}", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Lokasi: ${report["location"]}", maxLines = 1) },
                        trailingContent = {
                            val status = report["status"] ?: ""
                            val statusColor = when(status) {
                                "Selesai" -> GreenPrimary
                                "Proses"  -> BlueStat
                                else      -> AmberAccent
                            }
                            Surface(
                                color = statusColor.copy(0.1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    status,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  TAB 3 — TAGIHAN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun PaymentTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Tagihan Pengangkutan", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            Text("Kelola pembayaran iuran kebersihan", fontSize = 13.sp, color = Color.Gray)
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Bulan Maret 2026", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Iuran Kebersihan Rutin", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text("Rp 25.000", fontWeight = FontWeight.Bold, color = RedAccent, fontSize = 18.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { /* Logic */ },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        Icon(Icons.Default.Payment, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Bayar Sekarang", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  SHARED COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun StatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = color, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = color)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
