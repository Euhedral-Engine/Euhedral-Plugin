package com.euhedral.gemini.core.tools

import com.euhedral.gemini.core.agent.ToolName
import com.euhedral.gemini.core.serialization.SerializableValue

@SerializableValue
enum class ToolParameterType {
    STRING,
    INTEGER,
    BOOLEAN,
}

@SerializableValue
data class ToolParameter(
    val name: String,
    val type: ToolParameterType,
    val required: Boolean,
    val description: String,
    val enumValues: List<String> = emptyList(),
    val minValue: Long? = null,
    val maxValue: Long? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
) {
    init {
        require(name.isNotBlank()) { "Parameter name cannot be blank" }
        require(description.isNotBlank()) { "Parameter description cannot be blank" }
    }
}

@SerializableValue
data class ToolObjectSchema(
    val parameters: List<ToolParameter>,
) {
    init {
        val names = parameters.map { it.name }
        require(names.toSet().size == names.size) { "Parameter names must be unique" }
    }
}

@SerializableValue
data class BoundedOutputPolicy(
    val maxItems: Int = 100,
    val maxCharacters: Int = 50_000,
) {
    init {
        require(maxItems > 0) { "maxItems must be positive: $maxItems" }
        require(maxCharacters > 0) { "maxCharacters must be positive: $maxCharacters" }
    }
}

@SerializableValue
data class ToolDescriptor(
    val name: ToolName,
    val description: String,
    val effect: ToolEffect,
    val schema: ToolObjectSchema,
    val outputPolicy: BoundedOutputPolicy = BoundedOutputPolicy(),
    val independentlyExecutable: Boolean,
) {
    init {
        require(description.isNotBlank()) { "Tool description cannot be blank" }
    }
}

object StandardToolDescriptors {
    private fun p(name: String, type: ToolParameterType, required: Boolean, desc: String) =
        ToolParameter(name = name, type = type, required = required, description = desc)

    val WORKSPACE_CONTEXT = ToolDescriptor(
        name = ToolName("workspace_context"),
        description = "Retrieve workspace layout, module roles, and rules summary",
        effect = ToolEffect.READ_ONLY,
        schema = ToolObjectSchema(emptyList()),
        independentlyExecutable = true,
    )

    val READ_FILE_RANGE = ToolDescriptor(
        name = ToolName("read_file_range"),
        description = "Read a specified range of lines from a workspace file",
        effect = ToolEffect.READ_ONLY,
        schema = ToolObjectSchema(
            listOf(
                p("path", ToolParameterType.STRING, true, "Project-relative file path"),
                p("start_line", ToolParameterType.INTEGER, true, "1-indexed start line"),
                p("end_line", ToolParameterType.INTEGER, true, "1-indexed end line"),
                p("continuation_token", ToolParameterType.STRING, false, "Opaque continuation token"),
            )
        ),
        independentlyExecutable = true,
    )

    val SEARCH_TEXT = ToolDescriptor(
        name = ToolName("search_text"),
        description = "Search text across workspace files",
        effect = ToolEffect.READ_ONLY,
        schema = ToolObjectSchema(
            listOf(
                p("query", ToolParameterType.STRING, true, "Search query text"),
                p("path", ToolParameterType.STRING, false, "Optional path prefix"),
                p("file_glob", ToolParameterType.STRING, false, "Optional file glob pattern"),
                p("continuation_token", ToolParameterType.STRING, false, "Opaque continuation token"),
            )
        ),
        independentlyExecutable = true,
    )

    val FIND_SYMBOL = ToolDescriptor(
        name = ToolName("find_symbol"),
        description = "Find code symbols by name",
        effect = ToolEffect.READ_ONLY,
        schema = ToolObjectSchema(
            listOf(
                p("name", ToolParameterType.STRING, true, "Symbol name or pattern"),
                p("kind", ToolParameterType.STRING, false, "Optional symbol kind"),
                p("scope", ToolParameterType.STRING, false, "Optional scope filter"),
                p("continuation_token", ToolParameterType.STRING, false, "Opaque continuation token"),
            )
        ),
        independentlyExecutable = true,
    )

