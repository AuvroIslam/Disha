package com.example.gemmachat.inference

import com.example.gemmachat.core.LlmEngine
import kotlinx.coroutines.runBlocking

/**
 * Adapts the on-device Gemma 4 [EngineHolder] to the core [LlmEngine] interface used by the
 * Disha reasoning engines (Triage, Rag, Gis, Summary, Compress). One shared engine instance.
 *
 * `generate` is synchronous per the core interface; callers invoke it from a background
 * coroutine (e.g. Dispatchers.Default), so the brief `runBlocking` bridge is safe here.
 */
class GemmaLlmEngine(private val holder: EngineHolder) : LlmEngine {
    override val modelName: String = "gemma-4-e2b"

    override fun generate(system: String, user: String, temperature: Double, maxTokens: Int): String =
        runBlocking { holder.generateWith(system, user, temperature) }
}
