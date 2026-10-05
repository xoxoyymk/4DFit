package com.fourdfit.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.fourdfit.app.domain.model.MotionType
import com.fourdfit.app.ui.theme.FourD
import kotlinx.coroutines.isActive
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The "kinetic figure": a neon stick athlete that demonstrates each exercise.
 * Poses are normalised (0..1) joint positions; keyframes are interpolated and played
 * back and forth. Drawing happens in the draw phase only, so playback never recomposes.
 *
 * Front-view poses face the viewer. Side-view poses face left (head at small x).
 * The floor sits at y = 0.90.
 */
@Composable
fun ExerciseAnimation(
    motion: MotionType,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    tempo: Float = 1f,
    primary: Color = FourD.colors.cyan,
    secondary: Color = FourD.colors.violet,
    showFloor: Boolean = true,
    contentDescription: String? = null,
) {
    val keyframes = remember(motion) { PoseLibrary.keyframes(motion) }
    val reduce = FourD.reduceMotion
    val progress = remember(motion) { Animatable(0f) }
    val segment = (PoseLibrary.segmentMillis(motion) * tempo).toInt().coerceAtLeast(200)

    LaunchedEffect(motion, playing, reduce, segment) {
        if (!playing || reduce || keyframes.size < 2) return@LaunchedEffect
        val last = (keyframes.size - 1).toFloat()
        val forward = tween<Float>(segment * (keyframes.size - 1), easing = FastOutSlowInEasing)
        while (isActive) {
            progress.animateTo(last, forward)
            progress.animateTo(0f, forward)
        }
    }

    Canvas(
        modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
        },
    ) {
        val pose = PoseLibrary.sample(keyframes, progress.value)
        drawFigure(pose, primary, secondary, showFloor)
    }
}

data class Pose(
    val head: Offset,
    val neck: Offset,
    val hip: Offset,
    val lElbow: Offset,
    val lHand: Offset,
    val rElbow: Offset,
    val rHand: Offset,
    val lKnee: Offset,
    val lFoot: Offset,
    val rKnee: Offset,
    val rFoot: Offset,
    val shoulderSpread: Float = 0f,
    val hipSpread: Float = 0f,
    /** Positive bends the spine toward the floor (cow), negative arches it up (cat). */
    val spineBend: Float = 0f,
) {
    fun mirrored(): Pose {
        fun m(o: Offset) = Offset(1f - o.x, o.y)
        return copy(
            head = m(head),
            neck = m(neck),
            hip = m(hip),
            lElbow = m(rElbow),
            lHand = m(rHand),
            rElbow = m(lElbow),
            rHand = m(lHand),
            lKnee = m(rKnee),
            lFoot = m(rFoot),
            rKnee = m(lKnee),
            rFoot = m(lFoot),
        )
    }

    fun shifted(dy: Float): Pose {
        fun s(o: Offset) = Offset(o.x, o.y + dy)
        return copy(
            head = s(head),
            neck = s(neck),
            hip = s(hip),
            lElbow = s(lElbow),
            lHand = s(lHand),
            rElbow = s(rElbow),
            rHand = s(rHand),
            lKnee = s(lKnee),
            lFoot = s(lFoot),
            rKnee = s(rKnee),
            rFoot = s(rFoot),
        )
    }
}

private fun o(
    x: Float,
    y: Float,
) = Offset(x, y)

private fun lerp(
    a: Float,
    b: Float,
    t: Float,
): Float = a + (b - a) * t

private fun lerpPose(
    a: Pose,
    b: Pose,
    t: Float,
) = Pose(
    head = lerp(a.head, b.head, t),
    neck = lerp(a.neck, b.neck, t),
    hip = lerp(a.hip, b.hip, t),
    lElbow = lerp(a.lElbow, b.lElbow, t),
    lHand = lerp(a.lHand, b.lHand, t),
    rElbow = lerp(a.rElbow, b.rElbow, t),
    rHand = lerp(a.rHand, b.rHand, t),
    lKnee = lerp(a.lKnee, b.lKnee, t),
    lFoot = lerp(a.lFoot, b.lFoot, t),
    rKnee = lerp(a.rKnee, b.rKnee, t),
    rFoot = lerp(a.rFoot, b.rFoot, t),
    shoulderSpread = lerp(a.shoulderSpread, b.shoulderSpread, t),
    hipSpread = lerp(a.hipSpread, b.hipSpread, t),
    spineBend = lerp(a.spineBend, b.spineBend, t),
)

