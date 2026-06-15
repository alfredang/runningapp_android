package com.tertiaryinfotech.runtrackgps.model

/**
 * Drives top-level navigation. The app is intentionally lightweight, so instead of
 * a NavHost we switch on this enum inside RootScreen (mirrors the iOS AppScreen).
 */
enum class AppScreen { HOME, RUNNING, COMPLETION }
