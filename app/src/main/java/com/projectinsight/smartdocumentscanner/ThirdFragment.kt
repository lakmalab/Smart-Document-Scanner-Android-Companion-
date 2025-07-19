package com.projectinsight.smartdocumentscanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import android.provider.MediaStore
import android.renderscript.ScriptGroup
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.projectinsight.smartdocumentscanner.databinding.FragmentThirdBinding
import com.projectinsight.smartdocumentscanner.util.Scan
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
        binding.buttonSync.setOnClickListener {
            findNavController().navigate(R.id.action_ThirdFragment_to_FirstFragment)
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
                            binding.bookcopy.setImageBitmap(bitmap)
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

                binding.ocrText.setText(visionText.text)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "OCR failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}