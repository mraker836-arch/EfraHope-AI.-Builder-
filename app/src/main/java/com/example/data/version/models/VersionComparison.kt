package com.example.data.version.models

import com.example.data.ai.models.FileDiff

data class MetadataDiff(
    val key: String,
    val oldValue: String?,
    val newValue: String?
)

data class SchemaDiff(
    val oldVersion: Int,
    val newVersion: Int,
    val summary: String
)

data class VersionComparisonResult(
    val versionAId: String,
    val versionBId: String,
    val versionANumber: Int,
    val versionBNumber: Int,
    val addedFiles: List<String>,
    val removedFiles: List<String>,
    val modifiedFiles: List<String>,
    val fileDiffs: List<FileDiff>,
    val metadataDiffs: List<MetadataDiff>,
    val schemaDiff: SchemaDiff? = null
)
