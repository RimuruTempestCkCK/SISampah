package com.example.sisampah.ui.screens.masyarakat

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.sisampah.data.MySqlHelper
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private val GreenPrimary   = Color(0xFF2E7D32)
private val GreenLight     = Color(0xFF4CAF50)
private val BlueStat       = Color(0xFF1565C0)
private val RedAccent      = Color(0xFFE53935)

data class UserLocation(
    val id: Int,
    val name: String,
    val role: String,
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val lastUpdate: String
)

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(username: String) {
    val context = LocalContext.current
    var locations by remember { mutableStateOf<List<UserLocation>>(emptyList()) }
    var myLocation by remember { mutableStateOf<UserLocation?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // OSMDroid Configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm", 0))
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun fetchLocations() {
        scope.launch(Dispatchers.IO) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    loc?.let {
                        myLocation = UserLocation(
                            0, username, "MASYARAKAT", 
                            it.latitude, it.longitude, 
                            "Lokasi Anda (GPS)", "Baru saja"
                        )
                    }
                }

                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val stmtPetugas = conn.prepareStatement("SELECT id, nama, role FROM users WHERE role LIKE 'PETUGAS%' LIMIT 5")
                    val rsPetugas = stmtPetugas.executeQuery()
                    val list = mutableListOf<UserLocation>()
                    
                    val dummyCoords = listOf(
                        Pair(-0.947, 100.417), Pair(-0.942, 100.366), 
                        Pair(-0.933, 100.400), Pair(-0.915, 100.420), 
                        Pair(-0.925, 100.440)
                    )
                    val dummyNames = listOf("Jl. Khatib Sulaiman", "Pasar Raya", "Anduring", "Kuranji", "Pauh")
                    
                    var idx = 0
                    while (rsPetugas.next()) {
                        val coords = dummyCoords.getOrElse(idx) { Pair(-0.9 + (idx*0.01), 100.3 + (idx*0.01)) }
                        list.add(UserLocation(
                            rsPetugas.getInt("id"),
                            rsPetugas.getString("nama"),
                            rsPetugas.getString("role"),
                            coords.first, coords.second,
                            dummyNames.getOrElse(idx) { "Area Operasional" },
                            "${(1..5).random()} menit lalu"
                        ))
                        idx++
                    }
                    rsPetugas.close(); stmtPetugas.close()
                    
                    withContext(Dispatchers.Main) {
                        locations = list
                        isLoading = false
                    }
                    conn.close()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    LaunchedEffect(Unit) {
        while(true) {
            fetchLocations()
            delay(15000)
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        // Header
        Box(
            Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(GreenPrimary, GreenLight))).padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Map, null, tint = Color.White) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Peta Live Petugas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Monitoring Armada LPS", color = Color.White.copy(0.8f), fontSize = 12.sp)
                }
            }
        }

        // Map View
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(14.5)
                        controller.setCenter(GeoPoint(-0.947, 100.417))
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()
                    
                    // Marker Petugas (Mobil Hijau)
                    locations.forEach { loc ->
                        val point = GeoPoint(loc.latitude, loc.longitude)
                        val marker = Marker(mapView)
                        marker.position = point
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = loc.name
                        marker.subDescription = loc.locationName
                        
                        // Custom Icon Mobil Hijau
                        val icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_directions)
                        icon?.setTint(android.graphics.Color.parseColor("#2E7D32"))
                        marker.icon = icon
                        
                        mapView.overlays.add(marker)
                    }

                    // Marker Saya
                    myLocation?.let {
                        val myPoint = GeoPoint(it.latitude, it.longitude)
                        val marker = Marker(mapView)
                        marker.position = myPoint
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = "Lokasi Saya"
                        
                        val icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                        icon?.setTint(android.graphics.Color.parseColor("#1565C0"))
                        marker.icon = icon
                        
                        mapView.overlays.add(marker)
                    }
                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )
            
            if (isLoading) {
                Box(Modifier.fillMaxSize().background(Color.White.copy(0.5f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
            }
        }

        // Bottom List Summary
        Card(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Armada Terdekat", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                    Spacer(Modifier.weight(1f))
                    Surface(color = GreenPrimary.copy(0.1f), shape = RoundedCornerShape(12.dp)) {
                        Text("${locations.size} Aktif", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(locations) { loc ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(GreenPrimary.copy(0.1f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.LocalShipping, null, tint = GreenPrimary, modifier = Modifier.size(22.dp)) }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(loc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(loc.locationName, fontSize = 12.sp, color = Color.Gray)
                            }
                            Text(loc.lastUpdate, fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}
