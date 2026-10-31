package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Feedback states for Duolingo-style Right & Wrong micro-animations across Mini-Games.
 */
enum class GameFeedbackState {
    IDLE,
    CORRECT,
    WRONG
}

object GameFeedbackColors {
    val EmeraldBorder = Color(0xFF34D399)
    val EmeraldTint = Color(0x2834D399)
    val EmeraldToastBg = Color(0xFF064E3B)
    val EmeraldToastText = Color(0xFF6EE7B7)

    val RoseBorder = Color(0xFFFB7185)
    val RoseTint = Color(0x2EFB7185)
    val RoseToastBg = Color(0xFF4C0519)
    val RoseToastText = Color(0xFFFDA4AF)
}

/**
 * Triggers crisp haptic feedback for right or wrong game actions.
 * - Right Answer: [HapticFeedbackType.TextHandleMove] (light crisp tap)
 * - Wrong Answer: [HapticFeedbackType.LongPress] (error buzz)
 */
fun HapticFeedback.performGameFeedback(isCorrect: Boolean) {
    runCatching {
        if (isCorrect) {
            performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else {
            performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

/**
 * Reusable controller that manages non-blocking Right (320ms) & Wrong (380ms) micro-animations
 * and floating toast badges.
 */
@Stable
class GameFeedbackController {
    var state by mutableStateOf(GameFeedbackState.IDLE)
        private set

    var toastMessage by mutableStateOf("")
        private set

    var toastVisible by mutableStateOf(false)
        private set

    var triggerCounter by mutableIntStateOf(0)
        private set

    fun triggerCorrect(message: String) {
        state = GameFeedbackState.CORRECT
        toastMessage = message
        toastVisible = true
        triggerCounter++
    }

    fun triggerWrong(message: String) {
        state = GameFeedbackState.WRONG
        toastMessage = message
        toastVisible = true
        triggerCounter++
    }

    fun resetCardState() {
        state = GameFeedbackState.IDLE
    }

    fun hideToast() {
        toastVisible = false
    }
}

@Composable
fun rememberGameFeedbackController(): GameFeedbackController {
    return remember { GameFeedbackController() }
}

/**
 * Applies the Horizontal Keyframe Shake (WrongState - 350ms) and Spring Scale Pop (CorrectState - 320ms)
 * to a card, button, or tile without occupying extra layout space.
 *
 * Keyframe Shake Spec:
 * 0ms -> 0f, 50ms -> -12f, 110ms -> 12f, 170ms -> -9f, 230ms -> 9f, 290ms -> -5f, 350ms -> 0f.
 *
 * Spring Scale Pop Spec:
 * 1.0f -> peakScale (default 1.05f) -> 1.0f using spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium).
 */
fun Modifier.gameFeedbackMotion(
    feedbackState: GameFeedbackState,
    triggerKey: Any = feedbackState,
    correctPeakScale: Float = 1.05f
): Modifier = composed {
    val shakeOffsetDp = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1f) }

    LaunchedEffect(feedbackState, triggerKey) {
        when (feedbackState) {
            GameFeedbackState.CORRECT -> {
                shakeOffsetDp.snapTo(0f)
                scaleAnim.snapTo(1f)
                scaleAnim.animateTo(
                    targetValue = correctPeakScale,
                    animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
                )
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            GameFeedbackState.WRONG -> {
                scaleAnim.snapTo(1f)
                shakeOffsetDp.snapTo(0f)
                shakeOffsetDp.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 350
                        0f at 0
                        -12f at 50
                        12f at 110
                        -9f at 170
                        9f at 230
                        -5f at 290
                        0f at 350
                    }
                )
            }
            GameFeedbackState.IDLE -> {
                shakeOffsetDp.snapTo(0f)
                scaleAnim.snapTo(1f)
            }
        }
    }

    this
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val xOffsetPx = shakeOffsetDp.value.dp.roundToPx()
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(xOffsetPx, 0)
            }
        }
        .graphicsLayer {
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
        }
}

/**
 * Animated border color that flashes Emerald (#34D399) on CORRECT or Rose (#FB7185) on WRONG
 * using animateColorAsState(tween(200)).
 */
@Composable
fun rememberFeedbackBorderColor(
    feedbackState: GameFeedbackState,
    idleColor: Color
): Color {
    val target = when (feedbackState) {
        GameFeedbackState.CORRECT -> GameFeedbackColors.EmeraldBorder
        GameFeedbackState.WRONG -> GameFeedbackColors.RoseBorder
        GameFeedbackState.IDLE -> idleColor
    }
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 200),
        label = "feedbackBorderColor"
    )
    return animated
}

/**
 * Animated background tint that flashes Emerald (0x2834D399) on CORRECT or Rose (0x2EFB7185) on WRONG
 * using animateColorAsState(tween(200)).
 */
@Composable
fun rememberFeedbackBackgroundColor(
    feedbackState: GameFeedbackState,
    idleColor: Color
): Color {
    val target = when (feedbackState) {
        GameFeedbackState.CORRECT -> GameFeedbackColors.EmeraldTint
        GameFeedbackState.WRONG -> GameFeedbackColors.RoseTint
        GameFeedbackState.IDLE -> idleColor
    }
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 200),
        label = "feedbackBackgroundColor"
    )
    return animated
}

