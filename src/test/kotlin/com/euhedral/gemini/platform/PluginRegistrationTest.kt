package com.euhedral.gemini.platform

import com.euhedral.gemini.bootstrap.GeminiProjectService
import com.euhedral.gemini.settings.GeminiSettingsConfigurable
import com.euhedral.gemini.ui.AgentToolWindowFactory
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.wm.ToolWindowEP
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import javax.swing.JComponent

class PluginRegistrationTest : BasePlatformTestCase() {

    fun testPluginDescriptorLoadedAndEnabled() {
        val pluginId = PluginId.getId("com.euhedral.gemini")
        val pluginDescriptor = PluginManagerCore.getPlugin(pluginId)
        assertNotNull("Plugin descriptor should be loaded", pluginDescriptor)
        assertFalse("Plugin should not be disabled", PluginManagerCore.isDisabled(pluginId))
    }

    fun testProjectServiceResolution() {
        val service1 = project.getService(GeminiProjectService::class.java)
        assertNotNull("Project service should be registered and resolved", service1)
        val service2 = project.getService(GeminiProjectService::class.java)
        assertSame("Project service should be a stable singleton instance for project", service1, service2)
        assertFalse("Service should not be disposed initially", service1.isDisposed)
    }

    fun testToolWindowRegistrationAndInstantiation() {
        val ep = ToolWindowEP.EP_NAME.extensions.firstOrNull { it.id == "Euhedral Gemini" }
        assertNotNull("ToolWindow extension point with id 'Euhedral Gemini' should be registered", ep)
        assertEquals("Factory class name should match", AgentToolWindowFactory::class.java.name, ep!!.factoryClass)
        val factory = ep.getToolWindowFactory(ep.pluginDescriptor)
        assertNotNull("ToolWindowFactory should instantiate", factory)
        assertTrue("Factory should be instance of AgentToolWindowFactory", factory is AgentToolWindowFactory)
    }

    fun testSettingsConfigurableRegistrationAndInstantiation() {
        val configurableEP = Configurable.APPLICATION_CONFIGURABLE.extensionList.firstOrNull {
            it.id == "com.euhedral.gemini.settings"
        }
        assertNotNull("Application configurable extension with id 'com.euhedral.gemini.settings' should be registered", configurableEP)
        assertEquals("Configurable instance class name should match", GeminiSettingsConfigurable::class.java.name, configurableEP!!.instanceClass)
        val configurable = configurableEP.createConfigurable()
        assertNotNull("Configurable instance should be created", configurable)
        assertTrue("Configurable should be instance of GeminiSettingsConfigurable", configurable is GeminiSettingsConfigurable)
        val component: JComponent? = configurable!!.createComponent()
        assertNotNull("Configurable createComponent should return a component", component)
        configurable.disposeUIResources()
    }
}
