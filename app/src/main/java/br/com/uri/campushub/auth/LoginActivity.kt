package br.com.uri.campushub.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.R
import br.com.uri.campushub.student.HomeActivity
import br.com.uri.campushub.viewmodel.AuthState
import br.com.uri.campushub.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var layoutEmail : TextInputLayout
    private lateinit var layoutPassword : TextInputLayout

    private lateinit var inputEmail : TextInputEditText
    private lateinit var inputPassword : TextInputEditText

    private lateinit var buttonLogin : MaterialButton
    private lateinit var buttonGoogle : MaterialButton
    private lateinit var progressLogin: ProgressBar

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        bindView()
        setupViewModel()
        setupListeners()
        observeAuthState()
    }
    private fun bindView() {
        layoutEmail = findViewById(R.id.layoutEmail)
        layoutPassword = findViewById(R.id.layoutPassword)

        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)

        buttonLogin = findViewById(R.id.buttonLogin)
        buttonGoogle = findViewById(R.id.buttonGoogle)
        progressLogin = findViewById(R.id.progressLogin)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
    }

    private fun setupListeners() {
        buttonLogin.setOnClickListener {
            attemptLogin()
        }

        findViewById<android.widget.TextView>(
            R.id.textForgotPassword
        ).setOnClickListener {
            Toast.makeText(
                this,
                "Recuperação de senha...",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<android.widget.TextView>(
            R.id.textRegister
        ).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        buttonGoogle.setOnClickListener {
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

        val email = inputEmail.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val password = inputPassword.text
            ?.toString()
            .orEmpty()

        var isValid = true

        if(email.isBlank()) {
            layoutEmail.error = "Informe seu e-mail."
            isValid = false
        } else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.error = "Informe um e-mail valido."
            isValid = false
        }

        if(password.isBlank()){
            layoutPassword.error = "Informe sua senha."
            isValid = false;
        }

        if (!isValid){
            return
        }

        viewModel.login(email, password)
    }

    private fun setLoading(isLoading: Boolean) {
        progressLogin.visibility = if (isLoading) View.VISIBLE else View.GONE
        buttonLogin.isEnabled = !isLoading
        buttonGoogle.isEnabled = !isLoading
        inputEmail.isEnabled = !isLoading
        inputPassword.isEnabled = !isLoading
    }

    private fun clearErrors() {
        layoutEmail.error = null
        layoutPassword.error = null
    }


}
