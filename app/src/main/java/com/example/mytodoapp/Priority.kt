package com.example.mytodoapp

import androidx.annotation.ColorRes
import java.io.Serializable

enum class Priority(@ColorRes val colorResId: Int) : Serializable { // 实现 Serializable
    HIGH(R.color.priority_high),
    MEDIUM(R.color.priority_medium),
    LOW(R.color.priority_low)
}