object PoseLibrary {
    // ------------------------------------------------------------- front view
    private val stand =
        Pose(
            head = o(0.50f, 0.17f),
            neck = o(0.50f, 0.28f),
            hip = o(0.50f, 0.54f),
            lElbow = o(0.40f, 0.41f),
            lHand = o(0.38f, 0.53f),
            rElbow = o(0.60f, 0.41f),
            rHand = o(0.62f, 0.53f),
            lKnee = o(0.46f, 0.72f),
            lFoot = o(0.45f, 0.90f),
            rKnee = o(0.54f, 0.72f),
            rFoot = o(0.55f, 0.90f),
            shoulderSpread = 0.08f,
            hipSpread = 0.045f,
        )
    private val standArmsForward =
        stand.copy(
            lElbow = o(0.42f, 0.38f),
            lHand = o(0.47f, 0.36f),
            rElbow = o(0.58f, 0.38f),
            rHand = o(0.53f, 0.36f),
        )
    private val squatDown =
        stand.copy(
            head = o(0.50f, 0.33f),
            neck = o(0.50f, 0.44f),
            hip = o(0.50f, 0.68f),
            lElbow = o(0.41f, 0.50f),
            lHand = o(0.47f, 0.47f),
            rElbow = o(0.59f, 0.50f),
            rHand = o(0.53f, 0.47f),
            lKnee = o(0.37f, 0.75f),
            lFoot = o(0.43f, 0.90f),
            rKnee = o(0.63f, 0.75f),
            rFoot = o(0.57f, 0.90f),
        )
    private val airborne =
        stand
            .copy(
                lElbow = o(0.36f, 0.20f),
                lHand = o(0.41f, 0.09f),
                rElbow = o(0.64f, 0.20f),
                rHand = o(0.59f, 0.09f),
                lKnee = o(0.47f, 0.71f),
                lFoot = o(0.47f, 0.88f),
                rKnee = o(0.53f, 0.71f),
                rFoot = o(0.53f, 0.88f),
            ).shifted(-0.07f)
    private val jackOpen =
        stand.copy(
            head = o(0.50f, 0.16f),
            lElbow = o(0.34f, 0.20f),
            lHand = o(0.40f, 0.07f),
            rElbow = o(0.66f, 0.20f),
            rHand = o(0.60f, 0.07f),
            lKnee = o(0.40f, 0.72f),
            lFoot = o(0.33f, 0.90f),
            rKnee = o(0.60f, 0.72f),
            rFoot = o(0.67f, 0.90f),
        )
    private val kneeUpLeft =
        stand.copy(
            lElbow = o(0.40f, 0.42f),
            lHand = o(0.40f, 0.51f),
            rElbow = o(0.61f, 0.40f),
            rHand = o(0.56f, 0.33f),
            lKnee = o(0.44f, 0.58f),
            lFoot = o(0.45f, 0.75f),
        )

    private fun armsOut(dy: Float) =
        stand.copy(
            lElbow = o(0.31f, 0.30f),
            lHand = o(0.20f, 0.30f + dy),
            rElbow = o(0.69f, 0.30f),
            rHand = o(0.80f, 0.30f + dy),
        )

