package com.github.fhilgers.compose.application.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

interface ThemeTypography {
    @Composable
    fun create(): Typography
}

class ThemeTypographyImpl: ThemeTypography {
    @Composable
    override fun create(): Typography {
        return Typography().withFontFamily(
            FontFamily(
                fonts = listOf(


//                    Font(Res.font.Nunito_Black, FontWeight.Black, FontStyle.Normal),
//                    Font(Res.font.Nunito_BlackItalic, FontWeight.Black, FontStyle.Italic),
//
//                    Font(Res.font.Nunito_Bold, FontWeight.Bold, FontStyle.Normal),
//                    Font(Res.font.Nunito_BoldItalic, FontWeight.Bold, FontStyle.Italic),
//
//                    Font(Res.font.Nunito_ExtraBold, FontWeight.Bold, FontStyle.Normal),
//                    Font(Res.font.Nunito_ExtraBoldItalic, FontWeight.Bold, FontStyle.Italic),
//
//                    Font(Res.font.Nunito_ExtraLight, FontWeight.ExtraLight, FontStyle.Normal),
//                    Font(Res.font.Nunito_ExtraLightItalic, FontWeight.ExtraLight, FontStyle.Italic),
//
//                    Font(Res.font.Nunito_Regular, FontWeight.Normal, FontStyle.Normal),
//                    Font(Res.font.Nunito_Italic, FontWeight.Normal, FontStyle.Italic),
//
//                    Font(Res.font.Nunito_Light, FontWeight.Light, FontStyle.Normal),
//                    Font(Res.font.Nunito_LightItalic, FontWeight.Light, FontStyle.Italic),
//
//                    Font(Res.font.Nunito_Medium, FontWeight.Medium, FontStyle.Normal),
//                    Font(Res.font.Nunito_MediumItalic, FontWeight.Medium, FontStyle.Italic),
//
//                    Font(Res.font.Nunito_SemiBold, FontWeight.SemiBold, FontStyle.Normal),
//                    Font(Res.font.Nunito_SemiBoldItalic, FontWeight.SemiBold, FontStyle.Italic),
                )
            )
        )
    }
}
