package com.example.sisampah.ui.screens.masyarakat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalFocusManager
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
import java.io.ByteArrayOutputStream
import java.util.*

private val Green700 = Color(0xFF2E7D32)
private val Green500 = Color(0xFF4CAF50)
private val RedAccent = Color(0xFFE53935)

@Composable
fun LaporScreen(currentUsername: String = "Warga") {
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                capturedBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat gambar dari galeri", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membuka kamera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
        }
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

    fun bitmapToBase64(bitmap: Bitmap): String? {
        return try {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val targetWidth = 800
            val targetHeight = (targetWidth / ratio).toInt().coerceAtLeast(1)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Pilih Sumber Gambar") },
            text = { Text("Ambil foto dari kamera atau pilih dari galeri?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        try {
                            cameraLauncher.launch()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membuka kamera", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Text("Kamera", color = Green700)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    galleryLauncher.launch("image/*")
                }) {
                    Text("Galeri", color = Green700)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Green700, Green500)))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Column {
                Text("Buat Laporan Baru", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Laporkan penumpukan sampah di sekitar Anda", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }

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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        
                        Text("Foto Sampah", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0F0F0))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                                .clickable { 
                                    focusManager.clearFocus()
                                    showDialog = true 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (capturedBitmap == null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Klik untuk Ambil Foto / Galeri", color = Color.Gray, fontSize = 13.sp)
                                }
                            } else {
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = "Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("Lokasi Kejadian") },
                                leadingIcon = { Icon(Icons.Default.Place, null, tint = Green700) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isLoading
                            )
                            
                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
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
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Green700))
                            ) {
                                Icon(Icons.Default.MyLocation, null, tint = Green700, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Dapatkan Lokasi (GPS)", color = Green700)
                            }
                        }

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Deskripsi Kondisi") },
                            leadingIcon = { Icon(Icons.Default.Notes, null, tint = Green700) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isLoading
                        )

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (location.isBlank() || description.isBlank() || capturedBitmap == null) {
                                    errorMsg = "Peringatan: Harap lengkapi semua data dan foto!"
                                    return@Button
                                }
                                isLoading = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val conn = MySqlHelper.getConnection()
                                        if (conn != null) {
                                            val imageBase64 = bitmapToBase64(capturedBitmap!!)
                                            val query = "INSERT INTO trash_reports (reporterName, location, description, status, image, timestamp) VALUES (?, ?, ?, ?, ?, NOW())"
                                            val stmt = conn.prepareStatement(query)
                                            stmt.setString(1, currentUsername)
                                            stmt.setString(2, location)
                                            stmt.setString(3, description)
                                            stmt.setString(4, "Menunggu")
                                            stmt.setString(5, imageBase64)
                                            stmt.executeUpdate()
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Berhasil: Laporan Anda telah terkirim!", Toast.LENGTH_SHORT).show()
                                                location = ""; description = ""; capturedBitmap = null; errorMsg = null
                                            }
                                            conn.close()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) { errorMsg = "Gagal: ${e.message}" }
                                    } finally {
                                        withContext(Dispatchers.Main) { isLoading = false }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Green700),
                            enabled = !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else {
                                Icon(Icons.Default.Send, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Kirim Laporan", fontWeight = FontWeight.Bold)
                            }
                        }
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
            Toast.makeText(context, "Peringatan: Gagal mendapatkan lokasi GPS. Pastikan GPS aktif.", Toast.LENGTH_SHORT).show()
        }
    }
}