    private val pressBottom =
        stand.copy(
            lElbow = o(0.32f, 0.32f),
            lHand = o(0.33f, 0.20f),
            rElbow = o(0.68f, 0.32f),
            rHand = o(0.67f, 0.20f),
        )
    private val pressTop =
        stand.copy(
            lElbow = o(0.38f, 0.17f),
            lHand = o(0.41f, 0.05f),
            rElbow = o(0.62f, 0.17f),
            rHand = o(0.59f, 0.05f),
        )
    private val sideBend =
        stand.copy(
            head = o(0.43f, 0.19f),
            neck = o(0.46f, 0.29f),
            lElbow = o(0.36f, 0.42f),
            lHand = o(0.44f, 0.52f),
            rElbow = o(0.57f, 0.15f),
            rHand = o(0.45f, 0.07f),
            lKnee = o(0.44f, 0.72f),
            lFoot = o(0.41f, 0.90f),
            rKnee = o(0.56f, 0.72f),
            rFoot = o(0.59f, 0.90f),
        )
    private val breatheIn =
        stand.copy(
            head = o(0.50f, 0.165f),
            neck = o(0.50f, 0.275f),
            lElbow = o(0.39f, 0.40f),
            lHand = o(0.36f, 0.51f),
            rElbow = o(0.61f, 0.40f),
            rHand = o(0.64f, 0.51f),
            shoulderSpread = 0.085f,
        )

