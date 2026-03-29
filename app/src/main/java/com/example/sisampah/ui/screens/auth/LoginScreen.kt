package com.example.sisampah.ui.screens.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sisampah.data.MySqlHelper
import com.example.sisampah.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(
    onLoginSuccess: (UserRole, String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Lapor-Sampah",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Aplikasi Pemantauan dan Pelaporan Pengangkutan Sampah",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (username.isEmpty() || password.isEmpty()) {
                        Toast.makeText(context, "Isi semua bidang!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isLoading = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            val conn = MySqlHelper.getConnection()
                            if (conn != null) {
                                val query = "SELECT role FROM users WHERE username = ? AND password = ?"
                                val statement = conn.prepareStatement(query)
                                statement.setString(1, username)
                                statement.setString(2, password)
                                val resultSet = statement.executeQuery()

                                if (resultSet.next()) {
                                    val roleRaw = resultSet.getString("role")
                                    if (roleRaw != null) {
                                        val roleStr = roleRaw.trim().uppercase()
                                        Log.d("LoginScreen", "Role found: '$roleStr'")
                                        
                                        val loggedInUsername = username // Capture current username
                                        withContext(Dispatchers.Main) {
                                            try {
                                                val role = UserRole.valueOf(roleStr)
                                                Toast.makeText(context, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess(role, loggedInUsername)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Role '$roleStr' tidak dikenal!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Data role di database kosong (NULL)!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Username atau Password salah", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                conn.close()
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Koneksi ke database gagal (NULL)!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                val errorMsg = e.message ?: e.toString()
                                Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                                Log.e("LoginScreen", "Exception: ", e)
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        TextButton(onClick = onNavigateToRegister, enabled = !isLoading) {
            Text("Belum punya akun? Daftar sekarang")
        }
    }
}