    val FIND_REFERENCES = ToolDescriptor(
        name = ToolName("find_references"),
        description = "Find usages of a symbol",
        effect = ToolEffect.READ_ONLY,
        schema = ToolObjectSchema(
            listOf(
                p("symbol_id", ToolParameterType.STRING, true, "Opaque symbol identifier"),
                p("continuation_token", ToolParameterType.STRING, false, "Opaque continuation token"),
            )
        ),
        independentlyExecutable = true,
    )

    val FIND_IMPLEMENTATIONS = ToolDescriptor(
        name = ToolName("find_implementations"),
        description = "Find implementations of an interface or abstract symbol",
        effect = ToolEffect.READ_ONLY,
        schema = ToolObjectSchema(
            listOf(
                p("symbol_id", ToolParameterType.STRING, true, "Opaque symbol identifier"),
                p("continuation_token", ToolParameterType.STRING, false, "Opaque continuation token"),
            )
        ),
        independentlyExecutable = true,
    )

    val FILE_METADATA = ToolDescriptor(
        name = ToolName("file_metadata"),
        description = "Retrieve metadata for a workspace path",
        effect = ToolEffect.READ_ONLY,
        schema = ToolObjectSchema(
            listOf(
                p("path", ToolParameterType.STRING, true, "Project-relative file path"),
            )
        ),
        independentlyExecutable = true,
    )

    val REPLACE_TEXT = ToolDescriptor(
        name = ToolName("replace_text"),
        description = "Replace specific text within a workspace file",
        effect = ToolEffect.MUTATING,
        schema = ToolObjectSchema(
            listOf(
                p("path", ToolParameterType.STRING, true, "Project-relative file path"),
                p("old_text", ToolParameterType.STRING, true, "Exact text to replace"),
                p("new_text", ToolParameterType.STRING, true, "Replacement text"),
                p("expected_hash", ToolParameterType.STRING, true, "Expected current file SHA-256 hash"),
            )
        ),
        independentlyExecutable = false,
    )

    val CREATE_FILE = ToolDescriptor(
        name = ToolName("create_file"),
        description = "Create a new file with initial content",
        effect = ToolEffect.MUTATING,
        schema = ToolObjectSchema(
            listOf(
                p("path", ToolParameterType.STRING, true, "Project-relative file path"),
                p("content", ToolParameterType.STRING, true, "Initial file content"),
            )
        ),
        independentlyExecutable = false,
    )

    val DELETE_FILE = ToolDescriptor(
        name = ToolName("delete_file"),
        description = "Delete a workspace file",
        effect = ToolEffect.MUTATING,
        schema = ToolObjectSchema(
            listOf(
                p("path", ToolParameterType.STRING, true, "Project-relative file path"),
                p("expected_hash", ToolParameterType.STRING, true, "Expected current file SHA-256 hash"),
            )
        ),
        independentlyExecutable = false,
    )

    val MOVE_FILE = ToolDescriptor(
        name = ToolName("move_file"),
        description = "Move or rename a workspace file",
        effect = ToolEffect.MUTATING,
        schema = ToolObjectSchema(
            listOf(
                p("source", ToolParameterType.STRING, true, "Current project-relative file path"),
                p("destination", ToolParameterType.STRING, true, "Target project-relative file path"),
                p("expected_hash", ToolParameterType.STRING, true, "Expected current file SHA-256 hash"),
            )
        ),
        independentlyExecutable = false,
    )

    val BUILD_PROJECT = ToolDescriptor(
        name = ToolName("build_project"),
        description = "Build the project and capture diagnostics",
        effect = ToolEffect.PROCESS,
        schema = ToolObjectSchema(emptyList()),
        independentlyExecutable = false,
    )

    val TEST_MODULE = ToolDescriptor(
        name = ToolName("test_module"),
        description = "Run test suite for a module",
        effect = ToolEffect.PROCESS,
        schema = ToolObjectSchema(
            listOf(
                p("module", ToolParameterType.STRING, true, "Module name"),
            )
        ),
        independentlyExecutable = false,
    )

    val TEST_CLASS = ToolDescriptor(
        name = ToolName("test_class"),
        description = "Run tests for a single test class",
        effect = ToolEffect.PROCESS,
        schema = ToolObjectSchema(
            listOf(
                p("module", ToolParameterType.STRING, true, "Module name"),
                p("class_name", ToolParameterType.STRING, true, "Fully qualified class name"),
            )
        ),
        independentlyExecutable = false,
    )

