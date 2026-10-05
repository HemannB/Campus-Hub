package br.com.uri.campushub.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.databinding.ActivityProfileBinding
import br.com.uri.campushub.model.User
import br.com.uri.campushub.viewmodel.ProfileState
import br.com.uri.campushub.viewmodel.ProfileViewModel

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupListeners()
        observeProfileState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    private fun setupListeners() {
        binding.buttonEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeProfileState() {
        viewModel.profileState.observe(this) { state ->
            when (state) {
                ProfileState.Idle -> {
                    setLoading(false)
                }

                ProfileState.Loading -> {
                    setLoading(true)
                }

                is ProfileState.Success -> {
                    setLoading(false)
                    showProfile(state.user)
                }

                is ProfileState.Error -> {
                    setLoading(false)
                    Toast.makeText(
                        this,
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressProfile.visibility =
            if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            binding.cardProfile.visibility = View.GONE
            binding.buttonEditProfile.visibility = View.GONE
        }
    }

    private fun showProfile(user: User) {
        binding.textName.text = user.name
        binding.textEmail.text = user.email
        binding.textStudentId.text = user.studentId
        binding.textCourse.text = user.course
        binding.textSemester.text = user.semester.ifBlank { "Não informado" }

        binding.cardProfile.visibility = View.VISIBLE
        binding.buttonEditProfile.visibility = View.VISIBLE
    }
}
