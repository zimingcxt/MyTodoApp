package com.example.mytodoapp

import java.io.Serializable

data class Task(
    val id: Long,
    val text: String,
    val startTime: Long?,
    val endTime: Long?,
    val priority: Priority,
    var isCompleted: Boolean = false
) : Serializable // 实现 Serializable
