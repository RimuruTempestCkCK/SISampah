package com.example.sisampah.ui.screens.masyarakat

import android.graphics.BitmapFactory
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
import com.example.sisampah.data.MySqlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val GreenPrimary = Color(0xFF2E7D32)

@Composable
fun LaporScreen() {
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    // ── Migrasi Database ──
    fun migrateDatabase() {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val meta = conn.metaData
                    val rs = meta.getColumns(null, null, "trash_reports", "image")
                    if (!rs.next()) {
                        val stmt = conn.createStatement()
                        stmt.executeUpdate("ALTER TABLE trash_reports ADD COLUMN image LONGTEXT NULL")
                        stmt.close()
                    }
                    rs.close()
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
                            .clickable { launcher.launch("image/*") },
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
                                val inputStream = context.contentResolver.openInputStream(imageUri!!)
                                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
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
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Lokasi Kejadian") },
                        leadingIcon = { Icon(Icons.Default.Place, null, tint = GreenPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    )

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
                                        stmt.setString(1, "Warga") 
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
