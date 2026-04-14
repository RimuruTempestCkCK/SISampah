package com.example.sisampah.ui.screens.masyarakat

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
                // Ambil lokasi saya secara berkala
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
            delay(10000)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth().zIndex(5f),
                shadowElevation = 4.dp
            ) {
                Box(
                    Modifier.background(Brush.horizontalGradient(listOf(GreenPrimary, GreenLight))).padding(20.dp)
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
            }

            // Map View
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                            controller.setCenter(GeoPoint(-0.947, 100.417))
                            mapReference = this
                        }
                    },
                    update = { mapView ->
                        mapView.overlays.clear()
                        
                        // Marker Petugas (Icon Truck/Mobil Hijau)
                        locations.forEach { loc ->
                            val point = GeoPoint(loc.latitude, loc.longitude)
                            val marker = Marker(mapView)
                            marker.position = point
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            marker.title = loc.name
                            marker.subDescription = loc.locationName
                            
                            // Custom Icon Truck/Mobil
                            marker.icon = createTruckMarkerIcon(context, GreenPrimary.toArgb(), 140)
                            
                            mapView.overlays.add(marker)
                        }

                        // Marker Saya (Icon User Biru)
                        myLocation?.let {
                            val myPoint = GeoPoint(it.latitude, it.longitude)
                            val marker = Marker(mapView)
                            marker.position = myPoint
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            marker.title = "Lokasi Saya"
                            
                            marker.icon = createMeMarkerIcon(context, BlueStat.toArgb(), 140)
                            
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
                modifier = Modifier.fillMaxWidth().height(220.dp).zIndex(5f),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(16.dp)
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

        // Floating Action Button for My Location
        FloatingActionButton(
            onClick = {
                myLocation?.let {
                    mapReference?.controller?.animateTo(GeoPoint(it.latitude, it.longitude))
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 240.dp, end = 16.dp)
                .zIndex(10f),
            containerColor = Color.White,
            contentColor = BlueStat,
            shape = CircleShape
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Lokasi Saya")
        }
    }
}

// Custom Marker for Garbage Truck
fun createTruckMarkerIcon(context: Context, color: Int, size: Int): Drawable {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = android.graphics.Paint()
    
    // Circle Background
    paint.color = color
    paint.isAntiAlias = true
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    
    // Simple Truck Shape (White)
    paint.color = android.graphics.Color.WHITE
    // Body
    canvas.drawRect(size * 0.25f, size * 0.35f, size * 0.65f, size * 0.7f, paint)
    // Cabin
    canvas.drawRect(size * 0.65f, size * 0.5f, size * 0.78f, size * 0.7f, paint)
    // Connecting bar
    canvas.drawRect(size * 0.6f, size * 0.65f, size * 0.7f, size * 0.7f, paint)
    // Wheels
    canvas.drawCircle(size * 0.35f, size * 0.75f, size * 0.08f, paint)
    canvas.drawCircle(size * 0.65f, size * 0.75f, size * 0.08f, paint)
    
    return BitmapDrawable(context.resources, bitmap)
}

// Custom Marker for User Location
fun createMeMarkerIcon(context: Context, color: Int, size: Int): Drawable {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = android.graphics.Paint()
    
    // Outer Circle (Blue)
    paint.color = color
    paint.isAntiAlias = true
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    
    // Inner Circle (White)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size * 0.35f, paint)
    
    // Center Dot (Blue)
    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size * 0.2f, paint)

    return BitmapDrawable(context.resources, bitmap)
}
