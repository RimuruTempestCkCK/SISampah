package com.example.sisampah.ui.screens.dlh

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

private val GreenPrimary   = Color(0xFF2E7D32)
private val GreenLight     = Color(0xFF4CAF50)
private val GreenSurface   = Color(0xFFE8F5E9)
private val RedAccent      = Color(0xFFE53935)
private val AmberAccent    = Color(0xFFFFC107)
private val BlueStat       = Color(0xFF1565C0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DLHDashboard(onLogout: () -> Unit) {
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
                            Icon(Icons.Default.Analytics, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Lapor-Sampah", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Panel DLH", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Triple("Monitor", Icons.Default.Engineering, 1),
                    Triple("Rekap",   Icons.Default.BarChart, 2)
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
                0 -> DLHHomeTab()
                1 -> MonitorPetugasScreen()
                2 -> RekapLaporanScreen()
            }
        }
    }
}

@Composable
fun DLHHomeTab() {
    var visible by remember { mutableStateOf(false) }
    var totalPetugas by remember { mutableStateOf("0") }
    var totalLaporan by remember { mutableStateOf("0") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        visible = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val stmtP = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE role = 'PETUGAS_LPS'")
                    val rsP = stmtP.executeQuery()
                    if (rsP.next()) totalPetugas = rsP.getInt(1).toString()
                    rsP.close(); stmtP.close()

                    val stmtL = conn.prepareStatement("SELECT COUNT(*) FROM trash_reports")
                    val rsL = stmtL.executeQuery()
                    if (rsL.next()) totalLaporan = rsL.getInt(1).toString()
                    rsL.close(); stmtL.close()

                    conn.close()
                }
            } catch (e: Exception) { e.printStackTrace() }
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
                            Text("Dinas Lingkungan Hidup", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                            Text("Monitoring Kebersihan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Sistem Pemantauan Terpadu", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.Public, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(56.dp))
                    }
                }
            }
        }

        item {
            Text("Ringkasan Operasional", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Petugas LPS", totalPetugas, Icons.Default.Groups, BlueStat)
                StatCard(Modifier.weight(1f), "Laporan Masuk", totalLaporan, Icons.Default.Report, AmberAccent)
            }
        }

        item {
            SectionCard(title = "Indikator Kinerja") {
                ActivityRow(Icons.Default.Speed, "Efisiensi Pengangkutan", "85%", GreenPrimary)
                ActivityRow(Icons.Default.AccessTime, "Rata-rata Respon", "2.4 Jam", BlueStat)
                ActivityRow(Icons.Default.AssignmentTurnedIn, "Laporan Selesai", "92%", GreenLight)
            }
        }
    }
}
