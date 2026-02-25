package com.arflix.tv.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.arflix.tv.data.model.CatalogSourceType
import com.arflix.tv.data.model.MediaItem

@Composable
fun SearchScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search movies and TV shows...") },
                singleLine = true
            )

            Spacer(modifier = Modifier.width(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Deep Search",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = uiState.isDeepSearchEnabled,
                    onCheckedChange = viewModel::toggleDeepSearch
                )
                Spacer(modifier = Modifier.width(16.dp))
                TextButton(onClick = onNavigateToSettings) {
                    Text("Settings", color = Color.White)
                }
            }
        }

        if (uiState.isDeepSearchEnabled) {
            Text(
                text = "Searching TMDB + CloudStream extensions",
                color = Color.Cyan,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        if (uiState.isDeepSearchLoading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Deep searching CloudStream...", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }

            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error ?: "", color = Color.Red)
                }
            }

            uiState.query.isNotBlank() -> {
                SearchResultsSection(
                    movies = uiState.movies,
                    tvShows = uiState.tvShows,
                    showSourceBadges = uiState.isDeepSearchEnabled
                )
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Search for your favorite movies and TV shows",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsSection(
    movies: List<MediaItem>,
    tvShows: List<MediaItem>,
    showSourceBadges: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (movies.isNotEmpty()) {
            Text(
                text = "Movies (${movies.size})",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(movies) { movie ->
                    MediaCard(item = movie, showSourceBadge = showSourceBadges)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (tvShows.isNotEmpty()) {
            Text(
                text = "TV Shows (${tvShows.size})",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(tvShows) { show ->
                    MediaCard(item = show, showSourceBadge = showSourceBadges)
                }
            }
        }

        if (movies.isEmpty() && tvShows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results found", color = Color.Gray, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun MediaCard(
    item: MediaItem,
    showSourceBadge: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 140.dp, height = 200.dp)
        ) {
            if (item.posterUrl != null) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.title.take(2), color = Color.White, fontSize = 24.sp)
                }
            }

            if (showSourceBadge && item.source == CatalogSourceType.CLOUDSTREAM) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "CS",
                        color = Color.Black,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 2
        )
        if (item.year != null) {
            Text(
                text = item.year.toString(),
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
        if (showSourceBadge && item.sourceLabel != null) {
            Text(
                text = item.sourceLabel,
                color = Color.Cyan,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
