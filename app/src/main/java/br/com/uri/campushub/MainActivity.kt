package br.com.uri.campushub

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.auth.LoginActivity
import br.com.uri.campushub.auth.RegisterActivity
import br.com.uri.campushub.databinding.ActivityMainBinding
import br.com.uri.campushub.model.UserRole
import br.com.uri.campushub.organizer.OrganizerHomeActivity
import br.com.uri.campushub.student.HomeActivity
import br.com.uri.campushub.viewmodel.SessionState
import br.com.uri.campushub.viewmodel.SessionViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionViewModel: SessionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionViewModel = ViewModelProvider(this)[SessionViewModel::class.java]
        observeSession()

        binding.buttonLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.buttonRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        sessionViewModel.loadSession()
    }

    private fun observeSession() {
        sessionViewModel.sessionState.observe(this) { state ->
            if (state is SessionState.Authenticated) {
                navigateToHome(state.role)
            }
        }
    }

    private fun navigateToHome(role: UserRole) {
        val destination = when (role) {
            UserRole.STUDENT -> HomeActivity::class.java
            UserRole.ORGANIZER -> OrganizerHomeActivity::class.java
        }

        val intent = Intent(this, destination).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}
