package com.projectinsight.smartdocumentscanner.util

import android.app.AlertDialog
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.projectinsight.smartdocumentscanner.model.OCRRecord
import com.projectinsight.smartdocumentscanner.R
import com.projectinsight.smartdocumentscanner.ThirdFragment
import io.getstream.photoview.PhotoView
import org.json.JSONObject

class OcrResultsAdapter(
    private val ocrResults: List<OCRRecord>,
    private val onSendJson: (OCRRecord) -> Unit,
    private val onDelete: (OCRRecord) -> Unit
) : RecyclerView.Adapter<OcrResultsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSelectedTemplate: TextView = itemView.findViewById(R.id.tvSelectedTemplate)
        val tvOcrDate: TextView = itemView.findViewById(R.id.tvOcrDate)
        val buttonSendJson: ImageButton = itemView.findViewById(R.id.buttonSendJson)
        val buttonView: ImageButton = itemView.findViewById(R.id.buttonView)
        val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ocr_result_card_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val result = ocrResults[position]
        holder.tvSelectedTemplate.text = result.templateName
        holder.tvOcrDate.text = result.date

        holder.buttonView.setOnClickListener {
           // ocrResulDialog("Result",  ocrResults.indexOf(position)., bitmap)

        }
        holder.buttonSendJson.setOnClickListener {
            onSendJson(result)
        }

        holder.buttonDelete.setOnClickListener {
            onDelete(result)
        }
    }

    override fun getItemCount() = ocrResults.size
}