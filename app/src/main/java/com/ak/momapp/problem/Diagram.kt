package com.ak.momapp.problem

/**
 * A small schematic figure the UI draws under the problem text — every
 * geometry problem carries one. Purely descriptive: the UI picks colors
 * and sizes. Labels arrive ready to print ("12 m", "A = 84 m²", "?"), so
 * the drawing code stays language-blind, and the unknown is always "?".
 * Figures are schematic, not to scale.
 */
sealed interface Diagram {

    /**
     * A rectangle labeled on its width and height. [aspect] is the raw
     * width/height ratio; the UI clamps it to something drawable.
     */
    data class Rectangle(
        val widthLabel: String,
        val heightLabel: String,
        val aspect: Float = 1.6f,
        /** Tile the inside with faint grid lines (area problems). */
        val grid: Boolean = false,
        /** Written in the middle (e.g. "A = 84 m²" for border problems). */
        val innerLabel: String? = null,
        /** Draws a dashed ring around the shape with this label ("× 4"). */
        val lapsLabel: String? = null,
    ) : Diagram

    /** A square with a dashed ring for its measured edge (fence, trim). */
    data class Square(
        val sideLabel: String,
        val edgeLabel: String,
    ) : Diagram

    /**
     * A right triangle: horizontal bottom leg, vertical right leg, and
     * the slanted long side (ladder, shortcut). [aspect] is the raw
     * bottom/right leg ratio; the UI clamps it.
     */
    data class RightTriangle(
        val bottomLabel: String,
        val rightLabel: String,
        val slantLabel: String,
        val aspect: Float = 1.2f,
    ) : Diagram

    /**
     * A triangle labeled by its angles and drawn with the two real base
     * angles, arcs marking each corner.
     */
    data class AngleTriangle(
        val leftDegrees: Int,
        val rightDegrees: Int,
        val leftLabel: String,
        val rightLabel: String,
        val topLabel: String,
    ) : Diagram

    /** A box in cavalier projection, labeled on three edges. */
    data class Box(
        val lengthLabel: String,
        val depthLabel: String,
        val heightLabel: String,
    ) : Diagram

    /**
     * A row of little boxes for what-comes-next riddles. The last value
     * is the unknown — "?" by convention — and drawn highlighted.
     */
    data class SequenceRow(val values: List<String>) : Diagram
}
