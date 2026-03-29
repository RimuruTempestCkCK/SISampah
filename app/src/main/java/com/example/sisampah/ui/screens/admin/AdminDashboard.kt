package com.example.sisampah.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.StrokeCap
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

// ═══════════════════════════════════════════════════════════════════════════════
//  ROOT DASHBOARD
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(onLogout: () -> Unit) {
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
                            Text("Panel Admin", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = RedAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val tabs = listOf(
                    Triple("Beranda",  Icons.Default.Dashboard,     0),
                    Triple("User",     Icons.Default.People,        1),
                    Triple("Jadwal",  Icons.Default.CalendarToday,    2),
                    Triple("Petugas", Icons.Default.Engineering, 3)
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
                1 -> ManageUserScreen()
                2 -> ManageScheduleScreen()
                3 -> PetugasScreen()
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  TAB 0 — BERANDA (DASHBOARD RINGKASAN)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun HomeTab() {
    var visible by remember { mutableStateOf(false) }
    var totalUser by remember { mutableStateOf("0") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { 
        visible = true 
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val stmt = conn.prepareStatement("SELECT COUNT(*) FROM users")
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        val count = rs.getInt(1).toString()
                        withContext(Dispatchers.Main) { totalUser = count }
                    }
                    rs.close()
                    stmt.close()
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
        // ── Header selamat datang ──
        item {
            AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { -40 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(listOf(GreenPrimary, GreenLight))
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Selamat Datang,", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                            Text("Admin Lapor-Sampah", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Jumat, 27 Maret 2026", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.AdminPanelSettings, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(56.dp))
                    }
                }
            }
        }

        // ── Kartu statistik (2 kolom) ──
        item {
            Text("Ringkasan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Total User",    totalUser,  Icons.Default.People,         BlueStat)
                StatCard(Modifier.weight(1f), "Laporan Baru",  "38",   Icons.Default.ReportProblem,  AmberAccent)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "Jadwal Aktif",  "12",   Icons.Default.EventAvailable, GreenPrimary)
                StatCard(Modifier.weight(1f), "Petugas Aktif", "9",    Icons.Default.Badge,          Color(0xFF6A1B9A))
            }
        }

        // ── Progress pengangkutan bulan ini ──
        item {
            SectionCard(title = "Pengangkutan Bulan Ini") {
                ProgressRow("Blok A",   0.85f, GreenLight)
                ProgressRow("Blok B",   0.62f, AmberAccent)
                ProgressRow("Blok C",   0.40f, RedAccent)
                ProgressRow("Blok D",   0.90f, GreenPrimary)
            }
        }

        // ── Aktivitas terbaru ──
        item {
            SectionCard(title = "Aktivitas Terbaru") {
                ActivityRow(Icons.Default.PersonAdd,     "User baru didaftarkan",   "5 menit lalu",  GreenPrimary)
                ActivityRow(Icons.Default.ReportProblem, "Laporan #1089 masuk",      "12 menit lalu", AmberAccent)
                ActivityRow(Icons.Default.EditCalendar,  "Jadwal Blok B diperbarui", "1 jam lalu",    BlueStat)
                ActivityRow(Icons.Default.DeleteForever, "User #48 dinonaktifkan",   "2 jam lalu",    RedAccent)
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
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
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

@Composable
fun ProgressRow(label: String, progress: Float, color: Color) {
    val animProgress by animateFloatAsState(targetValue = progress, label = "progress")
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp)
            Text("${(progress * 100).toInt()}%", fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = animProgress,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
fun ActivityRow(icon: ImageVector, title: String, time: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(color.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(time, fontSize = 11.sp, color = Color.Gray)
        }
    }
}




@Composable
fun ScheduleInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", fontSize = 13.sp, color = Color.Gray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}