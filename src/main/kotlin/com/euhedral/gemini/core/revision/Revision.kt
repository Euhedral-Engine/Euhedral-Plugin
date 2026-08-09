package com.euhedral.gemini.core.revision

import com.euhedral.gemini.core.agent.Sha256Digest
import com.euhedral.gemini.core.agent.TransactionId
import com.euhedral.gemini.core.agent.TransactionRevision
import com.euhedral.gemini.core.agent.VerificationRunId
import com.euhedral.gemini.core.serialization.SerializableValue

@SerializableValue
data class TransactionRevisionDigest(
    val transactionId: TransactionId,
    val revision: TransactionRevision,
    val digest: Sha256Digest,
)

@SerializableValue
data class VerifiedTransactionDigest(
    val transaction: TransactionRevisionDigest,
    val verificationRunId: VerificationRunId,
)