    // ------------------------------------------------------------- side view (facing left)
    private val pushUpTop =
        Pose(
            head = o(0.198f, 0.621f),
            neck = o(0.29f, 0.66f),
            hip = o(0.528f, 0.761f),
            lElbow = o(0.29f, 0.78f),
            lHand = o(0.29f, 0.90f),
            rElbow = o(0.30f, 0.78f),
            rHand = o(0.30f, 0.90f),
            lKnee = o(0.694f, 0.83f),
            lFoot = o(0.86f, 0.90f),
            rKnee = o(0.70f, 0.83f),
            rFoot = o(0.87f, 0.90f),
        )
    private val pushUpBottom =
        Pose(
            head = o(0.146f, 0.807f),
            neck = o(0.245f, 0.82f),
            hip = o(0.503f, 0.854f),
            lElbow = o(0.355f, 0.795f),
            lHand = o(0.29f, 0.90f),
            rElbow = o(0.365f, 0.795f),
            rHand = o(0.30f, 0.90f),
            lKnee = o(0.682f, 0.877f),
            lFoot = o(0.86f, 0.90f),
            rKnee = o(0.69f, 0.877f),
            rFoot = o(0.87f, 0.90f),
        )
    private val forearmPlank =
        Pose(
            head = o(0.154f, 0.761f),
            neck = o(0.252f, 0.78f),
            hip = o(0.507f, 0.83f),
            lElbow = o(0.252f, 0.90f),
            lHand = o(0.14f, 0.90f),
            rElbow = o(0.262f, 0.90f),
            rHand = o(0.15f, 0.90f),
            lKnee = o(0.684f, 0.865f),
            lFoot = o(0.86f, 0.90f),
            rKnee = o(0.69f, 0.865f),
            rFoot = o(0.87f, 0.90f),
        )
    private val forearmPlankBreath = forearmPlank.copy(hip = o(0.507f, 0.815f), lKnee = o(0.684f, 0.857f), rKnee = o(0.69f, 0.857f))
    private val shoulderTap = pushUpTop.copy(rElbow = o(0.36f, 0.73f), rHand = o(0.31f, 0.67f))
    private val climberLeft =
        pushUpTop.copy(
            hip = o(0.528f, 0.745f),
            lKnee = o(0.42f, 0.77f),
            lFoot = o(0.50f, 0.87f),
        )
    private val climberRight =
        pushUpTop.copy(
            hip = o(0.528f, 0.745f),
            rKnee = o(0.42f, 0.77f),
            rFoot = o(0.50f, 0.87f),
        )
    private val sideStand =
        Pose(
            head = o(0.50f, 0.17f),
            neck = o(0.50f, 0.28f),
            hip = o(0.50f, 0.54f),
            lElbow = o(0.49f, 0.41f),
            lHand = o(0.48f, 0.53f),
            rElbow = o(0.51f, 0.41f),
            rHand = o(0.50f, 0.53f),
            lKnee = o(0.49f, 0.72f),
            lFoot = o(0.49f, 0.90f),
            rKnee = o(0.51f, 0.72f),
            rFoot = o(0.51f, 0.90f),
        )
    private val lungeTop =
        Pose(
            head = o(0.50f, 0.17f),
            neck = o(0.50f, 0.28f),
            hip = o(0.50f, 0.54f),
            lElbow = o(0.57f, 0.41f),
            lHand = o(0.50f, 0.52f),
            rElbow = o(0.58f, 0.42f),
            rHand = o(0.51f, 0.53f),
            lKnee = o(0.47f, 0.72f),
            lFoot = o(0.46f, 0.90f),
            rKnee = o(0.53f, 0.72f),
            rFoot = o(0.55f, 0.90f),
        )
    private val lungeBottom =
        Pose(
            head = o(0.50f, 0.30f),
            neck = o(0.50f, 0.41f),
            hip = o(0.50f, 0.67f),
            lElbow = o(0.57f, 0.54f),
            lHand = o(0.50f, 0.65f),
            rElbow = o(0.58f, 0.55f),
            rHand = o(0.51f, 0.66f),
            lKnee = o(0.34f, 0.70f),
            lFoot = o(0.34f, 0.90f),
            rKnee = o(0.58f, 0.86f),
            rFoot = o(0.72f, 0.90f),
        )
    private val bridgeDown =
        Pose(
            head = o(0.20f, 0.86f),
            neck = o(0.29f, 0.88f),
            hip = o(0.55f, 0.88f),
            lElbow = o(0.40f, 0.895f),
            lHand = o(0.50f, 0.895f),
            rElbow = o(0.41f, 0.895f),
            rHand = o(0.51f, 0.895f),
            lKnee = o(0.67f, 0.72f),
            lFoot = o(0.76f, 0.90f),
            rKnee = o(0.68f, 0.72f),
            rFoot = o(0.77f, 0.90f),
        )
    private val bridgeUp =
        bridgeDown.copy(
            hip = o(0.55f, 0.73f),
            lKnee = o(0.68f, 0.69f),
            rKnee = o(0.69f, 0.69f),
        )
    private val crunchDown =
        Pose(
            head = o(0.21f, 0.86f),
            neck = o(0.30f, 0.88f),
            hip = o(0.56f, 0.88f),
            lElbow = o(0.27f, 0.80f),
            lHand = o(0.22f, 0.84f),
            rElbow = o(0.28f, 0.80f),
            rHand = o(0.23f, 0.84f),
            lKnee = o(0.67f, 0.72f),
            lFoot = o(0.77f, 0.90f),
            rKnee = o(0.68f, 0.72f),
            rFoot = o(0.78f, 0.90f),
        )
    private val crunchUp =
        crunchDown.copy(
            head = o(0.30f, 0.72f),
            neck = o(0.37f, 0.79f),
            lElbow = o(0.40f, 0.71f),
            lHand = o(0.30f, 0.71f),
            rElbow = o(0.41f, 0.71f),
            rHand = o(0.31f, 0.71f),
            spineBend = 0.02f,
        )
    private val cat =
        Pose(
            head = o(0.25f, 0.74f),
            neck = o(0.32f, 0.64f),
            hip = o(0.62f, 0.64f),
            lElbow = o(0.32f, 0.77f),
            lHand = o(0.32f, 0.90f),
            rElbow = o(0.33f, 0.77f),
            rHand = o(0.33f, 0.90f),
            lKnee = o(0.62f, 0.90f),
            lFoot = o(0.80f, 0.90f),
            rKnee = o(0.63f, 0.90f),
            rFoot = o(0.81f, 0.90f),
            spineBend = -0.07f,
        )
    private val cow = cat.copy(head = o(0.24f, 0.58f), neck = o(0.32f, 0.66f), hip = o(0.62f, 0.65f), spineBend = 0.05f)
    private val fold =
        Pose(
            head = o(0.40f, 0.82f),
            neck = o(0.42f, 0.74f),
            hip = o(0.53f, 0.54f),
            lElbow = o(0.42f, 0.82f),
            lHand = o(0.43f, 0.90f),
            rElbow = o(0.43f, 0.82f),
            rHand = o(0.44f, 0.90f),
            lKnee = o(0.51f, 0.72f),
            lFoot = o(0.50f, 0.90f),
            rKnee = o(0.52f, 0.72f),
            rFoot = o(0.51f, 0.90f),
            spineBend = -0.02f,
        )
    private val burpeeCrouch =
        Pose(
            head = o(0.36f, 0.55f),
            neck = o(0.40f, 0.62f),
            hip = o(0.58f, 0.74f),
            lElbow = o(0.36f, 0.76f),
            lHand = o(0.33f, 0.90f),
            rElbow = o(0.37f, 0.76f),
            rHand = o(0.34f, 0.90f),
            lKnee = o(0.45f, 0.70f),
            lFoot = o(0.52f, 0.90f),
            rKnee = o(0.46f, 0.70f),
            rFoot = o(0.53f, 0.90f),
        )

