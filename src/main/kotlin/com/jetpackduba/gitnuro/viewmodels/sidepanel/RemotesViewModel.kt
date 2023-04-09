package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.exceptions.InvalidRemoteUrlException
import com.jetpackduba.gitnuro.extensions.lowercaseContains
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.DeleteLocallyRemoteBranchesUseCase
import com.jetpackduba.gitnuro.git.branches.GetRemoteBranchesUseCase
import com.jetpackduba.gitnuro.git.remote_operations.DeleteRemoteBranchUseCase
import com.jetpackduba.gitnuro.git.remotes.*
import com.jetpackduba.gitnuro.ui.dialogs.RemoteWrapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RemoteSetUrlCommand
import org.eclipse.jgit.lib.Ref

class RemotesViewModel @AssistedInject constructor(
    private val tabState: TabState,
    private val deleteRemoteBranchUseCase: DeleteRemoteBranchUseCase,
    private val getRemoteBranchesUseCase: GetRemoteBranchesUseCase,
    private val getRemotesUseCase: GetRemotesUseCase,
    private val deleteRemoteUseCase: DeleteRemoteUseCase,
    private val addRemoteUseCase: AddRemoteUseCase,
    private val updateRemoteUseCase: UpdateRemoteUseCase,
    private val deleteLocallyRemoteBranchesUseCase: DeleteLocallyRemoteBranchesUseCase,
    private val tabScope: CoroutineScope,
    @Assisted
    private val filter: StateFlow<String>
) : SidePanelChildViewModel(false) {
    private val remotes = MutableStateFlow<List<RemoteView>>(listOf())

    val remoteState: StateFlow<RemotesState> = combine(remotes, isExpanded, filter) { remotes, isExpanded, filter ->
        val remotesFiltered = remotes.map { remote ->
            val remoteInfo = remote.remoteInfo

            val newRemoteInfo = remoteInfo.copy(
                branchesList = remoteInfo.branchesList.filter { branch ->
                    branch.simpleName.lowercaseContains(filter)
                }
            )

            remote.copy(remoteInfo = newRemoteInfo)
        }

        RemotesState(
            remotesFiltered,
            isExpanded
        )
    }.stateIn(
        scope = tabScope,
        started = SharingStarted.Eagerly,
        initialValue = RemotesState(emptyList(), isExpanded.value)
    )

    init {
        tabScope.launch {
            tabState.refreshFlowFiltered(RefreshType.ALL_DATA, RefreshType.REMOTES) {
                refresh(tabState.git)
            }
        }
    }

    private suspend fun loadRemotes(git: Git) = withContext(Dispatchers.IO) {
        val remotes = git.remoteList()
            .call()
        val allRemoteBranches = getRemoteBranchesUseCase(git)

        val remoteInfoList = remotes.map { remoteConfig ->
            val remoteBranches = allRemoteBranches.filter { branch ->
                branch.name.startsWith("refs/remotes/${remoteConfig.name}")
            }
            RemoteInfo(remoteConfig, remoteBranches)
        }

        val remoteViewList = remoteInfoList.map { remoteInfo ->
            RemoteView(remoteInfo, true)
        }

        this@RemotesViewModel.remotes.value = remoteViewList
    }

    fun deleteRemoteBranch(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        deleteRemoteBranchUseCase(git, ref)
    }

    suspend fun refresh(git: Git) = withContext(Dispatchers.IO) {
        loadRemotes(git)
    }

    fun onRemoteClicked(remoteClicked: RemoteView) {
        val remoteName = remoteClicked.remoteInfo.remoteConfig.name
        val remotes = this.remotes.value
        val remoteInfo = remotes.firstOrNull { it.remoteInfo.remoteConfig.name == remoteName }

        if (remoteInfo != null) {
            val newRemoteInfo = remoteInfo.copy(isExpanded = !remoteClicked.isExpanded)
            val newRemotesList = remotes.toMutableList()
            val indexToReplace = newRemotesList.indexOf(remoteInfo)
            newRemotesList[indexToReplace] = newRemoteInfo

            this.remotes.value = newRemotesList
        }
    }

    fun selectBranch(ref: Ref) {
        tabState.newSelectedRef(ref.objectId)
    }

    fun deleteRemote(remoteName: String, isNew: Boolean) = tabState.safeProcessing(
        refreshType = if (isNew) RefreshType.REMOTES else RefreshType.ALL_DATA,
        showError = true,
    ) { git ->
        deleteRemoteUseCase(git, remoteName)

        val remoteBranches = getRemoteBranchesUseCase(git)
        val remoteToDeleteBranchesNames = remoteBranches.filter {
            it.name.startsWith("refs/remotes/$remoteName/")
        }.map {
            it.name
        }

        deleteLocallyRemoteBranchesUseCase(git, remoteToDeleteBranchesNames)
    }


    fun addRemote(selectedRemoteConfig: RemoteWrapper) = tabState.runOperation(
        refreshType = RefreshType.REMOTES,
        showError = true,
    ) { git ->
        if (selectedRemoteConfig.fetchUri.isBlank()) {
            throw InvalidRemoteUrlException("Invalid empty fetch URI")
        }

        if (selectedRemoteConfig.pushUri.isBlank()) {
            throw InvalidRemoteUrlException("Invalid empty push URI")
        }

        addRemoteUseCase(git, selectedRemoteConfig.remoteName, selectedRemoteConfig.fetchUri)

        updateRemote(selectedRemoteConfig) // Sets both, fetch and push uri
    }

    fun updateRemote(selectedRemoteConfig: RemoteWrapper) = tabState.runOperation(
        refreshType = RefreshType.REMOTES,
        showError = true,
    ) { git ->

        if (selectedRemoteConfig.fetchUri.isBlank()) {
            throw InvalidRemoteUrlException("Invalid empty fetch URI")
        }

        if (selectedRemoteConfig.pushUri.isBlank()) {
            throw InvalidRemoteUrlException("Invalid empty push URI")
        }

        updateRemoteUseCase(
            git = git,
            remoteName = selectedRemoteConfig.remoteName,
            uri = selectedRemoteConfig.fetchUri,
            uriType = RemoteSetUrlCommand.UriType.FETCH
        )

        updateRemoteUseCase(
            git = git,
            remoteName = selectedRemoteConfig.remoteName,
            uri = selectedRemoteConfig.pushUri,
            uriType = RemoteSetUrlCommand.UriType.PUSH
        )
    }
}

data class RemoteView(val remoteInfo: RemoteInfo, val isExpanded: Boolean)

data class RemotesState(val remotes: List<RemoteView>, val isExpanded: Boolean)