package com.dotkios.ulaaa.data.repository

import com.dotkios.ulaaa.data.model.Post
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface PostRepository {
    /** Realtime stream of a single user's posts, newest first. */
    fun userPosts(uid: String): Flow<List<Post>>
    /** Realtime feed of posts from the given users (self + friends), newest first. */
    fun feedPosts(uids: List<String>): Flow<List<Post>>
    suspend fun addPost(post: Post): Result<Unit>
    /** A pre-allocated post id so the image can be uploaded before the doc is written. */
    fun newPostId(): String
    suspend fun toggleLike(postId: String, uid: String, like: Boolean): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>
}

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : PostRepository {

    private val posts get() = firestore.collection(COLLECTION)

    override fun newPostId(): String = posts.document().id

    override fun userPosts(uid: String): Flow<List<Post>> = observe { it.whereEqualTo("uid", uid) }

    override fun feedPosts(uids: List<String>): Flow<List<Post>> {
        val ids = uids.filter { it.isNotBlank() }.distinct().take(10) // whereIn caps at 10
        if (ids.isEmpty()) return flowOf(emptyList())
        return observe { it.whereIn("uid", ids) }
    }

    /** Single-field query + client sort avoids needing a composite index. */
    private fun observe(query: (com.google.firebase.firestore.CollectionReference) -> com.google.firebase.firestore.Query): Flow<List<Post>> =
        callbackFlow {
            val registration = query(posts).addSnapshotListener { snapshot, error ->
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

    override suspend fun addPost(post: Post): Result<Unit> = runCatching {
        posts.document(post.id).set(post).await()
    }

    override suspend fun toggleLike(postId: String, uid: String, like: Boolean): Result<Unit> = runCatching {
        val op = if (like) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
        posts.document(postId).update("likes", op).await()
    }

    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        posts.document(postId).delete().await()
    }

    private companion object {
        const val COLLECTION = "posts"
    }
}
