package br.com.uri.campushub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uri.campushub.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditProfileViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var authRepository: FakeCurrentUserDataSource
    private lateinit var userRepository: FakeUserDataSource
    private lateinit var viewModel: EditProfileViewModel

    @Before
    fun setUp() {
        authRepository = FakeCurrentUserDataSource()
        userRepository = FakeUserDataSource()
        viewModel = EditProfileViewModel(authRepository, userRepository)
    }

    @Test
    fun `load profile rejects unauthenticated user`() {
        authRepository.userId = null

        viewModel.loadProfile()

        assertEquals(
            EditProfileState.Error("Usuário não autenticado."),
            viewModel.editProfileState.value
        )
    }

    @Test
    fun `load profile changes from loading to ready`() {
        val user = User(id = "user-1", name = "Bruno")

        viewModel.loadProfile()
        assertSame(EditProfileState.Loading, viewModel.editProfileState.value)

        userRepository.completeProfile(Result.success(user))

        assertEquals(EditProfileState.Ready(user), viewModel.editProfileState.value)
    }

    @Test
    fun `load profile failure exposes repository message`() {
        viewModel.loadProfile()

        userRepository.completeProfile(Result.failure(Exception("Perfil indisponível.")))

        assertEquals(
            EditProfileState.Error("Perfil indisponível."),
            viewModel.editProfileState.value
        )
    }

    @Test
    fun `update profile requires loaded user`() {
        viewModel.updateProfile("Bruno", "123", "Computação", "6")

        assertEquals(
            EditProfileState.Error("Carregue o perfil antes de salvar."),
            viewModel.editProfileState.value
        )
        assertEquals(null, userRepository.updateRequest)
    }

    @Test
    fun `update profile sends fields and succeeds`() {
        val user = User(id = "user-1", name = "Nome antigo")
        viewModel.loadProfile()
        userRepository.completeProfile(Result.success(user))

        viewModel.updateProfile("Bruno", "123", "Computação", "6")

        assertSame(EditProfileState.Saving, viewModel.editProfileState.value)
        assertEquals(
            FakeUserDataSource.UpdateRequest(
                userId = "user-1",
                name = "Bruno",
                studentId = "123",
                course = "Computação",
                semester = "6"
            ),
            userRepository.updateRequest
        )

        userRepository.completeUpdate(Result.success(Unit))

        assertSame(EditProfileState.Success, viewModel.editProfileState.value)
    }

    @Test
    fun `update profile failure exposes repository message`() {
        val user = User(id = "user-1", name = "Nome antigo")
        viewModel.loadProfile()
        userRepository.completeProfile(Result.success(user))

        viewModel.updateProfile("Bruno", "123", "Computação", "6")
        userRepository.completeUpdate(Result.failure(Exception("Falha ao atualizar.")))

        assertEquals(
            EditProfileState.Error("Falha ao atualizar."),
            viewModel.editProfileState.value
        )
    }
}
