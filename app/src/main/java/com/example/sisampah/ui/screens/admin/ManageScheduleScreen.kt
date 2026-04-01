@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.sisampah.ui.screens.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.window.Dialog
import com.example.sisampah.data.MySqlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// ─── Warna ─────────────────────────────────────────────────────────────────────
private val Green700    = Color(0xFF2E7D32)
private val Green500    = Color(0xFF4CAF50)
private val RedAccent   = Color(0xFFE53935)
private val Blue700     = Color(0xFF1565C0)
private val Amber500    = Color(0xFFFFC107)

// ─── Model ─────────────────────────────────────────────────────────────────────
data class Schedule(
    val id: Int,
    val lokasi: String,
    val hari: String,
    val jam: String,
    val petugasId: Int?,
    val petugasNama: String?
)

private val HARI = listOf(
    "SEMUA", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"
)

@Composable
fun ManageScheduleScreen() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var schedules   by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var petugasList by remember { mutableStateOf<List<Petugas>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterHari  by remember { mutableStateOf("SEMUA") }

    var showAddDialog    by remember { mutableStateOf(false) }
    var scheduleToEdit   by remember { mutableStateOf<Schedule?>(null) }
    var scheduleToDelete by remember { mutableStateOf<Schedule?>(null) }
    var errorMsg         by remember { mutableStateOf<String?>(null) }

    fun loadPetugas() {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                val list = mutableListOf<Petugas>()
                if (conn != null) {
                    val stmt = conn.prepareStatement("SELECT id, username, nama FROM users WHERE role = 'PETUGAS_LPS'")
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        list.add(Petugas(rs.getInt("id"), rs.getString("username"), rs.getString("nama") ?: ""))
                    }
                    rs.close(); stmt.close(); conn.close()
                }
                withContext(Dispatchers.Main) { petugasList = list }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun loadData() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                val list = mutableListOf<Schedule>()
                if (conn != null) {
                    val sql = """
                        SELECT s.*, u.nama as petugas_nama 
                        FROM schedules s 
                        LEFT JOIN users u ON s.petugas_id = u.id 
                        ORDER BY s.id ASC
                    """.trimIndent()
                    val rs = conn.createStatement().executeQuery(sql)
                    while (rs.next()) {
                        list.add(
                            Schedule(
                                id          = rs.getInt("id"),
                                lokasi      = rs.getString("lokasi"),
                                hari        = rs.getString("hari"),
                                jam         = rs.getString("jam"),
                                petugasId   = if (rs.getObject("petugas_id") != null) rs.getInt("petugas_id") else null,
                                petugasNama = rs.getString("petugas_nama")
                            )
                        )
                    }
                    rs.close(); conn.close()
                }
                withContext(Dispatchers.Main) { schedules = list; errorMsg = null }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { errorMsg = "Error: ${e.message}" }
            } finally {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    // ── Fungsi DB dengan pengecekan bentrok ──
    fun getConflictMessage(lokasi: String, hari: String, jam: String, petugasId: Int?, excludeId: Int? = null): String? {
        // 1. Cek bentrok Petugas
        if (petugasId != null) {
            val petugasConflict = schedules.find { 
                it.petugasId == petugasId && it.hari == hari && it.jam == jam && it.id != excludeId 
            }
            if (petugasConflict != null) {
                return "Petugas tersebut sudah memiliki jadwal di ${petugasConflict.lokasi} pada waktu yang sama!"
            }
        }
        
        // 2. Cek bentrok Lokasi
        val lokasiConflict = schedules.find {
            it.lokasi.equals(lokasi, ignoreCase = true) && it.hari == hari && it.jam == jam && it.id != excludeId
        }
        if (lokasiConflict != null) {
            return "Lokasi ini sudah memiliki jadwal pengangkutan pada waktu tersebut!"
        }
        
        return null
    }

    fun insertSchedule(lokasi: String, hari: String, jam: String, petugasId: Int?, onDone: (Boolean) -> Unit) {
        val conflict = getConflictMessage(lokasi, hari, jam, petugasId)
        if (conflict != null) {
            Toast.makeText(context, conflict, Toast.LENGTH_LONG).show()
            onDone(false)
            return
        }
        
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val stmt = conn.prepareStatement("INSERT INTO schedules (lokasi, hari, jam, petugas_id) VALUES (?, ?, ?, ?)")
                    stmt.setString(1, lokasi)
                    stmt.setString(2, hari)
                    stmt.setString(3, jam)
                    if (petugasId != null) stmt.setInt(4, petugasId) else stmt.setNull(4, java.sql.Types.INTEGER)
                    stmt.executeUpdate()
                    stmt.close(); conn.close()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Jadwal berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        onDone(true)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    onDone(false)
                }
            }
        }
    }

    fun updateSchedule(id: Int, lokasi: String, hari: String, jam: String, petugasId: Int?, onDone: (Boolean) -> Unit) {
        val conflict = getConflictMessage(lokasi, hari, jam, petugasId, excludeId = id)
        if (conflict != null) {
            Toast.makeText(context, conflict, Toast.LENGTH_LONG).show()
            onDone(false)
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val stmt = conn.prepareStatement("UPDATE schedules SET lokasi = ?, hari = ?, jam = ?, petugas_id = ? WHERE id = ?")
                    stmt.setString(1, lokasi)
                    stmt.setString(2, hari)
                    stmt.setString(3, jam)
                    if (petugasId != null) stmt.setInt(4, petugasId) else stmt.setNull(4, java.sql.Types.INTEGER)
                    stmt.setInt(5, id)
                    stmt.executeUpdate()
                    stmt.close(); conn.close()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Jadwal berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        onDone(true)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    onDone(false)
                }
            }
        }
    }

    fun deleteSchedule(id: Int, onDone: (Boolean) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val stmt = conn.prepareStatement("DELETE FROM schedules WHERE id = ?")
                    stmt.setInt(1, id)
                    stmt.executeUpdate()
                    stmt.close(); conn.close()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Jadwal berhasil dihapus!", Toast.LENGTH_SHORT).show()
                        onDone(true)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    onDone(false)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadPetugas()
        loadData()
    }

    val displayedSchedules = schedules.filter {
        (filterHari == "SEMUA" || it.hari == filterHari) &&
                (searchQuery.isEmpty() || it.lokasi.contains(searchQuery, ignoreCase = true))
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Green700, Green500)))
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Kelola Jadwal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("${schedules.size} jadwal tersedia", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
                IconButton(onClick = { loadData() }) {
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

        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari lokasi...") },
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

            Spacer(Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(HARI) { hari ->
                    FilterChip(
                        selected = filterHari == hari,
                        onClick = { filterHari = hari },
                        label = { Text(hari, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Green700,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green700)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Tambah Jadwal Baru", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Green700)
                    }
                }
                displayedSchedules.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada jadwal ditemukan", color = Color.Gray)
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(displayedSchedules, key = { it.id }) { schedule ->
                            ScheduleItemCard(
                                schedule = schedule,
                                onEdit   = { scheduleToEdit = schedule },
                                onDelete = { scheduleToDelete = schedule }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ScheduleFormDialog(
            title = "Tambah Jadwal",
            initial = null,
            petugasList = petugasList,
            onDismiss = { showAddDialog = false },
            onSave = { lokasi, hari, jam, petugasId ->
                insertSchedule(lokasi, hari, jam, petugasId) { if (it) { loadData(); showAddDialog = false } }
            }
        )
    }

    scheduleToEdit?.let { s ->
        ScheduleFormDialog(
            title = "Edit Jadwal",
            initial = s,
            petugasList = petugasList,
            onDismiss = { scheduleToEdit = null },
            onSave = { lokasi, hari, jam, petugasId ->
                updateSchedule(s.id, lokasi, hari, jam, petugasId) { if (it) { loadData(); scheduleToEdit = null } }
            }
        )
    }

    scheduleToDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = { Text("Hapus Jadwal", fontWeight = FontWeight.Bold) },
            text = { Text("Yakin ingin menghapus jadwal di ${s.lokasi} hari ${s.hari}?") },
            confirmButton = {
                Button(
                    onClick = { deleteSchedule(s.id) { if (it) { loadData(); scheduleToDelete = null } } },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                ) { Text("Hapus") }
            },
            dismissButton = {
                OutlinedButton(onClick = { scheduleToDelete = null }) { Text("Batal") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ScheduleItemCard(schedule: Schedule, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Green700.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Event, null, tint = Green700)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(schedule.lokasi, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("${schedule.hari} • ${schedule.jam}", fontSize = 12.sp, color = Color.Gray)
                if (schedule.petugasNama != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.Person, null, tint = Blue700, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(schedule.petugasNama, fontSize = 12.sp, color = Blue700, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, null, tint = Blue700, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null, tint = RedAccent, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ScheduleFormDialog(
    title: String,
    initial: Schedule?,
    petugasList: List<Petugas>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int?) -> Unit
) {
    var lokasi by remember { mutableStateOf(initial?.lokasi ?: "") }
    var hari   by remember { mutableStateOf(initial?.hari ?: "Senin") }
    var jam    by remember { mutableStateOf(initial?.jam ?: "") }
    var selectedPetugasId by remember { mutableStateOf(initial?.petugasId) }

    var hariExpanded by remember { mutableStateOf(false) }
    var petugasExpanded by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = if (jam.isNotEmpty() && jam.contains(":")) jam.split(":")[0].toInt() else calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = if (jam.isNotEmpty() && jam.contains(":")) jam.split(":")[1].toInt() else calendar.get(Calendar.MINUTE),
        is24Hour = true
    )

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Pilih Jam", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Green700)
                    Spacer(Modifier.height(20.dp))
                    TimePicker(state = timePickerState)
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Batal", color = Color.Gray) }
                        TextButton(onClick = {
                            jam = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) { Text("OK", color = Green700) }
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Green700.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (initial == null) Icons.Default.AddLocation else Icons.Default.EditLocation,
                        null, tint = Green700, modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = lokasi,
                    onValueChange = { lokasi = it },
                    label = { Text("Lokasi") },
                    leadingIcon = { Icon(Icons.Default.Place, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green700)
                )

                OutlinedCard(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, null, tint = if (jam.isEmpty()) Color.Gray else Green700)
                        Spacer(Modifier.width(12.dp))
                        Text(if (jam.isEmpty()) "Pilih Jam" else jam, color = if (jam.isEmpty()) Color.Gray else Color.Black)
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = hariExpanded,
                    onExpandedChange = { hariExpanded = it }
                ) {
                    OutlinedTextField(
                        value = hari,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hari") },
                        leadingIcon = { Icon(Icons.Default.Today, null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(hariExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green700)
                    )
                    ExposedDropdownMenu(
                        expanded = hariExpanded,
                        onDismissRequest = { hariExpanded = false }
                    ) {
                        HARI.drop(1).forEach { h ->
                            DropdownMenuItem(
                                text = { Text(h) },
                                onClick = { hari = h; hariExpanded = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = petugasExpanded,
                    onExpandedChange = { petugasExpanded = it }
                ) {
                    val currentPetugas = petugasList.find { it.id == selectedPetugasId }
                    OutlinedTextField(
                        value = if (selectedPetugasId == null) "Pilih Petugas (Opsional)" else (if (currentPetugas?.nama?.isNotEmpty() == true) currentPetugas.nama else currentPetugas?.username ?: ""),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Petugas") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(petugasExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green700)
                    )
                    ExposedDropdownMenu(
                        expanded = petugasExpanded,
                        onDismissRequest = { petugasExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tanpa Petugas") },
                            onClick = { selectedPetugasId = null; petugasExpanded = false }
                        )
                        petugasList.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(if (p.nama.isNotEmpty()) p.nama else p.username) },
                                onClick = { selectedPetugasId = p.id; petugasExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (lokasi.isNotBlank() && jam.isNotBlank()) onSave(lokasi, hari, jam, selectedPetugasId) },
                colors  = ButtonDefaults.buttonColors(containerColor = Green700),
                shape   = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Simpan")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape   = RoundedCornerShape(8.dp)
            ) { Text("Batal") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
