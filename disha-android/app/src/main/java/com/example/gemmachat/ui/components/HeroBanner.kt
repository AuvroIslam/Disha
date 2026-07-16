package com.example.gemmachat.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Rounded hero illustration banner. Uses [aspectRatio] (width / height) so the art is only
 * lightly cropped — the provided images are ~3:2 with the subject on the right and empty space
 * on the left, where an optional [title]/[subtitle] is overlaid.
 */
@Composable
fun HeroBanner(
    @DrawableRes resId: Int,
    aspectRatio: Float = 1.72f,
    title: String? = null,
    subtitle: String? = null,
) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(aspectRatio).clip(RoundedCornerShape(20.dp)),
    ) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (title != null) {
            Column(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.5f)
                    .padding(start = 20.dp, end = 6.dp),
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp, lineHeight = 26.sp)
                if (subtitle != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(subtitle, color = Color.White.copy(alpha = 0.94f),
                        fontSize = 13.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}
