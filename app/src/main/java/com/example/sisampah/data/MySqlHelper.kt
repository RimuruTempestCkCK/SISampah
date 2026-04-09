package com.example.sisampah.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager

object MySqlHelper {
    private const val IP = "172.20.10.5"
    private const val PORT = "3306"
    private const val DB_NAME = "sisampah_db"
    private const val USER = "root"
    private const val PASS = ""
    
    // Optimasi URL dengan Timeout agar tidak loading selamanya
    private const val URL = "jdbc:mysql://$IP:$PORT/$DB_NAME?" +
            "useSSL=false&" +
            "autoReconnect=true&" +
            "connectTimeout=5000&" + // Maksimal 5 detik untuk mencoba konek
            "socketTimeout=10000"    // Maksimal 10 detik untuk kirim/terima data

    suspend fun getConnection(): Connection? = withContext(Dispatchers.IO) {
        try {
            Class.forName("com.mysql.jdbc.Driver")
            Log.d("MySQL", "Mencoba koneksi ke: $URL")
            val conn = DriverManager.getConnection(URL, USER, PASS)
            Log.d("MySQL", "Koneksi Berhasil!")
            conn
        } catch (e: Exception) {
            Log.e("MySQL", "Koneksi Gagal Detail: ${e.message}")
            null
        }
    }
}
