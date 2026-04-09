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
import com.example.sisampah.ui.screens.admin.StatCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Green700 = Color(0xFF2E7D32)
private val Green500 = Color(0xFF4CAF50)
private val BlueStat = Color(0xFF1565C0)
private val RedAccent = Color(0xFFE53935)
private val Amber500 = Color(0xFFFFC107)

data class PaymentSummary(
    val username: String,
    val totalDibayar: Double,
    val totalTransaksi: Int,
    val statusTerakhir: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentReportScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var paymentSummaries by remember { mutableStateOf<List<PaymentSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalPendapatan by remember { mutableStateOf(0.0) }
    var totalLunas by remember { mutableStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    fun loadData() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val query = """
                        SELECT username, SUM(jumlah) as total, COUNT(*) as transaksi, 
                        (SELECT status FROM payments p2 WHERE p2.username = p1.username ORDER BY id DESC LIMIT 1) as status_terakhir
                        FROM payments p1
                        GROUP BY username
                        ORDER BY total DESC
                    """.trimIndent()
                    
                    val rs = conn.createStatement().executeQuery(query)
                    val list = mutableListOf<PaymentSummary>()
                    var grandTotal = 0.0
                    var lunasCount = 0
                    
                    while (rs.next()) {
                        val total = rs.getDouble("total")
                        val status = rs.getString("status_terakhir") ?: "BELUM BAYAR"
                        
                        grandTotal += total
                        if (status == "LUNAS") lunasCount++
                        
                        list.add(PaymentSummary(
                            rs.getString("username") ?: "Unknown",
                            total,
                            rs.getInt("transaksi"),
                            status
                        ))
                    }
                    
                    withContext(Dispatchers.Main) {
                        paymentSummaries = list
                        totalPendapatan = grandTotal
                        totalLunas = lunasCount
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

    val displayedPayments = paymentSummaries.filter {
        searchQuery.isEmpty() || it.username.contains(searchQuery, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        // Header Banner
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Green700, Green500)))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Laporan Pembayaran Iuran", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Pantau rekapitulasi iuran kebersihan warga", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
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

        if (isLoading && paymentSummaries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green700)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Stats
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(Modifier.weight(1.3f), "Total Pendapatan", "Rp ${totalPendapatan.toInt()}", Icons.Default.Payments, BlueStat)
                        StatCard(Modifier.weight(0.7f), "User Lunas", totalLunas.toString(), Icons.Default.VerifiedUser, Green700)
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari warga...") },
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
                }

                item {
                    Text("Rekapitulasi per Warga", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.DarkGray)
                }

                items(displayedPayments) { payment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).background(Green700.copy(0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    payment.username.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Green700,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(payment.username, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${payment.totalTransaksi} kali pembayaran", fontSize = 12.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Rp ${payment.totalDibayar.toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Green700
                                )
                                Surface(
                                    color = if (payment.statusTerakhir == "LUNAS") Green700.copy(0.1f) else RedAccent.copy(0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        payment.statusTerakhir,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 9.sp,
                                        color = if (payment.statusTerakhir == "LUNAS") Green700 else RedAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
