package com.euhedral.gemini.ports

import com.euhedral.gemini.core.agent.ContentHash
import com.euhedral.gemini.core.agent.ContinuationToken
import com.euhedral.gemini.core.agent.ProjectPath
import com.euhedral.gemini.core.agent.SessionId
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.serialization.SourceBearingValue
import com.euhedral.gemini.core.tools.BoundedOutputMetadata

@SerializableValue
@JvmInline
value class SymbolId(val value: String) {
    init {
        require(value.isNotBlank()) { "SymbolId cannot be blank" }
    }
}

@SerializableValue
data class WorkspaceContextRequest(
    val sessionId: SessionId,
)

@SerializableValue
data class ModuleInfo(
    val id: String,
    val role: String,
    val confidence: Double,
    val sourceSets: List<String> = emptyList(),
    val sdkName: String? = null,
    val languageLevel: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Module id cannot be blank" }
        require(role.isNotBlank()) { "Module role cannot be blank" }
        require(confidence in 0.0..1.0) { "Confidence must be between 0.0 and 1.0: $confidence" }
    }
}

@SerializableValue
data class WorkspaceContext(
    val projectIdentity: String,
    val gradleIdentity: String? = null,
    val modules: List<ModuleInfo> = emptyList(),
    val activeFiles: List<FileMetadataResult> = emptyList(),
    val rulesSummary: String = "",
) {
    init {
        require(projectIdentity.isNotBlank()) { "projectIdentity cannot be blank" }
    }
}

@SerializableValue
data class ReadFileRangeRequest(
    val path: ProjectPath,
    val startLine: Int,
    val endLine: Int,
    val continuationToken: ContinuationToken? = null,
) {
    init {
        require(startLine >= 1) { "startLine must be >= 1" }
        require(endLine >= startLine) { "endLine must be >= startLine" }
    }
}

@SourceBearingValue
data class FileRangeResult(
    val path: ProjectPath,
    val startLine: Int,
    val endLine: Int,
    val content: String,
    val contentHash: ContentHash,
    val boundedMetadata: BoundedOutputMetadata,
) {
    init {
        require(startLine >= 1) { "startLine must be >= 1" }
        require(endLine >= startLine) { "endLine must be >= startLine" }
    }
}

@SerializableValue
data class SearchTextRequest(
    val query: String,
    val path: ProjectPath? = null,
    val fileGlob: String? = null,
    val continuationToken: ContinuationToken? = null,
) {
    init {
        require(query.isNotBlank()) { "Search query cannot be blank" }
    }
}

@SourceBearingValue
data class TextMatch(
    val path: ProjectPath,
    val line: Int,
    val lineContent: String,
) {
    init {
        require(line >= 1) { "line must be >= 1" }
    }
}

@SourceBearingValue
data class TextSearchResult(
    val matches: List<TextMatch>,
    val boundedMetadata: BoundedOutputMetadata,
)

@SerializableValue
data class FindSymbolRequest(
    val name: String,
    val kind: String? = null,
    val scope: String? = null,
    val continuationToken: ContinuationToken? = null,
) {
    init {
        require(name.isNotBlank()) { "Symbol name query cannot be blank" }
    }
}

@SerializableValue
data class SymbolInfo(
    val id: SymbolId,
    val name: String,
    val kind: String,
    val path: ProjectPath,
    val line: Int,
) {
    init {
        require(name.isNotBlank()) { "Symbol name cannot be blank" }
        require(kind.isNotBlank()) { "Symbol kind cannot be blank" }
        require(line >= 1) { "line must be >= 1" }
    }
}

@SerializableValue
data class SymbolSearchResult(
    val symbols: List<SymbolInfo>,
    val boundedMetadata: BoundedOutputMetadata,
)

@SerializableValue
data class FindReferencesRequest(
    val symbolId: SymbolId,
    val continuationToken: ContinuationToken? = null,
)

@SerializableValue
data class FindImplementationsRequest(
    val symbolId: SymbolId,
    val continuationToken: ContinuationToken? = null,
)

@SerializableValue
data class SymbolLocation(
    val path: ProjectPath,
    val line: Int,
) {
    init {
        require(line >= 1) { "line must be >= 1" }
    }
}

@SerializableValue
data class SymbolLocationsResult(
    val locations: List<SymbolLocation>,
    val boundedMetadata: BoundedOutputMetadata,
)

@SerializableValue
data class FileMetadataRequest(
    val path: ProjectPath,
)

@SerializableValue
data class FileMetadataResult(
    val path: ProjectPath,
    val exists: Boolean,
    val sizeBytes: Long,
    val contentHash: ContentHash? = null,
    val isWritable: Boolean = true,
    val languageId: String? = null,
) {
    init {
        require(sizeBytes >= 0L) { "sizeBytes must be non-negative" }
    }
}
