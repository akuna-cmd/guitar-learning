package com.example.thetest1.data.repository

import android.content.Context
import android.net.Uri
import com.example.thetest1.domain.repository.TabFileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabFileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TabFileRepository {

    override suspend fun readTabBytes(path: String): ByteArray {
        return when {
            path.startsWith("content://") -> {
                val stream = context.contentResolver.openInputStream(Uri.parse(path))
                    ?: throw IllegalStateException()
                stream.use { it.readBytes() }
            }
            path.startsWith("file://") -> {
                val stream = context.contentResolver.openInputStream(Uri.parse(path))
                    ?: throw IllegalStateException()
                stream.use { it.readBytes() }
            }
            path.startsWith("/") -> {
                File(path).inputStream().use { it.readBytes() }
            }
            else -> {
                context.assets.open(path).use { it.readBytes() }
            }
        }
    }
}
