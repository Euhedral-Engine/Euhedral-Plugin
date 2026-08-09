package com.euhedral.gemini.ports

interface BuildSystemPort {
    suspend fun buildProject(request: BuildProjectRequest, sink: ProcessOutputSink): PortResult<BuildResult>
    suspend fun testModule(request: TestModuleRequest, sink: ProcessOutputSink): PortResult<BuildResult>
    suspend fun testClass(request: TestClassRequest, sink: ProcessOutputSink): PortResult<BuildResult>
    suspend fun testMethod(request: TestMethodRequest, sink: ProcessOutputSink): PortResult<BuildResult>
}
