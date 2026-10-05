package br.com.uri.campushub.student

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.R
import br.com.uri.campushub.model.User
import br.com.uri.campushub.viewmodel.EditProfileState
import br.com.uri.campushub.viewmodel.EditProfileViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class EditProfileActivity : AppCompatActivity() {

    private lateinit var viewModel: EditProfileViewModel
    private var hasLoadedProfile = false

    private lateinit var formEditProfile: LinearLayout
    private lateinit var progressEditProfile: ProgressBar
    private lateinit var buttonSaveProfile: MaterialButton

    private lateinit var layoutName: TextInputLayout
    private lateinit var layoutStudentId: TextInputLayout
    private lateinit var layoutCourse: TextInputLayout

    private lateinit var inputName: TextInputEditText
    private lateinit var inputEmail: TextInputEditText
    private lateinit var inputStudentId: TextInputEditText
    private lateinit var inputCourse: TextInputEditText
    private lateinit var inputSemester: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        bindViews()
        setupViewModel()
        setupListeners()
        observeEditProfileState()

        viewModel.loadProfile()
    }

    private fun bindViews() {
        formEditProfile = findViewById(R.id.formEditProfile)
        progressEditProfile = findViewById(R.id.progressEditProfile)
        buttonSaveProfile = findViewById(R.id.buttonSaveProfile)

        layoutName = findViewById(R.id.layoutEditName)
        layoutStudentId = findViewById(R.id.layoutEditStudentId)
        layoutCourse = findViewById(R.id.layoutEditCourse)

        inputName = findViewById(R.id.inputEditName)
        inputEmail = findViewById(R.id.inputEditEmail)
        inputStudentId = findViewById(R.id.inputEditStudentId)
        inputCourse = findViewById(R.id.inputEditCourse)
        inputSemester = findViewById(R.id.inputEditSemester)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[EditProfileViewModel::class.java]
    }

    private fun setupListeners() {
        findViewById<MaterialButton>(R.id.buttonBackEditProfile).setOnClickListener {
            finish()
        }

        buttonSaveProfile.setOnClickListener {
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
        progressEditProfile.visibility = if (isLoading) View.VISIBLE else View.GONE
        formEditProfile.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showProfile(user: User) {
        hasLoadedProfile = true
        inputName.setText(user.name)
        inputEmail.setText(user.email)
        inputStudentId.setText(user.studentId)
        inputCourse.setText(user.course)
        inputSemester.setText(user.semester)

        showInitialLoading(false)
    }

    private fun attemptProfileUpdate() {
        clearErrors()

        val name = inputName.text?.toString()?.trim().orEmpty()
        val studentId = inputStudentId.text?.toString()?.trim().orEmpty()
        val course = inputCourse.text?.toString()?.trim().orEmpty()
        val semester = inputSemester.text?.toString()?.trim().orEmpty()

        var isValid = true

        if (name.isBlank()) {
            layoutName.error = "Informe seu nome completo."
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
        progressEditProfile.visibility = if (isSaving) View.VISIBLE else View.GONE
        buttonSaveProfile.isEnabled = !isSaving
        inputName.isEnabled = !isSaving
        inputStudentId.isEnabled = !isSaving
        inputCourse.isEnabled = !isSaving
        inputSemester.isEnabled = !isSaving
    }

    private fun clearErrors() {
        layoutName.error = null
        layoutStudentId.error = null
        layoutCourse.error = null
    }
}
