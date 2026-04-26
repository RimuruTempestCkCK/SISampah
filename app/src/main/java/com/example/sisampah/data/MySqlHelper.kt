package com.example.sisampah.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager

object MySqlHelper {
    // Database Cloud (Clever Cloud)
    private const val IP = "b2ymn6kyptz9437ll7sn-mysql.services.clever-cloud.com" 
    private const val PORT = "3306"
    private const val DB_NAME = "b2ymn6kyptz9437ll7sn"
    private const val USER = "ug3cxbs24did1hbo"
    private const val PASS = "bJDLyVd0fEnyXvoMt7i3"
    
    private const val URL = "jdbc:mysql://$IP:$PORT/$DB_NAME?" +
            "useSSL=false&" +
            "allowPublicKeyRetrieval=true&" + 
            "autoReconnect=true"

    suspend fun getConnection(): Connection? = withContext(Dispatchers.IO) {
        try {
            Class.forName("com.mysql.jdbc.Driver")
            Log.d("MyS  QL", "Menghubungkan ke Cloud: $URL")
            val conn = DriverManager.getConnection(URL, USER, PASS)
            Log.d("MySQL", "Koneksi Cloud Berhasil!")
            conn
        } catch (e: Exception) {
            Log.e("MySQL", "Gagal koneksi Cloud: ${e.message}")
            null
        }
    }
}
