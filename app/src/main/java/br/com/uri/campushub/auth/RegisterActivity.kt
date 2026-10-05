package br.com.uri.campushub.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.databinding.ActivityRegisterBinding
import br.com.uri.campushub.student.HomeActivity
import br.com.uri.campushub.viewmodel.RegisterState
import br.com.uri.campushub.viewmodel.RegisterViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupListeners()
        observeRegisterState()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[RegisterViewModel::class.java]
    }

    private fun setupListeners() {
        binding.buttonRegister.setOnClickListener {
            attemptRegistration()
        }

        binding.textLogin.setOnClickListener {
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

        val name = binding.inputName.text?.toString()?.trim().orEmpty()
        val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
        val studentId = binding.inputStudentId.text?.toString()?.trim().orEmpty()
        val course = binding.inputCourse.text?.toString()?.trim().orEmpty()
        val password = binding.inputPassword.text?.toString().orEmpty()
        val confirmPassword = binding.inputConfirmPassword.text?.toString().orEmpty()

        var isValid = true

        if (name.isBlank()) {
            binding.layoutName.error = "Informe seu nome completo."
            isValid = false
        }

        if (email.isBlank()) {
            binding.layoutEmail.error = "Informe seu e-mail."
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.error = "Informe um e-mail válido."
            isValid = false
        }

        if (studentId.isBlank()) {
            binding.layoutStudentId.error = "Informe sua matrícula."
            isValid = false
        }

        if (course.isBlank()) {
            binding.layoutCourse.error = "Informe seu curso."
            isValid = false
        }

        if (password.isBlank()) {
            binding.layoutPassword.error = "Informe uma senha."
            isValid = false
        } else if (password.length < 6) {
            binding.layoutPassword.error = "A senha deve ter pelo menos 6 caracteres."
            isValid = false
        }

        if (confirmPassword.isBlank()) {
            binding.layoutConfirmPassword.error = "Confirme sua senha."
            isValid = false
        } else if (confirmPassword != password) {
            binding.layoutConfirmPassword.error = "As senhas não coincidem."
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
        binding.progressRegister.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonRegister.isEnabled = !isLoading
        binding.inputName.isEnabled = !isLoading
        binding.inputEmail.isEnabled = !isLoading
        binding.inputStudentId.isEnabled = !isLoading
        binding.inputCourse.isEnabled = !isLoading
        binding.inputPassword.isEnabled = !isLoading
        binding.inputConfirmPassword.isEnabled = !isLoading
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun clearErrors() {
        binding.layoutName.error = null
        binding.layoutEmail.error = null
        binding.layoutStudentId.error = null
        binding.layoutCourse.error = null
        binding.layoutPassword.error = null
        binding.layoutConfirmPassword.error = null
    }
}
