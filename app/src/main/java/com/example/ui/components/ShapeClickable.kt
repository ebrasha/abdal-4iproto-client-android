/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : ShapeClickable.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 04:40:54
 * Description : Shared shapes and clipped bounded-ripple click modifiers for pill and card UI.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/** Full capsule / pill shape for chips and status pills. */
val PillShape: Shape = RoundedCornerShape(50)

/** Home dashboard card corners. */
val DashboardCardShape: Shape = RoundedCornerShape(20.dp)

/** Small rounded toggle / icon button on the server card. */
val ToggleButtonShape: Shape = RoundedCornerShape(10.dp)

/** List item cards in server management. */
val ListCardShape: Shape = RoundedCornerShape(12.dp)

/**
 * Applies [clip] then a bounded [ripple] so touch feedback matches [shape] exactly.
 * Use the same [shape] for background and border on the same component.
 */
@Composable
fun Modifier.shapeClickable(
    shape: Shape,
    onClick: () -> Unit,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier = this
    .clip(shape)
    .clickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true),
        enabled = enabled,
        onClick = onClick
    )

/**
 * Pill chip with matching background and bounded capsule ripple.
 */
@Composable
fun Modifier.pillBackgroundClickable(
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    horizontalPadding: Dp = 10.dp,
    verticalPadding: Dp = 5.dp
): Modifier = this
    .clip(PillShape)
    .background(color, PillShape)
    .shapeClickable(PillShape, onClick, enabled)
    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
