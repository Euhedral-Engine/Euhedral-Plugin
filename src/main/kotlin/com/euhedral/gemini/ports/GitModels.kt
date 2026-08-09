package com.euhedral.gemini.ports

import com.euhedral.gemini.core.agent.ContinuationToken
import com.euhedral.gemini.core.agent.ProjectPath
import com.euhedral.gemini.core.agent.SessionId
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.serialization.SourceBearingValue
import com.euhedral.gemini.core.tools.BoundedOutputMetadata

@SerializableValue
enum class FileStatus {
    MODIFIED,
    ADDED,
    DELETED,
    RENAMED,
    UNTRACKED,
}

@SerializableValue
data class GitFileStatus(
    val path: ProjectPath,
    val status: FileStatus,
)

@SerializableValue
data class GitStatusRequest(
    val sessionId: SessionId,
)

@SerializableValue
data class GitStatusResult(
    val statuses: List<GitFileStatus>,
    val branch: String? = null,
)

@SerializableValue
data class GitDiffRequest(
    val continuationToken: ContinuationToken? = null,
)

@SerializableValue
data class GitDiffFileRequest(
    val path: ProjectPath,
    val continuationToken: ContinuationToken? = null,
)

@SourceBearingValue
data class GitDiffResult(
    val diffText: String,
    val boundedMetadata: BoundedOutputMetadata,
)

@SerializableValue
data class GitLogRequest(
    val limit: Int,
    val continuationToken: ContinuationToken? = null,
) {
    init {
        require(limit > 0) { "limit must be positive: $limit" }
    }
}

@SerializableValue
data class GitCommitSummary(
    val hash: String,
    val author: String,
    val dateEpochMillis: Long,
    val message: String,
) {
    init {
        require(hash.isNotBlank()) { "hash cannot be blank" }
        require(author.isNotBlank()) { "author cannot be blank" }
        require(dateEpochMillis >= 0L) { "dateEpochMillis must be non-negative" }
    }
}

@SerializableValue
data class GitLogResult(
    val commits: List<GitCommitSummary>,
    val boundedMetadata: BoundedOutputMetadata,
)

@SerializableValue
data class GitBlameRequest(
    val path: ProjectPath,
    val line: Int,
) {
    init {
        require(line >= 1) { "line must be >= 1" }
    }
}

@SerializableValue
data class GitBlameLine(
    val line: Int,
    val commitHash: String,
    val author: String,
    val dateEpochMillis: Long,
) {
    init {
        require(line >= 1) { "line must be >= 1" }
        require(commitHash.isNotBlank()) { "commitHash cannot be blank" }
        require(author.isNotBlank()) { "author cannot be blank" }
        require(dateEpochMillis >= 0L) { "dateEpochMillis must be non-negative" }
    }
}

@SerializableValue
data class GitBlameResult(
    val line: GitBlameLine,
)
