package com.example.sisampah.ui.screens.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import com.example.sisampah.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── Warna ─────────────────────────────────────────────────────────────────────
private val Green700    = Color(0xFF2E7D32)
private val Green500    = Color(0xFF4CAF50)
private val RedAccent   = Color(0xFFE53935)
private val Blue700     = Color(0xFF1565C0)
private val Purple700   = Color(0xFF6A1B9A)
private val Amber500    = Color(0xFFFFC107)

// ─── Model ─────────────────────────────────────────────────────────────────────
data class User(
    val id: Int,
    val username: String,
    val password: String,
    val role: String,
    val nama: String          // Tambahan: Nama Lengkap
)

// ─── Role yang tersedia ───────────────────────
private val ROLES = listOf(
    UserRole.MASYARAKAT.name,
    UserRole.PETUGAS_LPS.name,
    UserRole.ADMIN.name,
    UserRole.DLH.name
)

// ═══════════════════════════════════════════════════════════════════════════════
//  SCREEN UTAMA
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUserScreen() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // ── State ──
    var users         by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading     by remember { mutableStateOf(false) }
    var searchQuery   by remember { mutableStateOf("") }
    var filterRole    by remember { mutableStateOf("SEMUA") }

    var showAddDialog    by remember { mutableStateOf(false) }
    var userToEdit       by remember { mutableStateOf<User?>(null) }
    var userToDelete     by remember { mutableStateOf<User?>(null) }
    var errorMsg         by remember { mutableStateOf<String?>(null) }

    // ── Fungsi: Migrasi database (tambah kolom nama jika belum ada) ──
    fun migrateDatabase() {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val meta = conn.metaData
                    val rs = meta.getColumns(null, null, "users", "nama")
                    if (!rs.next()) {
                        val stmt = conn.createStatement()
                        stmt.executeUpdate("ALTER TABLE users ADD COLUMN nama VARCHAR(100) DEFAULT '' AFTER username")
                        stmt.close()
                    }
                    rs.close()
                    conn.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ── Fungsi: Load semua user dari DB ──
    fun loadUsers() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                val result = mutableListOf<User>()
                if (conn != null) {
                    val stmt = conn.prepareStatement("SELECT id, username, password, role, nama FROM users ORDER BY id ASC")
                    val rs   = stmt.executeQuery()
                    while (rs.next()) {
                        result.add(
                            User(
                                id       = rs.getInt("id"),
                                username = rs.getString("username"),
                                password = rs.getString("password"),
                                role     = rs.getString("role"),
                                nama     = rs.getString("nama") ?: ""
                            )
                        )
                    }
                    rs.close(); stmt.close(); conn.close()
                } else {
                    withContext(Dispatchers.Main) { errorMsg = "Gagal terhubung ke database." }
                }
                withContext(Dispatchers.Main) { users = result }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { errorMsg = "Error: ${e.message}" }
            } finally {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    // ── Fungsi: Tambah user ──
    fun insertUser(username: String, nama: String, password: String, role: String, onDone: (Boolean) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val check = conn.prepareStatement("SELECT id FROM users WHERE username = ?")
                    check.setString(1, username)
                    val rs = check.executeQuery()
                    if (rs.next()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Username '$username' sudah digunakan!", Toast.LENGTH_SHORT).show()
                            onDone(false)
                        }
                        rs.close(); check.close(); conn.close()
                        return@launch
                    }
                    rs.close(); check.close()

                    val stmt = conn.prepareStatement(
                        "INSERT INTO users (username, nama, password, role) VALUES (?, ?, ?, ?)"
                    )
                    stmt.setString(1, username)
                    stmt.setString(2, nama)
                    stmt.setString(3, password)
                    stmt.setString(4, role)
                    val rows = stmt.executeUpdate()
                    stmt.close(); conn.close()
                    withContext(Dispatchers.Main) {
                        if (rows > 0) {
                            Toast.makeText(context, "User berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                            onDone(true)
                        } else {
                            onDone(false)
                        }
                    }
                } else {
                    onDone(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    onDone(false)
                }
            }
        }
    }

    // ── Fungsi: Update user ──
    fun updateUser(id: Int, username: String, nama: String, password: String, role: String, onDone: (Boolean) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val check = conn.prepareStatement("SELECT id FROM users WHERE username = ? AND id != ?")
                    check.setString(1, username)
                    check.setInt(2, id)
                    val rs = check.executeQuery()
                    if (rs.next()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Username '$username' sudah digunakan!", Toast.LENGTH_SHORT).show()
                            onDone(false)
                        }
                        rs.close(); check.close(); conn.close()
                        return@launch
                    }
                    rs.close(); check.close()

                    val stmt = conn.prepareStatement(
                        "UPDATE users SET username = ?, nama = ?, password = ?, role = ? WHERE id = ?"
                    )
                    stmt.setString(1, username)
                    stmt.setString(2, nama)
                    stmt.setString(3, password)
                    stmt.setString(4, role)
                    stmt.setInt(5, id)
                    val rows = stmt.executeUpdate()
                    stmt.close(); conn.close()
                    withContext(Dispatchers.Main) {
                        if (rows > 0) {
                            Toast.makeText(context, "User berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            onDone(true)
                        } else {
                            onDone(false)
                        }
                    }
                } else {
                    onDone(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    onDone(false)
                }
            }
        }
    }

    // ── Fungsi: Hapus user ──
    fun deleteUser(id: Int, onDone: (Boolean) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val stmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")
                    stmt.setInt(1, id)
                    val rows = stmt.executeUpdate()
                    stmt.close(); conn.close()
                    withContext(Dispatchers.Main) {
                        if (rows > 0) {
                            Toast.makeText(context, "User berhasil dihapus!", Toast.LENGTH_SHORT).show()
                            onDone(true)
                        } else {
                            onDone(false)
                        }
                    }
                } else {
                    onDone(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    onDone(false)
                }
            }
        }
    }

    // ── Load pertama kali ──
    LaunchedEffect(Unit) { 
        migrateDatabase()
        loadUsers() 
    }

    // ── Filter ──
    val displayedUsers = users.filter { u ->
        (filterRole == "SEMUA" || u.role == filterRole) &&
                (searchQuery.isEmpty() || u.username.contains(searchQuery, ignoreCase = true) || u.nama.contains(searchQuery, ignoreCase = true))
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
                        "Kelola Data Pengguna",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        "${users.size} pengguna terdaftar",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = { loadUsers() }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
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
                    shape = RoundedCornerShape(10.dp)
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
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari username atau nama...") },
                leadingIcon  = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty())
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                },
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Green700,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor   = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(Modifier.height(10.dp))

            // ── Filter Role Chip ──
            val allFilters = listOf("SEMUA") + ROLES
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allFilters) { role ->
                    FilterChip(
                        selected = filterRole == role,
                        onClick  = { filterRole = role },
                        label    = { Text(roleLabel(role), fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Green700,
                            selectedLabelColor     = Color.White,
                            containerColor         = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Tombol Tambah ──
            Button(
                onClick  = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Green700)
            ) {
                Icon(Icons.Default.PersonAdd, null)
                Spacer(Modifier.width(8.dp))
                Text("Tambah Pengguna Baru", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            // ── Konten ──
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Green700)
                    }
                }
                displayedUsers.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Pengguna tidak ditemukan.", color = Color.Gray)
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(displayedUsers, key = { it.id }) { user ->
                            UserItemCard(
                                user     = user,
                                onEdit   = { userToEdit   = user },
                                onDelete = { userToDelete = user }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════
    //  DIALOG TAMBAH
    // ════════════════════════════════════════════
    if (showAddDialog) {
        UserFormDialog(
            title   = "Tambah Pengguna",
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = { username, nama, password, role ->
                insertUser(username, nama, password, role) { success ->
                    if (success) { showAddDialog = false; loadUsers() }
                }
            }
        )
    }

    // ════════════════════════════════════════════
    //  DIALOG EDIT
    // ════════════════════════════════════════════
    userToEdit?.let { u ->
        UserFormDialog(
            title   = "Edit Pengguna",
            initial = u,
            onDismiss = { userToEdit = null },
            onSave = { username, nama, password, role ->
                updateUser(u.id, username, nama, password, role) { success ->
                    if (success) { userToEdit = null; loadUsers() }
                }
            }
        )
    }

    // ════════════════════════════════════════════
    //  DIALOG KONFIRMASI HAPUS
    // ════════════════════════════════════════════
    userToDelete?.let { u ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Hapus Pengguna") },
            text  = { Text("Apakah Anda yakin ingin menghapus '${u.username}'?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteUser(u.id) { success ->
                        if (success) { userToDelete = null; loadUsers() }
                    }
                }) { Text("Hapus", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) { Text("Batal") }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  KOMPONEN: Kartu satu user
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun UserItemCard(
    user: User,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val roleColor = roleColor(user.role)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(roleColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (if (user.nama.isNotEmpty()) user.nama else user.username).first().uppercaseChar().toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = roleColor
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    if (user.nama.isNotEmpty()) user.nama else user.username,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp
                )
                if (user.nama.isNotEmpty()) {
                    Text(
                        user.username,
                        fontSize = 12.sp,
                        color    = Color.Gray
                    )
                }
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(roleColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        roleLabel(user.role),
                        color      = roleColor,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

// ═══════════════════════════════════════════════════════════════════════════════
//  DIALOG: Form Tambah / Edit
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFormDialog(
    title   : String,
    initial : User?,
    onDismiss: () -> Unit,
    onSave  : (username: String, nama: String, password: String, role: String) -> Unit
) {
    var username        by remember { mutableStateOf(initial?.username ?: "") }
    var nama            by remember { mutableStateOf(initial?.nama ?: "") }
    var password        by remember { mutableStateOf(initial?.password ?: "") }
    var selectedRole    by remember { mutableStateOf(initial?.role ?: UserRole.MASYARAKAT.name) }
    var showPassword    by remember { mutableStateOf(false) }
    var roleExpanded    by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value         = nama,
                    onValueChange = { nama = it },
                    label         = { Text("Nama Lengkap") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value         = username,
                    onValueChange = { username = it },
                    label         = { Text("Username") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value         = password,
                    onValueChange = { password = it },
                    label         = { Text("Password") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon  = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    modifier   = Modifier.fillMaxWidth(),
                    shape      = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenuBox(
                    expanded        = roleExpanded,
                    onExpandedChange = { roleExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = roleLabel(selectedRole),
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Role") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(roleExpanded) },
                        modifier      = Modifier.menuAnchor().fillMaxWidth(),
                        shape         = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded        = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        ROLES.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(roleLabel(role)) },
                                onClick = { selectedRole = role; roleExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (username.isNotBlank() && password.isNotBlank()) onSave(username.trim(), nama.trim(), password, selectedRole) },
                colors  = ButtonDefaults.buttonColors(containerColor = Green700)
            ) { Text("Simpan") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Helper: label & warna role (diambil dari versi sebelumnya)
// ═══════════════════════════════════════════════════════════════════════════════
fun roleLabel(role: String): String = when (role.uppercase()) {
    "ADMIN"       -> "Admin"
    "PETUGAS_LPS" -> "Petugas"
    "MASYARAKAT"  -> "Masyarakat"
    "DLH"         -> "DLH"
    "SEMUA"       -> "Semua"
    else          -> role
}

fun roleColor(role: String): Color = when (role.uppercase()) {
    "ADMIN"       -> Purple700
    "PETUGAS_LPS" -> Blue700
    "MASYARAKAT"  -> Green700
    "DLH"         -> Amber500
    else          -> Color.Gray
}
