package com.example.sisampah.ui.screens.masyarakat

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val GreenPrimary = Color(0xFF2E7D32)
private val GreenSurface = Color(0xFFE8F5E9)
private val RedAccent = Color(0xFFE53935)

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

    // ── Migrasi Database: Tambah tabel payments jika belum ada ──
    fun migrateDatabase() {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val stmt = conn.createStatement()
                    val sql = """
                        CREATE TABLE IF NOT EXISTS payments (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            bulan VARCHAR(50) NOT NULL,
                            jumlah DOUBLE NOT NULL,
                            status VARCHAR(20) DEFAULT 'BELUM BAYAR'
                        )
                    """.trimIndent()
                    stmt.executeUpdate(sql)
                    
                    // Tambah data dummy jika kosong
                    val rs = stmt.executeQuery("SELECT COUNT(*) FROM payments")
                    if (rs.next() && rs.getInt(1) == 0) {
                        stmt.executeUpdate("INSERT INTO payments (bulan, jumlah, status) VALUES ('Maret 2026', 25000, 'BELUM BAYAR')")
                        stmt.executeUpdate("INSERT INTO payments (bulan, jumlah, status) VALUES ('Februari 2026', 25000, 'LUNAS')")
                    }
                    rs.close(); stmt.close(); conn.close()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

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
                }
                withContext(Dispatchers.Main) { tagihanList = list; isLoading = false }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    LaunchedEffect(Unit) {
        migrateDatabase()
        loadData()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Tagihan Pengangkutan", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            Text("Kelola pembayaran iuran kebersihan rutin", fontSize = 13.sp, color = Color.Gray)
        }

        if (isLoading) {
            item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GreenPrimary) } }
        } else if (tagihanList.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("Tidak ada data tagihan", color = Color.Gray) } }
        } else {
            items(tagihanList) { tagihan ->
                TagihanItem(tagihan) {
                    // Logic Bayar
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
                        Icon(Icons.Default.ReceiptLong, null, tint = if (isLunas) GreenPrimary else RedAccent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(tagihan.bulan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Iuran Kebersihan", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Text("Rp ${tagihan.jumlah.toInt()}", fontWeight = FontWeight.Bold, color = if (isLunas) GreenPrimary else RedAccent, fontSize = 17.sp)
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = (if (isLunas) GreenPrimary else Color(0xFFFF9800)).copy(0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        tagihan.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isLunas) GreenPrimary else Color(0xFFFF9800),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isLunas) {
                    Button(
                        onClick = onPay,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
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
