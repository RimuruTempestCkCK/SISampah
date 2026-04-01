package com.example.sisampah.ui.screens.masyarakat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private val Green700 = Color(0xFF2E7D32)
private val Green500 = Color(0xFF4CAF50)
private val GreenSurface = Color(0xFFE8F5E9)
private val RedAccent = Color(0xFFE53935)
private val Amber500 = Color(0xFFFFC107)
private val Blue700 = Color(0xFF1565C0)

data class Tagihan(
    val id: Int,
    val bulan: String,
    val jumlah: Double,
    val status: String,
    val imageBukti: String? = null
)

@Composable
fun TagihanScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var tagihanList by remember { mutableStateOf<List<Tagihan>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    var selectedTagihanId by remember { mutableIntStateOf(-1) }

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
                            rs.getString("status"),
                            rs.getString("image_bukti")
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

    fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap != null) {
                val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                val targetWidth = 800
                val targetHeight = (targetWidth / ratio).toInt()
                val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
                val bytes = outputStream.toByteArray()
                Base64.encodeToString(bytes, Base64.DEFAULT)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && selectedTagihanId != -1) {
            isLoading = true
            scope.launch(Dispatchers.IO) {
                try {
                    val base64Image = uriToBase64(uri)
                    val conn = MySqlHelper.getConnection()
                    if (conn != null && base64Image != null) {
                        val stmt = conn.prepareStatement(
                            "UPDATE payments SET status = 'MENUNGGU', image_bukti = ? WHERE id = ?"
                        )
                        stmt.setString(1, base64Image)
                        stmt.setInt(2, selectedTagihanId)
                        stmt.executeUpdate()
                        conn.close()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Bukti pembayaran terkirim! Menunggu konfirmasi admin.", Toast.LENGTH_LONG).show()
                            loadData()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { 
                        Toast.makeText(context, "Gagal upload: ${e.message}", Toast.LENGTH_SHORT).show()
                        isLoading = false
                    }
                } finally {
                    selectedTagihanId = -1
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tagihan Pengangkutan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Pilih tagihan untuk upload bukti pembayaran", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
                IconButton(onClick = { 
                    focusManager.clearFocus()
                    loadData() 
                }) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
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
                        if (tagihan.status.uppercase() == "BELUM BAYAR") {
                            focusManager.clearFocus()
                            selectedTagihanId = tagihan.id
                            imageLauncher.launch("image/*")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TagihanItem(tagihan: Tagihan, onUploadBukti: () -> Unit) {
    val status = tagihan.status.uppercase()
    val isLunas = status == "LUNAS"
    val isWaiting = status == "MENUNGGU"
    val isBelumBayar = status == "BELUM BAYAR"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isBelumBayar) Modifier.clickable { onUploadBukti() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(
                            when {
                                isLunas -> GreenSurface
                                isWaiting -> Blue700.copy(0.1f)
                                else -> RedAccent.copy(0.1f)
                            }
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                isLunas -> Icons.Default.CheckCircle
                                isWaiting -> Icons.Default.HourglassEmpty
                                else -> Icons.Default.ReceiptLong
                            }, 
                            null, 
                            tint = when {
                                isLunas -> Green700
                                isWaiting -> Blue700
                                else -> RedAccent
                            }, 
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
                    color = when {
                        isLunas -> Green700.copy(0.1f)
                        isWaiting -> Blue700.copy(0.1f)
                        else -> Amber500.copy(0.1f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when {
                            isLunas -> Green700
                            isWaiting -> Blue700
                            else -> Amber500
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isBelumBayar) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Klik untuk Bayar", fontSize = 11.sp, color = Green700, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.CloudUpload, null, tint = Green700, modifier = Modifier.size(16.dp))
                    }
                } else if (isWaiting) {
                    Text("Menunggu Konfirmasi", fontSize = 11.sp, color = Blue700, fontWeight = FontWeight.Medium)
                } else if (isLunas) {
                    Text("Selesai", fontSize = 11.sp, color = Green700, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
