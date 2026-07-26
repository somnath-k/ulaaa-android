package com.dotkios.ulaaa.data.repository

import com.dotkios.ulaaa.data.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface PostRepository {
    /** Realtime stream of a user's posts, newest first. */
    fun userPosts(uid: String): Flow<List<Post>>
    suspend fun addPost(uid: String, imageUrl: String, caption: String, postId: String): Result<Unit>
    /** A pre-allocated post id so the image can be uploaded before the doc is written. */
    fun newPostId(): String
    suspend fun deletePost(postId: String): Result<Unit>
}

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : PostRepository {

    private val posts get() = firestore.collection(COLLECTION)

    override fun newPostId(): String = posts.document().id

    override fun userPosts(uid: String): Flow<List<Post>> = callbackFlow {
        // Single-field query + client sort avoids needing a composite index.
        val registration = posts.whereEqualTo("uid", uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snapshot?.documents
                ?.mapNotNull { it.toObject(Post::class.java) }
                ?.sortedByDescending { it.createdAt }
                .orEmpty()
            trySend(items)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addPost(
        uid: String,
        imageUrl: String,
        caption: String,
        postId: String,
    ): Result<Unit> = runCatching {
        val post = Post(
            id = postId,
            uid = uid,
            imageUrl = imageUrl,
            caption = caption.trim(),
            createdAt = System.currentTimeMillis(),
        )
        posts.document(postId).set(post).await()
    }

    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        posts.document(postId).delete().await()
    }

    private companion object {
        const val COLLECTION = "posts"
    }
}
