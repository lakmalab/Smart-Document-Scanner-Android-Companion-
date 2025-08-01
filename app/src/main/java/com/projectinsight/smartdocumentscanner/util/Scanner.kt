package com.projectinsight.smartdocumentscanner.util

import android.content.ContentValues.TAG
import android.content.IntentSender
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning

@Composable
fun Scan(
    activity: ComponentActivity,
    start: (IntentSender) -> Unit
) {
    val options = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setPageLimit(1)
        .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
        .setScannerMode(SCANNER_MODE_FULL)
        .build()

    val scanner = GmsDocumentScanning.getClient(options)

    scanner.getStartScanIntent(activity)
        .addOnSuccessListener { intentSender ->
            start(intentSender)
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Error starting scan: ${e.message}")
            // Handle failure appropriately
        }
}