/**
 * Floating Score / Streak Pop Badge (`+1 🔥` / `+10 pts • أحسنت!` / `✗ خطأ! -1 ❤️`).
 * Uses AnimatedVisibility with `slideInVertically { it / 2 } + fadeIn()` -> `slideOutVertically { -it } + fadeOut()`.
 */
@Composable
fun FloatingFeedbackBadge(
    visible: Boolean,
    feedbackState: GameFeedbackState,
    text: String,
    modifier: Modifier = Modifier
) {
    val isCorrect = feedbackState != GameFeedbackState.WRONG
    val bgColor = if (isCorrect) GameFeedbackColors.EmeraldToastBg else GameFeedbackColors.RoseToastBg
    val borderColor = if (isCorrect) GameFeedbackColors.EmeraldBorder else GameFeedbackColors.RoseBorder
    val textColor = if (isCorrect) GameFeedbackColors.EmeraldToastText else GameFeedbackColors.RoseToastText

    AnimatedVisibility(
        visible = visible && text.isNotBlank(),
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 2 }
        ) + fadeIn(animationSpec = tween(durationMillis = 150)),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            targetOffsetY = { -it }
        ) + fadeOut(animationSpec = tween(durationMillis = 160)),
        modifier = modifier.testTag("game_floating_feedback_badge")
    ) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(99.dp),
            border = BorderStroke(1.5.dp, borderColor),
            shadowElevation = 6.dp
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                ),
                color = textColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Survival Mode Heart Icon with Heart Break Animation:
 * When a life is lost (`isBreaking` or transition from alive to lost), animates with a scale pulse
 * (`1.35f -> 0.85f -> 1.0f`), tilt, and horizontal shake before settling into an empty muted heart outline.
 */
@Composable
fun AnimatedSurvivalHeartIcon(
    isAlive: Boolean,
    isBreakingNow: Boolean = false,
    size: Dp = 26.dp,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(if (isAlive) 1.0f else 0.85f) }
    val shakeOffsetDp = remember { Animatable(0f) }
    val rotationAnim = remember { Animatable(0f) }
    var wasAlive by remember { mutableStateOf(isAlive) }

    LaunchedEffect(isAlive, isBreakingNow) {
        if (isBreakingNow || (wasAlive && !isAlive)) {
            coroutineScope {
                launch {
                    scaleAnim.snapTo(1.0f)
                    scaleAnim.animateTo(1.35f, tween(110, easing = FastOutSlowInEasing))
                    scaleAnim.animateTo(0.85f, tween(130, easing = FastOutSlowInEasing))
                    scaleAnim.animateTo(
                        targetValue = 0.88f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                launch {
                    rotationAnim.snapTo(0f)
                    rotationAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            durationMillis = 350
                            0f at 0
                            -12f at 90
                            12f at 200
                            0f at 350
                        }
                    )
                }
                launch {
                    shakeOffsetDp.snapTo(0f)
                    shakeOffsetDp.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            durationMillis = 350
                            0f at 0
                            -6f at 60
                            6f at 130
                            -4f at 210
                            4f at 280
                            0f at 350
                        }
                    )
                }
            }
        } else if (!wasAlive && isAlive) {
            // Heart recovered!
            scaleAnim.snapTo(0.85f)
            scaleAnim.animateTo(1.25f, tween(140))
            scaleAnim.animateTo(
                1.0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        } else {
            scaleAnim.snapTo(if (isAlive) 1.0f else 0.85f)
        }
        wasAlive = isAlive
    }

    val showFilled = isAlive && !isBreakingNow
    val tintColor by animateColorAsState(
        targetValue = when {
            isBreakingNow -> Color(0xFFF43F5E)
            isAlive -> CoralAccent
            else -> TextMuted
        },
        animationSpec = tween(200),
        label = "survivalHeartTint"
    )

    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val xPx = shakeOffsetDp.value.dp.roundToPx()
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(xPx, 0)
                }
            }
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                rotationZ = rotationAnim.value
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (showFilled) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(size)
        )
    }
}

/**
 * Streak / Score pill scale bump modifier when streak increases.
 */
@Composable
fun Modifier.streakBumpScale(activeBump: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (activeBump) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "streakBumpScale"
    )
    return this.scale(scale)
}

/**
 * Snappy 180ms Horizontal Card Transition for center word/question content
 * in Survival Mode, 60-Second Blitz, and Grammar Error Hunter.
 *
 * Spec: `(slideInHorizontally { it / 4 } + fadeIn(tween(180))) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut(tween(140)))`
 */
@Composable
fun <S> SmoothQuestionSlideContent(
    targetState: S,
    modifier: Modifier = Modifier,
    contentKey: (S) -> Any? = { it },
    content: @Composable AnimatedContentScope.(targetState: S) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            (slideInHorizontally(
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                initialOffsetX = { (it / 4).coerceAtLeast(1) }
            ) + fadeIn(animationSpec = tween(durationMillis = 180))) togetherWith
                (slideOutHorizontally(
                    animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                    targetOffsetX = { -(it / 4).coerceAtLeast(1) }
                ) + fadeOut(animationSpec = tween(durationMillis = 140)))
        },
        contentKey = contentKey,
        label = "smoothQuestionSlide",
        content = content
    )
}
