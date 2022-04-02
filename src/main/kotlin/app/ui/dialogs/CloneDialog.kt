package app.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.git.CloneStatus
import app.theme.primaryTextColor
import app.viewmodels.CloneViewModel
import openDirectoryDialog
import java.io.File

@Composable
fun CloneDialog(
    cloneViewModel: CloneViewModel,
    onClose: () -> Unit,
    onOpenRepository: (File) -> Unit,
) {
    val cloneStatus = cloneViewModel.cloneStatus.collectAsState()
    val cloneStatusValue = cloneStatus.value

    MaterialDialog {
        Box(
            modifier = Modifier
                .width(400.dp)
                .animateContentSize()
        ) {
            when (cloneStatusValue) {
                CloneStatus.CheckingOut -> {
                    Cloning(cloneViewModel)
                }
                is CloneStatus.Cloning -> {
                    Cloning(cloneViewModel)
                }
                is CloneStatus.Cancelling -> {
                    onClose()
                }
                is CloneStatus.Completed -> {
                    onOpenRepository(cloneStatusValue.repoDir)
                    onClose()
                }
                is CloneStatus.Fail -> CloneInput(
                    cloneViewModel = cloneViewModel,
                    onClose = onClose,
                    errorMessage = cloneStatusValue.reason
                )
                CloneStatus.None -> CloneInput(
                    cloneViewModel = cloneViewModel,
                    onClose = onClose,
                )
            }
        }
    }
}

@Composable
private fun CloneInput(
    cloneViewModel: CloneViewModel,
    onClose: () -> Unit,
    errorMessage: String? = null,
) {
    var url by remember { mutableStateOf(cloneViewModel.url) }
    var directory by remember { mutableStateOf(cloneViewModel.directory) }
    var errorHasBeenNoticed by remember { mutableStateOf(false) }

    val urlFocusRequester = remember { FocusRequester() }
    val directoryFocusRequester = remember { FocusRequester() }
    val directoryButtonFocusRequester = remember { FocusRequester() }
    val cloneButtonFocusRequester = remember { FocusRequester() }
    val cancelButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        urlFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Clone a new repository",
            color = MaterialTheme.colors.primaryTextColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .focusOrder(urlFocusRequester) {
                    previous = cancelButtonFocusRequester
                    next = directoryFocusRequester
                },
            label = { Text("URL") },
            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colors.primaryTextColor),
            maxLines = 1,
            value = url,
            onValueChange = {
                errorHasBeenNoticed = true
                url = it
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
                    .focusOrder(directoryFocusRequester) {
                        previous = urlFocusRequester
                        next = directoryButtonFocusRequester
                    },
                textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colors.primaryTextColor),
                maxLines = 1,
                label = { Text("Directory") },
                value = directory,
                onValueChange = {
                    errorHasBeenNoticed = true
                    directory = it
                }
            )

            IconButton(
                onClick = {
                    errorHasBeenNoticed = true
                    val newDirectory = openDirectoryDialog()
                    if (newDirectory != null)
                        directory = newDirectory
                },
                modifier = Modifier
                    .focusOrder(directoryButtonFocusRequester) {
                        previous = directoryFocusRequester
                        next = cloneButtonFocusRequester
                    }
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primaryTextColor,
                )
            }
        }

        AnimatedVisibility(errorMessage != null && !errorHasBeenNoticed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colors.error)
            ) {
                Text(
                    errorMessage.orEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    color = MaterialTheme.colors.onError,
                )
            }

        }

        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.End)
        ) {
            TextButton(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .focusOrder(cancelButtonFocusRequester) {
                        previous = cloneButtonFocusRequester
                        next = urlFocusRequester
                    },
                onClick = {
                    onClose()
                }
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    cloneViewModel.clone(directory, url)
                },
                modifier = Modifier
                    .focusOrder(cloneButtonFocusRequester) {
                        previous = directoryButtonFocusRequester
                        next = cancelButtonFocusRequester
                    }
            ) {
                Text("Clone")
            }
        }
    }
}

@Composable
private fun Cloning(cloneViewModel: CloneViewModel) {
    Column (
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))

        TextButton(
            modifier = Modifier
                .padding(
                    top = 36.dp,
                    end = 8.dp
                )
                .align(Alignment.End),
            onClick =  {
                cloneViewModel.cancelClone()
            }
        ) {
            Text("Cancel")
        }
    }
}

@Composable
private fun Cancelling() {
    Column (
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Text(
            text = "Cancelling clone operation...",
            color = MaterialTheme.colors.primaryTextColor,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}