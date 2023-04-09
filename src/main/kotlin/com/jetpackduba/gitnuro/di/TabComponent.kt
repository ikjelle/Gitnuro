package com.jetpackduba.gitnuro.di

import com.jetpackduba.gitnuro.di.modules.NetworkModule
import com.jetpackduba.gitnuro.di.modules.TabModule
import com.jetpackduba.gitnuro.di.modules.TerminalModule
import com.jetpackduba.gitnuro.ui.components.TabInformation
import dagger.Component

@TabScope
@Component(
    modules = [
        NetworkModule::class,
        TabModule::class,
        TerminalModule::class,
    ],
    dependencies = [
        AppComponent::class
    ],
)
interface TabComponent {
    fun inject(tabInformation: TabInformation)
}