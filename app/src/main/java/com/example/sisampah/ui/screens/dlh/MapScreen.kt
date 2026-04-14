package com.example.sisampah.ui.screens.dlh

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.sisampah.data.MySqlHelper
import com.example.sisampah.ui.screens.masyarakat.createProfessionalMarker
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
    var mapReference by remember { mutableStateOf<MapView?>(null) }
    val scope = rememberCoroutineScope()

    // OSMDroid Configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm", 0))
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun fetchLocations() {
        scope.launch(Dispatchers.IO) {
            try {
                // Ambil lokasi saya (Admin/DLH) secara berkala
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    loc?.let {
                        myLocation = UserLocation(
                            0, username, "DLH", 
                            it.latitude, it.longitude, 
                            "Lokasi Kantor", "Live"
                        )
                    }
                }

                val conn = MySqlHelper.getConnection()
                if (conn != null) {
                    val stmtPetugas = conn.prepareStatement("SELECT id, nama, role, latitude, longitude FROM users WHERE role LIKE 'PETUGAS%' AND latitude IS NOT NULL")
                    val rsPetugas = stmtPetugas.executeQuery()
                    val list = mutableListOf<UserLocation>()
                    
                    while (rsPetugas.next()) {
                        list.add(UserLocation(
                            rsPetugas.getInt("id"),
                            rsPetugas.getString("nama"),
                            rsPetugas.getString("role"),
                            rsPetugas.getDouble("latitude"),
                            rsPetugas.getDouble("longitude"),
                            "Area Operasional Petugas",
                            "Aktif"
                        ))
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
            delay(10000)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth().zIndex(10f),
                shadowElevation = 8.dp
            ) {
                Box(
                    Modifier.background(Brush.horizontalGradient(listOf(GreenPrimary, GreenLight))).padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.AdminPanelSettings, null, tint = Color.White) }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Monitoring Seluruh Petugas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Panel Pengawasan DLH", color = Color.White.copy(0.8f), fontSize = 12.sp)
                        }
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
                            controller.setZoom(14.0)
                            controller.setCenter(GeoPoint(-0.947, 100.417))
                            mapReference = this
                        }
                    },
                    update = { mapView ->
                        mapView.overlays.clear()
                        
                        // Marker Petugas (Professional Pin)
                        locations.forEach { loc ->
                            val marker = Marker(mapView)
                            marker.position = GeoPoint(loc.latitude, loc.longitude)
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = loc.name
                            marker.subDescription = "Petugas LPS: ${loc.role}"
                            marker.icon = createProfessionalMarker(context, "truck", GreenPrimary.toArgb())
                            mapView.overlays.add(marker)
                        }

                        // Marker Saya (DLH Center)
                        myLocation?.let {
                            val marker = Marker(mapView)
                            marker.position = GeoPoint(it.latitude, it.longitude)
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            marker.icon = createProfessionalMarker(context, "me", BlueStat.toArgb())
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

            // Bottom Card
            Card(
                modifier = Modifier.fillMaxWidth().height(240.dp).zIndex(10f),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Daftar Petugas Lapangan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                        Spacer(Modifier.weight(1f))
                        Surface(color = GreenPrimary.copy(0.1f), shape = RoundedCornerShape(12.dp)) {
                            Text("${locations.size} Total", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(locations) { loc ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(GreenPrimary.copy(0.1f)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.Engineering, null, tint = GreenPrimary, modifier = Modifier.size(20.dp)) }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(loc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(loc.role, fontSize = 11.sp, color = Color.Gray)
                                }
                                Spacer(Modifier.weight(1f))
                                Surface(color = GreenLight.copy(0.1f), shape = CircleShape) {
                                    Box(Modifier.size(8.dp).background(GreenLight))
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB Lokasi Kantor
        FloatingActionButton(
            onClick = {
                myLocation?.let { mapReference?.controller?.animateTo(GeoPoint(it.latitude, it.longitude)) }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 260.dp, end = 20.dp).zIndex(15f),
            containerColor = Color.White,
            contentColor = BlueStat,
            shape = CircleShape
        ) { Icon(Icons.Default.Home, "Center Office") }
    }
}
