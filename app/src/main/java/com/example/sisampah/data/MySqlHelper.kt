package com.example.sisampah.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager

object MySqlHelper {
    private const val IP = "10.0.2.2" 
    private const val PORT = "3306"
    private const val DB_NAME = "sisampah_db"
    private const val USER = "root"
    private const val PASS = ""
    
    // Parameter tambahan untuk menghindari kegagalan handshake
    private const val URL = "jdbc:mysql://$IP:$PORT/$DB_NAME?" +
            "useSSL=false&" +
            "autoReconnect=true&" +
            "failOverReadOnly=false&" +
            "maxReconnects=10"

    suspend fun getConnection(): Connection? = withContext(Dispatchers.IO) {
        try {
            Class.forName("com.mysql.jdbc.Driver")
            Log.d("MySQL", "Mencoba koneksi ke: $URL")
            val conn = DriverManager.getConnection(URL, USER, PASS)
            Log.d("MySQL", "Koneksi Berhasil!")
            conn
        } catch (e: Exception) {
            Log.e("MySQL", "Koneksi Gagal Detail: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
