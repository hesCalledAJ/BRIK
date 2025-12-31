package com.alijafari.brik.main.presentation

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.brik.main.presentation.wheel_picker_compose.WheelTimePicker
import com.alijafari.brik.main.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MainScreen(
    viewModel: MainViewModel,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val total by viewModel.totalSeconds.collectAsState()
    val remaining by viewModel.remainingSeconds.collectAsState()

    val missingPermissions = viewModel.missingPermissions

    val isSessionActive = total > 0
    val hasMissingPermissions = missingPermissions.isNotEmpty()


    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        val screenWidthPx = with(density) { constraints.maxWidth.toFloat() }
        val screenHeightPx = with(density) { constraints.maxHeight.toFloat() }

        val shapeHeightPx = screenWidthPx
        val shapeHeightDp = with(density) { shapeHeightPx.toDp() }

        val startAnchor = -shapeHeightPx * 0.5f
        val centerAnchor = (screenHeightPx / 2f) - (shapeHeightPx / 2f)

        val state = remember(centerAnchor, startAnchor) {
            SessionGestureState(
                scope = scope,
                startAnchor = startAnchor,
                centerAnchor = centerAnchor,
                initialOffset = if (isSessionActive) centerAnchor else startAnchor,
                onSessionStart = {
                    if (viewModel.selectedDuration.value != 0){
                        viewModel.sessionStart()
                        true
                    }
                    else false
                }
            )
        }
        LaunchedEffect(isSessionActive) {
            if (isSessionActive) {
                if (state.offset != centerAnchor){
                    state.snapToCenter()
                }
            } else {
                state.snapToStart()
            }
        }
        if (hasMissingPermissions) {
            PermissionCarousel(
                requirements = missingPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        } else {
            DurationPicker(state, viewModel)
        }

        val infiniteTransition = rememberInfiniteTransition()
        val rotation by infiniteTransition.animateFloat(
            0f, 360f, infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(0, state.offset.roundToInt()) }
                .fillMaxWidth()
                .height(shapeHeightDp)
                .graphicsLayer {
                    rotationZ = rotation
                }
                .background(
                    color = if (hasMissingPermissions) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    shape = MaterialShapes.Cookie12Sided.toShape()
                ),
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(0, state.offset.roundToInt()) }
                .fillMaxWidth()
                .height(shapeHeightDp)
                .pointerInput(isSessionActive, hasMissingPermissions) {
                    if (isSessionActive || hasMissingPermissions) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = { state.isDragging = true },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            state.onDrag(dragAmount)
                        },
                        onDragEnd = { state.onRelease() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(isSessionActive || state.progress > 0.7f) {
                RemainingTime(remaining, total, isSessionActive)
            }
        }


        AnimatedVisibility(
            state.offset == startAnchor && !hasMissingPermissions
        ) {
            SwipeHintArrows(
                isDraggingUp = false,
                hint = "",
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        IntOffset(
                            0,
                            (state.offset + shapeHeightPx).roundToInt()
                        )
                    }
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.DurationPicker(
    state: SessionGestureState,
    viewModel: MainViewModel,
) {
    val currentDurationSeconds by viewModel.selectedDuration
    var showAllShortcuts by remember { mutableStateOf(false) }

    val endTime = remember(currentDurationSeconds) {
        LocalTime.now().plusSeconds(currentDurationSeconds.toLong())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    val currentTime = remember(currentDurationSeconds) {
        val h = (currentDurationSeconds / 3600)
        val m = ((currentDurationSeconds % 3600) / 60)
        LocalTime.of(h.coerceIn(0, 23), m.coerceIn(0, 59))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(bottom = 40.dp)
            .graphicsLayer {
                alpha = (1f - (state.progress * 1.2f)).coerceIn(0f, 1f)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text(
            "BRIK",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(5.dp))
        WheelTimePicker(
            time = currentTime,
            onSnappedTime = { newTime ->
                viewModel.setDuration((newTime.hour * 3600) + (newTime.minute * 60))
            }
        )

        FlowRow(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            maxItemsInEachRow = 4
        ) {
            val shortcuts = if (showAllShortcuts) {
                listOf(15, 25, 45, 60, 90, 120)
            } else {
                listOf(25, 45, 60)
            }

            shortcuts.forEach { mins ->
                ShortcutChip(minutes = mins) {
                    viewModel.setDuration(mins * 60)
                }
            }
            ShortcutChip(
                label = if (showAllShortcuts) "−" else "+",
                minutes = 0,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                showAllShortcuts = !showAllShortcuts
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "*Phone will be blocked until $endTime",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ShortcutChip(
    label: String? = null,
    minutes: Int,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onClick: () -> Unit,
) {
    val text = label ?: if (minutes >= 60)
        "${minutes / 60}h" + if (minutes % 60 == 0) "" else "${minutes % 60}m"
    else
        "${minutes}m"


    Surface(
        onClick = onClick,
        modifier = Modifier.padding(3.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

class SessionGestureState(
    private val scope: CoroutineScope,
    private val startAnchor: Float,
    private val centerAnchor: Float,
    initialOffset : Float,
    private val onSessionStart: () -> Boolean,
) {
    private val _offset = Animatable(initialOffset)
    val offset: Float get() = _offset.value

    var isDragging by mutableStateOf(false)

    val progress by derivedStateOf {
        ((offset - startAnchor) / (centerAnchor - startAnchor)).coerceIn(0f, 1f)
    }

    fun onDrag(delta: Float) {
        scope.launch {
            _offset.snapTo((_offset.value + delta).coerceIn(startAnchor, centerAnchor))
        }
    }
    fun snapToCenter() {
        scope.launch {
            _offset.animateTo(
                centerAnchor,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
            )
        }
    }

    fun snapToStart() {
        scope.launch {
            _offset.animateTo(
                startAnchor,
                spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
            )
        }
    }
    fun onRelease() {
        isDragging = false
        scope.launch {

            val distanceToCenter = abs(offset - centerAnchor)
            val snapThreshold = 300f
            val shouldStart = distanceToCenter < snapThreshold || progress > 0.6f

            if (shouldStart) {
                val success = onSessionStart()
                if (!success) {
                    snapToStart()
                }
            } else {
                snapToStart()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RemainingTime(
    remainingSeconds: Int,
    totalSeconds: Int,
    isSessionActive: Boolean,
) {
    val remainingFormatted = remember(remainingSeconds) {
        val h = remainingSeconds / 3600
        val m = (remainingSeconds % 3600) / 60
        val s = remainingSeconds % 60
        if (h > 0) {
            "%02d:%02d:%02d".format(h, m, s)
        } else {
            "%02d:%02d".format(m, s)
        }
    }

    val totalFormatted = remember(totalSeconds) {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        "%02d:%02d".format(h, m)
    }
    AnimatedContent(isSessionActive) {
        if (it) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "REMAINING",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = remainingFormatted,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 48.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "of ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                    )
                    Text(
                        text = totalFormatted,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            Text(
                text = "RELEASE\nTO START",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
