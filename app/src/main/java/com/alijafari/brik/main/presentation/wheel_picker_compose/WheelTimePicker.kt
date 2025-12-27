package com.alijafari.brik.main.presentation.wheel_picker_compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alijafari.brik.main.presentation.wheel_picker_compose.core.WheelPicker
import java.time.LocalTime

@Composable
fun WheelTimePicker(
    time: LocalTime,
    modifier: Modifier = Modifier,
    size: DpSize = DpSize(258.dp, 200.dp),
    rowCount: Int = 3,
    textStyle: TextStyle = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.W800),
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onSnappedTime: (LocalTime) -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(size.width, size.height / rowCount),
            shape = RoundedCornerShape(19.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(5.dp, MaterialTheme.colorScheme.primary)
        ) {}

        Row(verticalAlignment = Alignment.CenterVertically) {
            WheelPicker(
                startIndex = time.hour,
                count = 12,
                size = DpSize(size.width / 2, size.height),
                rowCount = rowCount,
                onScrollFinished = { snappedIndex ->
                    onSnappedTime(time.withHour(snappedIndex))
                }
            ) { index ->
                Text(
                    text = index.toString().padStart(2, '0'),
                    style = textStyle,
                    color = textColor
                )
            }
            Text(":", style = textStyle, color = textColor)

            WheelPicker(
                startIndex = time.minute,
                count = 59,
                size = DpSize(size.width / 2, size.height),
                rowCount = rowCount,
                onScrollFinished = { snappedIndex ->
                    onSnappedTime(time.withMinute(snappedIndex))
                }
            ) { index ->
                Text(
                    text = index.toString().padStart(2, '0'),
                    style = textStyle,
                    color = textColor
                )
            }
        }
    }
}