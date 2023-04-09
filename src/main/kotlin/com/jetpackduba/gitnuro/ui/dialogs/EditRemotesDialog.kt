package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.theme.backgroundSelected
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.theme.outlinedTextFieldColors
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.viewmodels.sidepanel.RemotesViewModel
import org.eclipse.jgit.transport.RemoteConfig

@Composable
fun EditRemotesDialog(
    remotesViewModel: RemotesViewModel,
    onDismiss: () -> Unit,
) {
    var remotesEditorData by remember {
        mutableStateOf(
            RemotesEditorData(
                emptyList(),
                null,
            )
        )
    }

    val remotesState by remotesViewModel.remoteState.collectAsState()
    val remotes = remotesState.remotes
    var remoteChanged by remember { mutableStateOf(false) }
    val selectedRemote = remotesEditorData.selectedRemote


    LaunchedEffect(remotes) {
        val newRemotesWrappers = remotes.map {
            val remoteConfig = it.remoteInfo.remoteConfig
            remoteConfig.toRemoteWrapper()
        }

        val safeSelectedRemote = remotesEditorData.selectedRemote
        var newSelectedRemote: RemoteWrapper? = null

        if (safeSelectedRemote != null) {
            newSelectedRemote = newRemotesWrappers.firstOrNull { it.remoteName == safeSelectedRemote.remoteName }
        }

        remoteChanged = newSelectedRemote?.haveUrisChanged ?: false

        remotesEditorData = remotesEditorData.copy(
            listRemotes = newRemotesWrappers,
            selectedRemote = newSelectedRemote,
        )
    }

    MaterialDialog(
        paddingVertical = 8.dp,
        paddingHorizontal = 16.dp,
        background = MaterialTheme.colors.surface,
        onCloseRequested = onDismiss
    ) {
        Column(
            modifier = Modifier
                .size(width = 900.dp, height = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Remotes",
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.h3,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .handOnHover()
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onBackground,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colors.background),
                ) {
                    if (remotesEditorData.listRemotes.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(remotesEditorData.listRemotes) { remote ->
                                val background = if (remote == selectedRemote) {
                                    MaterialTheme.colors.backgroundSelected
                                } else
                                    MaterialTheme.colors.background

                                Text(
                                    text = remote.remoteName,
                                    color = MaterialTheme.colors.onBackground,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .handMouseClickable {
                                            remotesEditorData = remotesEditorData.copy(selectedRemote = remote)
                                        }
                                        .background(background)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No available remotes",
                                style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onBackgroundSecondary)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        IconButton(
                            modifier = Modifier
                                .size(36.dp)
                                .handOnHover(),
                            onClick = {
                                val remotesWithNew = remotesEditorData.listRemotes.toMutableList()
                                val newRemote = RemoteWrapper(
                                    remoteName = "new_remote",
                                    fetchUri = "",
                                    originalFetchUri = "",
                                    pushUri = "",
                                    originalPushUri = "",
                                    isNew = true
                                )

                                remotesWithNew.add(newRemote)

                                remotesEditorData = remotesEditorData.copy(
                                    listRemotes = remotesWithNew,
                                    selectedRemote = newRemote
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(AppIcons.ADD),
                                contentDescription = null,
                                tint = MaterialTheme.colors.onBackground,
                            )
                        }
                        IconButton(
                            modifier = Modifier
                                .size(36.dp)
                                .handOnHover(),
                            enabled = selectedRemote != null,
                            onClick = {
                                if (selectedRemote != null)
                                    remotesViewModel.deleteRemote(selectedRemote.remoteName, selectedRemote.isNew)
                            }
                        ) {
                            Icon(
                                painter = painterResource(AppIcons.REMOVE),
                                contentDescription = null,
                                tint = if (selectedRemote != null)
                                    MaterialTheme.colors.onBackground
                                else
                                    MaterialTheme.colors.onBackgroundSecondary,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    if (selectedRemote != null) {
                        Column {
                            if (selectedRemote.isNew) {
                                Text(
                                    text = "New remote name",
                                    color = MaterialTheme.colors.onBackground,
                                    modifier = Modifier.padding(top = 8.dp),
                                )

                                AdjustableOutlinedTextField(
                                    value = selectedRemote.remoteName,
                                    onValueChange = { newValue ->
                                        val newSelectedRemoteConfig = selectedRemote.copy(remoteName = newValue)
                                        val listRemotes = remotesEditorData.listRemotes.toMutableList()
                                        val newRemoteToBeReplacedIndex = listRemotes.indexOfFirst { it.isNew }
                                        listRemotes[newRemoteToBeReplacedIndex] = newSelectedRemoteConfig

                                        remotesEditorData = remotesEditorData.copy(
                                            listRemotes = listRemotes,
                                            selectedRemote = newSelectedRemoteConfig
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                            }

                            Text(
                                text = "Fetch URL",
                                color = MaterialTheme.colors.onBackground,
                                modifier = Modifier.padding(top = 8.dp),
                            )

                            AdjustableOutlinedTextField(
                                value = selectedRemote.fetchUri,
                                onValueChange = { newValue ->
                                    val newSelectedRemoteConfig = selectedRemote.copy(fetchUri = newValue)
                                    remotesEditorData = remotesEditorData.copy(selectedRemote = newSelectedRemoteConfig)
                                    remoteChanged = newSelectedRemoteConfig.haveUrisChanged
                                },
                                singleLine = true,
                                colors = outlinedTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )

                            Text(
                                text = "Push URL",
                                color = MaterialTheme.colors.onBackground,
                                modifier = Modifier.padding(top = 8.dp),
                            )

                            AdjustableOutlinedTextField(
                                value = selectedRemote.pushUri,
                                onValueChange = { newValue ->
                                    val newSelectedRemoteConfig = selectedRemote.copy(pushUri = newValue)
                                    remotesEditorData = remotesEditorData.copy(selectedRemote = newSelectedRemoteConfig)
                                    remoteChanged = newSelectedRemoteConfig.haveUrisChanged
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Row(
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Spacer(modifier = Modifier.weight(1f))

                                if (!selectedRemote.isNew) {
                                    PrimaryButton(
                                        text = "Discard changes",
                                        modifier = Modifier.padding(end = 8.dp),
                                        onClick = {
                                            remotesEditorData = remotesEditorData.copy(
                                                selectedRemote = selectedRemote.copy(
                                                    fetchUri = selectedRemote.originalFetchUri,
                                                    pushUri = selectedRemote.originalPushUri,
                                                )
                                            )

                                            remoteChanged = false
                                        },
                                        backgroundColor = Color.Transparent,
                                        backgroundDisabled = Color.Transparent,
                                        enabled = remoteChanged,
                                        textColor = MaterialTheme.colors.onBackground,
                                        disabledTextColor = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
                                    )
                                }

                                val text = if (selectedRemote.isNew)
                                    "Add new remote"
                                else
                                    "Save ${selectedRemote.remoteName} changes"
                                PrimaryButton(
                                    modifier = Modifier,
                                    enabled = remoteChanged,
                                    onClick = {
                                        if (selectedRemote.isNew)
                                            remotesViewModel.addRemote(selectedRemote)
                                        else
                                            remotesViewModel.updateRemote(selectedRemote)
                                    },
                                    text = text,
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

data class RemoteWrapper(
    val remoteName: String,
    val fetchUri: String,
    val originalFetchUri: String,
    val pushUri: String,
    val originalPushUri: String,
    val isNew: Boolean = false,
) {
    val haveUrisChanged: Boolean = isNew ||
            fetchUri != originalFetchUri ||
            pushUri != originalPushUri
}


data class RemotesEditorData(
    val listRemotes: List<RemoteWrapper>,
    val selectedRemote: RemoteWrapper?,
)

fun RemoteConfig.toRemoteWrapper(): RemoteWrapper {
    val fetchUri = this.urIs.firstOrNull()
    val pushUri = this.pushURIs.firstOrNull()
        ?: this.urIs.firstOrNull() // If push URI == null, take fetch URI

    return RemoteWrapper(
        remoteName = this.name,
        fetchUri = fetchUri?.toString().orEmpty(),
        originalFetchUri = fetchUri?.toString().orEmpty(),
        pushUri = pushUri?.toString().orEmpty(),
        originalPushUri = pushUri?.toString().orEmpty(),
    )
}