package com.ledgerlite.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 分类图标映射。预置分类用 Material 线框图标，icon 字段存字符串 key。
 * 用户新建分类 icon 为空，UI 回退到色块。key 不识别时也回退色块。
 */
object CategoryIcon {
    const val FOOD = "food"
    const val TRANSPORT = "transport"
    const val SHOPPING = "shopping"
    const val DAILY = "daily"
    const val ENTERTAINMENT = "entertainment"
    const val MEDICAL = "medical"
    const val STUDY = "study"
    const val SNACK = "snack"
    const val FRUIT = "fruit"
    const val HOUSING = "housing"
    const val TELECOM = "telecom"
    const val SUPERMARKET = "supermarket"
    const val OTHER = "other"

    fun vector(key: String): ImageVector? = when (key) {
        FOOD -> Icons.Outlined.Restaurant
        TRANSPORT -> Icons.Outlined.DirectionsBus
        SHOPPING -> Icons.Outlined.ShoppingBag
        DAILY -> Icons.Outlined.ShoppingBasket
        ENTERTAINMENT -> Icons.Outlined.SportsEsports
        MEDICAL -> Icons.Outlined.LocalHospital
        STUDY -> Icons.AutoMirrored.Outlined.MenuBook
        SNACK -> Icons.Outlined.Icecream
        FRUIT -> Icons.Outlined.Eco
        HOUSING -> Icons.Outlined.Home
        TELECOM -> Icons.Outlined.Call
        SUPERMARKET -> Icons.Outlined.LocalGroceryStore
        OTHER -> Icons.Outlined.Category
        else -> null
    }
}
