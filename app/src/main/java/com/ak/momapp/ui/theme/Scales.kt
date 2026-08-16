package com.ak.momapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

/** The most a control grows for accessibility before it stops. */
private const val MaxControlScale = 1.45f

/**
 * How much larger to draw touch targets at a given system font scale.
 *
 * Never below 1: a button must not shrink because the skin's text baseline
 * did. Never above [MaxControlScale]: a keypad that grew with a 3x font
 * setting would take the whole screen.
 */
fun controlScaleFor(fontScale: Float): Float =
    fontScale.coerceIn(1f, MaxControlScale)

val LocalControlScale = staticCompositionLocalOf { 1f }

/** This dimension, grown for accessibility but not past the ceiling. */
@Composable
@ReadOnlyComposable
fun Dp.asControl(): Dp = this * LocalControlScale.current

/**
 * Where the problem text's shrink floor stops growing.
 *
 * Above this scale the floor holds its absolute size instead of growing
 * with her setting, which is what lets a long question shrink onto one
 * screen at a large font size.
 *
 * This is NOT the chrome ceiling, and must not be wired to it. They share
 * a number today and answer different questions: the chrome ceiling is
 * about rows keeping their shape, and Modern has none because its rows
 * reflow. Reading this from the skin would give Modern an infinite floor
 * ceiling, so the floor would grow forever and long text could never
 * shrink to fit, in exactly the accessibility case that matters most.
 */
const val ProblemFloorScaleCeiling = 1.5f
