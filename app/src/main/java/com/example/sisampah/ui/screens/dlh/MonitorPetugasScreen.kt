package com.example.sisampah.ui.screens.dlh

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Green700 = Color(0xFF2E7D32)
private val Green500 = Color(0xFF4CAF50)
private val Blue700 = Color(0xFF1565C0)
private val RedAccent = Color(0xFFE53935)
private val Amber500 = Color(0xFFFFC107)

data class PetugasActivity(
    val id: Int,
    val nama: String,
    val username: String,
    val totalTugas: Int,
    val totalLaporanSelesai: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorPetugasScreen() {
    val scope = rememberCoroutineScope()
    var petugasList by remember { mutableStateOf<List<PetugasActivity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun loadData() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val sql = """
                        SELECT u.id, u.nama, u.username,
                        (SELECT COUNT(*) FROM schedules s WHERE s.petugas_id = u.id) as total_tugas,
                        (SELECT COUNT(*) FROM trash_reports tr WHERE tr.status = 'Selesai') as total_laporan
                        FROM users u 
                        WHERE u.role = 'PETUGAS_LPS'
                        ORDER BY u.nama ASC
                    """.trimIndent()
                    val rs = conn.createStatement().executeQuery(sql)
                    val list = mutableListOf<PetugasActivity>()
                    while (rs.next()) {
                        list.add(PetugasActivity(
                            rs.getInt("id"),
                            rs.getString("nama") ?: rs.getString("username"),
                            rs.getString("username"),
                            rs.getInt("total_tugas"),
                            rs.getInt("total_laporan") // Ini simulasi, idealnya ada relasi petugas di laporan
                        ))
                    }
                    withContext(Dispatchers.Main) {
                        petugasList = list
                        isLoading = false
                        errorMsg = null
                    }
                    conn.close()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMsg = "Error: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Green700, Green500)))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Monitor Petugas LPS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Pantau performa petugas di lapangan", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
                IconButton(onClick = { loadData() }) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        AnimatedVisibility(visible = errorMsg != null, enter = fadeIn(), exit = fadeOut()) {
            errorMsg?.let { msg ->
                Card(
                    Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RedAccent.copy(0.1f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, null, tint = RedAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(msg, color = RedAccent, fontSize = 13.sp)
                    }
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green700)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(petugasList) { petugas ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(Blue700.copy(0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Engineering, null, tint = Blue700)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(petugas.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("@${petugas.username}", fontSize = 12.sp, color = Color.Gray)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    InfoBadge(label = "${petugas.totalTugas} Jadwal", color = Blue700)
                                    InfoBadge(label = "Aktif", color = Green700)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoBadge(label: String, color: Color) {
    Surface(
        color = color.copy(0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
