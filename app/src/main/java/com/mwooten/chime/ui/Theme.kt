package com.mwooten.chime.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mwooten.chime.R

/** Forest Temple palette: deep canopy greens, weathered stone, aged brass, and Mote's green-gold glow. */
object C {
    val night = Color(0xFF08130F)
    val canopy = Color(0xFF10231B)
    val moss = Color(0xFF1B3A2C)
    val stone = Color(0xFF26352F)
    val stoneHi = Color(0xFF34473F)
    val parchment = Color(0xFFEDE6CF)
    val mist = Color(0xFF9DB3A6)
    val faint = Color(0xFF66796F)
    val brass = Color(0xFFD9C27A)
    val brassDim = Color(0xFF9C8A55)
    val glow = Color(0xFFC8F7A8)
    val glowCore = Color(0xFFF6FFE8)
    val ember = Color(0xFFE58A5F)
    val done = Color(0xFF8FC7B0)
    val line = Color(0x38D9C27A)
    val brassBrush = Brush.verticalGradient(listOf(Color(0xFFEBD597), Color(0xFFB99A55)))
    val ink = Color(0xFF1E1A0C)
}

object F {
    val display = FontFamily(Font(R.font.uncial))
    val voice = FontFamily(
        Font(R.font.cormorant_medium, FontWeight.Medium),
        Font(R.font.cormorant_italic, FontWeight.Normal, FontStyle.Italic),
    )
    val body = FontFamily(
        Font(R.font.alegreya_regular, FontWeight.Normal),
        Font(R.font.alegreya_medium, FontWeight.Medium),
        Font(R.font.alegreya_bold, FontWeight.Bold),
    )
}

object T {
    val logo = TextStyle(fontFamily = F.display, fontSize = 64.sp, letterSpacing = 2.sp, color = C.brass)
    val menu = TextStyle(fontFamily = F.display, fontSize = 26.sp, letterSpacing = 1.sp)
    val eyebrow = TextStyle(fontFamily = F.display, fontSize = 12.sp, letterSpacing = 3.sp, color = C.brassDim)
    val heading = TextStyle(fontFamily = F.voice, fontStyle = FontStyle.Italic, fontSize = 36.sp, lineHeight = 40.sp, color = C.parchment)
    val title = TextStyle(fontFamily = F.display, fontSize = 26.sp, letterSpacing = 1.sp, color = C.parchment)
    val body = TextStyle(fontFamily = F.body, fontSize = 17.sp, lineHeight = 23.sp, color = C.parchment)
    val small = TextStyle(fontFamily = F.body, fontSize = 14.sp, lineHeight = 19.sp, color = C.mist)
    val voice = TextStyle(fontFamily = F.voice, fontStyle = FontStyle.Italic, fontSize = 22.sp, lineHeight = 28.sp, color = C.ink)
}
