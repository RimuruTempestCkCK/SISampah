package com.example.sisampah.ui.screens.petugas_dokumentasi

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.util.Base64
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.sisampah.data.MySqlHelper
import com.example.sisampah.model.TrashReport
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URISyntaxException

private val Green700 = Color(0xFF2E7D32)
private val Green500 = Color(0xFF4CAF50)
private val RedAccent = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetugasUpdateLaporan() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reports by remember { mutableStateOf<List<TrashReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDetailDialog by remember { mutableStateOf<TrashReport?>(null) }
    
    var currentUserLocation by remember { mutableStateOf<Location?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { 
                    if (currentUserLocation == null || it.distanceTo(currentUserLocation!!) > 3f) {
                        currentUserLocation = it 
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startLocationUpdates(context, fusedLocationClient, locationCallback)
        }
    }

    LaunchedEffect(Unit) {
        loadData(context, scope) { list -> reports = list; isLoading = false }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates(context, fusedLocationClient, locationCallback)
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    DisposableEffect(Unit) {
        onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
    }

    if (showDetailDialog != null) {
        val report = showDetailDialog!!
        AlertDialog(
            onDismissRequest = { showDetailDialog = null },
            title = { Text("Detail Laporan Dokumentasi") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text("Pelapor: ${report.reporterName}", fontWeight = FontWeight.Bold) }
                    item { Text("Lokasi Sampah: ${report.location}") }
                    item { Text("Keterangan: ${report.description}") }
                    item { Text("Status: ${report.status}") }
                    
                    item {
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
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    item {
                        Divider(Modifier.padding(vertical = 8.dp))
                        Text("Rute Navigasi Live:", fontWeight = FontWeight.Bold)
                        
                        Card(
                            Modifier.fillMaxWidth().height(350.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0))
                        ) {
                            if (currentUserLocation != null) {
                                MapWebView(
                                    petugasLat = currentUserLocation!!.latitude,
                                    petugasLng = currentUserLocation!!.longitude,
                                    reportLocation = report.location
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Green700)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Mengunci Sinyal GPS...", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (report.status == "Menunggu") {
                    Button(onClick = {
                        updateStatus(report.id, "Selesai", context, scope) {
                            loadData(context, scope) { list -> reports = list; isLoading = false }
                        }
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

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Green700, Green500)))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Update Laporan Sampah", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Kelola status laporan dari masyarakat", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
                IconButton(onClick = { 
                    isLoading = true
                    loadData(context, scope) { list -> 
                        reports = list; isLoading = false 
                        Toast.makeText(context, "Berhasil: Laporan diperbarui", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        if (isLoading && reports.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green700)
            }
        } else if (reports.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada laporan", color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reports) { report ->
                    Card(
                        onClick = { showDetailDialog = report },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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

private fun startLocationUpdates(context: Context, client: FusedLocationProviderClient, callback: LocationCallback) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
        .setMinUpdateIntervalMillis(2000)
        .build()
    client.requestLocationUpdates(request, callback, context.mainLooper)
}

private fun loadData(context: Context, scope: kotlinx.coroutines.CoroutineScope, onResult: (List<TrashReport>) -> Unit) {
    scope.launch(Dispatchers.IO) {
        try {
            val conn = MySqlHelper.getConnection()
            if (conn != null) {
                val query = """
                    SELECT tr.*, u.nama as real_name 
                    FROM trash_reports tr
                    LEFT JOIN users u ON tr.reporterName = u.username
                    ORDER BY tr.id DESC
                """.trimIndent()
                val rs = conn.createStatement().executeQuery(query)
                val list = mutableListOf<TrashReport>()
                while (rs.next()) {
                    val realName = rs.getString("real_name") ?: rs.getString("reporterName")
                    list.add(TrashReport(
                        id = rs.getInt("id").toString(),
                        reporterName = realName ?: "Unknown",
                        location = rs.getString("location") ?: "",
                        description = rs.getString("description") ?: "",
                        status = rs.getString("status") ?: "Menunggu",
                        timestamp = rs.getString("timestamp") ?: "",
                        image = rs.getString("image")
                    ))
                }
                withContext(Dispatchers.Main) { onResult(list) }
                conn.close()
            }
        } catch (e: Exception) {}
    }
}

private fun updateStatus(id: String, newStatus: String, context: Context, scope: kotlinx.coroutines.CoroutineScope, onComplete: () -> Unit) {
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
                    Toast.makeText(context, "Berhasil: Laporan ditandai Selesai!", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            }
        } catch (e: Exception) {}
    }
}

@Composable
fun MapWebView(petugasLat: Double, petugasLng: Double, reportLocation: String) {
    val encodedAddr = Uri.encode(reportLocation)
    val mapUrl = "https://maps.google.com/maps?saddr=$petugasLat,$petugasLng&daddr=$encodedAddr&output=embed"
    
    val htmlData = """
        <html>
        <body style="margin:0;padding:0;">
            <iframe width="100%" height="100%" frameborder="0" style="border:0" 
                src="$mapUrl" allowfullscreen>
            </iframe>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                loadDataWithBaseURL("https://maps.google.com", htmlData, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { webView ->
            val lastLoc = webView.tag as? String
            val currentLocStr = "$petugasLat,$petugasLng"
            if (lastLoc != currentLocStr) {
                webView.tag = currentLocStr
                webView.loadDataWithBaseURL("https://maps.google.com", htmlData, "text/html", "UTF-8", null)
            }
        }
    )
}