    fun keyframes(motion: MotionType): List<Pose> =
        when (motion) {
            MotionType.SQUAT -> listOf(standArmsForward, squatDown)
            MotionType.JUMP_SQUAT -> listOf(squatDown, airborne)
            MotionType.JUMPING_JACK -> listOf(stand, jackOpen)
            MotionType.PUSH_UP -> listOf(pushUpTop, pushUpBottom)
            MotionType.PLANK -> listOf(forearmPlank, forearmPlankBreath)
            MotionType.PLANK_UP_DOWN -> listOf(forearmPlank, pushUpTop)
            MotionType.SHOULDER_TAP -> listOf(pushUpTop, shoulderTap)
            MotionType.MOUNTAIN_CLIMBER -> listOf(climberLeft, climberRight)
            MotionType.HIGH_KNEES -> listOf(kneeUpLeft, kneeUpLeft.mirrored())
            MotionType.LUNGE -> listOf(lungeTop, lungeBottom)
            MotionType.GLUTE_BRIDGE -> listOf(bridgeDown, bridgeUp)
            MotionType.CRUNCH -> listOf(crunchDown, crunchUp)
            MotionType.CAT_COW -> listOf(cat, cow)
            MotionType.SIDE_STRETCH -> listOf(sideBend, sideBend.mirrored())
            MotionType.FORWARD_FOLD -> listOf(sideStand, fold)
            MotionType.ARM_CIRCLES -> listOf(armsOut(-0.06f), armsOut(0f), armsOut(0.06f))
            MotionType.OVERHEAD_PRESS -> listOf(pressBottom, pressTop)
            MotionType.BURPEE -> listOf(sideStand, burpeeCrouch, pushUpTop)
            MotionType.BREATH -> listOf(stand, breatheIn)
        }

    /** Milliseconds per keyframe transition at normal tempo. */
    fun segmentMillis(motion: MotionType): Int =
        when (motion) {
            MotionType.SQUAT -> 900
            MotionType.JUMP_SQUAT -> 550
            MotionType.JUMPING_JACK -> 420
            MotionType.PUSH_UP -> 900
            MotionType.PLANK -> 1800
            MotionType.PLANK_UP_DOWN -> 1000
            MotionType.SHOULDER_TAP -> 600
            MotionType.MOUNTAIN_CLIMBER -> 330
            MotionType.HIGH_KNEES -> 380
            MotionType.LUNGE -> 1000
            MotionType.GLUTE_BRIDGE -> 1000
            MotionType.CRUNCH -> 900
            MotionType.CAT_COW -> 2200
            MotionType.SIDE_STRETCH -> 2400
            MotionType.FORWARD_FOLD -> 2200
            MotionType.ARM_CIRCLES -> 450
            MotionType.OVERHEAD_PRESS -> 900
            MotionType.BURPEE -> 650
            MotionType.BREATH -> 2400
        }

    fun sample(
        keyframes: List<Pose>,
        progress: Float,
    ): Pose {
        if (keyframes.size == 1) return keyframes[0]
        val p = progress.coerceIn(0f, (keyframes.size - 1).toFloat())
        val i = floor(p).toInt().coerceAtMost(keyframes.size - 2)
        return lerpPose(keyframes[i], keyframes[i + 1], p - i)
    }
}

