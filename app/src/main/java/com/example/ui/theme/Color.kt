package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// TerminalHouse Official Palette (Spec Mockup Paso 5)
val FondoGlobal = Color(0xFF0F0F12)
val FondoTerminal = Color(0xFF1E1E1E)
val FondoToolbar = Color(0xFF0F0F12)
val FondoHeader = Color(0xFF0F0F12)
val FondoSheetIa = Color(0xFF16161A)
val FondoMenuContextual = Color(0xFF1F1F24)
val FondoTarjetas = Color(0xFF1A1A1E)

val TextoPrimario = Color(0xFFE0E0E0)
val TextoSecundario = Color(0xFF8A8A8A)
val TextoAtenuado = Color(0xFF5A5A5A)

val AcentoNaranja = Color(0xFFE95420)
val PromptUsuario = Color(0xFF4E9A06)
val PromptPath = Color(0xFF3465A4)
val PromptRoot = Color(0xFFE0E0E0)
val ColorExito = Color(0xFF22C55E)
val ColorError = Color(0xFFEF4444)
val ColorAviso = Color(0xFFF59E0B)

val BordesSutiles = Color(0xFF26262C)
val Divisores = Color(0xFF1F1F24)

// Backwards-compatible aliases for existing components
val BackgroundDark = FondoGlobal
val TerminalDark = FondoTerminal
val SurfaceDark = FondoSheetIa
val SurfaceVariantDark = FondoTarjetas
val CardBorderDark = BordesSutiles
val TextPrimaryDark = TextoPrimario
val TextSecondaryDark = TextoSecundario
val TextMutedDark = TextoAtenuado

// Accent
val AccentOrange = AcentoNaranja
val AccentOrangeHover = Color(0xFFFF6E38)
val AccentOrangeDark = Color(0xFFC73E0F)
val AccentOrangeContainer = Color(0x33E95420)

// System status & terminal colors
val StatusOnlineGreen = ColorExito
val TerminalGreen = PromptUsuario
val TerminalBlue = PromptPath
val TerminalYellow = ColorAviso
val TerminalRed = ColorError
val TerminalCyan = Color(0xFF06B6D4)
val TerminalPurple = Color(0xFFA855F7)

// Light Mode Colors (from mockup optional preview)
val BackgroundLight = Color(0xFFF3F4F6)
val TerminalLight = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE5E7EB)
val CardBorderLight = Color(0xFFD1D5DB)
val TextPrimaryLight = Color(0xFF111827)
val TextSecondaryLight = Color(0xFF4B5563)
