package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val workingDirectory: String, // e.g. "/workspace/opencode-phone"
    val description: String,
    val language: String, // "Kotlin", "TypeScript", "Python", "Rust", "Go"
    val gitBranch: String = "main",
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)
