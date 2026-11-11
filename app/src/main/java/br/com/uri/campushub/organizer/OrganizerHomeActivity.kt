package br.com.uri.campushub.organizer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import br.com.uri.campushub.MainActivity
import br.com.uri.campushub.databinding.ActivityOrganizerHomeBinding
import br.com.uri.campushub.viewmodel.AuthViewModel

class OrganizerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrganizerHomeBinding
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrganizerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        binding.buttonLogout.setOnClickListener {
            authViewModel.logout()
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }
}
