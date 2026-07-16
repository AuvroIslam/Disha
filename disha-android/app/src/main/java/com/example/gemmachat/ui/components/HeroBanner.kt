package com.example.gemmachat.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Rounded hero illustration banner. The provided art keeps the subject on the right and empty
 * space on the left, so an optional [title]/[subtitle] is overlaid on the left half.
 */
@Composable
fun HeroBanner(
    @DrawableRes resId: Int,
    height: Dp = 128.dp,
    title: String? = null,
    subtitle: String? = null,
) {
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(18.dp))) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        )
        if (title != null) {
            Column(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.52f)
                    .padding(start = 18.dp, end = 6.dp),
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp, lineHeight = 24.sp)
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, color = Color.White.copy(alpha = 0.92f),
                        fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}
