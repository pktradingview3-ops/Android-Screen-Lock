package com.timewall.app.domain

/** Pure Kotlin models. No Android imports, so these are unit-testable on the JVM. */

enum class Orientation { VERTICAL, HORIZONTAL }

/**
 * The five approved layouts from design/wallpaper-mockups.
 * Positions and sizes live in WallpaperRenderer, measured from those mockups.
 */
enum class LayoutId(
    val label: String,
    val orientation: Orientation,
    /** Whether this layout shows the date line (matches the approved mockups). */
    val supportsDate: Boolean,
) {
    L1("Vertical stack, left", Orientation.VERTICAL, supportsDate = true),
    L2("Vertical stack, center", Orientation.VERTICAL, supportsDate = false),
    L3("Vertical condensed, right", Orientation.VERTICAL, supportsDate = true),
    L4("Horizontal single line", Orientation.HORIZONTAL, supportsDate = true),
    L5("Horizontal wide, dot", Orientation.HORIZONTAL, supportsDate = false);

    companion object {
        fun fromName(name: String?): LayoutId =
            values().firstOrNull { it.name == name } ?: L1
    }
}

data class WallpaperConfig(
    val layout: LayoutId = LayoutId.L1,
    val use24h: Boolean = false,
    val showDate: Boolean = true,
    val name: String = "",
) {
    /** Name as it will be drawn: trimmed and capped at MAX_NAME_LENGTH characters. */
    val displayName: String
        get() = name.trim().take(MAX_NAME_LENGTH)

    /** Date is drawn only if the user wants it AND the layout has room for it. */
    val isDateVisible: Boolean
        get() = showDate && layout.supportsDate

    companion object {
        const val MAX_NAME_LENGTH = 20
    }
}
