package com.projectinsight.smartdocumentscanner

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.projectinsight.smartdocumentscanner.databinding.FragmentThirdBinding
import com.projectinsight.smartdocumentscanner.db.OCRDatabase
import com.projectinsight.smartdocumentscanner.model.OCRRecord
import com.projectinsight.smartdocumentscanner.model.TemplateItem
import com.projectinsight.smartdocumentscanner.util.OcrResultsAdapter
import com.projectinsight.smartdocumentscanner.util.PreferencesManager
import com.projectinsight.smartdocumentscanner.util.Scan
import com.projectinsight.smartdocumentscanner.util.TokenInterceptor
import io.getstream.photoview.PhotoView
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class ThirdFragment : Fragment() {

    private var _binding: FragmentThirdBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    companion object {
        private const val REQUEST_CODE_GALLERY = 1002
        private const val REQUEST_CODE_DOCUMENT_SCAN = 123
    }
    // New vars to hold OCR info
    private var currentOcrText: String = ""
    private var currentTemplateName: String = "None"
    private var currentOcrDate: String = "-"

    // UI Elements for OCR Card
    private lateinit var tvOcrResultText: TextView
    private lateinit var tvSelectedTemplate: TextView
    private lateinit var tvOcrDate: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var ocrResultsAdapter: OcrResultsAdapter
    private val ocrResultsList = mutableListOf<OCRRecord>()
    // Floating Buttons
    private lateinit var buttonCamera: androidx.cardview.widget.CardView
    private lateinit var buttonGallery: androidx.cardview.widget.CardView
    private lateinit var dropdownAdapter: ArrayAdapter<String>
    private val templateNameList = mutableListOf<String>()
    private val templateIdMap = mutableMapOf<String, Int>()
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to use this feature", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThirdBinding.inflate(inflater, container, false)
        Glide.with(this)
            .asGif()
            .load(R.drawable.scann)
            .into(binding.bookcopy)
        binding.textView3.setText(PreferencesManager.getUserName(requireContext()))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchTemplateListFromApi()

        binding.ProfileButton.setOnClickListener {
            profileDialog()

        }
        buttonCamera = binding.root.findViewById(R.id.buttonCamera)
        buttonGallery = binding.root.findViewById(R.id.buttonGallery)

        binding.textView3.setText(PreferencesManager.getUserName(requireContext()))
        recyclerView = binding.root.findViewById(R.id.recyclerViewOcrResults)
        ocrResultsAdapter = OcrResultsAdapter(ocrResultsList, { record ->
            // Handle send JSON logic

            val jsonObject = JSONObject().apply {
                put("userId", PreferencesManager.getUserID(requireContext()))
                put("templateId", PreferencesManager.getSelectedTemplateId(requireContext()))
                put("rawText", record.ocrText)  // Use actual text if needed
            }
            var url = PreferencesManager.getUrl(requireContext())+"/api/ocr/submit"
            // Send the POST request
            sendPostRequest(url, jsonObject.toString())
        }, { record ->
            // Handle delete logic
            lifecycleScope.launch {
                OCRDatabase.getDatabase(requireContext()).ocrDao().delete(record)
                ocrResultsList.remove(record)
                ocrResultsAdapter.notifyDataSetChanged()
            }
        })
        recyclerView.adapter = ocrResultsAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe LiveData from the database
        OCRDatabase.getDatabase(requireContext()).ocrDao().getAll().observe(viewLifecycleOwner) { records ->
            ocrResultsList.clear()  // Clear old results
            ocrResultsList.addAll(records)  // Add new records from the database
            ocrResultsAdapter.notifyDataSetChanged()  // Notify the adapter
        }
        setupFloatingButtons()

    }
    private fun setupFloatingButtons() {
        buttonCamera.setOnClickListener {
            CheckCameraPermissionAndOpenScanner()
        }
        buttonGallery.setOnClickListener {
            checkGalleryPermissionAndOpenGallery()
        }
    }
    private fun updateOcrCard() {
        tvOcrResultText.text = if (currentOcrText.isNotBlank()) currentOcrText else "No OCR result yet."
        tvSelectedTemplate.text = "Selected Template: $currentTemplateName"
        tvOcrDate.text = "Date: $currentOcrDate"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun fetchTemplateListFromApi() {
        val url = PreferencesManager.getUrl(requireContext()) + "/api/templates/by-user/${PreferencesManager.getUserID(requireContext())}"

        // Validate the URL
        if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            Log.e("FetchTemplates", "Invalid URL: $url")
            Toast.makeText(requireContext(), "Invalid URL", Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(TokenInterceptor(requireContext()))
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        Log.e("FetchTemplates", "Request: $url")
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("FetchTemplates", "Request failed: ${e.message}")
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to load templates", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("FetchTemplates", "Response code: ${response.code}")

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    try {
                        Log.d("FetchTemplates", "Response body: $responseBody")
                        val templateList = parseTemplateListJson(responseBody)

                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "Templates loaded: ${templateList.size}", Toast.LENGTH_SHORT).show()
                            updateDropdownWithTemplates(templateList)
                        }

                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.e("FetchTemplates", "JSON Error: ${e.localizedMessage}")
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "JSON Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("FetchTemplates", "Failed to fetch templates: ${response.code} ${response.message}")
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Failed to fetch templates", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    private fun parseTemplateListJson(jsonString: String): List<TemplateItem> {
        val jsonArray = org.json.JSONArray(jsonString)
        val list = mutableListOf<TemplateItem>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val id = obj.getInt("templateId")
            val name = obj.getString("templateName")
            list.add(TemplateItem(id, name))
        }

        return list
    }
    private fun updateDropdownWithTemplates(templates: List<TemplateItem>) {
        templateNameList.clear()
        templateIdMap.clear()

        for (template in templates) {
            templateNameList.add(template.templateName)
            templateIdMap[template.templateName] = template.templateId
        }

        dropdownAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, templateNameList)
        binding.templateDropdown.setAdapter(dropdownAdapter)

        binding.templateDropdown.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position).toString()
            val selectedId = templateIdMap[selectedName]
            PreferencesManager.setSelectedTemplateId(requireContext(), selectedId!!)
            currentTemplateName = selectedName
            //updateOcrCard()
        }
    }


    @Composable
    private fun CheckCameraPermissionAndOpenScanner() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            openCamera()
        }
    }
    private fun checkGalleryPermissionAndOpenGallery() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            openGallery()
        }
    }
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_GALLERY)
    }
    private fun ocrResulDialog(titleText: String, ageText: String, image: Bitmap) {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.ocr_text_dialog, null)

        // Set the message in the TextView
        val messageTextView: TextInputEditText = dialogView.findViewById(R.id.ocrResult)
        val messageTitleTextView: TextView = dialogView.findViewById(R.id.titilemsg)
        val imageinput : PhotoView =  dialogView.findViewById(R.id.bookcopy)
        messageTextView.setText(ageText)
        messageTitleTextView.text = titleText
        imageinput.setImageBitmap(image)
        // Create the AlertDialog builder
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)

        // Set up the action buttons
        val action2: TextView = dialogView.findViewById(R.id.action1)
        val action1: TextView = dialogView.findViewById(R.id.action2)


        // Create and show the dialog
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()

        action1.setOnClickListener {
            // Retrieve the OCR text and the current date
            val ocrText = ageText // Assuming ageText contains the OCR result
            val id = PreferencesManager.getUserID(requireContext())// Replace with the actual ID you want to send
            val currentDate = System.currentTimeMillis() // Get the current date as a timestamp
            val templateid = PreferencesManager.getSelectedTemplateId(requireContext())
            Log.e("SendPostRequest", "Request: ${templateid}")
            val jsonObject = JSONObject().apply {
                put("userId", id)  // Use the real ID here
                put("templateId", templateid)
                put("rawText", ocrText)
            }

            var url = PreferencesManager.getUrl(requireContext())+"/api/ocr/submit"
            // Send the POST request
            sendPostRequest(url, jsonObject.toString())
            Log.e("SendPostRequest", "Request: ${url}")
            dialog.dismiss()
        }

        action2.setOnClickListener {

            dialog.dismiss()
        }

    }
    private fun sendPostRequest(url: String, json: String) {
        val client = OkHttpClient.Builder()
            .addInterceptor(TokenInterceptor(requireContext()))
            .build()

        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e("SendPostRequest", "Request failed: ${e.message}")
                requireActivity().runOnUiThread {
                    showMessage("Request failed: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                requireActivity().runOnUiThread {
                    if (response.isSuccessful) {
                        val responseData = response.body?.string()
                        Log.d("SendPostRequest", "Response successful: $responseData")
                        showMessage("Response received: $responseData")
                    } else {
                        val errorBody = response.body?.string() ?: "No error body"
                        Log.e("SendPostRequest", "Response failed: ${response.code} ${response.message} - $errorBody")
                        showMessage("Response failed: ${response.code} ${response.message} - $errorBody")
                    }
                }
            }
        })
    }

    private fun showMessage(message: String) {
        // Implementation to show a message (e.g., Toast, Snackbar, etc.)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    private fun profileDialog() {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.profile_dialog, null)

        // Set the message in the TextView
        val messageTextView: TextView = dialogView.findViewById(R.id.userName)
        val messageTitleTextView: TextView = dialogView.findViewById(R.id.userTitle)
       // val imageinput : ImageView =  dialogView.findViewById(R.id.profileImage)
        messageTextView.setText(PreferencesManager.getUserName(requireContext()))
        messageTitleTextView.text = PreferencesManager.getUserID(requireContext()).toString()

        // Create the AlertDialog builder
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)

        // Set up the action buttons

        val action3: TextView = dialogView.findViewById(R.id.action1)
        val action1: Button = dialogView.findViewById(R.id.messageButton)
        val action2: Button = dialogView.findViewById(R.id.connectButton)

        // Create and show the dialog
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()

        action1.setOnClickListener {
            PreferencesManager.saveUserID(requireContext() ,0)
            PreferencesManager.saveUserName(requireContext() ,"")
            PreferencesManager.saveToken(requireContext() ,null)
            findNavController().navigate(R.id.action_ThirdFragment_to_SecondFragment)
            dialog.dismiss()
        }
        action3.setOnClickListener {

            dialog.dismiss()
        }
        action2.setOnClickListener {

            dialog.dismiss()
        }

    }
    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "External storage permission is required to use this feature", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_DOCUMENT_SCAN -> {
                    val result = GmsDocumentScanningResult.fromActivityResultIntent(data)

                    result?.pages?.let { pages ->
                        for (page in pages) {
                            val imageUri = page.imageUri
                            try {
                                val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
                                // Assuming 'bookcopy' is your ImageView where you want to display the scanned document
                               // bookcopy.setImageBitmap(bitmap)
                                performImageProcessing(bitmap)
                            } catch (e: IOException) {
                                e.printStackTrace()
                                // Handle exception
                            }
                        }
                    }
                }
                REQUEST_CODE_GALLERY -> {
                    val imageUri: Uri? = data?.data
                    if (imageUri != null) {
                        try {
                            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
                            //binding.bookcopy.setImageBitmap(bitmap)
                            performImageProcessing(bitmap)
                        } catch (e: IOException) {
                            e.printStackTrace()
                            // Handle exception
                        }
                    }
                }
                else -> {
                    // Handle other request codes if necessary
                }
            }
        }
    }
    @Composable
    private fun openCamera() {
        Scan(requireActivity() as ComponentActivity) { intentSender ->
            try {
                startIntentSenderForResult(intentSender, REQUEST_CODE_DOCUMENT_SCAN, null, 0, 0, 0, null)
            } catch (e: IntentSender.SendIntentException) {
                Toast.makeText(requireContext(), "Error starting camera scan", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun performImageProcessing(bitmap: Bitmap) {
            performOCRoldmethode(bitmap)
    }
    private fun performOCRoldmethode(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                currentOcrText = visionText.text
                currentOcrDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())

                // Create a new OCRRecord
                val record = OCRRecord(
                    ocrText = currentOcrText,
                    templateName = currentTemplateName,
                    date = currentOcrDate
                )

                // Insert into database
                lifecycleScope.launch {
                    OCRDatabase.getDatabase(requireContext()).ocrDao().insert(record)

                    // Add the record to the list and notify adapter
                    ocrResultsList.add(record)  // Add to the local list
                    ocrResultsAdapter.notifyItemInserted(ocrResultsList.size - 1)  // Notify adapter
                }

                // Optionally show dialog
                ocrResulDialog("Result", currentOcrText, bitmap)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}