package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var authRepository: FakeCurrentUserDataSource
    private lateinit var userRepository: FakeUserDataSource
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        authRepository = FakeCurrentUserDataSource()
        userRepository = FakeUserDataSource()
        viewModel = ProfileViewModel(authRepository, userRepository)
    }

    @Test
    fun `load profile rejects unauthenticated user`() {
        authRepository.userId = null

        viewModel.loadProfile()

        assertEquals(
            ProfileState.Error("Usuário não autenticado."),
            viewModel.profileState.value
        )
        assertEquals(null, userRepository.requestedUserId)
    }

    @Test
    fun `load profile changes from loading to success`() {
        val user = User(id = "user-1", name = "Bruno")

        viewModel.loadProfile()

        assertSame(ProfileState.Loading, viewModel.profileState.value)
        assertEquals("user-1", userRepository.requestedUserId)

        userRepository.completeProfile(Result.success(user))

        assertEquals(ProfileState.Success(user), viewModel.profileState.value)
    }

    @Test
    fun `load profile failure exposes repository message`() {
        viewModel.loadProfile()

        userRepository.completeProfile(Result.failure(Exception("Perfil indisponível.")))

        assertEquals(
            ProfileState.Error("Perfil indisponível."),
            viewModel.profileState.value
        )
    }
}
