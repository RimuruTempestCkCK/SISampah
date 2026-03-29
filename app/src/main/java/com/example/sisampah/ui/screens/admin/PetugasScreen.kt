@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.sisampah.ui.screens.admin

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── Warna ─────────────────────────────────────────────────────────────────────
private val Green700  = Color(0xFF2E7D32)
private val Green500  = Color(0xFF4CAF50)
private val Blue700   = Color(0xFF1565C0)
private val RedAccent = Color(0xFFE53935)

// ─── Model ─────────────────────────────────────────────────────────────────────
data class Petugas(
    val id: Int,
    val username: String,
    val nama: String          // Tambahan: Nama Lengkap
)

// ═══════════════════════════════════════════════════════════════════════════════
//  SCREEN UTAMA
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun PetugasScreen() {
    val scope = rememberCoroutineScope()

    var petugasList by remember { mutableStateOf<List<Petugas>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var errorMsg    by remember { mutableStateOf<String?>(null) }
    
    // State untuk detail petugas
    var selectedPetugas by remember { mutableStateOf<Petugas?>(null) }

    // ── Load Data ──
    fun loadData() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                val list = mutableListOf<Petugas>()
                if (conn != null) {
                    val stmt = conn.prepareStatement(
                        "SELECT id, username, nama FROM users WHERE role = 'PETUGAS_LPS' ORDER BY id ASC"
                    )
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        list.add(
                            Petugas(
                                id       = rs.getInt("id"),
                                username = rs.getString("username"),
                                nama     = rs.getString("nama") ?: ""
                            )
                        )
                    }
                    rs.close(); stmt.close(); conn.close()
                } else {
                    withContext(Dispatchers.Main) { errorMsg = "Gagal terhubung ke database." }
                }
                withContext(Dispatchers.Main) {
                    petugasList = list
                    errorMsg    = null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { errorMsg = "Error: ${e.message}" }
            } finally {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val displayed = petugasList.filter {
        searchQuery.isEmpty() || 
        it.username.contains(searchQuery, ignoreCase = true) || 
        it.nama.contains(searchQuery, ignoreCase = true)
    }

    // ════════════════════════════════════════════
    //  UI
    // ════════════════════════════════════════════
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {

        // ── Header Banner ──
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Green700, Green500)))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Data Petugas",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp
                    )
                    Text(
                        "${petugasList.size} petugas terdaftar",
                        color    = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = { loadData() }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint     = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // ── Error Banner ──
        AnimatedVisibility(visible = errorMsg != null, enter = fadeIn(), exit = fadeOut()) {
            errorMsg?.let { msg ->
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = RedAccent.copy(0.1f)),
                    shape  = RoundedCornerShape(10.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, null, tint = RedAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(msg, color = RedAccent, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { errorMsg = null }) {
                            Icon(Icons.Default.Close, null, tint = RedAccent)
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))

            // ── Search Bar ──
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder   = { Text("Cari nama atau username...") },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                trailingIcon  = {
                    if (searchQuery.isNotEmpty())
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                },
                modifier   = Modifier.fillMaxWidth(),
                shape      = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Green700,
                    unfocusedBorderColor    = Color.LightGray,
                    focusedContainerColor   = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(Modifier.height(16.dp))

            // ── Konten ──
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Green700)
                            Spacer(Modifier.height(12.dp))
                            Text("Memuat data...", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
                displayed.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PersonSearch,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint     = Color.LightGray
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (searchQuery.isEmpty()) "Belum ada petugas terdaftar."
                                else "Petugas tidak ditemukan.",
                                color = Color.Gray, fontSize = 14.sp
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(displayed, key = { it.id }) { petugas ->
                            PetugasItemCard(
                                petugas = petugas,
                                onClick = { selectedPetugas = petugas }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
    
    // ════════════════════════════════════════════
    //  DIALOG DETAIL
    // ════════════════════════════════════════════
    selectedPetugas?.let { petugas ->
        PetugasDetailDialog(
            petugas = petugas,
            onDismiss = { selectedPetugas = null }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  KOMPONEN: Kartu satu petugas
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PetugasItemCard(petugas: Petugas, onClick: () -> Unit) {
    val petugasColor = Blue700

    Card(
        onClick = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Avatar inisial ──
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(petugasColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (if (petugas.nama.isNotEmpty()) petugas.nama else petugas.username).first().uppercaseChar().toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = petugasColor
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                // ── Nama & Username ──
                Text(
                    text       = if (petugas.nama.isNotEmpty()) petugas.nama else petugas.username,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp
                )
                if (petugas.nama.isNotEmpty()) {
                    Text(
                        text     = petugas.username,
                        fontSize = 12.sp,
                        color    = Color.Gray
                    )
                }
                
                Spacer(Modifier.height(5.dp))
                
                // ── ID & Role ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(petugasColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "Petugas LPS",
                            color      = petugasColor,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "#${petugas.id}",
                        fontSize = 11.sp,
                        color    = Color.LightGray
                    )
                }
            }

            // ── Ikon panah detail ──
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = "Detail",
                tint     = Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  KOMPONEN: Dialog Detail Petugas
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PetugasDetailDialog(petugas: Petugas, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Blue700.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Badge, null, tint = Blue700, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Detail Petugas", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailItem(label = "Nama Lengkap", value = if (petugas.nama.isNotEmpty()) petugas.nama else "-")
                DetailItem(label = "Username", value = petugas.username)
                DetailItem(label = "ID Petugas", value = "#${petugas.id}")
                DetailItem(label = "Jabatan", value = "Petugas LPS")
                DetailItem(label = "Status", value = "Aktif")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors  = ButtonDefaults.buttonColors(containerColor = Green700),
                shape   = RoundedCornerShape(8.dp)
            ) {
                Text("Tutup")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Divider(Modifier.padding(top = 4.dp), thickness = 0.5.dp, color = Color.LightGray)
    }
}
