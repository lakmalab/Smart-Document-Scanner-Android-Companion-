package com.projectinsight.smartdocumentscanner

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.projectinsight.smartdocumentscanner.databinding.FragmentFirstBinding
import com.projectinsight.smartdocumentscanner.util.PreferencesManager

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val token = PreferencesManager.getToken(requireContext())
        binding.buttonFirst.setOnClickListener {
            if (token != null) {
                findNavController().navigate(R.id.action_FirstFragment_to_ThirdFragment)
            } else {
                // Optionally handle the case where the token is null
                findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}