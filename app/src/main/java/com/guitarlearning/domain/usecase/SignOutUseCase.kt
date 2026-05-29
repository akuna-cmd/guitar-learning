package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.repository.SyncRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val auth: FirebaseAuth
) {
    suspend operator fun invoke(): Result<Unit> {
        val clearLocalDataResult = syncRepository.clearLocalUserData()
        if (clearLocalDataResult.isFailure) return clearLocalDataResult
        auth.signOut()
        return Result.success(Unit)
    }
}
