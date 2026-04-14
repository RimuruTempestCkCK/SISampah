package com.example.sisampah.ui.screens.petugas

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import com.example.sisampah.ui.screens.admin.StatCard
import com.example.sisampah.ui.screens.admin.SectionCard
import com.example.sisampah.ui.screens.admin.ActivityRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private val GreenPrimary   = Color(0xFF2E7D32)
private val GreenLight     = Color(0xFF4CAF50)
private val GreenSurface   = Color(0xFFE8F5E9)
private val RedAccent      = Color(0xFFE53935)
private val AmberAccent    = Color(0xFFFFC107)
private val BlueStat       = Color(0xFF1565C0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetugasDashboard(username: String, onLogout: () -> Unit) {
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
                            Icon(Icons.Default.Engineering, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Lapor-Sampah", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Petugas Lapangan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Triple("Beranda", Icons.Default.Dashboard, 0),
                    Triple("Jadwal",  Icons.Default.CalendarToday, 1),
                    Triple("Laporan", Icons.Default.Assessment, 2)
                )
                tabs.forEach { (label, icon, idx) ->
                    NavigationBarItem(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        icon = { Icon(icon, null) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
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
                0 -> PetugasHomeTab(username)
                1 -> PetugasJadwalScreen(username)
                2 -> PetugasLaporanScreen()
            }
        }
    }
}

@Composable
fun PetugasHomeTab(username: String) {
    var visible by remember { mutableStateOf(false) }
    var totalJadwal by remember { mutableStateOf("0") }
    var totalLaporan by remember { mutableStateOf("0") }
    val scope = rememberCoroutineScope()
    val today = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date()) }

    LaunchedEffect(Unit) {
        visible = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    // Total Jadwal Saya (berdasarkan petugas_id yang login)
                    val stmtJadwal = conn.prepareStatement(
                        "SELECT COUNT(*) FROM schedules s " +
                        "JOIN users u ON s.petugas_id = u.id " +
                        "WHERE u.username = ?"
                    )
                    stmtJadwal.setString(1, username)
                    val rsJadwal = stmtJadwal.executeQuery()
                    if (rsJadwal.next()) {
                        val count = rsJadwal.getInt(1).toString()
                        withContext(Dispatchers.Main) { totalJadwal = count }
                    }
                    rsJadwal.close(); stmtJadwal.close()

                    // Total Laporan Darurat/Masyarakat yang belum Selesai
                    val stmtLaporan = conn.prepareStatement("SELECT COUNT(*) FROM trash_reports WHERE status != 'Selesai'")
                    val rsLaporan = stmtLaporan.executeQuery()
                    if (rsLaporan.next()) {
                        val count = rsLaporan.getInt(1).toString()
                        withContext(Dispatchers.Main) { totalLaporan = count }
                    }
                    rsLaporan.close(); stmtLaporan.close()

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
                            Text("Selamat Bekerja,", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                            Text(username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(today, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.Engineering, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(56.dp))
                    }
                }
            }
        }

        item {
            Text("Ringkasan Tugas", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Jadwal Saya", totalJadwal, Icons.Default.Assignment, BlueStat)
                StatCard(Modifier.weight(1f), "Laporan Aktif", totalLaporan, Icons.Default.NewReleases, AmberAccent)
            }
        }

        item {
            SectionCard(title = "Panduan Operasional") {
                ActivityRow(Icons.Default.CheckCircle, "Periksa rute di menu Jadwal", "Harian", GreenPrimary)
                ActivityRow(Icons.Default.LocationOn, "Kunjungi lokasi titik sampah", "Sesuai Plotting", BlueStat)
                ActivityRow(Icons.Default.Warning, "Laporkan kendala armada", "Prioritas", RedAccent)
            }
        }
    }
}
