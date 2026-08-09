package com.euhedral.gemini.core.policy

import com.euhedral.gemini.core.serialization.SerializableValue

@SerializableValue
enum class DecisionType {
    ALLOW,
    DENY,
    REQUIRE_APPROVAL,
}

@SerializableValue
data class PolicyDecision(
    val type: DecisionType,
    val reasonCode: String,
    val explanation: String,
    val findings: List<ValidationFinding> = emptyList(),
) {
    init {
        require(reasonCode.isNotBlank()) { "reasonCode cannot be blank" }
        require(explanation.isNotBlank()) { "explanation cannot be blank" }
    }
}
