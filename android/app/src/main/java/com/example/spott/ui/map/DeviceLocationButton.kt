package com.example.spott.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import com.example.spott.map.GeoPoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun DeviceLocationButton(onLocation: (GeoPoint) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    fun locate() {
        if (busy) return
        busy = true
        scope.launch {
            val cancellation = CancellationTokenSource()
            try {
                val location = withTimeoutOrNull(15_000) {
                    LocationServices.getFusedLocationProviderClient(context)
                        .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token).await()
                }
                if (location == null) message = "Location unavailable. Check device location settings or choose a point on the map."
                else onLocation(GeoPoint(location.latitude, location.longitude))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message = "Could not get your location. You can still search or choose a point."
            } finally {
                cancellation.cancel()
                busy = false
            }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) locate()
        else message = "Location permission was not granted. Search and saved drafts are still available."
    }
    SmallFloatingActionButton(onClick = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate()
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }, modifier = Modifier.semantics {
        contentDescription = "My location"
        stateDescription = if (busy) "Finding location" else "Ready"
    }, containerColor = MaterialTheme.colorScheme.surface) {
        if (busy) CircularProgressIndicator() else Icon(Icons.Rounded.MyLocation, null)
    }
    message?.let {
        AlertDialog(onDismissRequest = { message = null }, title = { Text("My location") }, text = { Text(it) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } })
    }
}
