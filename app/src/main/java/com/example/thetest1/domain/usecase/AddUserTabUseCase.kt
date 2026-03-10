package com.example.thetest1.domain.usecase

import android.net.Uri
import com.example.thetest1.domain.repository.TabRepository

class AddUserTabUseCase(private val tabRepository: TabRepository) {
    suspend operator fun invoke(uri: Uri) = tabRepository.addUserTab(uri)
}