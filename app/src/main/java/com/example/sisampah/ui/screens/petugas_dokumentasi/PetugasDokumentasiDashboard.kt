package com.example.sisampah.ui.screens.petugas_dokumentasi

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import com.example.sisampah.model.TrashReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Green700 = Color(0xFF2E7D32)
private val Green500 = Color(0xFF4CAF50)
private val RedAccent = Color(0xFFE53935)
private val Blue700 = Color(0xFF1565C0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetugasDokumentasiDashboard(username: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reports by remember { mutableStateOf<List<TrashReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDetailDialog by remember { mutableStateOf<TrashReport?>(null) }

    fun loadData() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val query = "SELECT * FROM trash_reports ORDER BY id DESC"
                    val rs = conn.createStatement().executeQuery(query)
                    val list = mutableListOf<TrashReport>()
                    while (rs.next()) {
                        list.add(TrashReport(
                            id = rs.getInt("id").toString(),
                            reporterName = rs.getString("reporterName") ?: "Unknown",
                            location = rs.getString("location") ?: "",
                            description = rs.getString("description") ?: "",
                            status = rs.getString("status") ?: "Menunggu",
                            timestamp = rs.getString("timestamp") ?: "",
                            image = rs.getString("image")
                        ))
                    }
                    withContext(Dispatchers.Main) {
                        reports = list
                        isLoading = false
                    }
                    conn.close()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateStatus(id: String, newStatus: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val stmt = conn.prepareStatement("UPDATE trash_reports SET status = ? WHERE id = ?")
                    stmt.setString(1, newStatus)
                    stmt.setInt(2, id.toInt())
                    stmt.executeUpdate()
                    conn.close()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Status diperbarui", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    if (showDetailDialog != null) {
        val report = showDetailDialog!!
        AlertDialog(
            onDismissRequest = { showDetailDialog = null },
            title = { Text("Detail Laporan Dokumentasi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pelapor: ${report.reporterName}", fontWeight = FontWeight.Bold)
                    Text("Lokasi: ${report.location}")
                    Text("Keterangan: ${report.description}")
                    Text("Status: ${report.status}")
                    
                    report.image?.let { base64 ->
                        val bitmap = remember(base64) {
                            try {
                                val bytes = Base64.decode(base64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) { null }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Foto Sampah",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (report.status == "Menunggu") {
                    Button(onClick = {
                        updateStatus(report.id, "Selesai")
                        showDetailDialog = null
                    }, colors = ButtonDefaults.buttonColors(containerColor = Green700)) {
                        Text("Tandai Selesai")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetailDialog = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                Box(
                    Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Blue700, Color(0xFF1976D2))))
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Petugas Dokumentasi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Halo, $username", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, null, tint = Color.White)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize().background(Color(0xFFF5F5F5))) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daftar Laporan Masyarakat", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = { loadData() }) {
                    Icon(Icons.Default.Refresh, null, tint = Blue700)
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Blue700)
                }
            } else if (reports.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada laporan", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reports) { report ->
                        Card(
                            onClick = { showDetailDialog = report },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(report.reporterName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(report.location, fontSize = 13.sp, color = Color.Gray, maxLines = 1)
                                    Spacer(Modifier.height(4.dp))
                                    Surface(
                                        color = if (report.status == "Selesai") Green700.copy(0.1f) else RedAccent.copy(0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            report.status,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp,
                                            color = if (report.status == "Selesai") Green700 else RedAccent,
                                            fontWeight = FontWeight.Bold
                                        )
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
}
