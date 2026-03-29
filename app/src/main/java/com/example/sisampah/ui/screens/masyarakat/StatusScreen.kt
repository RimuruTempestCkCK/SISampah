package com.example.sisampah.ui.screens.masyarakat

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Green700 = Color(0xFF2E7D32)
private val Green500 = Color(0xFF4CAF50)
private val RedAccent = Color(0xFFE53935)
private val Blue700 = Color(0xFF1565C0)
private val Amber500 = Color(0xFFFFC107)

@Composable
fun StatusScreen() {
    var reports by remember { mutableStateOf(listOf<Map<String, String>>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    fun loadData() {
        isLoading = true
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
                            "id" to (rs.getString("id") ?: ""),
                            "description" to (rs.getString("description") ?: ""),
                            "timestamp" to (rs.getString("timestamp") ?: "")
                        ))
                    }
                    withContext(Dispatchers.Main) { 
                        reports = list
                        isLoading = false
                        errorMsg = null
                    }
                    conn.close()
                } else {
                    withContext(Dispatchers.Main) { 
                        errorMsg = "Gagal terhubung ke database"
                        isLoading = false 
                    }
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
        // ── Header Banner ──
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Green700, Green500)))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Riwayat Laporan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("${reports.size} laporan telah dikirim", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
                IconButton(onClick = { loadData() }) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        // ── Error Banner ──
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
                        IconButton(onClick = { errorMsg = null }) { Icon(Icons.Default.Close, null, tint = RedAccent) }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                item { 
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator(color = Green700) 
                    } 
                }
            } else if (reports.isEmpty()) {
                item { 
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            Modifier.padding(32.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.History, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Belum ada riwayat laporan", color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                items(reports) { report ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text("Laporan #${report["id"]}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Green700)
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Place, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(report["location"] ?: "", fontSize = 13.sp, color = Color.Gray, maxLines = 1)
                                    }
                                }
                                
                                val status = report["status"] ?: ""
                                val statusColor = when(status) {
                                    "Selesai" -> Green700
                                    "Proses"  -> Blue700
                                    else      -> Amber500
                                }
                                Surface(
                                    color = statusColor.copy(0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        status.uppercase(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Divider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray)
                            
                            Text(report["description"] ?: "", fontSize = 14.sp, color = Color.DarkGray)
                            
                            Spacer(Modifier.height(8.dp))
                            Text(report["timestamp"] ?: "", fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}
