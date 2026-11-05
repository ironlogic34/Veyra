package com.ironlogic.veyra

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ironlogic.veyra.data.video.MediaStoreVideoDataSource
import com.ironlogic.veyra.data.video.SafVideoFolderDataSource
import com.ironlogic.veyra.data.video.VideoItem
import com.ironlogic.veyra.permissions.VideoAccessLevel
import com.ironlogic.veyra.permissions.VideoAccessPermission
import com.ironlogic.veyra.ui.theme.VeyraTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VeyraTheme {
                var accessLevel by remember { mutableStateOf(VideoAccessPermission.evaluate(this)) }
                var folderUris by remember { mutableStateOf(readPersistedFolderUris()) }
                var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
                var status by remember { mutableStateOf("Choose how Veyra can access your videos.") }
                var isLoading by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    accessLevel = VideoAccessPermission.evaluate(this)
                    status = when (accessLevel) {
                        VideoAccessLevel.FULL_LIBRARY -> "Full video-library access granted."
                        VideoAccessLevel.SELECTED_VIDEOS -> "Access is limited to the videos you selected."
                        VideoAccessLevel.NONE -> "Video permission was not granted. You can still add a folder."
                    }
                }

                val folderPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    if (uri != null) {
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            folderUris = readPersistedFolderUris()
                            status = "Folder access saved. Refresh to scan its videos."
                        } catch (_: SecurityException) {
                            status = "Android could not save access to that folder. Select it again."
                        }
                    }
                }

                fun refreshLibrary() {
                    scope.launch {
                        isLoading = true
                        val found = linkedMapOf<String, VideoItem>()
                        val errors = mutableListOf<String>()

                        if (accessLevel != VideoAccessLevel.NONE) {
                            try {
                                MediaStoreVideoDataSource(contentResolver).getVideos()
                                    .forEach { found[it.contentUri.toString()] = it }
                            } catch (_: SecurityException) {
                                errors += "Device-library permission changed."
                                accessLevel = VideoAccessPermission.evaluate(this@MainActivity)
                            } catch (_: Exception) {
                                errors += "Could not read the device video library."
                            }
                        }

                        folderUris.forEach { folderUri ->
                            try {
                                SafVideoFolderDataSource(contentResolver).getVideos(folderUri)
                                    .forEach { found[it.contentUri.toString()] = it }
                            } catch (_: SecurityException) {
                                errors += "A saved folder permission was revoked."
                            } catch (_: Exception) {
                                errors += "A selected folder could not be scanned."
                            }
                        }

                        videos = found.values.sortedWith(
                            compareByDescending<VideoItem> { it.dateModifiedSeconds }
                                .thenBy { it.displayName.lowercase() }
                        )
                        status = when {
                            errors.isNotEmpty() -> errors.distinct().joinToString(" ")
                            videos.isEmpty() -> "No videos found in the accessible locations."
                            else -> "Found ${videos.size} video(s)."
                        }
                        isLoading = false
                    }
                }

                LaunchedEffect(accessLevel, folderUris) { refreshLibrary() }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Veyra · Video access", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            when (accessLevel) {
                                VideoAccessLevel.FULL_LIBRARY -> "Device library: full access"
                                VideoAccessLevel.SELECTED_VIDEOS -> "Device library: selected videos only"
                                VideoAccessLevel.NONE -> "Device library: permission not granted"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                permissionLauncher.launch(VideoAccessPermission.requiredPermissions())
                            }) {
                                Text(if (accessLevel == VideoAccessLevel.NONE) "Grant video access" else "Manage access")
                            }
                            OutlinedButton(onClick = { folderPicker.launch(null) }) { Text("Add folder") }
                        }
                        Text("Saved folders: ${folderUris.size}")
                        folderUris.forEach { folderUri ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    folderUri.lastPathSegment ?: "Selected folder",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedButton(onClick = {
                                    try {
                                        contentResolver.releasePersistableUriPermission(
                                            folderUri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    } catch (_: SecurityException) {
                                        // The provider or user may already have revoked the grant.
                                    }
                                    folderUris = readPersistedFolderUris()
                                }) { Text("Remove") }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                accessLevel = VideoAccessPermission.evaluate(this@MainActivity)
                                folderUris = readPersistedFolderUris()
                                refreshLibrary()
                            }) { Text("Refresh") }
                            if (isLoading) Text("Scanning…")
                        }
                        Text(status, style = MaterialTheme.typography.bodySmall)
                        Text("Videos (${videos.size})", style = MaterialTheme.typography.titleMedium)
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(videos, key = { it.contentUri.toString() }) { video ->
                                Column {
                                    Text(video.displayName, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        video.relativePath ?: "Device library",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun readPersistedFolderUris(): List<Uri> =
        contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { it.uri }
            .distinct()
}
