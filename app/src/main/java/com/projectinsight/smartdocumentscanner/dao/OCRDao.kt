package com.projectinsight.smartdocumentscanner.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.projectinsight.smartdocumentscanner.model.OCRRecord

@Dao
interface OCRDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: OCRRecord)

    @Query("SELECT * FROM ocr_records ORDER BY id DESC")
    fun getAll(): LiveData<List<OCRRecord>>

    @Delete
    suspend fun delete(record: OCRRecord)

    @Query("DELETE FROM ocr_records")
    suspend fun deleteAll()
}
