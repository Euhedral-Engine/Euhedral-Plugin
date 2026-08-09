package com.euhedral.gemini.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

internal class GeminiSettingsConfigurable : Configurable {
    private var mainPanel: JComponent? = null

    override fun getDisplayName(): String = "Euhedral Gemini Agent"

    override fun createComponent(): JComponent {
        val panel = panel {
            row {
                label("Configuration will be added in a later phase.")
            }
        }
        mainPanel = panel
        return panel
    }

    override fun isModified(): Boolean = false

    override fun apply() {}

    override fun reset() {}

    override fun disposeUIResources() {
        mainPanel = null
    }
}
