package com.photoedit.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun PhotoPreview(bitmap: Bitmap, description: String, modifier: Modifier = Modifier) {
    Image(bitmap.asImageBitmap(), description, modifier.fillMaxWidth().heightIn(max = 320.dp), contentScale = ContentScale.Fit)
}
