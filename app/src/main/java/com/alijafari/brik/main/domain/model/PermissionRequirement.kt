package com.alijafari.brik.main.domain.model

data class PermissionRequirement(
    val title: String,
    val description: String,
    val icon: Int,
    val isGranted: Boolean,
    val onClick: () -> Unit
)