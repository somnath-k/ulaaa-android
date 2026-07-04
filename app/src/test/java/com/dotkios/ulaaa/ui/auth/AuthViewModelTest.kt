package com.dotkios.ulaaa.ui.auth

import com.dotkios.ulaaa.MainDispatcherRule
import com.dotkios.ulaaa.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakeAuthRepository(
    private val result: Result<FirebaseUser> = Result.failure(RuntimeException("no-op")),
) : AuthRepository {
    var signInCalled = false
    override val currentUser: FirebaseUser? = null
    override fun authState(): Flow<FirebaseUser?> = flowOf(null)
    override suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        signInCalled = true
        return result
    }
    override suspend fun signUp(name: String, email: String, password: String): Result<FirebaseUser> = result
    override fun signOut() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `blank email shows error and does not call repository`() = runTest {
        val repo = FakeAuthRepository()
        val vm = AuthViewModel(repo)

        vm.login()

        assertEquals("Enter your email", vm.uiState.value.error)
        assertFalse(repo.signInCalled)
    }

    @Test
    fun `short password is rejected before hitting the network`() = runTest {
        val repo = FakeAuthRepository()
        val vm = AuthViewModel(repo)

        vm.onEmailChange("boss@ulaaa.app")
        vm.onPasswordChange("123")
        vm.login()

        assertEquals("Password must be at least 6 characters", vm.uiState.value.error)
        assertFalse(repo.signInCalled)
    }

    @Test
    fun `valid input with failed sign-in surfaces the error`() = runTest {
        val repo = FakeAuthRepository(Result.failure(RuntimeException("Invalid credentials")))
        val vm = AuthViewModel(repo)

        vm.onEmailChange("boss@ulaaa.app")
        vm.onPasswordChange("secret1")
        vm.login()
        advanceUntilIdle()

        assertTrue(repo.signInCalled)
        assertEquals("Invalid credentials", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }
}
