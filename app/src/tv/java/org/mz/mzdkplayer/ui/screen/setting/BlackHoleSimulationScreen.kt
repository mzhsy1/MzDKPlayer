/*
 * Title: Black Hole Ray Tracing Simulation
 * Description: A specialized Canvas simulation for light rays passing a Schwarzschild black hole (Kerr with J=0).
 * Features:
 * - Schwarzschild radius (Rs) = 70.dp
 * - Light speed (c) = 50.dp/s (Visual speed)
 * - Simulates 10 rays per batch entering from random sides.
 * - Uses physics-based deflection approximating General Relativity lensing.
 * - Targeted for TV/Landscape resolution (960x540 dp).
 */

package org.mz.mzdkplayer.ui.screen.setting

import androidx.compose.ui.tooling.preview.Preview

/*
 * Title: Black Hole Ray Tracing Simulation
 * Description: A specialized Canvas simulation for light rays passing a Schwarzschild black hole (Kerr with J=0).
 * Features:
 * - Schwarzschild radius (Rs) = 70.dp
 * - Light speed (c) = 50.dp/s (Visual speed)
 * - Simulates 10 rays per batch entering from random sides.
 * - Uses physics-based deflection approximating General Relativity lensing.
 * - Targeted for TV/Landscape resolution (960x540 dp).
 * - Fixed: Added explicit state trigger to force Canvas recomposition on every physics frame.
 */



import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

// --- Constants ---
const val LIGHT_SPEED_DP_PER_SEC = 50f
const val SCHWARZSCHILD_RADIUS_DP = 70f
const val TARGET_WIDTH_DP = 960f
const val TARGET_HEIGHT_DP = 540f

// --- Data Classes ---

data class Vector2(var x: Float, var y: Float) {
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector2(x * scalar, y * scalar)

    fun magnitude(): Float = sqrt(x * x + y * y)

    fun normalize(): Vector2 {
        val mag = magnitude()
        return if (mag > 0) Vector2(x / mag, y / mag) else Vector2(0f, 0f)
    }
}

data class LightRay(
    val id: Int,
    var position: Vector2,
    var velocity: Vector2,
    val path: MutableList<Offset> = mutableListOf(),
    var isTrapped: Boolean = false,
    var isEscaped: Boolean = false,
    val color: Color
)


@Composable
fun BlackHoleSimulationScreen(modifier: Modifier) {
    // Basic setup for a dark space theme
    Box(
        modifier = modifier
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        BlackHoleCanvas()
    }
}

