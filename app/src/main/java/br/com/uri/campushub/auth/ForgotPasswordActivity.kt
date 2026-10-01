package br.com.uri.campushub.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.R
import br.com.uri.campushub.viewmodel.PasswordResetState
import br.com.uri.campushub.viewmodel.PasswordResetViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var layoutEmail: TextInputLayout
    private lateinit var inputEmail: TextInputEditText
    private lateinit var buttonSendReset: MaterialButton
    private lateinit var progressReset: ProgressBar

    private lateinit var viewModel: PasswordResetViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        bindViews()
        setupViewModel()
        setupListeners()
        observeResetState()
    }

    private fun bindViews() {
        layoutEmail = findViewById(R.id.layoutEmail)
        inputEmail = findViewById(R.id.inputEmail)
        buttonSendReset = findViewById(R.id.buttonSendReset)
        progressReset = findViewById(R.id.progressReset)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[PasswordResetViewModel::class.java]
    }

    private fun setupListeners() {
        buttonSendReset.setOnClickListener {
            requestPasswordReset()
        }

        findViewById<TextView>(R.id.textBackToLogin).setOnClickListener {
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
        layoutEmail.error = null

        val email = inputEmail.text
            ?.toString()
            ?.trim()
            .orEmpty()

        if (email.isBlank()) {
            layoutEmail.error = "Informe seu e-mail."
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.error = "Informe um e-mail válido."
            return
        }

        viewModel.resetPassword(email)
    }

    private fun setLoading(isLoading: Boolean) {
        progressReset.visibility = if (isLoading) View.VISIBLE else View.GONE
        buttonSendReset.isEnabled = !isLoading
        inputEmail.isEnabled = !isLoading
    }
}
