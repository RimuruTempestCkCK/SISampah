package com.example.sisampah.ui.screens.masyarakat

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
private val GreenSurface = Color(0xFFE8F5E9)
private val RedAccent = Color(0xFFE53935)
private val Amber500 = Color(0xFFFFC107)

data class Tagihan(
    val id: Int,
    val bulan: String,
    val jumlah: Double,
    val status: String
)

@Composable
fun TagihanScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tagihanList by remember { mutableStateOf<List<Tagihan>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun loadData() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                val list = mutableListOf<Tagihan>()
                if (conn != null) {
                    val rs = conn.createStatement().executeQuery("SELECT * FROM payments ORDER BY id DESC")
                    while (rs.next()) {
                        list.add(Tagihan(
                            rs.getInt("id"),
                            rs.getString("bulan"),
                            rs.getDouble("jumlah"),
                            rs.getString("status")
                        ))
                    }
                    rs.close(); conn.close()
                    withContext(Dispatchers.Main) { 
                        tagihanList = list
                        isLoading = false 
                        errorMsg = null
                    }
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
        // Migrasi & Load Data
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val stmt = conn.createStatement()
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS payments (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            bulan VARCHAR(50) NOT NULL,
                            jumlah DOUBLE NOT NULL,
                            status VARCHAR(20) DEFAULT 'BELUM BAYAR'
                        )
                    """.trimIndent())
                    
                    val rs = stmt.executeQuery("SELECT COUNT(*) FROM payments")
                    if (rs.next() && rs.getInt(1) == 0) {
                        stmt.executeUpdate("INSERT INTO payments (bulan, jumlah, status) VALUES ('Maret 2026', 25000, 'BELUM BAYAR')")
                        stmt.executeUpdate("INSERT INTO payments (bulan, jumlah, status) VALUES ('Februari 2026', 25000, 'LUNAS')")
                    }
                    rs.close(); stmt.close(); conn.close()
                }
            } catch (e: Exception) { e.printStackTrace() }
            loadData()
        }
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
                    Text("Tagihan Pengangkutan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Kelola pembayaran iuran kebersihan rutin", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                item { 
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator(color = Green700) 
                    } 
                }
            } else if (tagihanList.isEmpty()) {
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
                            Icon(Icons.Default.ReceiptLong, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Tidak ada data tagihan", color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                items(tagihanList) { tagihan ->
                    TagihanItem(tagihan) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val conn = MySqlHelper.getConnection()
                                if (conn != null) {
                                    val stmt = conn.prepareStatement("UPDATE payments SET status = 'LUNAS' WHERE id = ?")
                                    stmt.setInt(1, tagihan.id)
                                    stmt.executeUpdate()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Pembayaran Berhasil!", Toast.LENGTH_SHORT).show()
                                        loadData()
                                    }
                                    conn.close()
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TagihanItem(tagihan: Tagihan, onPay: () -> Unit) {
    val isLunas = tagihan.status == "LUNAS"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(if (isLunas) GreenSurface else RedAccent.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isLunas) Icons.Default.CheckCircle else Icons.Default.ReceiptLong, 
                            null, 
                            tint = if (isLunas) Green700 else RedAccent, 
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(tagihan.bulan, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isLunas) Green700 else Color.DarkGray)
                        Text("Iuran Kebersihan", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Text("Rp ${tagihan.jumlah.toInt()}", fontWeight = FontWeight.Bold, color = if (isLunas) Green700 else RedAccent, fontSize = 17.sp)
            }
            
            Divider(Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = (if (isLunas) Green700 else Amber500).copy(0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        tagihan.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isLunas) Green700 else Amber500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isLunas) {
                    Button(
                        onClick = onPay,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green700),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Payment, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Bayar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
