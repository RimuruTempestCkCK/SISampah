package com.example.sisampah.ui.screens.petugas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import com.example.sisampah.ui.screens.admin.Schedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Green700 = Color(0xFF2E7D32)
private val Green500 = Color(0xFF4CAF50)
private val GreenSurface = Color(0xFFE8F5E9)
private val Blue700 = Color(0xFF1565C0)
private val RedAccent = Color(0xFFE53935)
private val AmberAccent = Color(0xFFFFC107)

private val HARI = listOf(
    "SEMUA", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetugasJadwalScreen(username: String) {
    val scope = rememberCoroutineScope()
    var schedules by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterHari by remember { mutableStateOf("SEMUA") }
    var filterMine by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun loadData() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                val list = mutableListOf<Schedule>()
                if (conn != null) {
                    val sql = """
                        SELECT s.*, u.nama as petugas_nama, u.username as petugas_username
                        FROM schedules s 
                        LEFT JOIN users u ON s.petugas_id = u.id 
                        ORDER BY s.id ASC
                    """.trimIndent()
                    val rs = conn.createStatement().executeQuery(sql)
                    while (rs.next()) {
                        list.add(
                            Schedule(
                                id = rs.getInt("id"),
                                lokasi = rs.getString("lokasi"),
                                hari = rs.getString("hari"),
                                jam = rs.getString("jam"),
                                petugasId = if (rs.getObject("petugas_id") != null) rs.getInt("petugas_id") else null,
                                petugasNama = rs.getString("petugas_nama") ?: rs.getString("petugas_username")
                            )
                        )
                    }
                    rs.close(); conn.close()
                }
                withContext(Dispatchers.Main) { 
                    schedules = list
                    errorMsg = null 
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { errorMsg = "Error: ${e.message}" }
            } finally {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val displayedSchedules = schedules.filter {
        val matchHari = (filterHari == "SEMUA" || it.hari == filterHari)
        val matchSearch = (searchQuery.isEmpty() || it.lokasi.contains(searchQuery, ignoreCase = true))
        val matchMine = if (filterMine) {
            it.petugasNama == username || it.petugasNama?.contains(username, ignoreCase = true) == true
        } else true
        
        matchHari && matchSearch && matchMine
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Green700, Green500)))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Jadwal Pengangkutan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Pantau jadwal pengangkutan sampah", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
                IconButton(onClick = { loadData() }) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        AnimatedVisibility(visible = errorMsg != null, enter = fadeIn(), exit = fadeOut()) {
            errorMsg?.let { msg ->
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = RedAccent.copy(0.1f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, null, tint = RedAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(msg, color = RedAccent, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari lokasi...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Green700,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = filterMine,
                    onClick = { filterMine = !filterMine },
                    label = { Text("Jadwal Saya", fontSize = 11.sp) },
                    leadingIcon = if (filterMine) {
                        { Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Blue700,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
                Spacer(Modifier.width(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(HARI) { hari ->
                        FilterChip(
                            selected = filterHari == hari,
                            onClick = { filterHari = hari },
                            label = { Text(hari, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Green700,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green700)
                }
            } else if (displayedSchedules.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada jadwal ditemukan", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(displayedSchedules, key = { it.id }) { schedule ->
                        PetugasScheduleCard(schedule = schedule, myUsername = username)
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun PetugasScheduleCard(schedule: Schedule, myUsername: String) {
    val isMine = schedule.petugasNama == myUsername || schedule.petugasNama?.contains(myUsername, ignoreCase = true) == true
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMine) GreenSurface else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(
                    (if (isMine) Green700 else Color.Gray).copy(alpha = 0.15f)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isMine) Icons.Default.AssignmentInd else Icons.Default.Event, 
                    null, 
                    tint = if (isMine) Green700 else Color.Gray
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(schedule.lokasi, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${schedule.hari} • ${schedule.jam}", fontSize = 13.sp, color = Color.DarkGray)
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        Icons.Default.AccountCircle, 
                        null, 
                        tint = if (isMine) Green700 else Color.Gray, 
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isMine) "Tugas Anda" else "Petugas: ${schedule.petugasNama ?: "Belum ada"}",
                        fontSize = 12.sp,
                        color = if (isMine) Green700 else Color.Gray,
                        fontWeight = if (isMine) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
            if (isMine) {
                Icon(Icons.Default.Star, null, tint = AmberAccent)
            }
        }
    }
}
