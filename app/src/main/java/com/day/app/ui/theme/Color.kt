package com.day.app.ui.theme

import androidx.compose.ui.graphics.Color

// =====================================================================
// THE DAY — Premium Minimal Monochrome Color System
// Inspired by Proton Pass, Apple Dark Mode, and minimal high-contrast UI
// =====================================================================

// Core Backgrounds & Surfaces
val DayBackground = Color(0xFF050505)         // Pitch black (#050505)
val DayBackgroundDark = Color(0xFF050505)     // Pitch black (#050505)
val DayBackgroundSecondary = Color(0xFF080808)// Subtle secondary background (#080808)
val DayBackgroundTertiary = Color(0xFF0B0B0B) // Tertiary background (#0B0B0B)

val DaySurface = Color(0xFF101010)            // Base dark surface
val DaySurfaceElevated = Color(0xFF141414)    // Elevated surface

// Glass Surface Tokens (Layered real frosted glass)
val DayGlassSurface = Color(0x0EFFFFFF)       // rgba(255,255,255, 0.055)
val DayGlassElevated = Color(0x16FFFFFF)      // rgba(255,255,255, 0.085)
val DayGlassFocused = Color(0x24FFFFFF)       // rgba(255,255,255, 0.14)
val DaySurfaceGlass = DayGlassSurface
val DaySurfaceHighlight = Color(0x18FFFFFF)   // Elevated surface highlight (10% white)

// Borders & Dividers
val DayBorder = Color(0xFF1A1A1A)             // Subtle hairline border
val DayBorderSubtle = Color(0x14FFFFFF)       // Low opacity glass border (8%)
val DayBorderHighlight = Color(0x28FFFFFF)    // Top glass highlight border (16%)
val DayDivider = Color(0x12FFFFFF)            // rgba(255,255,255, 0.07)

// Typography Colors
val DayTextPrimary = Color(0xFFF5F5F5)        // Pure white primary text (#F5F5F5)
val DayTextSecondary = Color(0xFFB0B0B0)      // Soft gray secondary text (#B0B0B0)
val DayTextTertiary = Color(0xFF707070)       // Tertiary muted text (#707070)
val DayTextDisabled = Color(0xFF444444)       // Disabled text (#444444)

// Accents & Functional Colors
val DayAccent = Color(0xFFFFFFFF)             // Monochrome primary accent (Pure White)
val DayAccentDark = Color(0xFF141414)         // Inverted accent (Dark on White)
val DaySuccess = Color(0xFF30D158)            // Subtle success green
val DayWarning = Color(0xFFFFD60A)            // Subtle warning yellow
val DayDestructive = Color(0xFFFF453A)        // Subtle destructive red
val DayInfo = Color(0xFF0A84FF)               // Subtle info blue

// Priority & Urgency Tokens (Clean & Subtle)
val PriorityLowColor = Color(0xFF636366)
val PriorityMediumColor = Color(0xFFA1A1A6)
val PriorityHighColor = Color(0xFFFFFFFF)

val UrgencyNormalColor = Color(0xFF8E8E93)
val UrgencyImportantColor = Color(0xFFFFD60A)
val UrgencyUrgentColor = Color(0xFFFF9F0A)
val UrgencyOverdueColor = Color(0xFFFF453A)

// =====================================================================
// Backward Compatibility Mappings for Existing Components
// Maps previous Neo-Brutalist tokens smoothly to the dark minimal palette
// =====================================================================
val NeoBlack = DayTextPrimary                 // Pure white text
val NeoWhite = DaySurface                     // Dark surface
val NeoPaper = DayBackground                  // Dark background
val NeoPaperDarker = DaySurfaceElevated       // Elevated dark surface
val NeoDarkGray = DaySurfaceHighlight
val NeoGray = DayTextSecondary
val NeoLightGray = DayBorder
val NeoDestructive = DayDestructive
val NeoGreen = DaySuccess
val NeoOrange = DayWarning
val NeoBlue = DayInfo

// Retain token names so legacy calls compile without error
val NeoTeal = Color(0xFFE5E5EA)
val NeoMagenta = Color(0xFFD1D1D6)
val NeoYellow = Color(0xFFFFFFFF)

val UrgencyNormalBg = DaySurfaceElevated
val UrgencyImportantBg = DaySurfaceHighlight
val UrgencyUrgentBg = DaySurfaceElevated
val UrgencyOverdueBg = DaySurfaceElevated
