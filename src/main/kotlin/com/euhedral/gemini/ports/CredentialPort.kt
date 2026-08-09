package com.euhedral.gemini.ports

interface CredentialPort {
    suspend fun status(reference: CredentialReference): PortResult<CredentialStatus>
    suspend fun acquire(request: CredentialRequest): PortResult<CredentialLease>
    suspend fun store(request: StoreCredentialRequest): PortResult<CredentialStatus>
    suspend fun remove(request: RemoveCredentialRequest): PortResult<CredentialStatus>
    suspend fun revoke(request: RevokeCredentialRequest): PortResult<CredentialRevokeResult>
}
