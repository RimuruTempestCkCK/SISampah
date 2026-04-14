package com.example.sisampah.ui.screens.masyarakat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── Warna tema ────────────────────────────────────────────────────────────────
private val GreenPrimary   = Color(0xFF2E7D32)
private val GreenLight     = Color(0xFF4CAF50)
private val GreenSurface   = Color(0xFFE8F5E9)
private val AmberAccent    = Color(0xFFFFC107)
private val RedAccent      = Color(0xFFE53935)
private val BlueStat       = Color(0xFF1565C0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasyarakatDashboard(username: String, onLogout: () -> Unit) {
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
                    Triple("Lokasi",  Icons.Default.Map, 4),
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
                0 -> HomeTab(username)
                1 -> LaporScreen(username)
                2 -> StatusScreen()
                3 -> TagihanScreen(username)
                4 -> MapScreen(username)
            }
        }
    }
}

@Composable
fun HomeTab(username: String) {
    var visible by remember { mutableStateOf(false) }
    var totalLaporan by remember { mutableStateOf("0") }
    var statusAktif by remember { mutableStateOf("0") }
    var jadwalHari by remember { mutableStateOf("Belum ada jadwal") }
    var jadwalJam by remember { mutableStateOf("-") }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        visible = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    // Cari Nama Asli dari username
                    var userRealName = username
                    val stmtUser = conn.prepareStatement("SELECT nama FROM users WHERE username = ?")
                    stmtUser.setString(1, username)
                    val rsUser = stmtUser.executeQuery()
                    if (rsUser.next()) {
                        userRealName = rsUser.getString("nama") ?: username
                    }
                    rsUser.close()
                    stmtUser.close()

                    // Hitung Laporan Saya
                    val stmtLaporan = conn.prepareStatement("SELECT COUNT(*) FROM trash_reports WHERE reporterName = ?")
                    stmtLaporan.setString(1, userRealName)
                    val rsLaporan = stmtLaporan.executeQuery()
                    if (rsLaporan.next()) {
                        val count = rsLaporan.getInt(1).toString()
                        withContext(Dispatchers.Main) { totalLaporan = count }
                    }
                    rsLaporan.close()
                    stmtLaporan.close()

                    // Hitung Status Aktif (Belum Selesai)
                    val stmtAktif = conn.prepareStatement("SELECT COUNT(*) FROM trash_reports WHERE reporterName = ? AND status != 'Selesai'")
                    stmtAktif.setString(1, userRealName)
                    val rsAktif = stmtAktif.executeQuery()
                    if (rsAktif.next()) {
                        val count = rsAktif.getInt(1).toString()
                        withContext(Dispatchers.Main) { statusAktif = count }
                    }
                    rsAktif.close()
                    stmtAktif.close()

                    // Ambil Jadwal Terdekat (Limit 1)
                    val stmtJadwal = conn.prepareStatement("SELECT hari, jam FROM schedules LIMIT 1")
                    val rsJadwal = stmtJadwal.executeQuery()
                    if (rsJadwal.next()) {
                        val hari = rsJadwal.getString("hari")
                        val jam = rsJadwal.getString("jam")
                        withContext(Dispatchers.Main) {
                            jadwalHari = hari
                            jadwalJam = "Pukul $jam WIB"
                        }
                    }
                    rsJadwal.close()
                    stmtJadwal.close()

                    conn.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
                            Text(username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                StatCard(Modifier.weight(1f), "Laporan Saya", totalLaporan, Icons.Default.Description, BlueStat)
                StatCard(Modifier.weight(1f), "Status Aktif", statusAktif, Icons.Default.Pending, AmberAccent)
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
                        Text(jadwalHari, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(jadwalJam, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

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
