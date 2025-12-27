package com.alijafari.brik.main.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.alijafari.brik.R

@Composable
fun SwipeHintArrows(isDraggingUp: Boolean, hint: String, modifier: Modifier = Modifier) {

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        @Composable
        fun hintText() = Text(
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f),
            text = hint
        )
        if (!isDraggingUp) {
            hintText()
            Spacer(Modifier.height(5.dp))
        }
        repeat(3) { _index ->
            val index = _index.takeIf { !isDraggingUp }?:(3-_index)

            val transition = rememberInfiniteTransition()

            val offset = index * 100
            val duration = 900
            val delay = 500

            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = duration,
                        delayMillis = delay,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(offset)
                )
            )

            val scale by transition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = duration,
                        delayMillis = delay,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(offset)
                )
            )

            val offsetY by transition.animateValue(
                initialValue = if (isDraggingUp) 5.dp else  0.dp,
                targetValue = if (isDraggingUp) 0.dp else 5.dp,
                typeConverter = Dp.VectorConverter,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        delayMillis = delay,
                        durationMillis = duration,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(offset)
                )
            )

            Icon(
                painter = painterResource(R.drawable.ic_arrow_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                modifier = Modifier
                    .size(20.dp)
                    .scale(scale)
                    .offset(y = offsetY)
                    .zIndex(-1f)
            )
            Spacer(Modifier.height(5.dp))
        }
        if (isDraggingUp) {
            hintText()
        }
    }
}