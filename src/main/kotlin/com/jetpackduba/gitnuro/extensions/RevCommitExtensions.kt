package com.jetpackduba.gitnuro.extensions

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit

fun RevCommit.fullData(repository: Repository): RevCommit? {
    return if (this.tree == null)
        repository.parseCommit(this)
    else
        this
}

val RevCommit.shortName: String
    get() = this.name().orEmpty().take(7)


fun RevCommit.getShortMessageTrimmed(): String {
    return (this.fullMessage ?: "")
        .trimStart()
        .replace("\r\n", "\n")
        .takeWhile { it != '\n' }
}
