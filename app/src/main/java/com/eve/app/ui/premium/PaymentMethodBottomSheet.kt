package com.eve.app.ui.premium

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.eve.app.databinding.BottomSheetPaymentMethodBinding
import com.eve.app.designsystem.TelegramBottomSheet

class PaymentMethodBottomSheet : DialogFragment() {

    private var _binding: BottomSheetPaymentMethodBinding? = null
    private val binding get() = _binding!!

    private var onMethodSelected: ((String) -> Unit)? = null

    companion object {
        private const val ARG_PRICE = "arg_price"
        private const val ARG_QR = "arg_qr"
        private const val ARG_UPI = "arg_upi"

        fun newInstance(
            priceInr: Int,
            qrEnabled: Boolean,
            upiEnabled: Boolean,
            onSelect: (String) -> Unit
        ): PaymentMethodBottomSheet {
            val sheet = PaymentMethodBottomSheet()
            sheet.arguments = Bundle().apply {
                putInt(ARG_PRICE, priceInr)
                putBoolean(ARG_QR, qrEnabled)
                putBoolean(ARG_UPI, upiEnabled)
            }
            sheet.onMethodSelected = onSelect
            return sheet
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        return TelegramBottomSheet(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPaymentMethodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val price = arguments?.getInt(ARG_PRICE, 99) ?: 99
        val qrEnabled = arguments?.getBoolean(ARG_QR, true) ?: true
        val upiEnabled = arguments?.getBoolean(ARG_UPI, true) ?: true

        binding.tvSheetSubtitle.text = "Select how you would like to pay ₹$price"

        binding.cardMethodQr.visibility = if (qrEnabled) View.VISIBLE else View.GONE
        binding.cardMethodUpi.visibility = if (upiEnabled) View.VISIBLE else View.GONE

        com.eve.app.designsystem.DesignPress.bind(binding.cardMethodQr) {
            dismiss()
            onMethodSelected?.invoke("qr")
        }

        com.eve.app.designsystem.DesignPress.bind(binding.cardMethodUpi) {
            dismiss()
            onMethodSelected?.invoke("upi")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
