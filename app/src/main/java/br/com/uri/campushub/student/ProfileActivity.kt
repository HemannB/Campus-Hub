package br.com.uri.campushub.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.MainActivity
import br.com.uri.campushub.databinding.ActivityProfileBinding
import br.com.uri.campushub.model.User
import br.com.uri.campushub.viewmodel.AuthViewModel
import br.com.uri.campushub.viewmodel.ProfileState
import br.com.uri.campushub.viewmodel.ProfileViewModel

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModels()
        setupListeners()
        observeProfileState()
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.loadProfile()
    }

    private fun setupViewModels() {
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
    }

    private fun setupListeners() {
        binding.buttonBackProfile.setOnClickListener {
            finish()
        }

        binding.buttonEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        binding.buttonMyEvents.setOnClickListener {
            val intent = Intent(this, MyEventsActivity::class.java)
            startActivity(intent)
        }

        binding.buttonMyFavorites.setOnClickListener {
            val intent = Intent(this, MyFavoritesActivity::class.java)
            startActivity(intent)
        }

        binding.buttonMapTest.setOnClickListener {
            val intent = Intent(this, MapTestActivity::class.java)
            startActivity(intent)
        }

        binding.buttonLogout.setOnClickListener {
            authViewModel.logout()
            navigateToMain()
        }
    }

    private fun observeProfileState() {
        profileViewModel.profileState.observe(this) { state ->
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

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}
