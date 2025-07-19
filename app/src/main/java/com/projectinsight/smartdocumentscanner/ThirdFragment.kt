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
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.projectinsight.smartdocumentscanner.databinding.FragmentThirdBinding
import com.projectinsight.smartdocumentscanner.util.PreferencesManager
import com.projectinsight.smartdocumentscanner.util.Scan
import io.getstream.photoview.PhotoView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
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

        binding.buttonFile.setOnClickListener {
            binding.textCard.visibility == View.VISIBLE
            checkGalleryPermissionAndOpenGallery()
        }
        binding.buttonCamera.setOnClickListener {
            CheckCameraPermissionAndOpenScanner()
        }
        binding.ProfileButton.setOnClickListener {
            profileDialog()

        }
        binding.buttonSync.setOnClickListener {

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        val action1: TextView = dialogView.findViewById(R.id.action1)
        val action2: TextView = dialogView.findViewById(R.id.action2)


        // Create and show the dialog
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()

        action1.setOnClickListener {
            // Retrieve the OCR text and the current date
            val ocrText = ageText // Assuming ageText contains the OCR result
            val id = PreferencesManager.getUserID(requireContext())// Replace with the actual ID you want to send
            val currentDate = System.currentTimeMillis() // Get the current date as a timestamp

            // Create a JSON object
            val jsonObject = """
        {
            "id": "$id",
            "ocrText": "$ocrText",
            "date": "$currentDate"
        }
    """.trimIndent()

            // Send the POST request
            sendPostRequest(PreferencesManager.getUrl(requireContext()).toString(), jsonObject)

            dialog.dismiss()
        }

        action2.setOnClickListener {

            dialog.dismiss()
        }

    }
    private fun sendPostRequest(url: String, json: String) {
        val client = OkHttpClient()

        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                // Handle failure
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val responseData = response.body?.string()
                    // Process the response if needed
                } else {
                    // Handle unsuccessful response
                }
            }
        })
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
                ocrResulDialog("Result",visionText.text,bitmap)
                //binding.ocrText.setText(visionText.text)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}