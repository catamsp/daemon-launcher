package com.catamsp.Daemon.preferences.theme

import com.catamsp.Daemon.R

/**
 * Enumeration of available transition animations for app launches.
 */
@Suppress("unused")
enum class TransitionAnimation(val animIn: Int, val animOut: Int) {
    BOTTOM_UP(R.anim.bottom_up, android.R.anim.fade_out),
    TOP_DOWN(R.anim.top_down, android.R.anim.fade_out),
    LEFT_RIGHT(R.anim.left_right, android.R.anim.fade_out),
    RIGHT_LEFT(R.anim.right_left, android.R.anim.fade_out),
    FADE(android.R.anim.fade_in, android.R.anim.fade_out),
    TUNNEL(R.anim.tunnel_in, R.anim.tunnel_out),
    CUBE(R.anim.cube_in, R.anim.cube_out),
    FLIP(R.anim.flip_in, R.anim.flip_out),
    UNFOLD(R.anim.unfold_in, android.R.anim.fade_out),
    SETTLE(R.anim.settle_in, android.R.anim.fade_out),
    CRT(R.anim.crt_in, android.R.anim.fade_out),
    FLICKER(R.anim.flicker_in, android.R.anim.fade_out),
    DIAGONAL(R.anim.diagonal_in, android.R.anim.fade_out),
    ELEVATOR(R.anim.elevator_in, android.R.anim.fade_out),
    MATRIX(android.R.anim.fade_in, R.anim.matrix_out),
    FLASH(R.anim.flash_in, android.R.anim.fade_out),
    BLOOM(R.anim.bloom_in, android.R.anim.fade_out),
    PARALLAX(R.anim.parallax_in, R.anim.parallax_out),
    SPIRAL(R.anim.spiral_in, android.R.anim.fade_out),
    GHOST(R.anim.ghost_in, android.R.anim.fade_out),
    SNAP(R.anim.snap_in, android.R.anim.fade_out),
    SLAM(R.anim.slam_in, android.R.anim.fade_out),
    PENDULUM(R.anim.pendulum_in, android.R.anim.fade_out),
    SPRING(R.anim.spring_in, android.R.anim.fade_out),
    WHIP(R.anim.whip_in, android.R.anim.fade_out),
    NONE(0, 0);

    fun getLabel(context: android.content.Context): String {
        return when (this) {
            BOTTOM_UP -> context.getString(R.string.settings_anim_item_bottom_up)
            TOP_DOWN -> context.getString(R.string.settings_anim_item_top_down)
            LEFT_RIGHT -> context.getString(R.string.settings_anim_item_left_right)
            RIGHT_LEFT -> context.getString(R.string.settings_anim_item_right_left)
            FADE -> context.getString(R.string.settings_anim_item_fade)
            TUNNEL -> context.getString(R.string.settings_anim_item_tunnel)
            CUBE -> context.getString(R.string.settings_anim_item_cube)
            FLIP -> context.getString(R.string.settings_anim_item_flip)
            UNFOLD -> context.getString(R.string.settings_anim_item_unfold)
            SETTLE -> context.getString(R.string.settings_anim_item_settle)
            CRT -> context.getString(R.string.settings_anim_item_crt)
            FLICKER -> context.getString(R.string.settings_anim_item_flicker)
            DIAGONAL -> context.getString(R.string.settings_anim_item_diagonal)
            ELEVATOR -> context.getString(R.string.settings_anim_item_elevator)
            MATRIX -> context.getString(R.string.settings_anim_item_matrix)
            FLASH -> context.getString(R.string.settings_anim_item_flash)
            BLOOM -> context.getString(R.string.settings_anim_item_bloom)
            PARALLAX -> context.getString(R.string.settings_anim_item_parallax)
            SPIRAL -> context.getString(R.string.settings_anim_item_spiral)
            GHOST -> context.getString(R.string.settings_anim_item_ghost)
            SNAP -> context.getString(R.string.settings_anim_item_snap)
            SLAM -> context.getString(R.string.settings_anim_item_slam)
            PENDULUM -> context.getString(R.string.settings_anim_item_pendulum)
            SPRING -> context.getString(R.string.settings_anim_item_spring)
            WHIP -> context.getString(R.string.settings_anim_item_whip)
            NONE -> context.getString(R.string.settings_anim_item_none)
        }
    }
}
