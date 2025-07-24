package com.projectinsight.smartdocumentscanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.projectinsight.smartdocumentscanner.databinding.FragmentSecondBinding
import com.projectinsight.smartdocumentscanner.util.PreferencesManager
import com.projectinsight.smartdocumentscanner.util.UserResponse
import com.squareup.moshi.Moshi
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody



class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!
    private var isScanned = false
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var Url = ""
    private var scanner = BarcodeScanning.getClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_ThirdFragment)
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.qrCameraPreview.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext())) { imageProxy ->
                processImageProxy(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, analysis)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (isScanned) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        rawValue?.let {
                            Log.d("QRScanner", "QR Code: $it")
                            isScanned = true
                            scanner.close()
                            sendJsonToScannedUrl(it)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("QRScanner", "Scan failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
    private fun sendJsonToScannedUrl(scannedUrl: String) {
        val client = OkHttpClient()

        val uri = Uri.parse(scannedUrl)
        val token = uri.getQueryParameter("token") ?: run {
            Toast.makeText(requireContext(), "Invalid QR code: No token", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔹 The backend endpoint is embedded in the scanned URL
        val postUrl = scannedUrl.substringBefore("?")
        Log.e("postUrl", "Server error: ${postUrl}")
        Url = postUrl.toString()
        val jsonBody = """
        {
            "token": "$token"
        }
    """.trimIndent()
        PreferencesManager.saveToken(requireContext(), token)
        PreferencesManager.saveUrl(requireContext(), postUrl)
        PreferencesManager.saveUserID(requireContext(), id)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(postUrl + "/mobile/confirm")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (isAdded) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("Connection", "Server error: ${e.message}")
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        val moshi = Moshi.Builder().build()
                        val adapter = moshi.adapter(UserResponse::class.java)

                        val user = adapter.fromJson(responseBody.string())
                        requireActivity().runOnUiThread {
                            if (user != null) {
                                val buttonSecond = binding.buttonSecond
                                val status = binding.tvStatus
                                binding.qrCameraPreview.visibility =  View.GONE
                                binding.userCard.visibility =  View.VISIBLE
                                binding.imageView.setImageResource(android.R.drawable.ic_menu_directions)
                                binding.textView.text = Url
                                status.text = "connected"
                                status.setTextColor(Color.GREEN)
                                binding.tvStatusdot.setBackgroundColor(Color.GREEN)
                                binding.textView.setTextColor(Color.CYAN)

                                buttonSecond.setBackgroundColor(Color.parseColor("#7B5CFA"))
                                binding.buttonSecond.isEnabled = true
                                binding.userName.setText(user.name)
                                PreferencesManager.saveUserName(requireContext(),user.name)
                                PreferencesManager.saveUserID(requireContext(), user.userId)
                                binding.userTitle.setText("Email : "+ user.email)
                            } else {
                                Toast.makeText(requireContext(), "Invalid response format", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Log.e("Network", "Server error: ${response.code}")
                    requireActivity().runOnUiThread {

                        Toast.makeText(requireContext(), "Server error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }



    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        scanner.close()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
