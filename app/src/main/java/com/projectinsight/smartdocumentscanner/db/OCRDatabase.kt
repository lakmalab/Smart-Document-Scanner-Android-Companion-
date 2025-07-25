package com.projectinsight.smartdocumentscanner.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.projectinsight.smartdocumentscanner.dao.OCRDao
import com.projectinsight.smartdocumentscanner.model.OCRRecord

@Database(entities = [OCRRecord::class], version = 1)
abstract class OCRDatabase : RoomDatabase() {
    abstract fun ocrDao(): OCRDao

    companion object {
        @Volatile private var INSTANCE: OCRDatabase? = null

        fun getDatabase(context: Context): OCRDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OCRDatabase::class.java,
                    "ocr_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
