package br.com.uri.campushub.auth

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.uri.campushub.R
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        bindView()
        setupListeners()
    }
    private fun bindView() {
        layoutEmail = findViewById(R.id.layoutEmail)
        layoutPassword = findViewById(R.id.layoutPassword)

        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)

        buttonLogin = findViewById(R.id.buttonLogin)
        buttonGoogle = findViewById(R.id.buttonGoogle)
    }

    private fun setupListeners() {
        buttonLogin.setOnClickListener {
            //validar login
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
            Toast.makeText(
                this,
                "Tela de cadastro....",
                Toast.LENGTH_SHORT
            ).show()
        }

        buttonGoogle.setOnClickListener {
            Toast.makeText(
                this,
                "Login com Google...",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun validateLogin() {
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

        Toast.makeText(
            this,
            "Dados válidos...",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun clearErrors() {
        layoutEmail.error = null
        layoutPassword.error = null
    }


}