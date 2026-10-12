package com.example.assignment03.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.assignment03.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val DMSansFont = GoogleFont("DM Sans")

val DMSansFontFamily = FontFamily(
    Font(googleFont = DMSansFont, fontProvider = provider),
    Font(googleFont = DMSansFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = DMSansFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = DMSansFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = DMSansFont, fontProvider = provider, weight = FontWeight.Black)
)

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = DMSansFontFamily),
    displayMedium = TextStyle(fontFamily = DMSansFontFamily),
    displaySmall = TextStyle(fontFamily = DMSansFontFamily),
    headlineLarge = TextStyle(fontFamily = DMSansFontFamily),
    headlineMedium = TextStyle(fontFamily = DMSansFontFamily),
    headlineSmall = TextStyle(fontFamily = DMSansFontFamily),
    titleLarge = TextStyle(fontFamily = DMSansFontFamily),
    titleMedium = TextStyle(fontFamily = DMSansFontFamily),
    titleSmall = TextStyle(fontFamily = DMSansFontFamily),
    bodyLarge = TextStyle(
        fontFamily = DMSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(fontFamily = DMSansFontFamily),
    bodySmall = TextStyle(fontFamily = DMSansFontFamily),
    labelLarge = TextStyle(fontFamily = DMSansFontFamily),
    labelMedium = TextStyle(fontFamily = DMSansFontFamily),
    labelSmall = TextStyle(fontFamily = DMSansFontFamily)
)
