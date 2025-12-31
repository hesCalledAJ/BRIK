package com.alijafari.brik.main.domain.model

import com.alijafari.brik.utils.PermissionType

data class PermissionRequirement(
    val type : PermissionType,
    val title: String,
    val description: String,
    val icon: Int,
    val onClick: () -> Unit
)