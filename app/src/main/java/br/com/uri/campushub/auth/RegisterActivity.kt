package br.com.uri.campushub.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.R
import br.com.uri.campushub.student.HomeActivity
import br.com.uri.campushub.viewmodel.RegisterState
import br.com.uri.campushub.viewmodel.RegisterViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var layoutName: TextInputLayout
    private lateinit var layoutEmail: TextInputLayout
    private lateinit var layoutStudentId: TextInputLayout
    private lateinit var layoutCourse: TextInputLayout
    private lateinit var layoutPassword: TextInputLayout
    private lateinit var layoutConfirmPassword: TextInputLayout

    private lateinit var inputName: TextInputEditText
    private lateinit var inputEmail: TextInputEditText
    private lateinit var inputStudentId: TextInputEditText
    private lateinit var inputCourse: TextInputEditText
    private lateinit var inputPassword: TextInputEditText
    private lateinit var inputConfirmPassword: TextInputEditText

    private lateinit var buttonRegister: MaterialButton
    private lateinit var progressRegister: ProgressBar

    private lateinit var viewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        bindViews()
        setupViewModel()
        setupListeners()
        observeRegisterState()
    }

    private fun bindViews() {
        layoutName = findViewById(R.id.layoutName)
        layoutEmail = findViewById(R.id.layoutEmail)
        layoutStudentId = findViewById(R.id.layoutStudentId)
        layoutCourse = findViewById(R.id.layoutCourse)
        layoutPassword = findViewById(R.id.layoutPassword)
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword)

        inputName = findViewById(R.id.inputName)
        inputEmail = findViewById(R.id.inputEmail)
        inputStudentId = findViewById(R.id.inputStudentId)
        inputCourse = findViewById(R.id.inputCourse)
        inputPassword = findViewById(R.id.inputPassword)
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword)

        buttonRegister = findViewById(R.id.buttonRegister)
        progressRegister = findViewById(R.id.progressRegister)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[RegisterViewModel::class.java]
    }

    private fun setupListeners() {
        buttonRegister.setOnClickListener {
            attemptRegistration()
        }

        findViewById<TextView>(R.id.textLogin).setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun observeRegisterState() {
        viewModel.registerState.observe(this) { state ->
            when (state) {
                RegisterState.Idle -> setLoading(false)
                RegisterState.Loading -> setLoading(true)
                RegisterState.Success -> {
                    setLoading(false)
                    navigateToHome()
                }
                is RegisterState.Error -> {
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

    private fun attemptRegistration() {
        clearErrors()

        val name = inputName.text?.toString()?.trim().orEmpty()
        val email = inputEmail.text?.toString()?.trim().orEmpty()
        val studentId = inputStudentId.text?.toString()?.trim().orEmpty()
        val course = inputCourse.text?.toString()?.trim().orEmpty()
        val password = inputPassword.text?.toString().orEmpty()
        val confirmPassword = inputConfirmPassword.text?.toString().orEmpty()

        var isValid = true

        if (name.isBlank()) {
            layoutName.error = "Informe seu nome completo."
            isValid = false
        }

        if (email.isBlank()) {
            layoutEmail.error = "Informe seu e-mail."
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.error = "Informe um e-mail válido."
            isValid = false
        }

        if (studentId.isBlank()) {
            layoutStudentId.error = "Informe sua matrícula."
            isValid = false
        }

        if (course.isBlank()) {
            layoutCourse.error = "Informe seu curso."
            isValid = false
        }

        if (password.isBlank()) {
            layoutPassword.error = "Informe uma senha."
            isValid = false
        } else if (password.length < 6) {
            layoutPassword.error = "A senha deve ter pelo menos 6 caracteres."
            isValid = false
        }

        if (confirmPassword.isBlank()) {
            layoutConfirmPassword.error = "Confirme sua senha."
            isValid = false
        } else if (confirmPassword != password) {
            layoutConfirmPassword.error = "As senhas não coincidem."
            isValid = false
        }

        if (!isValid) {
            return
        }

        viewModel.register(
            name = name,
            email = email,
            studentId = studentId,
            course = course,
            password = password
        )
    }

    private fun setLoading(isLoading: Boolean) {
        progressRegister.visibility = if (isLoading) View.VISIBLE else View.GONE
        buttonRegister.isEnabled = !isLoading
        inputName.isEnabled = !isLoading
        inputEmail.isEnabled = !isLoading
        inputStudentId.isEnabled = !isLoading
        inputCourse.isEnabled = !isLoading
        inputPassword.isEnabled = !isLoading
        inputConfirmPassword.isEnabled = !isLoading
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun clearErrors() {
        layoutName.error = null
        layoutEmail.error = null
        layoutStudentId.error = null
        layoutCourse.error = null
        layoutPassword.error = null
        layoutConfirmPassword.error = null
    }
}
