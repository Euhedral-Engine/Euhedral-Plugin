package com.euhedral.gemini.architecture.fixtures

import com.intellij.openapi.project.Project

internal interface ForbiddenPortTypeFixture {
    fun getProject(): Project?
}
