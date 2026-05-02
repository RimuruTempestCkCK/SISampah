package com.example.sisampah.ui.screens.dlh

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
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
import java.text.SimpleDateFormat
import java.util.*

private val Green700 = Color(0xFF2E7D32)
private val Green500 = Color(0xFF4CAF50)
private val RedAccent = Color(0xFFE53935)

data class PaymentSummary(
    val username: String,
    val totalDibayar: Double,
    val totalTransaksi: Int,
    val statusTerakhir: String,
    val bulanBelumBayar: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentReportScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var paymentSummaries by remember { mutableStateOf<List<PaymentSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalPendapatan by remember { mutableStateOf(0.0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    fun loadData() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val query = """
                        SELECT username, SUM(CASE WHEN status = 'LUNAS' THEN jumlah ELSE 0 END) as total, 
                        COUNT(CASE WHEN status = 'LUNAS' THEN 1 END) as transaksi, 
                        (SELECT status FROM payments p2 WHERE p2.username = p1.username ORDER BY id DESC LIMIT 1) as status_terakhir
                        FROM payments p1
                        GROUP BY username
                        ORDER BY total DESC
                    """.trimIndent()
                    
                    val rs = conn.createStatement().executeQuery(query)
                    val list = mutableListOf<PaymentSummary>()
                    var grandTotal = 0.0
                    
                    while (rs.next()) {
                        val user = rs.getString("username") ?: "Unknown"
                        val total = rs.getDouble("total")
                        
                        grandTotal += total

                        // Get unpaid months for this user
                        val unpaidList = mutableListOf<String>()
                        val unpaidStmt = conn.prepareStatement("SELECT bulan FROM payments WHERE username = ? AND status != 'LUNAS'")
                        unpaidStmt.setString(1, user)
                        val unpaidRs = unpaidStmt.executeQuery()
                        while (unpaidRs.next()) {
                            unpaidList.add(unpaidRs.getString("bulan"))
                        }
                        unpaidRs.close()
                        unpaidStmt.close()
                        
                        list.add(PaymentSummary(
                            user,
                            total,
                            rs.getInt("transaksi"),
                            rs.getString("status_terakhir") ?: "BELUM BAYAR",
                            unpaidList
                        ))
                    }
                    
                    withContext(Dispatchers.Main) {
                        paymentSummaries = list
                        totalPendapatan = grandTotal
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
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Green700, Green500)))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Laporan Pembayaran Iuran", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Row {
                    IconButton(onClick = { 
                        if (paymentSummaries.isNotEmpty()) {
                            printPaymentReport(context, paymentSummaries, totalPendapatan)
                        } else {
                            Toast.makeText(context, "Tidak ada data untuk dicetak", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Print, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
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
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                            
                            if (payment.bulanBelumBayar.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Divider(thickness = 0.5.dp, color = Color.LightGray)
                                Spacer(Modifier.height(8.dp))
                                Text("Bulan Belum Bayar:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RedAccent)
                                Text(
                                    payment.bulanBelumBayar.joinToString(", "),
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

fun printPaymentReport(context: Context, data: List<PaymentSummary>, totalPendapatan: Double) {
    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "Laporan_Pembayaran_SISAMPAH"
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }

    val html = StringBuilder()
    html.append("<html><head><style>")
    html.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; font-family: sans-serif; }")
    html.append("th, td { border: 1px solid #ddd; padding: 10px; text-align: left; font-size: 12px; }")
    html.append("th { background-color: #2E7D32; color: white; }")
    html.append("h2 { color: #2E7D32; text-align: center; margin-bottom: 5px; }")
    html.append(".header { text-align: center; margin-bottom: 30px; }")
    html.append(".total { font-weight: bold; background-color: #f9f9f9; }")
    html.append("</style></head><body>")
    
    html.append("<div class='header'>")
    html.append("<h2>LAPORAN REKAPITULASI PEMBAYARAN IURAN</h2>")
    html.append("<p style='margin:0;'>Sistem Informasi Pengelolaan Sampah (SISAMPAH)</p>")
    html.append("<p style='margin:5px 0;'>Dinas Lingkungan Hidup</p>")
    html.append("</div>")

    html.append("<p><b>Total Pendapatan:</b> Rp ${totalPendapatan.toInt()}</p>")

    html.append("<table>")
    html.append("<tr><th>No</th><th>Nama Warga</th><th>Total Bayar</th><th>Transaksi</th><th>Status</th><th>Bulan Belum Bayar</th></tr>")
    
    data.forEachIndexed { index, p ->
        html.append("<tr>")
        html.append("<td>${index + 1}</td>")
        html.append("<td>${p.username}</td>")
        html.append("<td>Rp ${p.totalDibayar.toInt()}</td>")
        html.append("<td>${p.totalTransaksi}</td>")
        html.append("<td>${p.statusTerakhir}</td>")
        html.append("<td>${if (p.bulanBelumBayar.isEmpty()) "-" else p.bulanBelumBayar.joinToString(", ")}</td>")
        html.append("</tr>")
    }
    
    html.append("</table>")
    html.append("<p style='margin-top: 30px; font-size: 10px; color: #666;'>Dicetak secara otomatis melalui aplikasi SISAMPAH pada: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}</p>")
    html.append("</body></html>")

    webView.loadDataWithBaseURL(null, html.toString(), "text/html", "UTF-8", null)
}
