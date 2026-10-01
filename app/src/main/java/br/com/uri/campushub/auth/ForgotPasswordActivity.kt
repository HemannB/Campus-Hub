package br.com.uri.campushub.auth

import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var layoutEmail: TextInputLayout
    private lateinit var inputEmail: TextInputEditText
    private lateinit var buttonSendReset: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        bindViews()
        setupListeners()
    }

    private fun bindViews() {
        layoutEmail = findViewById(R.id.layoutEmail)
        inputEmail = findViewById(R.id.inputEmail)
        buttonSendReset = findViewById(R.id.buttonSendReset)
    }

    private fun setupListeners() {
        buttonSendReset.setOnClickListener {
            validateEmail()
        }

        findViewById<TextView>(R.id.textBackToLogin).setOnClickListener {
            finish()
        }
    }

    private fun validateEmail() {
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

        Toast.makeText(
            this,
            "E-mail válido.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
