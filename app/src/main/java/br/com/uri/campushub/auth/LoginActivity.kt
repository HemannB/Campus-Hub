package br.com.uri.campushub.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.databinding.ActivityLoginBinding
import br.com.uri.campushub.model.UserRole
import br.com.uri.campushub.organizer.OrganizerHomeActivity
import br.com.uri.campushub.student.HomeActivity
import br.com.uri.campushub.viewmodel.AuthState
import br.com.uri.campushub.viewmodel.AuthViewModel
import br.com.uri.campushub.viewmodel.SessionState
import br.com.uri.campushub.viewmodel.SessionViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var sessionViewModel: SessionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupListeners()
        observeAuthState()
        observeSessionState()
    }

    private fun setupViewModel() {
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
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
        authViewModel.authState.observe(this) { state ->
            when (state) {
                AuthState.Idle -> setLoading(false)
                AuthState.Loading -> setLoading(true)
                AuthState.Success -> {
                    sessionViewModel.loadSession()
                }
                is AuthState.Error -> {
                    setLoading(false)
                    Toast.makeText(
                        this,
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                    authViewModel.resetState()
                }
            }
        }
    }

    private fun observeSessionState() {
        sessionViewModel.sessionState.observe(this) { state ->
            when (state) {
                SessionState.Idle,
                SessionState.Unauthenticated -> Unit
                SessionState.Loading -> setLoading(true)
                is SessionState.Authenticated -> {
                    setLoading(false)
                    navigateToHome(state.role)
                }
                is SessionState.Error -> {
                    setLoading(false)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToHome(role: UserRole) {
        val destination = when (role) {
            UserRole.STUDENT -> HomeActivity::class.java
            UserRole.ORGANIZER -> OrganizerHomeActivity::class.java
        }

        val intent = Intent(this, destination).apply {
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

        authViewModel.login(email, password)
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
