package com.euhedral.gemini.ports

interface WorkspaceEditPort {
    suspend fun openTransaction(request: OpenTransactionRequest): PortResult<TransactionSnapshot>
    suspend fun inspectTransaction(request: InspectTransactionRequest): PortResult<TransactionSnapshot>
    suspend fun replaceText(request: ReplaceTextRequest): PortResult<WorkspaceMutationResult>
    suspend fun createFile(request: CreateFileRequest): PortResult<WorkspaceMutationResult>
    suspend fun deleteFile(request: DeleteFileRequest): PortResult<WorkspaceMutationResult>
    suspend fun moveFile(request: MoveFileRequest): PortResult<WorkspaceMutationResult>
    suspend fun rollbackOperation(request: RollbackOperationRequest): PortResult<RollbackResult>
    suspend fun rollbackTransaction(request: RollbackTransactionRequest): PortResult<RollbackResult>
    suspend fun saveTransactionDocuments(request: SaveTransactionDocumentsRequest): PortResult<SaveDocumentsResult>
}
