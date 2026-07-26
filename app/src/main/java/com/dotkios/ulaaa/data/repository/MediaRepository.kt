package com.dotkios.ulaaa.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Uploads image files to Firebase Storage and returns their download URLs. */
interface MediaRepository {
    suspend fun uploadProfilePhoto(uid: String, image: Uri): Result<String>
    suspend fun uploadPostImage(uid: String, postId: String, image: Uri): Result<String>
}

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage,
    @param:ApplicationContext private val context: Context,
) : MediaRepository {

    override suspend fun uploadProfilePhoto(uid: String, image: Uri): Result<String> =
        upload("users/$uid/profile.jpg", image)

    override suspend fun uploadPostImage(uid: String, postId: String, image: Uri): Result<String> =
        upload("users/$uid/posts/$postId.jpg", image)

    private suspend fun upload(path: String, image: Uri): Result<String> = runCatching {
        // Read the bytes ourselves — putFile can misreport an unreadable content:// Uri
        // as "object does not exist at location". putBytes avoids that entirely.
        val bytes = context.contentResolver.openInputStream(image)?.use { it.readBytes() }
            ?: error("Couldn't read the selected image.")
        val ref = storage.reference.child(path)
        ref.putBytes(bytes).await()
        ref.downloadUrl.await().toString()
    }
}
