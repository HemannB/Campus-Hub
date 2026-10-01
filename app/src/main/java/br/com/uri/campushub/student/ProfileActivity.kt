package br.com.uri.campushub.student

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.R
import br.com.uri.campushub.model.User
import br.com.uri.campushub.viewmodel.ProfileState
import br.com.uri.campushub.viewmodel.ProfileViewModel
import com.google.android.material.card.MaterialCardView

class ProfileActivity : AppCompatActivity() {

    private lateinit var viewModel: ProfileViewModel

    private lateinit var progressProfile: ProgressBar
    private lateinit var cardProfile: MaterialCardView

    private lateinit var textName: TextView
    private lateinit var textEmail: TextView
    private lateinit var textStudentId: TextView
    private lateinit var textCourse: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        bindViews()
        setupViewModel()
        observeProfileState()

        viewModel.loadProfile()
    }

    private fun bindViews() {
        progressProfile = findViewById(R.id.progressProfile)
        cardProfile = findViewById(R.id.cardProfile)

        textName = findViewById(R.id.textName)
        textEmail = findViewById(R.id.textEmail)
        textStudentId = findViewById(R.id.textStudentId)
        textCourse = findViewById(R.id.textCourse)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
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
        progressProfile.visibility =
            if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            cardProfile.visibility = View.GONE
        }
    }

    private fun showProfile(user: User) {
        textName.text = user.name
        textEmail.text = user.email
        textStudentId.text = user.studentId
        textCourse.text = user.course

        cardProfile.visibility = View.VISIBLE
    }
}