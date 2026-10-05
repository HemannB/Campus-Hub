package br.com.uri.campushub.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.databinding.ActivityLoginBinding
import br.com.uri.campushub.student.HomeActivity
import br.com.uri.campushub.viewmodel.AuthState
import br.com.uri.campushub.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupListeners()
        observeAuthState()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
    }

    private fun setupListeners() {
        binding.buttonLogin.setOnClickListener {
            attemptLogin()
        }

        binding.textForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        binding.textRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.buttonGoogle.setOnClickListener {
            Toast.makeText(
                this,
                "Login com Google...",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                AuthState.Idle -> setLoading(false)
                AuthState.Loading -> setLoading(true)
                AuthState.Success -> {
                    setLoading(false)
                    navigateToHome()
                }
                is AuthState.Error -> {
                    setLoading(false)
                    Toast.makeText(
                        this,
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.resetState()
                }
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun attemptLogin() {
        clearErrors()

        val email = binding.inputEmail.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val password = binding.inputPassword.text
            ?.toString()
            .orEmpty()

        var isValid = true

        if (email.isBlank()) {
            binding.layoutEmail.error = "Informe seu e-mail."
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.error = "Informe um e-mail válido."
            isValid = false
        }

        if (password.isBlank()) {
            binding.layoutPassword.error = "Informe sua senha."
            isValid = false
        }

        if (!isValid) {
            return
        }

        viewModel.login(email, password)
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressLogin.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !isLoading
        binding.buttonGoogle.isEnabled = !isLoading
        binding.inputEmail.isEnabled = !isLoading
        binding.inputPassword.isEnabled = !isLoading
    }

    private fun clearErrors() {
        binding.layoutEmail.error = null
        binding.layoutPassword.error = null
    }
}
