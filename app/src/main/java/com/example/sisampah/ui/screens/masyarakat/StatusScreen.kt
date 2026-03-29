package com.example.sisampah.ui.screens.masyarakat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val GreenPrimary = Color(0xFF2E7D32)
private val BlueStat = Color(0xFF1565C0)
private val AmberAccent = Color(0xFFFFC107)

@Composable
fun StatusScreen() {
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