    val TEST_METHOD = ToolDescriptor(
        name = ToolName("test_method"),
        description = "Run a single test method",
        effect = ToolEffect.PROCESS,
        schema = ToolObjectSchema(
            listOf(
                p("module", ToolParameterType.STRING, true, "Module name"),
                p("class_name", ToolParameterType.STRING, true, "Fully qualified class name"),
                p("method_name", ToolParameterType.STRING, true, "Method name"),
            )
        ),
        independentlyExecutable = false,
    )

    val GIT_STATUS = ToolDescriptor(
        name = ToolName("git_status"),
        description = "Query git status for working tree",
        effect = ToolEffect.READ_ONLY,
        schema = ToolObjectSchema(emptyList()),
        independentlyExecutable = true,
    )

    val GIT_DIFF = ToolDescriptor(
        name = ToolName("git_diff"),
        description = "Query git diff for entire workspace",
        effect = ToolEffect.READ_ONLY,
        schema = ToolObjectSchema(
            listOf(
                p("continuation_token", ToolParameterType.STRING, false, "Opaque continuation token"),
            )
        ),
        independentlyExecutable = true,
    )

    val GIT_DIFF_FILE = ToolDescriptor(
        name = ToolName("git_diff_file"),
        description = "Query git diff for a single file",
        effect = ToolEffect.READ_ONLY,
        schema = ToolObjectSchema(
            listOf(
                p("path", ToolParameterType.STRING, true, "Project-relative file path"),
                p("continuation_token", ToolParameterType.STRING, false, "Opaque continuation token"),
            )
        ),
        independentlyExecutable = true,
    )

    val GIT_LOG = ToolDescriptor(
        name = ToolName("git_log"),
        description = "Query commit history",
        effect = ToolEffect.READ_ONLY,
        schema = ToolObjectSchema(
            listOf(
                p("limit", ToolParameterType.INTEGER, true, "Maximum commits to return"),
                p("continuation_token", ToolParameterType.STRING, false, "Opaque continuation token"),
            )
        ),
        independentlyExecutable = true,
    )

    val GIT_BLAME = ToolDescriptor(
        name = ToolName("git_blame"),
        description = "Query blame info for a specific line",
        effect = ToolEffect.READ_ONLY,
        schema = ToolObjectSchema(
            listOf(
                p("path", ToolParameterType.STRING, true, "Project-relative file path"),
                p("line", ToolParameterType.INTEGER, true, "1-indexed line number"),
            )
        ),
        independentlyExecutable = true,
    )

    val COMPLETE_TASK = ToolDescriptor(
        name = ToolName("complete_task"),
        description = "Signal task completion and provide final summary",
        effect = ToolEffect.CONTROL,
        schema = ToolObjectSchema(
            listOf(
                p("summary", ToolParameterType.STRING, true, "Final task completion summary"),
            )
        ),
        independentlyExecutable = false,
    )

    val REQUEST_COMMIT = ToolDescriptor(
        name = ToolName("request_commit"),
        description = "Request commit of current changes",
        effect = ToolEffect.CONTROL,
        schema = ToolObjectSchema(
            listOf(
                p("message", ToolParameterType.STRING, true, "Commit message"),
            )
        ),
        independentlyExecutable = false,
    )

    val ALL: List<ToolDescriptor> = listOf(
        WORKSPACE_CONTEXT, READ_FILE_RANGE, SEARCH_TEXT, FIND_SYMBOL,
        FIND_REFERENCES, FIND_IMPLEMENTATIONS, FILE_METADATA, REPLACE_TEXT,
        CREATE_FILE, DELETE_FILE, MOVE_FILE, BUILD_PROJECT, TEST_MODULE,
        TEST_CLASS, TEST_METHOD, GIT_STATUS, GIT_DIFF, GIT_DIFF_FILE,
        GIT_LOG, GIT_BLAME, COMPLETE_TASK, REQUEST_COMMIT
    )

    val BY_NAME: Map<ToolName, ToolDescriptor> = ALL.associateBy { it.name }
}