@Composable
fun BlackHoleCanvas() {
    val density = LocalDensity.current

    // Convert Dp constants to Pixels
    val rsPx = with(density) { SCHWARZSCHILD_RADIUS_DP.dp.toPx() }
    val cPx = with(density) { LIGHT_SPEED_DP_PER_SEC.dp.toPx() } // pixels per second

    // Simulation State
    val rays = remember { mutableStateListOf<LightRay>() }
    var widthPx by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableFloatStateOf(0f) }

    // TRICK: A state that changes every frame to force Canvas recomposition
    var simulationTick by remember { mutableLongStateOf(0L) }

    // Physics Loop
    LaunchedEffect(Unit) {
        var lastFrameTime = System.nanoTime()

        while (isActive) {
            val currentTime = System.nanoTime()
            val dt = (currentTime - lastFrameTime) / 1_000_000_000f // Delta time in Seconds
            lastFrameTime = currentTime

            // 1. Initialize rays if empty or all done
            // Wait for layout measurement (widthPx > 0)
            if (widthPx > 0f && heightPx > 0f) {
                val activeRays = rays.filter { !it.isTrapped && !it.isEscaped }
                if (activeRays.isEmpty()) {
                    // Small delay before restart to see the result
                    if (rays.isNotEmpty()) delay(1000)

                    rays.clear()
                    rays.addAll(spawnRays(10, widthPx, heightPx, cPx))
                }

                // 2. Physics Update
                // We run this update loop regardless of visual frame rate to keep physics consistent,
                // but here we tie it to the coroutine loop which is roughly frame-bound.
                rays.forEach { ray ->
                    if (!ray.isTrapped && !ray.isEscaped) {
                        updateRayPhysics(ray, widthPx, heightPx, rsPx, cPx, dt)
                    }
                }

                // Force recomposition
                simulationTick++
            }

            // Frame delay (approx 60fps target)
            delay(16)
        }
    }

    Canvas(
        modifier = Modifier
            .size(960.dp, 540.dp) // Target TV Size
            .background(Color(0xFF050510)) // Deep space blue-black
            .onSizeChanged { size ->
                widthPx = size.width.toFloat()
                heightPx = size.height.toFloat()
            }
    ) {
        // READ the tick state to ensure this block re-runs when simulation updates
        val tick = simulationTick

        if (widthPx == 0f) return@Canvas

        val centerX = widthPx / 2
        val centerY = heightPx / 2

        // 1. Draw Event Horizon (Schwarzschild Radius)
        drawCircle(
            color = Color.Black,
            radius = rsPx,
            center = Offset(centerX, centerY)
        )

        // Optional: Draw a subtle accretion glow or photon sphere limit for visual reference
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Gray.copy(alpha=0.8f), Color.Transparent),
                center = Offset(centerX, centerY),
                radius = rsPx * 1.5f
            ),
            radius = rsPx * 1.5f,
            center = Offset(centerX, centerY)
        )

        // 2. Draw Rays
        // We create a copy of the list iteration to avoid concurrent modification issues if needed,
        // though in this single-threaded UI model (mostly), iterating the snapshot state list is safe.
        rays.forEach { ray ->
            if (ray.path.isNotEmpty()) {
                val path = Path().apply {
                    val start = ray.path.first()
                    moveTo(start.x, start.y)
                    // Draw all segments
                    // Using a loop instead of simple lineTo allows for curved path optimization if we wanted later
                    for (i in 1 until ray.path.size) {
                        val p = ray.path[i]
                        lineTo(p.x, p.y)
                    }
                    // Connect to current head position for smooth leading edge
                    lineTo(ray.position.x, ray.position.y)
                }

                drawPath(
                    path = path,
                    color = ray.color,
                    style = Stroke(width = 2.dp.toPx()),
                    alpha = if (ray.isTrapped) 0.3f else 1.0f
                )
            }
        }
    }
}

/**
 * Updates the position and velocity of a single light ray.
 * Uses a modified Newtonian force to approximate GR lensing.
 */
