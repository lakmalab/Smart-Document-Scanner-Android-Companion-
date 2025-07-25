package com.projectinsight.smartdocumentscanner.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ocr_records")
data class OCRRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ocrText: String,
    val templateName: String,
    val date: String
)
