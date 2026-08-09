package com.euhedral.gemini.ports

interface WorkspaceInspectionPort {
    suspend fun workspaceContext(request: WorkspaceContextRequest): PortResult<WorkspaceContext>
    suspend fun readFileRange(request: ReadFileRangeRequest): PortResult<FileRangeResult>
    suspend fun searchText(request: SearchTextRequest): PortResult<TextSearchResult>
    suspend fun findSymbol(request: FindSymbolRequest): PortResult<SymbolSearchResult>
    suspend fun findReferences(request: FindReferencesRequest): PortResult<SymbolLocationsResult>
    suspend fun findImplementations(request: FindImplementationsRequest): PortResult<SymbolLocationsResult>
    suspend fun fileMetadata(request: FileMetadataRequest): PortResult<FileMetadataResult>
}
