package com.arflix.tv.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arflix.tv.data.model.Addon
import com.arflix.tv.data.model.CloudStreamExtension

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
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
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onNavigateBack) {
                Text("Back", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        uiState.successMessage?.let { msg ->
            Text(text = msg, color = Color.Green, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
        }
        uiState.error?.let { err ->
            Text(text = err, color = Color.Red, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (uiState.isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                Text("Loading...", color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                StremioAddonsSection(
                    addons = uiState.stremioAddons,
                    onAdd = { url ->
                        viewModel.addStremioAddon(url)
                        viewModel.clearMessage()
                    },
                    onRemove = { url ->
                        viewModel.removeStremioAddon(url)
                        viewModel.clearMessage()
                    }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color.DarkGray
                )
            }

            item {
                CloudStreamSection(
                    extensions = uiState.cloudStreamExtensions,
                    onAdd = { url ->
                        viewModel.addCloudStreamRepository(url)
                        viewModel.clearMessage()
                    },
                    onRemove = { url ->
                        viewModel.removeCloudStreamRepository(url)
                        viewModel.clearMessage()
                    }
                )
            }
        }
    }
}

@Composable
private fun StremioAddonsSection(
    addons: List<Addon>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var addonUrl by remember { mutableStateOf("") }

    Column {
        Text(
            text = "Stremio Addons",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = addonUrl,
                onValueChange = { addonUrl = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter Stremio addon URL...") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (addonUrl.isNotBlank()) {
                        onAdd(addonUrl.trim())
                        addonUrl = ""
                    }
                }
            ) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (addons.isEmpty()) {
            Text("No Stremio addons added", color = Color.Gray, fontSize = 14.sp)
        } else {
            addons.forEach { addon ->
                AddonRow(
                    name = addon.manifest.name,
                    url = addon.url,
                    description = addon.manifest.description,
                    badge = addon.type.name,
                    onRemove = { onRemove(addon.url) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun CloudStreamSection(
    extensions: List<CloudStreamExtension>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var repoUrl by remember { mutableStateOf("") }

    Column {
        Text(
            text = "CloudStream Extensions",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Add CloudStream plugin repositories to enable deep search across additional sources.",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = repoUrl,
                onValueChange = { repoUrl = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter CloudStream repository URL...") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (repoUrl.isNotBlank()) {
                        onAdd(repoUrl.trim())
                        repoUrl = ""
                    }
                }
            ) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (extensions.isEmpty()) {
            Text("No CloudStream repositories added", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tip: Add a repository URL to enable CloudStream deep search",
                color = Color.DarkGray,
                fontSize = 12.sp
            )
        } else {
            extensions.forEach { extension ->
                AddonRow(
                    name = extension.manifest.name,
                    url = extension.repositoryUrl,
                    description = extension.manifest.description
                        ?: "${extension.plugins.size} plugins available",
                    badge = "CLOUDSTREAM",
                    onRemove = { onRemove(extension.repositoryUrl) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun AddonRow(
    name: String,
    url: String,
    description: String?,
    badge: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = badge,
                    color = Color.Cyan,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            if (description != null) {
                Text(text = description, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
            Text(text = url, color = Color.DarkGray, fontSize = 11.sp, maxLines = 1)
        }

        TextButton(onClick = onRemove) {
            Text("Remove", color = Color.Red, fontSize = 13.sp)
        }
    }
}
