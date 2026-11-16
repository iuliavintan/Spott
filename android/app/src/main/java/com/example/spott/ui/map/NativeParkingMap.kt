package com.example.spott.ui.map

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.spott.map.*
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView

/** Native MapLibre is isolated here; Room and the ViewModel contain no renderer types. */
@Composable
fun NativeParkingMap(state: MapUiState, dark: Boolean, bottomInset: Dp,
    onSelectPoint: (GeoPoint) -> Unit, onSelectDraft: (String) -> Unit,
    onCameraMoved: (GeoPoint) -> Unit, onLoaded: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val pointCallback by rememberUpdatedState(onSelectPoint)
    val draftCallback by rememberUpdatedState(onSelectDraft)
    val cameraCallback by rememberUpdatedState(onCameraMoved)
    val loadedCallback by rememberUpdatedState(onLoaded)
    var latitude by rememberSaveable { mutableDoubleStateOf(state.cameraTarget.latitude) }
    var longitude by rememberSaveable { mutableDoubleStateOf(state.cameraTarget.longitude) }
    var zoom by rememberSaveable { mutableDoubleStateOf(14.0) }
    var lastRequest by rememberSaveable { mutableIntStateOf(state.cameraRequest) }
    val controller = remember {
        MapLibre.getInstance(context)
        NativeMapController(MapView(context, MapLibreMapOptions.createFromAttributes(context).textureMode(true)),
            GeoPoint(latitude, longitude), zoom,
            { pointCallback(it) }, { draftCallback(it) }, { point, level ->
                latitude = point.latitude
                longitude = point.longitude
                zoom = level
                cameraCallback(point)
            }, { loadedCallback() })
    }
    DisposableEffect(controller, owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> controller.view.onStart()
                Lifecycle.Event.ON_RESUME -> controller.view.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.view.onPause()
                Lifecycle.Event.ON_STOP -> controller.view.onStop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) controller.view.onStart()
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) controller.view.onResume()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            controller.destroy()
        }
    }
    AndroidView(factory = { controller.view }, modifier = Modifier.fillMaxSize(), update = {
        controller.update(state, dark, bottomInset.value)
        if (state.cameraRequest != lastRequest) {
            // Retain the requested viewport even if navigation disposes an unready renderer.
            latitude = state.cameraTarget.latitude
            longitude = state.cameraTarget.longitude
            zoom = 16.0
            controller.center(state.cameraTarget)
            lastRequest = state.cameraRequest
        }
    })
}

@Suppress("DEPRECATION")
private class NativeMapController(
    val view: MapView,
    initial: GeoPoint,
    initialZoom: Double,
    private val selectPoint: (GeoPoint) -> Unit,
    private val selectDraft: (String) -> Unit,
    private val cameraMoved: (GeoPoint, Double) -> Unit,
    private val loaded: () -> Unit
) {
    private var map: MapLibreMap? = null
    private var state = MapUiState()
    private var styleUrl = ""
    private var destroyed = false
    private var dragging = false
    private var requestedCenter: GeoPoint? = null
    private var bottomPadding = 128f
    private val markers = mutableMapOf<String, Marker>()
    private var temporary: Marker? = null
    private val density = view.resources.displayMetrics.density
    private val savedIcon = icon(Color.rgb(0, 107, 96))
    private val selectionIcon = icon(Color.rgb(239, 157, 20))

    init {
        view.onCreate(null)
        view.contentDescription = "Parking draft map. Touch and hold to choose a location."
        view.getMapAsync { ready ->
            if (!destroyed) {
                map = ready
                updatePadding()
                ready.cameraPosition = CameraPosition.Builder().target((requestedCenter ?: initial).latLng())
                    .zoom(if (requestedCenter != null) 16.0 else initialZoom).build()
                ready.addOnMapLongClickListener { selectPoint(it.point()); true }
                ready.setOnMarkerClickListener { marker ->
                    markers.entries.firstOrNull { it.value.id == marker.id }?.let { selectDraft(it.key) }
                    true
                }
                ready.addOnCameraIdleListener {
                    ready.cameraPosition.target?.let { cameraMoved(it.point(), ready.cameraPosition.zoom) }
                }
                ready.uiSettings.isLogoEnabled = false
                ready.setStyle(styleUrl.ifBlank { "https://tiles.openfreemap.org/styles/liberty" }) {
                    if (!destroyed) { syncMarkers(); requestedCenter?.let { center(it) }; loaded() }
                }
            }
        }
        setupDragging()
    }

    fun update(next: MapUiState, dark: Boolean, bottom: Float) {
        state = next
        bottomPadding = bottom
        val desired = "https://tiles.openfreemap.org/styles/" + if (dark) "dark" else "liberty"
        if (desired != styleUrl) {
            styleUrl = desired
            map?.setStyle(desired) { if (!destroyed) { syncMarkers(); loaded() } }
        }
        map?.uiSettings?.setAttributionMargins((8 * density).toInt(), 0, 0, ((bottom + 12) * density).toInt())
        updatePadding()
        syncMarkers()
    }

    private fun updatePadding() {
        val height = view.height.takeIf { it > 0 } ?: return
        map?.setPadding(0, minOf((144 * density).toInt(), height / 3), 0,
            minOf(((bottomPadding + 12) * density).toInt(), height / 2))
    }

    fun center(point: GeoPoint) {
        requestedCenter = point
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(point.latLng(), 16.0))
    }

    private fun syncMarkers() {
        val ready = map ?: return
        val stored = state.markers.associateBy { it.id }
        (markers.keys - stored.keys).forEach { id -> markers.remove(id)?.let { ready.removeMarker(it) } }
        stored.forEach { (id, draft) ->
            val point = draft.mapPointOrNull()!!.latLng()
            val existing = markers[id]
            if (existing == null) markers[id] = ready.addMarker(MarkerOptions().position(point).title(draft.location).icon(savedIcon))
            else { existing.position = point; existing.title = draft.location }
        }
        val selected = state.selection
        if (selected == null) {
            temporary?.let { ready.removeMarker(it) }
            temporary = null
        } else if (!dragging) {
            if (temporary == null) temporary = ready.addMarker(MarkerOptions().position(selected.point.latLng()).icon(selectionIcon))
            else temporary!!.position = selected.point.latLng()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragging() {
        view.setOnTouchListener { _, event ->
            val ready = map
            val marker = temporary
            if (ready == null || marker == null) false else {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        val screen = ready.projection.toScreenLocation(marker.position)
                        dragging = kotlin.math.hypot(event.x - screen.x, event.y - screen.y) < 28 * density
                        if (dragging) view.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_MOVE -> if (dragging) marker.position = ready.projection.fromScreenLocation(android.graphics.PointF(event.x, event.y))
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (dragging) {
                        dragging = false
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                        if (event.actionMasked == MotionEvent.ACTION_UP) selectPoint(marker.position.point())
                        else syncMarkers()
                        return@setOnTouchListener true
                    }
                }
                dragging
            }
        }
    }

    private fun icon(color: Int): Icon {
        val size = (36 * density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        Canvas(bitmap).apply {
            paint.color = Color.WHITE
            drawCircle(size / 2f, size / 2f, size * 0.43f, paint)
            paint.color = color
            drawCircle(size / 2f, size / 2f, size * 0.32f, paint)
        }
        return IconFactory.getInstance(view.context).fromBitmap(bitmap)
    }

    fun destroy() {
        destroyed = true
        view.onPause()
        view.onStop()
        view.onDestroy()
    }
}

private fun GeoPoint.latLng() = LatLng(latitude, longitude)
private fun LatLng.point() = GeoPoint(latitude, longitude)
