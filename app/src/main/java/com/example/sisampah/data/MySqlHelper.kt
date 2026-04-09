package com.example.sisampah.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager

object MySqlHelper {
    private const val IP = "192.168.18.89"
    private const val PORT = "3306"
    private const val DB_NAME = "sisampah_db"
    private const val USER = "root"
    private const val PASS = ""
    
    // Optimasi URL dengan Timeout dan Parameter Keamanan tambahan
    private const val URL = "jdbc:mysql://$IP:$PORT/$DB_NAME?" +
            "useSSL=false&" +
            "allowPublicKeyRetrieval=true&" + 
            "autoReconnect=true&" +
            "connectTimeout=5000&" + 
            "socketTimeout=10000"

    suspend fun getConnection(): Connection? = withContext(Dispatchers.IO) {
        try {
            Class.forName("com.mysql.jdbc.Driver")
            Log.d("MySQL", "Mencoba koneksi ke: $URL")
            val conn = DriverManager.getConnection(URL, USER, PASS)
            Log.d("MySQL", "Koneksi Berhasil!")
            conn
        } catch (e: Exception) {
            // Log detail error agar kita tahu penyebab pastinya (Firewall vs Permission)
            Log.e("MySQL", "Koneksi Gagal Detail: ${e.message}")
            if (e.message?.contains("Communications link failure") == true) {
                Log.e("MySQL", "SOLUSI: Cek Firewall Laptop, izinkan Port 3306.")
            } else if (e.message?.contains("Access denied") == true) {
                Log.e("MySQL", "SOLUSI: Cek Izin User 'root'@'%' di MySQL Shell.")
            }
            null
        }
    }
}
