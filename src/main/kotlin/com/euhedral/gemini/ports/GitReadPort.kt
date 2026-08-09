package com.euhedral.gemini.ports

interface GitReadPort {
    suspend fun status(request: GitStatusRequest): PortResult<GitStatusResult>
    suspend fun diff(request: GitDiffRequest): PortResult<GitDiffResult>
    suspend fun diffFile(request: GitDiffFileRequest): PortResult<GitDiffResult>
    suspend fun log(request: GitLogRequest): PortResult<GitLogResult>
    suspend fun blame(request: GitBlameRequest): PortResult<GitBlameResult>
}
