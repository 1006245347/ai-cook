package com.hwj.cook.models

import kotlinx.io.files.Path
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfoCell(
    val cpuCores: Int?,
    val cpuArch: String?,
    val totalMemoryMB: Long?,
    val brand: String?,
    val model: String?,
    val osVersion: String?,
    val platform: String,
)
