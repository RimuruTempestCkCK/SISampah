package com.example.sisampah.ui.screens.admin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Green700 = Color(0xFF2E7D32)
private val Green500 = Color(0xFF4CAF50)
private val RedAccent = Color(0xFFE53935)
private val Blue700 = Color(0xFF1565C0)
private val Amber500 = Color(0xFFFFC107)

data class PaymentRecord(
    val id: Int,
    val username: String,
    val bulan: String,
    val jumlah: Double,
    val status: String,
    val imageBukti: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentValidationScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var payments by remember { mutableStateOf<List<PaymentRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDetailDialog by remember { mutableStateOf<PaymentRecord?>(null) }

    fun loadData() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val query = "SELECT * FROM payments WHERE status = 'MENUNGGU' ORDER BY id DESC"
                    val rs = conn.createStatement().executeQuery(query)
                    val list = mutableListOf<PaymentRecord>()
                    while (rs.next()) {
                        list.add(PaymentRecord(
                            rs.getInt("id"),
                            rs.getString("username") ?: "Unknown",
                            rs.getString("bulan"),
                            rs.getDouble("jumlah"),
                            rs.getString("status"),
                            rs.getString("image_bukti")
                        ))
                    }
                    withContext(Dispatchers.Main) {
                        payments = list
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

    fun updateStatus(id: Int, newStatus: String) {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val stmt = conn.prepareStatement("UPDATE payments SET status = ? WHERE id = ?")
                    stmt.setString(1, newStatus)
                    stmt.setInt(2, id)
                    stmt.executeUpdate()
                    conn.close()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Berhasil: Status diperbarui menjadi $newStatus", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal memperbarui data: ${e.message}", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    if (showDetailDialog != null) {
        val payment = showDetailDialog!!
        AlertDialog(
            onDismissRequest = { showDetailDialog = null },
            title = { Text("Detail Pembayaran - ${payment.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bulan: ${payment.bulan}")
                    Text("Jumlah: Rp ${payment.jumlah.toInt()}")
                    Text("Bukti Transfer:")
                    payment.imageBukti?.let { base64 ->
                        val bitmap = remember(base64) {
                            try {
                                val bytes = Base64.decode(base64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) { null }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Bukti Pembayaran",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("Gagal memuat gambar", color = Color.Gray)
                        }
                    } ?: Text("Tidak ada bukti gambar", color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        updateStatus(payment.id, "LUNAS")
                        showDetailDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Green700)
                ) {
                    Text("Validasi (Lunas)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        updateStatus(payment.id, "BELUM BAYAR")
                        showDetailDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RedAccent)
                ) {
                    Text("Tolak")
                }
            }
        )
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Green700, Green500)))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Validasi Pembayaran", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Konfirmasi bukti iuran warga", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
                IconButton(onClick = { loadData() }) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        if (isLoading && payments.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green700)
            }
        } else if (payments.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircleOutline, null, Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text("Tidak ada pembayaran yang perlu divalidasi", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(payments) { payment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        onClick = { showDetailDialog = payment }
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(payment.username, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(payment.bulan, fontSize = 13.sp, color = Color.Gray)
                                Text("Rp ${payment.jumlah.toInt()}", fontWeight = FontWeight.SemiBold, color = Green700)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = Blue700.copy(0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        payment.status,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = Blue700,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }
}
