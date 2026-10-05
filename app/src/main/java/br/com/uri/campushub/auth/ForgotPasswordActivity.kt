package br.com.uri.campushub.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.databinding.ActivityForgotPasswordBinding
import br.com.uri.campushub.viewmodel.PasswordResetState
import br.com.uri.campushub.viewmodel.PasswordResetViewModel

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var viewModel: PasswordResetViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupListeners()
        observeResetState()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[PasswordResetViewModel::class.java]
    }

    private fun setupListeners() {
        binding.buttonSendReset.setOnClickListener {
            requestPasswordReset()
        }

        binding.textBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun observeResetState() {
        viewModel.resetState.observe(this) { state ->
            when (state) {
                PasswordResetState.Idle -> setLoading(false)
                PasswordResetState.Loading -> setLoading(true)
                PasswordResetState.Success -> {
                    setLoading(false)
                    Toast.makeText(
                        this,
                        "Se o e-mail estiver cadastrado, você receberá as instruções.",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.clearState()
                    finish()
                }
                is PasswordResetState.Error -> {
                    setLoading(false)
                    Toast.makeText(
                        this,
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.clearState()
                }
            }
        }
    }

    private fun requestPasswordReset() {
        binding.layoutEmail.error = null

        val email = binding.inputEmail.text
            ?.toString()
            ?.trim()
            .orEmpty()

        if (email.isBlank()) {
            binding.layoutEmail.error = "Informe seu e-mail."
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.error = "Informe um e-mail válido."
            return
        }

        viewModel.resetPassword(email)
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressReset.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonSendReset.isEnabled = !isLoading
        binding.inputEmail.isEnabled = !isLoading
    }
}
