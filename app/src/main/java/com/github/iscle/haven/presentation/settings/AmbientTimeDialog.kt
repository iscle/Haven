package com.github.iscle.haven.presentation.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.iscle.haven.presentation.common.HavenFilledButton
import com.github.iscle.haven.presentation.common.HavenMotion
import com.github.iscle.haven.presentation.common.HavenOutlinedButton
import com.github.iscle.haven.presentation.common.HavenSwitch
import com.github.iscle.haven.presentation.common.formatMinutesOfDay
import com.github.iscle.haven.presentation.common.formatTime
import com.github.iscle.haven.ui.theme.HavenAccent
import com.github.iscle.haven.ui.theme.HavenOnSurface
import com.github.iscle.haven.ui.theme.HavenOnSurfaceFaint
import com.github.iscle.haven.ui.theme.HavenOnSurfaceMuted

/** Which end of the ambient window is being edited. */
enum class AmbientEdge { START, END }

/**
 * 2J — the hour/minute picker behind "Starts" and "Ends", with the option to
 * hand the schedule over to the sun instead.
 */
@Composable
fun AmbientTimeDialog(
    edge: AmbientEdge,
    initialMinutes: Int,
    followSun: Boolean,
    sunTimeMillis: Long?,
    cityName: String,
    use24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int, followSun: Boolean) -> Unit
) {
    var hour by remember { mutableIntStateOf((initialMinutes / 60) % 24) }
    var minute by remember { mutableIntStateOf(initialMinutes % 60) }
    var useSun by remember { mutableStateOf(followSun) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .width(820.dp)
                .background(Color(0xFF171A20), RoundedCornerShape(28.dp))
                // The panel is only 800px tall, so the dialog scrolls rather
                // than pushing its buttons off screen.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 44.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (edge == AmbientEdge.START) {
                        "Ambient mode starts"
                    } else {
                        "Ambient mode ends"
                    },
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HavenOnSurface
                )
                Text(
                    text = if (edge == AmbientEdge.START) {
                        "The photo fades out and the clock dims to warm amber"
                    } else {
                        "The photo comes back and the clock returns to the day layout"
                    },
                    fontSize = 19.sp,
                    color = HavenOnSurfaceFaint
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeSpinner(
                    value = hour,
                    highlighted = true,
                    enabled = !useSun,
                    onChange = { hour = (it + 24) % 24 }
                )
                Text(
                    text = ":",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light,
                    color = HavenOnSurface.copy(alpha = 0.35f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TimeSpinner(
                    value = minute,
                    highlighted = false,
                    enabled = !useSun,
                    step = 5,
                    onChange = { minute = (it + 60) % 60 }
                )
            }

            if (sunTimeMillis != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(alpha = 0.045f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { useSun = !useSun }
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbTwilight,
                        contentDescription = null,
                        tint = HavenAccent,
                        modifier = Modifier.size(26.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (edge == AmbientEdge.START) {
                                "Follow sunset instead"
                            } else {
                                "Follow sunrise instead"
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HavenOnSurface
                        )
                        Text(
                            text = "Today in $cityName: ${formatTime(sunTimeMillis, use24Hour)}",
                            fontSize = 18.sp,
                            color = HavenOnSurfaceFaint
                        )
                    }
                    HavenSwitch(checked = useSun, onCheckedChange = { useSun = it })
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HavenOutlinedButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    height = 56.dp,
                    contentColor = HavenOnSurfaceMuted
                )
                HavenFilledButton(
                    text = if (useSun) {
                        "Follow the sun"
                    } else {
                        "Set ${formatMinutesOfDay(hour * 60 + minute, use24Hour)}"
                    },
                    onClick = { onConfirm(hour * 60 + minute, useSun) },
                    height = 56.dp
                )
            }
        }
    }
}

@Composable
private fun TimeSpinner(
    value: Int,
    highlighted: Boolean,
    enabled: Boolean,
    step: Int = 1,
    onChange: (Int) -> Unit
) {
    // Remember which way the value moved so the digits roll that way.
    var goingUp by remember { mutableStateOf(true) }
    val alpha = if (enabled) 1f else 0.4f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Increase",
            tint = HavenOnSurface.copy(alpha = 0.4f * alpha),
            modifier = Modifier
                .size(28.dp)
                .clickable(enabled = enabled) {
                    goingUp = true
                    onChange(value + step)
                }
        )
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(104.dp)
                .background(
                    color = if (highlighted) {
                        HavenAccent.copy(alpha = 0.14f * alpha)
                    } else {
                        Color.White.copy(alpha = 0.06f * alpha)
                    },
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    val height = if (goingUp) -1 else 1
                    (slideInVertically(tween(HavenMotion.CONTROL_MILLIS)) { it * height } +
                            fadeIn(tween(HavenMotion.CONTROL_MILLIS))) togetherWith
                            (slideOutVertically(tween(HavenMotion.CONTROL_MILLIS)) { -it * height } +
                                    fadeOut(tween(HavenMotion.CONTROL_MILLIS)))
                },
                label = "time_digit"
            ) { animatedValue ->
                Text(
                    text = "%02d".format(animatedValue),
                    fontSize = 62.sp,
                    color = if (highlighted) {
                        HavenAccent.copy(alpha = alpha)
                    } else {
                        HavenOnSurface.copy(alpha = alpha)
                    }
                )
            }
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Decrease",
            tint = HavenOnSurface.copy(alpha = 0.4f * alpha),
            modifier = Modifier
                .size(28.dp)
                .clickable(enabled = enabled) {
                    goingUp = false
                    onChange(value - step)
                }
        )
    }
}
