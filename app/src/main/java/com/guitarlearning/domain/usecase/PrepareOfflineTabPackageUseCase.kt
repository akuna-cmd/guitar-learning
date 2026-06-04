package com.guitarlearning.domain.usecase

import com.guitarlearning.domain.model.TabItem
import com.guitarlearning.domain.repository.SoundFontRepository
import com.guitarlearning.domain.repository.TabFileRepository
import com.guitarlearning.domain.repository.TabRepository
import javax.inject.Inject

class PrepareOfflineTabPackageUseCase @Inject constructor(
    private val tabFileRepository: TabFileRepository,
    private val soundFontRepository: SoundFontRepository,
    private val tabRepository: TabRepository
) {
    suspend operator fun invoke(tab: TabItem) {
        tab.filePath?.takeIf { it.isNotBlank() }?.let { path ->
            tabFileRepository.readTabBytes(path)
        }
        soundFontRepository.readSoundFontBytes()
        tabRepository.markOfflineReady(tab.id, true)
        tabRepository.updateTabTags(tab.id, (tab.tags + "offline").distinct())
    }
}
