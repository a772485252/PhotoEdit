package com.photoedit.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.photoedit.app.R

@Composable
fun PhotoEditNavigation(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("拆分", "AI 图片", "助手", "变装")
    val icons = listOf(R.drawable.nav_grid, R.drawable.nav_image, R.drawable.nav_chat, R.drawable.nav_video)
    NavigationBar {
        labels.forEachIndexed { index, label ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = { Icon(painterResource(icons[index]), contentDescription = null) },
                label = { Text(label) }
            )
        }
    }
}
