package com.example.sisampah.ui.screens.dlh

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import com.example.sisampah.model.TrashReport
import com.example.sisampah.ui.screens.admin.StatCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Green700 = Color(0xFF2E7D32)
private val Green500 = Color(0xFF4CAF50)
private val BlueStat = Color(0xFF1565C0)
private val RedAccent = Color(0xFFE53935)
private val Amber500 = Color(0xFFFFC107)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RekapLaporanScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var reports by remember { mutableStateOf<List<TrashReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalCount by remember { mutableStateOf(0) }
    var finishedCount by remember { mutableStateOf(0) }
    var pendingCount by remember { mutableStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun loadData() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val query = "SELECT * FROM trash_reports ORDER BY id DESC"
                    val rs = conn.createStatement().executeQuery(query)
                    val list = mutableListOf<TrashReport>()
                    var finished = 0
                    var pending = 0
                    
                    while (rs.next()) {
                        val status = rs.getString("status") ?: "Menunggu"
                        if (status == "Selesai") finished++ else pending++
                        
                        list.add(TrashReport(
                            id = rs.getInt("id").toString(),
                            reporterName = rs.getString("reporterName") ?: "Unknown",
                            location = rs.getString("location") ?: "",
                            description = rs.getString("description") ?: "",
                            status = status,
                            timestamp = rs.getString("timestamp") ?: "",
                            image = rs.getString("image")
                        ))
                    }
                    
                    withContext(Dispatchers.Main) {
                        reports = list
                        totalCount = list.size
                        finishedCount = finished
                        pendingCount = pending
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
                    Text("Rekap Laporan Masyarakat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Pantau data pelaporan sampah secara real-time", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(Modifier.weight(1f), "Total", totalCount.toString(), Icons.Default.Assessment, BlueStat)
                        StatCard(Modifier.weight(1f), "Selesai", finishedCount.toString(), Icons.Default.CheckCircle, Green700)
                        StatCard(Modifier.weight(1f), "Pending", pendingCount.toString(), Icons.Default.Pending, Amber500)
                    }
                }

                item {
                    Text("Daftar Laporan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.DarkGray)
                }

                items(reports) { report ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(report.reporterName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(report.location, fontSize = 13.sp, color = Color.Gray)
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    color = if (report.status == "Selesai") Green700.copy(0.1f) else RedAccent.copy(0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        report.status,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        color = if (report.status == "Selesai") Green700 else RedAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    report.timestamp.take(10),
                                    fontSize = 12.sp,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(4.dp))
                                Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                            }
                        }
                    }
                }
                
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
