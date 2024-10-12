package com.andb.apps.biblio.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.andb.apps.biblio.R

@OptIn(ExperimentalTextApi::class)
val eczarRegular: FontFamily = FontFamily(
    Font(
        R.font.eczar_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400)
        ),
    )
)

@OptIn(ExperimentalTextApi::class)
val eczarSemibold: FontFamily = FontFamily(
    Font(
        R.font.eczar_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600)
        ),
    )
)

data class BiblioTypography(
    val body: TextStyle = TextStyle(
        fontFamily = eczarRegular,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 18.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Proportional,
            trim = LineHeightStyle.Trim.None
        ),
        platformStyle = PlatformTextStyle(
            includeFontPadding = false,
        )
    ),
    val title: TextStyle = TextStyle(
        fontFamily = eczarSemibold,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 18.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Proportional,
            trim = LineHeightStyle.Trim.None
        ),
        platformStyle = PlatformTextStyle(
            includeFontPadding = false,
        )
    ),
    val heading: TextStyle = TextStyle(
        fontFamily = eczarSemibold,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 32.sp,
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = eczarRegular,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 14.sp,
    ),
)

// Set of Material typography styles to start with
val Typography = BiblioTypography()