fun updateRayPhysics(
    ray: LightRay,
    width: Float,
    height: Float,
    rs: Float,
    c: Float,
    dt: Float
) {
    val centerX = width / 2
    val centerY = height / 2

    // Vector from Ray to Black Hole Center
    val dx = centerX - ray.position.x
    val dy = centerY - ray.position.y
    val distSq = dx * dx + dy * dy
    val dist = sqrt(distSq)

    // Check for Event Horizon collision
    if (dist < rs) {
        ray.isTrapped = true
        // Snap to edge of black hole for visual cleanliness
        val dir = Vector2(dx, dy).normalize()
        ray.path.add(Offset(centerX - dir.x * rs, centerY - dir.y * rs))
        return
    }

    // Check for Screen Exit
    // We give it a bit of buffer so it clears the screen fully
    if (ray.position.x < -100 || ray.position.x > width + 100 ||
        ray.position.y < -100 || ray.position.y > height + 100) {
        ray.isEscaped = true
        return
    }

    // --- Physics Calculation ---
    // We want the light to curve.
    // In GR, the "force" effectively creates an orbit at 1.5 * Rs (Photon Sphere).
    // The centripetal acceleration needed for orbit is a = v^2 / r.
    // Here v = c, r = 1.5 * Rs.
    // So needed Force/Mass (Acceleration) Magnitude at 1.5Rs is: Acc = c^2 / (1.5 * Rs).
    // We model this with a gravity-like force F = K / r^2.
    // So K / (1.5 * Rs)^2 = c^2 / (1.5 * Rs)
    // K = c^2 * (1.5 * Rs).

    val gravitationalConstantProxy = c.pow(2) * (1.5f * rs)

    // Calculate acceleration magnitude based on current distance
    // a = K / r^2
    val accMag = gravitationalConstantProxy / distSq

    // Direction of force is towards center (normalized dx, dy)
    val accX = (dx / dist) * accMag
    val accY = (dy / dist) * accMag

    // Update Velocity Vector
    // Note: Light speed magnitude technically shouldn't change in local frame, only direction.
    // But applying force changes magnitude. We must re-normalize speed to c after applying force.
    ray.velocity.x += accX * dt
    ray.velocity.y += accY * dt

    // Re-normalize to Light Speed (c)
    val currentSpeed = ray.velocity.magnitude()
    if (currentSpeed > 0) {
        ray.velocity.x = (ray.velocity.x / currentSpeed) * c
        ray.velocity.y = (ray.velocity.y / currentSpeed) * c
    }

    // Update Position
    ray.position.x += ray.velocity.x * dt
    ray.position.y += ray.velocity.y * dt

    // Record Path
    // Optimization: Don't add every single sub-pixel point, only if moved enough
    // Reduced threshold slightly to make slow movement smoother
    val lastPoint = ray.path.lastOrNull()
    if (lastPoint == null ||
        (lastPoint.x - ray.position.x).pow(2) + (lastPoint.y - ray.position.y).pow(2) > 1f) {
        ray.path.add(Offset(ray.position.x, ray.position.y))
    }
}

/**
 * Spawns a batch of rays from a random side of the screen.
 */
fun spawnRays(count: Int, width: Float, height: Float, c: Float): List<LightRay> {
    val newRays = mutableListOf<LightRay>()
    val side = Random.nextInt(4) // 0:Left, 1:Right, 2:Top, 3:Bottom

    // Colors for rays (Sci-fi palette)
    val colors = listOf(
        Color(0xFF00E5FF), // Cyan
        Color(0xFF64FFDA), // Teal
        Color(0xFFFF4081), // Pink
        Color(0xFFE040FB), // Purple
        Color.White
    )

    for (i in 0 until count) {
        var startX = 0f
        var startY = 0f
        var velX = 0f
        var velY = 0f

        // Spread factor
        val spread = 0.6f // Use 60% of the side length

        when (side) {
            0 -> { // Left -> Right
                startX = 0f
                startY = (height * (1 - spread) / 2) + Random.nextFloat() * (height * spread)
                velX = c
                velY = 0f
            }
            1 -> { // Right -> Left
                startX = width
                startY = (height * (1 - spread) / 2) + Random.nextFloat() * (height * spread)
                velX = -c
                velY = 0f
            }
            2 -> { // Top -> Down
                startX = (width * (1 - spread) / 2) + Random.nextFloat() * (width * spread)
                startY = 0f
                velX = 0f
                velY = c
            }
            3 -> { // Bottom -> Up
                startX = (width * (1 - spread) / 2) + Random.nextFloat() * (width * spread)
                startY = height
                velX = 0f
                velY = -c
            }
        }

        newRays.add(
            LightRay(
                id = i,
                position = Vector2(startX, startY),
                velocity = Vector2(velX, velY),
                color = colors.random(),
                path = mutableListOf(Offset(startX, startY))
            )
        )
    }
    return newRays
}
@Preview( device = "spec:width=960dp,height=540dp,dpi=240")
@Composable
fun BlackHoleSimPreview() {

        // 使用新的 BlackHoleSim Composable
        BlackHoleSimulationScreen(
            modifier = Modifier
                .fillMaxSize()
        )

}