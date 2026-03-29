package com.example.sisampah.ui.screens.masyarakat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sisampah.data.MySqlHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

private val GreenPrimary = Color(0xFF2E7D32)

@Composable
fun LaporScreen(currentUsername: String = "Warga") {
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            getCurrentLocation(context, fusedLocationClient) { addr ->
                location = addr
            }
        } else {
            Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Migrasi Database ──
    fun migrateDatabase() {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val meta = conn.metaData
                    // Pastikan kolom image ada
                    val rsImg = meta.getColumns(null, null, "trash_reports", "image")
                    if (!rsImg.next()) {
                        conn.createStatement().executeUpdate("ALTER TABLE trash_reports ADD COLUMN image LONGTEXT NULL")
                    }
                    rsImg.close()
                    conn.close()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    LaunchedEffect(Unit) {
        migrateDatabase()
    }

    // Helper: Convert Uri to Base64
    fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) Base64.encodeToString(bytes, Base64.DEFAULT) else null
        } catch (e: Exception) {
            null
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Buat Laporan Baru", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            Text("Laporkan penumpukan sampah di sekitar Anda", fontSize = 13.sp, color = Color.Gray)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    // ── Foto Sampah ──
                    Text("Foto Sampah", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F0F0))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                            .clickable { imageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri == null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Pilih Foto Sampah", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            val bitmap = remember(imageUri) {
                                try {
                                    val inputStream = context.contentResolver.openInputStream(imageUri!!)
                                    BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                                } catch (e: Exception) { null }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    // ── Lokasi ──
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Lokasi Kejadian") },
                            leadingIcon = { Icon(Icons.Default.Place, null, tint = GreenPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLoading
                        )
                        
                        OutlinedButton(
                            onClick = {
                                val permissions = arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                                    getCurrentLocation(context, fusedLocationClient) { addr ->
                                        location = addr
                                    }
                                } else {
                                    permissionLauncher.launch(permissions)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(GreenPrimary))
                        ) {
                            Icon(Icons.Default.MyLocation, null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Dapatkan Lokasi (GPS)", color = GreenPrimary)
                        }
                    }

                    // ── Deskripsi ──
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi Kondisi") },
                        leadingIcon = { Icon(Icons.Default.Notes, null, tint = GreenPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    )

                    Button(
                        onClick = {
                            if (location.isBlank() || description.isBlank() || imageUri == null) {
                                Toast.makeText(context, "Harap lengkapi semua data dan foto!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val conn = MySqlHelper.getConnection()
                                    if (conn != null) {
                                        val imageBase64 = uriToBase64(imageUri!!)
                                        val query = "INSERT INTO trash_reports (reporterName, location, description, status, image, timestamp) VALUES (?, ?, ?, ?, ?, NOW())"
                                        val stmt = conn.prepareStatement(query)
                                        stmt.setString(1, currentUsername) // Menggunakan username yang sedang login
                                        stmt.setString(2, location)
                                        stmt.setString(3, description)
                                        stmt.setString(4, "Menunggu")
                                        stmt.setString(5, imageBase64)
                                        stmt.executeUpdate()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Laporan Terkirim!", Toast.LENGTH_SHORT).show()
                                            location = ""; description = ""; imageUri = null
                                        }
                                        conn.close()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                                } finally {
                                    withContext(Dispatchers.Main) { isLoading = false }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Kirim Laporan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getCurrentLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReceived: (String) -> Unit
) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
        if (loc != null) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                if (addresses?.isNotEmpty() == true) {
                    val address = addresses[0].getAddressLine(0)
                    onLocationReceived(address)
                } else {
                    onLocationReceived("${loc.latitude}, ${loc.longitude}")
                }
            } catch (e: Exception) {
                onLocationReceived("${loc.latitude}, ${loc.longitude}")
            }
        } else {
            Toast.makeText(context, "Gagal mendapatkan lokasi GPS. Pastikan GPS aktif.", Toast.LENGTH_SHORT).show()
        }
    }
}
