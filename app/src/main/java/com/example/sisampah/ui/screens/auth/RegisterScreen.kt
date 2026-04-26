package com.example.sisampah.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import com.example.sisampah.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var waterType by remember { mutableStateOf("PDAM") }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val waterOptions = listOf("PDAM", "NON-PDAM")

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val meta = conn.metaData
                    val rs = meta.getColumns(null, null, "users", "water_type")
                    if (!rs.next()) {
                        conn.createStatement().executeUpdate("ALTER TABLE users ADD COLUMN water_type VARCHAR(20) DEFAULT 'PDAM'")
                    }
                    rs.close()
                    conn.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Daftar Akun",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Bergabung bersama Lapor-Sampah",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = nama,
            onValueChange = { nama = it },
            label = { Text("Nama Lengkap") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Konfirmasi Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Jenis Pelanggan Air:",
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )

        Row(
            Modifier
                .fillMaxWidth()
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            waterOptions.forEach { text ->
                Row(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .selectable(
                            selected = (text == waterType),
                            onClick = { waterType = text },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = (text == waterType),
                        onClick = null // null recommended for accessibility with selectable modifier
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    when {
                        nama.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ->
                            Toast.makeText(context, "Peringatan: Isi semua bidang!", Toast.LENGTH_SHORT).show()
                        password != confirmPassword ->
                            Toast.makeText(context, "Gagal: Password tidak cocok!", Toast.LENGTH_SHORT).show()
                        else -> {
                            isLoading = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val conn = MySqlHelper.getConnection()
                                    if (conn != null) {
                                        val checkStmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?")
                                        checkStmt.setString(1, username)
                                        val resultSet = checkStmt.executeQuery()

                                        if (resultSet.next()) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Username '$username' sudah digunakan!", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            val insertStmt = conn.prepareStatement(
                                                "INSERT INTO users (username, nama, password, role, water_type) VALUES (?, ?, ?, ?, ?)"
                                            )
                                            insertStmt.setString(1, username)
                                            insertStmt.setString(2, nama)
                                            insertStmt.setString(3, password)
                                            insertStmt.setString(4, UserRole.MASYARAKAT.name)
                                            insertStmt.setString(5, waterType)

                                            val rowsAffected = insertStmt.executeUpdate()
                                            withContext(Dispatchers.Main) {
                                                if (rowsAffected > 0) {
                                                    Toast.makeText(context, "Berhasil: Akun berhasil terdaftar!", Toast.LENGTH_SHORT).show()
                                                    onRegisterSuccess()
                                                } else {
                                                    Toast.makeText(context, "Gagal: Terjadi kesalahan pendaftaran!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        conn.close()
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Gagal: Terputus dari server database!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    withContext(Dispatchers.Main) { isLoading = false }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Daftar Sekarang", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = {
            focusManager.clearFocus()
            onNavigateToLogin()
        }, enabled = !isLoading) {
            Text("Sudah punya akun? Login")
        }
    }
}
