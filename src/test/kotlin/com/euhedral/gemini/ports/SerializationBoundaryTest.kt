package com.euhedral.gemini.ports

import com.euhedral.gemini.core.agent.AgentSession
import com.euhedral.gemini.core.agent.AgentEventEnvelope
import com.euhedral.gemini.core.agent.AgentStateEventEnvelope
import com.euhedral.gemini.core.agent.MutatingCallLedger
import com.euhedral.gemini.core.agent.SessionRecoveryMetadata
import com.euhedral.gemini.core.error.AgentError
import com.euhedral.gemini.core.serialization.AdapterLocalValue
import com.euhedral.gemini.core.serialization.SerializableValue
import com.euhedral.gemini.core.serialization.SourceBearingValue
import com.euhedral.gemini.core.tools.ToolDescriptor
import com.euhedral.gemini.core.tools.ToolCall
import com.euhedral.gemini.core.tools.ToolResult
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializationBoundaryTest {

    @Test
    fun everyCoreAndPortValueHasExactlyOneBoundaryClassification() {
        val classes = listOf(
            SessionRecoveryMetadata::class.java,
            AgentSession::class.java,
            SecretValue::class.java,
            CredentialLease::class.java,
            CheckpointBlob::class.java,
        )

        for (cls in classes) {
            val annotations = listOfNotNull(
                cls.getAnnotation(SerializableValue::class.java),
                cls.getAnnotation(SourceBearingValue::class.java),
                cls.getAnnotation(AdapterLocalValue::class.java),
            )
            assertEquals("Class ${cls.simpleName} must have exactly one boundary annotation", 1, annotations.size)
        }
    }

    @Test
    fun serializableGraphsContainOnlyAllowedStructuralTypes() {
        val roots = listOf(
            SessionRecoveryMetadata::class.java,
            AgentStateEventEnvelope::class.java,
            AgentEventEnvelope::class.java,
            MutatingCallLedger::class.java,
            ToolCall::class.java,
            ToolResult::class.java,
            ToolDescriptor::class.java,
            ApprovalRequest::class.java,
            WorkspaceContext::class.java,
            FileRangeResult::class.java,
            BuildResult::class.java,
            GitStatusResult::class.java,
        )
        roots.forEach(::assertPortableGraph)
    }

    @Test
    fun sourceBearingGraphsRemainStructurallySerializable() {
        assertPortableGraph(AgentSession::class.java)
    }

    @Test
    fun serializableGraphsNeverReachAdapterLocalValues() {
        assertPortableGraph(SessionRecoveryMetadata::class.java)
    }

    @Test
    fun agentSessionContainsNoSecretOrRuntimeHandle() {
        val fields = AgentSession::class.java.declaredFields.map { it.type }
        assertTrue(fields.none { SecretValue::class.java.isAssignableFrom(it) })
        assertTrue(fields.none { AutoCloseable::class.java.isAssignableFrom(it) })
    }

    @Test
    fun agentEventsContainNoSecretThrowableOrNativeType() {
        val fields = com.euhedral.gemini.core.agent.AgentEventPayload::class.java.declaredFields.map { it.type }
        assertTrue(fields.none { Throwable::class.java.isAssignableFrom(it) })
        assertTrue(fields.none { SecretValue::class.java.isAssignableFrom(it) })
    }

    @Test
    fun toolDescriptorsContainNoExecutorOrPort() {
        val fields = ToolDescriptor::class.java.declaredFields.map { it.type }
        assertTrue(fields.none { it.name.contains("Port") })
        assertTrue(fields.none { it.name.contains("Executor") })
    }

    @Test
    fun portRequestsAndResultsContainNoPlatformWireOrFilesystemType() {
        val forbiddenTypes = listOf("com.intellij", "org.jetbrains", "com.google", "org.gradle", "java.io.File", "java.nio.file.Path")
        val classes = listOf(
            WorkspaceContext::class.java,
            FileRangeResult::class.java,
            BuildResult::class.java,
            GitStatusResult::class.java,
        )
        for (cls in classes) {
            for (field in cls.declaredFields) {
                val typeName = field.type.name
                for (forbidden in forbiddenTypes) {
                    assertFalse("Field ${field.name} of ${cls.simpleName} contains forbidden type $forbidden", typeName.startsWith(forbidden))
                }
            }
        }
    }

    @Test
    fun credentialLeaseAndSinksAreAdapterLocal() {
        assertTrue(CredentialLease::class.java.isAnnotationPresent(AdapterLocalValue::class.java))
        assertTrue(SecretValue::class.java.isAnnotationPresent(AdapterLocalValue::class.java))
        assertTrue(InteractionStreamSink::class.java.isAnnotationPresent(AdapterLocalValue::class.java))
        assertTrue(ProcessOutputSink::class.java.isAnnotationPresent(AdapterLocalValue::class.java))
    }

    @Test
    fun checkpointContentIsSourceBearingAndAbsentFromSessionMetadata() {
        assertTrue(CheckpointBlob::class.java.isAnnotationPresent(SourceBearingValue::class.java))
        val fields = SessionRecoveryMetadata::class.java.declaredFields.map { it.type }
        assertTrue(fields.none { CheckpointBlob::class.java.isAssignableFrom(it) })
    }

    private fun assertPortableGraph(root: Class<*>) {
        val visited = mutableSetOf<Class<*>>()
        fun visit(type: Type) {
            val raw = when (type) {
                is Class<*> -> type
                is ParameterizedType -> type.rawType as? Class<*> ?: return
                else -> return
            }
            if (!visited.add(raw) || raw.isPrimitive || raw.isEnum || raw.name.startsWith("java.") || raw.name.endsWith("\$Companion")) return
            assertFalse("${raw.name} must not be adapter-local in a portable graph", raw.isAnnotationPresent(AdapterLocalValue::class.java))
            if (raw.name.startsWith("com.euhedral.gemini")) {
                assertTrue(
                    "${raw.name} must declare a serialization boundary",
                    raw.isAnnotationPresent(SerializableValue::class.java) || raw.isAnnotationPresent(SourceBearingValue::class.java),
                )
            }
            raw.declaredFields.filterNot { it.isSynthetic }.forEach { field ->
                visit(field.genericType)
                if (field.genericType is ParameterizedType) {
                    (field.genericType as ParameterizedType).actualTypeArguments.forEach(::visit)
                }
            }
        }
        visit(root)
    }
}
