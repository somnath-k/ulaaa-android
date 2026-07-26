package com.dotkios.ulaaa.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
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
) : MediaRepository {

    override suspend fun uploadProfilePhoto(uid: String, image: Uri): Result<String> =
        upload("users/$uid/profile.jpg", image)

    override suspend fun uploadPostImage(uid: String, postId: String, image: Uri): Result<String> =
        upload("users/$uid/posts/$postId.jpg", image)

    private suspend fun upload(path: String, image: Uri): Result<String> = runCatching {
        val ref = storage.reference.child(path)
        ref.putFile(image).await()
        ref.downloadUrl.await().toString()
    }
}
