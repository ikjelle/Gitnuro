package com.jetpackduba.gitnuro.preferences

import com.jetpackduba.gitnuro.extensions.defaultWindowPlacement
import com.jetpackduba.gitnuro.theme.ColorsScheme
import com.jetpackduba.gitnuro.theme.Theme
import com.jetpackduba.gitnuro.viewmodels.TextDiffType
import com.jetpackduba.gitnuro.viewmodels.textDiffTypeFromValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.prefs.Preferences
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFERENCES_NAME = "GitnuroConfig"

private const val PREF_LATEST_REPOSITORIES_TABS_OPENED = "latestRepositoriesTabsOpened"
private const val PREF_LATEST_OPENED_TAB_INDEX = "lastOpenedTabIndex"
private const val PREF_LAST_OPENED_REPOSITORIES_PATH = "lastOpenedRepositoriesList"
private const val PREF_THEME = "theme"
private const val PREF_COMMITS_LIMIT = "commitsLimit"
private const val PREF_COMMITS_LIMIT_ENABLED = "commitsLimitEnabled"
private const val PREF_WINDOW_PLACEMENT = "windowsPlacement"
private const val PREF_CUSTOM_THEME = "customTheme"
private const val PREF_UI_SCALE = "ui_scale"
private const val PREF_DIFF_TYPE = "diffType"

private const val PREF_STAGING_LAYOUT_REVERSED = "stagingLayoutReversed"


private const val PREF_GIT_FF_MERGE = "gitFFMerge"

private const val DEFAULT_COMMITS_LIMIT = 1000
private const val DEFAULT_COMMITS_LIMIT_ENABLED = true
const val DEFAULT_UI_SCALE = -1f

@Singleton
class AppSettings @Inject constructor() {
    private val preferences: Preferences = Preferences.userRoot().node(PREFERENCES_NAME)

    private val _themeState = MutableStateFlow(theme)
    val themeState: StateFlow<Theme> = _themeState

    private val _commitsLimitEnabledFlow = MutableStateFlow(commitsLimitEnabled)
    val commitsLimitEnabledFlow: MutableStateFlow<Boolean> = _commitsLimitEnabledFlow

    private val _ffMergeFlow = MutableStateFlow(ffMerge)
    val ffMergeFlow: StateFlow<Boolean> = _ffMergeFlow

    private val _commitsLimitFlow = MutableSharedFlow<Int>()
    val commitsLimitFlow: SharedFlow<Int> = _commitsLimitFlow

    private val _customThemeFlow = MutableStateFlow<ColorsScheme?>(null)
    val customThemeFlow: StateFlow<ColorsScheme?> = _customThemeFlow

    private val _scaleUiFlow = MutableStateFlow(scaleUi)
    val scaleUiFlow: StateFlow<Float> = _scaleUiFlow

    private val _textDiffTypeFlow = MutableStateFlow(textDiffType)
    val textDiffTypeFlow: StateFlow<TextDiffType> = _textDiffTypeFlow

    private val _stagingLayoutReversed = MutableStateFlow(stagingLayoutReversed)
    val stagingLayoutReversedEnabledFlow: MutableStateFlow<Boolean> = _stagingLayoutReversed

    var latestTabsOpened: String
        get() = preferences.get(PREF_LATEST_REPOSITORIES_TABS_OPENED, "")
        set(value) {
            preferences.put(PREF_LATEST_REPOSITORIES_TABS_OPENED, value)
        }
    var latestTabsIndex: Int
        get() = preferences.getInt(PREF_LATEST_OPENED_TAB_INDEX, 0)
        set(value) {
            preferences.putInt(PREF_LATEST_OPENED_TAB_INDEX, value)
    }

    var latestOpenedRepositoriesPath: String
        get() = preferences.get(PREF_LAST_OPENED_REPOSITORIES_PATH, "")
        set(value) {
            preferences.put(PREF_LAST_OPENED_REPOSITORIES_PATH, value)
        }

    var theme: Theme
        get() {
            val lastTheme = preferences.get(PREF_THEME, Theme.DARK.toString())
            return try {
                Theme.valueOf(lastTheme)
            } catch (ex: Exception) {
                ex.printStackTrace()
                Theme.DARK
            }
        }
        set(value) {
            preferences.put(PREF_THEME, value.toString())
            _themeState.value = value
        }

    var commitsLimitEnabled: Boolean
        get() {
            return preferences.getBoolean(PREF_COMMITS_LIMIT_ENABLED, DEFAULT_COMMITS_LIMIT_ENABLED)
        }
        set(value) {
            preferences.putBoolean(PREF_COMMITS_LIMIT_ENABLED, value)
            _commitsLimitEnabledFlow.value = value
        }

    var scaleUi: Float
        get() {
            return preferences.getFloat(PREF_UI_SCALE, DEFAULT_UI_SCALE)
        }
        set(value) {
            preferences.putFloat(PREF_UI_SCALE, value)
            _scaleUiFlow.value = value
        }

    /**
     * Property that decides if the merge should fast-forward when possible
     */
    var ffMerge: Boolean
        get() {
            return preferences.getBoolean(PREF_GIT_FF_MERGE, true)
        }
        set(value) {
            preferences.putBoolean(PREF_GIT_FF_MERGE, value)
            _ffMergeFlow.value = value
        }

    val commitsLimit: Int
        get() {
            return preferences.getInt(PREF_COMMITS_LIMIT, DEFAULT_COMMITS_LIMIT)
        }

    var stagingLayoutReversed: Boolean
        get() {
            return preferences.getBoolean(PREF_STAGING_LAYOUT_REVERSED, false)
        }
        set(value) {
            preferences.putBoolean(PREF_STAGING_LAYOUT_REVERSED, value)
            _stagingLayoutReversed.value = value
        }

    suspend fun setCommitsLimit(value: Int) {
        preferences.putInt(PREF_COMMITS_LIMIT, value)
        _commitsLimitFlow.emit(value)
    }

    var windowPlacement: WindowsPlacementPreference
        get() {
            val placement = preferences.getInt(PREF_WINDOW_PLACEMENT, defaultWindowPlacement.value)

            return WindowsPlacementPreference(placement)
        }
        set(placement) {
            preferences.putInt(PREF_WINDOW_PLACEMENT, placement.value)
        }

    var textDiffType: TextDiffType
        get() {
            val diffTypeValue = preferences.getInt(PREF_DIFF_TYPE, TextDiffType.UNIFIED.value)

            return textDiffTypeFromValue(diffTypeValue)
        }
        set(placement) {
            preferences.putInt(PREF_DIFF_TYPE, placement.value)

            _textDiffTypeFlow.value = textDiffType
        }

    fun saveCustomTheme(filePath: String) {
        try {
            val file = File(filePath)
            val content = file.readText()

            Json.decodeFromString<ColorsScheme>(content) // Load to see if it's valid (it will crash if not)

            preferences.put(PREF_CUSTOM_THEME, content)
            loadCustomTheme()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun loadCustomTheme() {
        val themeJson = preferences.get(PREF_CUSTOM_THEME, null)
        if (themeJson != null) {
            _customThemeFlow.value = Json.decodeFromString<ColorsScheme>(themeJson)
        }
    }
}