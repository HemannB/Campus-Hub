package br.com.uri.campushub.student

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.databinding.ActivityEditProfileBinding
import br.com.uri.campushub.model.User
import br.com.uri.campushub.viewmodel.EditProfileState
import br.com.uri.campushub.viewmodel.EditProfileViewModel

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var viewModel: EditProfileViewModel
    private var hasLoadedProfile = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupListeners()
        observeEditProfileState()

        viewModel.loadProfile()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[EditProfileViewModel::class.java]
    }

    private fun setupListeners() {
        binding.buttonBackEditProfile.setOnClickListener {
            finish()
        }

        binding.buttonSaveProfile.setOnClickListener {
            attemptProfileUpdate()
        }
    }

    private fun observeEditProfileState() {
        viewModel.editProfileState.observe(this) { state ->
            when (state) {
                EditProfileState.Idle -> Unit
                EditProfileState.Loading -> showInitialLoading(true)
                is EditProfileState.Ready -> showProfile(state.user)
                EditProfileState.Saving -> setSaving(true)
                EditProfileState.Success -> {
                    setSaving(false)
                    Toast.makeText(
                        this,
                        "Perfil atualizado com sucesso.",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                is EditProfileState.Error -> {
                    setSaving(false)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()

                    if (!hasLoadedProfile) {
                        finish()
                    }
                }
            }
        }
    }

    private fun showInitialLoading(isLoading: Boolean) {
        binding.progressEditProfile.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.formEditProfile.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showProfile(user: User) {
        hasLoadedProfile = true
        binding.inputEditName.setText(user.name)
        binding.inputEditEmail.setText(user.email)
        binding.inputEditStudentId.setText(user.studentId)
        binding.inputEditCourse.setText(user.course)
        binding.inputEditSemester.setText(user.semester)

        showInitialLoading(false)
    }

    private fun attemptProfileUpdate() {
        clearErrors()

        val name = binding.inputEditName.text?.toString()?.trim().orEmpty()
        val studentId = binding.inputEditStudentId.text?.toString()?.trim().orEmpty()
        val course = binding.inputEditCourse.text?.toString()?.trim().orEmpty()
        val semester = binding.inputEditSemester.text?.toString()?.trim().orEmpty()

        var isValid = true

        if (name.isBlank()) {
            binding.layoutEditName.error = "Informe seu nome completo."
            isValid = false
        }

        if (studentId.isBlank()) {
            binding.layoutEditStudentId.error = "Informe sua matrícula."
            isValid = false
        }

        if (course.isBlank()) {
            binding.layoutEditCourse.error = "Informe seu curso."
            isValid = false
        }

        if (!isValid) {
            return
        }

        viewModel.updateProfile(
            name = name,
            studentId = studentId,
            course = course,
            semester = semester
        )
    }

    private fun setSaving(isSaving: Boolean) {
        binding.progressEditProfile.visibility = if (isSaving) View.VISIBLE else View.GONE
        binding.buttonSaveProfile.isEnabled = !isSaving
        binding.inputEditName.isEnabled = !isSaving
        binding.inputEditStudentId.isEnabled = !isSaving
        binding.inputEditCourse.isEnabled = !isSaving
        binding.inputEditSemester.isEnabled = !isSaving
    }

    private fun clearErrors() {
        binding.layoutEditName.error = null
        binding.layoutEditStudentId.error = null
        binding.layoutEditCourse.error = null
    }
}
