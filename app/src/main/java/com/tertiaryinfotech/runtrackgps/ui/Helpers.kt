package com.tertiaryinfotech.runtrackgps.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tap without the ripple/indication — used for the custom preset tiles. */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}

private val dateFormat = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())

/** Formats an epoch-millis timestamp like "14 Jun 2026, 5:30 PM". */
fun formatDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))
