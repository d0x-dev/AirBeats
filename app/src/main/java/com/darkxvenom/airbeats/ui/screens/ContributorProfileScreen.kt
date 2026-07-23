package com.darkxvenom.airbeats.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.darkxvenom.airbeats.utils.*
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch

private fun cleanMarkdownReadme(raw: String): String {
    var cleaned = raw
    cleaned = cleaned.replace(Regex("""<img\s+[^>]*>""", RegexOption.IGNORE_CASE), "")
    cleaned = cleaned.replace(Regex("""!\[[^\]]*\]\([^)]+\)"""), "")
    cleaned = cleaned.replace(Regex("""</?(picture|source)[^>]*>""", RegexOption.IGNORE_CASE), "")
    cleaned = cleaned.replace("\uFFFC", "")
    cleaned = cleaned.replace(Regex("""<a\s+[^>]*>\s*</a>""", RegexOption.IGNORE_CASE), "")
    return cleaned
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributorProfileScreen(
    navController: NavController,
    username: String
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val client = remember { GitHubApiClient() }
    
    var profile by remember { mutableStateOf<GitHubProfile?>(null) }
    var repos by remember { mutableStateOf<List<GitHubRepo>>(emptyList()) }
    var events by remember { mutableStateOf<List<GitHubEvent>>(emptyList()) }
    var commits by remember { mutableStateOf<List<GitHubCommitWrap>>(emptyList()) }
    var readme by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val svgImageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }

    LaunchedEffect(username) {
        launch { profile = client.getUserProfile(username) }
        launch { repos = client.getUserRepos(username) }
        launch { events = client.getUserEvents(username) }
        launch { commits = client.getRepoCommits("d0x-dev", "AirBeats", username) }
        launch { readme = client.getProfileReadme(username) }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile?.name ?: username, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // HEADER SECTION
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = profile?.avatar_url,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = profile?.name ?: username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(text = "@${profile?.login}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row {
                                Text(text = "${profile?.followers} Followers", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "${profile?.following} Following", style = MaterialTheme.typography.labelSmall)
                            }
                            if (!profile?.location.isNullOrBlank()) {
                                Text(text = "📍 ${profile?.location}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if (!profile?.bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = profile?.bio ?: "", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { profile?.html_url?.let { uriHandler.openUri(it) } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View on GitHub")
                    }
                }



                // AIRBEATS COMMITS
                if (commits.isNotEmpty()) {
                    item {
                        Text("AirBeats Commits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(commits.take(5)) { commitWrap ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { uriHandler.openUri(commitWrap.html_url) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(commitWrap.commit.message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(commitWrap.commit.author.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // REPOSITORIES
                if (repos.isNotEmpty()) {
                    item {
                        Text("Repositories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(repos) { repo ->
                                Card(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clickable { uriHandler.openUri(repo.html_url) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(repo.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(repo.description ?: "No description", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("⭐ ${repo.stargazers_count}", style = MaterialTheme.typography.labelSmall)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("🍴 ${repo.forks_count}", style = MaterialTheme.typography.labelSmall)
                                            if (repo.language != null) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(repo.language, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // RECENT ACTIVITY
                if (events.isNotEmpty()) {
                    item {
                        Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(events.take(5)) { event ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔹", modifier = Modifier.padding(end = 8.dp))
                            Column {
                                Text(event.type.replace("Event", ""), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(event.repo.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // README
                if (!readme.isNullOrBlank()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Profile README", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                MarkdownText(
                                    markdown = cleanMarkdownReadme(readme!!),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    imageLoader = svgImageLoader,
                                    onLinkClicked = { url ->
                                        try {
                                            uriHandler.openUri(url)
                                        } catch (e: Exception) {
                                            // Handle invalid url gracefully
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