private fun DrawScope.drawFigure(
    pose: Pose,
    primary: Color,
    secondary: Color,
    showFloor: Boolean,
) {
    val s = min(size.width, size.height)
    val ox = (size.width - s) / 2f
    val oy = (size.height - s) / 2f

    fun px(p: Offset) = Offset(ox + p.x * s, oy + p.y * s)

    val limb = s * 0.034f
    val glow = limb * 2.8f

    if (showFloor) {
        val floorY = oy + 0.905f * s
        drawOval(
            brush =
                Brush.radialGradient(
                    listOf(primary.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(ox + 0.5f * s, floorY),
                    radius = s * 0.42f,
                ),
            topLeft = Offset(ox + 0.08f * s, floorY - 0.035f * s),
            size = Size(0.84f * s, 0.07f * s),
        )
        drawLine(
            brush =
                Brush.horizontalGradient(
                    listOf(Color.Transparent, primary.copy(alpha = 0.55f), Color.Transparent),
                    startX = ox + 0.05f * s,
                    endX = ox + 0.95f * s,
                ),
            start = Offset(ox + 0.05f * s, floorY),
            end = Offset(ox + 0.95f * s, floorY),
            strokeWidth = s * 0.006f,
        )
    }

    val neck = px(pose.neck)
    val hip = px(pose.hip)
    val lShoulder = px(Offset(pose.neck.x - pose.shoulderSpread, pose.neck.y + 0.012f))
    val rShoulder = px(Offset(pose.neck.x + pose.shoulderSpread, pose.neck.y + 0.012f))
    val lHip = px(Offset(pose.hip.x - pose.hipSpread, pose.hip.y))
    val rHip = px(Offset(pose.hip.x + pose.hipSpread, pose.hip.y))

    fun bone(
        a: Offset,
        b: Offset,
        color: Color,
    ) {
        drawLine(color.copy(alpha = 0.22f), a, b, strokeWidth = glow, cap = StrokeCap.Round)
        drawLine(color, a, b, strokeWidth = limb, cap = StrokeCap.Round)
    }

    // Far side first (secondary colour), near side last.
    bone(rShoulder, px(pose.rElbow), secondary)
    bone(px(pose.rElbow), px(pose.rHand), secondary)
    bone(rHip, px(pose.rKnee), secondary)
    bone(px(pose.rKnee), px(pose.rFoot), secondary)

    // Torso as a curved spine.
    val dir = hip - neck
    val len = sqrt(dir.x * dir.x + dir.y * dir.y).coerceAtLeast(1f)
    val normal = Offset(-dir.y / len, dir.x / len)
    val mid = Offset((neck.x + hip.x) / 2f, (neck.y + hip.y) / 2f)
    val control = mid + normal * (pose.spineBend * 2f * s)
    val spine =
        Path().apply {
            moveTo(neck.x, neck.y)
            quadraticBezierTo(control.x, control.y, hip.x, hip.y)
        }
    drawPath(spine, primary.copy(alpha = 0.22f), style = Stroke(glow * 1.15f, cap = StrokeCap.Round))
    drawPath(spine, Brush.linearGradient(listOf(primary, secondary), neck, hip), style = Stroke(limb * 1.25f, cap = StrokeCap.Round))
    if (pose.shoulderSpread > 0.01f) bone(lShoulder, rShoulder, primary)
    if (pose.hipSpread > 0.01f) bone(lHip, rHip, secondary)

    bone(lHip, px(pose.lKnee), primary)
    bone(px(pose.lKnee), px(pose.lFoot), primary)
    bone(lShoulder, px(pose.lElbow), primary)
    bone(px(pose.lElbow), px(pose.lHand), primary)

    // Joints
    val joint = limb * 0.48f
    listOf(pose.lElbow, pose.rElbow, pose.lKnee, pose.rKnee, pose.lHand, pose.rHand).forEach {
        drawCircle(Color.White.copy(alpha = 0.9f), radius = joint, center = px(it))
    }

    // Head
    val head = px(pose.head)
    val headRadius = s * 0.056f
    drawCircle(primary.copy(alpha = 0.18f), radius = headRadius * 1.6f, center = head)
    drawCircle(primary.copy(alpha = 0.22f), radius = headRadius, center = head)
    drawCircle(primary, radius = headRadius, center = head, style = Stroke(limb * 0.9f))
}
