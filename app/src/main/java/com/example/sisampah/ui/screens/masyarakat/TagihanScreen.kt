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
import java.util.Locale

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
    val imageBukti: String? = null,
    val vaNumber: String = ""
)

@Composable
fun TagihanScreen(username: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var tagihanList by remember { mutableStateOf<List<Tagihan>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var userWaterType by remember { mutableStateOf("PDAM") }
    
    var selectedTagihanId by remember { mutableIntStateOf(-1) }

    val bulanList = listOf(
        "Januari 2026", "Februari 2026", "Maret 2026", "April 2026", 
        "Mei 2026", "Juni 2026", "Juli 2026", "Agustus 2026", 
        "September 2026", "Oktober 2026", "November 2026", "Desember 2026"
    )

    fun loadData() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    var userId: Int? = null
                    var waterType = "PDAM"
                    val userStmt = conn.prepareStatement("SELECT id, water_type FROM users WHERE username = ?")
                    userStmt.setString(1, username)
                    val userRs = userStmt.executeQuery()
                    if (userRs.next()) {
                        userId = userRs.getInt("id")
                        waterType = userRs.getString("water_type") ?: "PDAM"
                    }
                    userRs.close()
                    userStmt.close()

                    if (userId == null) {
                        withContext(Dispatchers.Main) { 
                            errorMsg = "User tidak ditemukan"
                            isLoading = false 
                        }
                        conn.close()
                        return@launch
                    }

                    val currentVa = "8888${String.format(Locale.US, "%04d", userId)}"
                    val amount = if (waterType == "PDAM") 25000.0 else 15000.0

                    bulanList.forEach { bulan ->
                        val checkStmt = conn.prepareStatement("SELECT id FROM payments WHERE bulan = ? AND username = ?")
                        checkStmt.setString(1, bulan)
                        checkStmt.setString(2, username)
                        val checkRs = checkStmt.executeQuery()
                        if (!checkRs.next()) {
                            val insertStmt = conn.prepareStatement("INSERT INTO payments (username, bulan, jumlah, status) VALUES (?, ?, ?, ?)")
                            insertStmt.setString(1, username)
                            insertStmt.setString(2, bulan)
                            insertStmt.setDouble(3, amount)
                            insertStmt.setString(4, "BELUM BAYAR")
                            insertStmt.executeUpdate()
                            insertStmt.close()
                        } else {
                            val existingId = checkRs.getInt("id")
                            val updateAmountStmt = conn.prepareStatement("UPDATE payments SET jumlah = ? WHERE id = ? AND status = 'BELUM BAYAR'")
                            updateAmountStmt.setDouble(1, amount)
                            updateAmountStmt.setInt(2, existingId)
                            updateAmountStmt.executeUpdate()
                            updateAmountStmt.close()
                        }
                        checkRs.close()
                        checkStmt.close()
                    }

                    val query = "SELECT * FROM payments WHERE username = ? AND bulan LIKE '%2026' ORDER BY FIELD(bulan, 'Januari 2026', 'Februari 2026', 'Maret 2026', 'April 2026', 'Mei 2026', 'Juni 2026', 'Juli 2026', 'Agustus 2026', 'September 2026', 'Oktober 2026', 'November 2026', 'Desember 2026')"
                    val fetchStmt = conn.prepareStatement(query)
                    fetchStmt.setString(1, username)
                    val rs = fetchStmt.executeQuery()
                    val list = mutableListOf<Tagihan>()
                    while (rs.next()) {
                        list.add(Tagihan(
                            rs.getInt("id"),
                            rs.getString("bulan"),
                            rs.getDouble("jumlah"),
                            rs.getString("status"),
                            rs.getString("image_bukti"),
                            currentVa
                        ))
                    }
                    rs.close()
                    fetchStmt.close()
                    conn.close()
                    withContext(Dispatchers.Main) { 
                        tagihanList = list
                        userWaterType = waterType
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
                    Text("Daftar tagihan iuran Januari - Desember 2026", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
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
                    TagihanItem(tagihan, userWaterType) {
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
fun TagihanItem(tagihan: Tagihan, waterType: String, onUploadBukti: () -> Unit) {
    val status = tagihan.status.uppercase()
    val isLunas = status == "LUNAS"
    val isWaiting = status == "MENUNGGU"
    val isBelumBayar = status == "BELUM BAYAR"
    val isPdam = waterType == "PDAM"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isBelumBayar && !isPdam) Modifier.clickable { onUploadBukti() } else Modifier),
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
                                isBelumBayar && isPdam -> GreenSurface
                                else -> RedAccent.copy(0.1f)
                            }
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                isLunas -> Icons.Default.CheckCircle
                                isWaiting -> Icons.Default.HourglassEmpty
                                isBelumBayar && isPdam -> Icons.Default.Sync
                                else -> Icons.Default.ReceiptLong
                            }, 
                            null, 
                            tint = if (isLunas || (isBelumBayar && isPdam)) Green700 else if (isWaiting) Blue700 else RedAccent, 
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(tagihan.bulan, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isLunas || (isBelumBayar && isPdam)) Green700 else Color.DarkGray)
                        Text("Iuran Kebersihan", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Text("Rp ${tagihan.jumlah.toInt()}", fontWeight = FontWeight.Bold, color = if (isLunas || (isBelumBayar && isPdam)) Green700 else RedAccent, fontSize = 17.sp)
            }
            
            Divider(Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
            
            if (isBelumBayar) {
                if (isPdam) {
                    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text("Metode Pembayaran:", fontSize = 12.sp, color = Color.Gray)
                        Text("Terintegrasi PDAM", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Green700)
                        Text("Iuran otomatis masuk ke tagihan PDAM Anda", fontSize = 11.sp, color = Color.Gray)
                    }
                } else {
                    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Text("No. Virtual Account (VA):", fontSize = 12.sp, color = Color.Gray)
                        Text(tagihan.vaNumber, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                        Text("Bank Transfer (Mandiri/BCA)", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = when {
                        isLunas -> Green700.copy(0.1f)
                        isWaiting -> Blue700.copy(0.1f)
                        isBelumBayar && isPdam -> Green700.copy(0.1f)
                        else -> Amber500.copy(0.1f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        if (isBelumBayar && isPdam) "VIA PDAM" else status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when {
                            isLunas -> Green700
                            isWaiting -> Blue700
                            isBelumBayar && isPdam -> Green700
                            else -> Amber500
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isBelumBayar) {
                    if (isPdam) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Otomatis via PDAM", fontSize = 11.sp, color = Green700, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Info, null, tint = Green700, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Klik untuk Upload Bukti", fontSize = 11.sp, color = Green700, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.CloudUpload, null, tint = Green700, modifier = Modifier.size(16.dp))
                        }
                    }
                } else if (isWaiting) {
                    Text("Menunggu Konfirmasi Admin", fontSize = 11.sp, color = Blue700, fontWeight = FontWeight.Medium)
                } else if (isLunas) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, null, tint = Green700, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tervalidasi", fontSize = 11.sp, color = Green700, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
