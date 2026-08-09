package com.euhedral.gemini.ports

import com.euhedral.gemini.core.agent.CheckpointBlobId
import com.euhedral.gemini.core.agent.ProjectFingerprint
import com.euhedral.gemini.core.agent.TransactionId

interface CheckpointStore {
    suspend fun initialize(request: InitializeCheckpointRequest): PortResult<CheckpointManifest>
    suspend fun persistBeforeMutation(request: PersistPendingMutationRequest): PortResult<CheckpointReceipt>
    suspend fun recordMutation(request: RecordAppliedMutationRequest): PortResult<CheckpointManifest>
    suspend fun load(transactionId: TransactionId): PortResult<CheckpointManifest?>
    suspend fun readBlob(blobId: CheckpointBlobId): PortResult<CheckpointBlob>
    suspend fun scanRecoverable(projectFingerprint: ProjectFingerprint): PortResult<List<RecoverableCheckpoint>>
    suspend fun setStatus(request: SetCheckpointStatusRequest): PortResult<CheckpointManifest>
    suspend fun discard(transactionId: TransactionId): PortResult<Unit>
    suspend fun prune(request: PruneCheckpointsRequest): PortResult<PruneCheckpointsResult>
}
