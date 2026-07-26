package com.dotkios.ulaaa.ui.feed

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.Post
import com.dotkios.ulaaa.data.repository.AuthRepository
import com.dotkios.ulaaa.data.repository.FriendRepository
import com.dotkios.ulaaa.data.repository.MediaRepository
import com.dotkios.ulaaa.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val currentUid: String = "",
    val stories: List<Post> = emptyList(),
    val feed: List<Post> = emptyList(),
    val posting: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    authRepository: AuthRepository,
    friendRepository: FriendRepository,
    private val postRepository: PostRepository,
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val myUid: String = authRepository.currentUser?.uid.orEmpty()
    private val userName: String =
        authRepository.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Explorer"
    private val authorPhoto: String = authRepository.currentUser?.photoUrl?.toString().orEmpty()

    private val posting = MutableStateFlow(false)

    // Feed = posts from me + my friends, newest first.
    private val feedFlow = friendRepository.friends().flatMapLatest { friends ->
        postRepository.feedPosts(listOf(myUid) + friends.map { it.uid })
    }

    val uiState: StateFlow<FeedUiState> = combine(feedFlow, posting) { feed, isPosting ->
        FeedUiState(
            currentUid = myUid,
            stories = feed.distinctBy { it.uid }.take(12),
            feed = feed,
            posting = isPosting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState(currentUid = myUid))

    fun toggleLike(post: Post) {
        if (myUid.isBlank()) return
        val nowLiked = !post.likes.contains(myUid)
        viewModelScope.launch { postRepository.toggleLike(post.id, myUid, nowLiked) }
    }

    /** Picks straight from the story tile: upload the image and publish it as a post. */
    fun addStory(image: Uri) {
        if (myUid.isBlank()) return
        posting.value = true
        viewModelScope.launch {
            val postId = postRepository.newPostId()
            mediaRepository.uploadPostImage(myUid, postId, image)
                .onSuccess { url ->
                    postRepository.addPost(
                        Post(
                            id = postId,
                            uid = myUid,
                            authorName = userName,
                            authorPhotoUrl = authorPhoto,
                            imageUrl = url,
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                }
            posting.value = false
        }
    }
}
