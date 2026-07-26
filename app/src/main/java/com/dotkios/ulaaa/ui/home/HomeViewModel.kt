package com.dotkios.ulaaa.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.Category
import com.dotkios.ulaaa.data.model.CuratedItinerary
import com.dotkios.ulaaa.data.model.Landmark
import com.dotkios.ulaaa.data.model.Post
import android.net.Uri
import com.dotkios.ulaaa.data.repository.AuthRepository
import com.dotkios.ulaaa.data.repository.FriendRepository
import com.dotkios.ulaaa.data.repository.LocationRepository
import com.dotkios.ulaaa.data.repository.MediaRepository
import com.dotkios.ulaaa.data.repository.PlaceImageRepository
import com.dotkios.ulaaa.data.repository.PostRepository
import com.dotkios.ulaaa.data.repository.RecommendationRepository
import com.dotkios.ulaaa.data.repository.TripRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    tripRepository: TripRepository,
    authRepository: AuthRepository,
    friendRepository: FriendRepository,
    private val postRepository: PostRepository,
    private val mediaRepository: MediaRepository,
    private val placeImageRepository: PlaceImageRepository,
    private val locationRepository: LocationRepository,
    private val recommendationRepository: RecommendationRepository,
) : ViewModel() {

    private val myUid: String = authRepository.currentUser?.uid.orEmpty()
    private val authorPhoto: String = authRepository.currentUser?.photoUrl?.toString().orEmpty()

    // Feed = posts from me + my friends, newest first.
    private val feedFlow = friendRepository.friends().flatMapLatest { friends ->
        postRepository.feedPosts(listOf(myUid) + friends.map { it.uid })
    }

    // Live rails — no mock; empty + loading until Gemini + photos resolve.
    private val curated = MutableStateFlow<List<CuratedItinerary>>(emptyList())
    private val curatedLoading = MutableStateFlow(true)
    private val landmarks = MutableStateFlow<List<Landmark>>(emptyList())
    private val landmarksLoading = MutableStateFlow(true)

    private val userName: String =
        authRepository.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Explorer"

    private data class Rails(
        val curated: List<CuratedItinerary>,
        val curatedLoading: Boolean,
        val landmarks: List<Landmark>,
        val landmarksLoading: Boolean,
    )

    private val rails = combine(curated, curatedLoading, landmarks, landmarksLoading) { c, cl, l, ll ->
        Rails(c, cl, l, ll)
    }

    val uiState: StateFlow<HomeUiState> =
        combine(tripRepository.trips, rails, feedFlow) { trips, r, feed ->
            HomeUiState(
                isLoading = false,
                userName = userName,
                currentUid = myUid,
                feed = feed,
                stories = feed.distinctBy { it.uid }.take(12),
                trips = trips,
                curated = r.curated,
                curatedLoading = r.curatedLoading,
                nearbyLandmarks = r.landmarks,
                nearbyLoading = r.landmarksLoading,
                categories = MockData.categories,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(isLoading = true, userName = userName),
        )

    init {
        loadRecommendations()
    }

    fun toggleLike(post: Post) {
        if (myUid.isBlank()) return
        val nowLiked = !post.likes.contains(myUid)
        viewModelScope.launch { postRepository.toggleLike(post.id, myUid, nowLiked) }
    }

    /** Picks straight from the story tile: upload the image and publish it as a post. */
    fun addStory(image: Uri) {
        if (myUid.isBlank()) return
        _posting.value = true
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
            _posting.value = false
        }
    }

    val posting: StateFlow<Boolean> get() = _posting
    private val _posting = MutableStateFlow(false)

    private fun loadRecommendations() {
        viewModelScope.launch {
            val place = locationRepository.currentPlaceLabel()
            launch {
                val list = recommendationRepository.nearbyLandmarks(place).getOrNull().orEmpty()
                landmarks.value = list.map { it.copy(imageUrl = placeImageRepository.resolve(it.name, it.category).url) }
                landmarksLoading.value = false
            }
            launch {
                val list = recommendationRepository.curatedItineraries(place).getOrNull().orEmpty()
                curated.value = list.map { it.copy(imageUrl = placeImageRepository.resolve(it.destination, it.title).url) }
                curatedLoading.value = false
            }
        }
    }

}

/** Only the category chips remain static; trips/curated/landmarks are all live. */
private object MockData {
    val categories = listOf(
        Category("cat1", "Beaches", "🏖️"),
        Category("cat2", "Mountains", "⛰️"),
        Category("cat3", "Heritage", "🏛️"),
        Category("cat4", "Food", "🍜"),
        Category("cat5", "Nature", "🌿"),
    )
}
