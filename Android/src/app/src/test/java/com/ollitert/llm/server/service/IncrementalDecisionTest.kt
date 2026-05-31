/*
 * Copyright 2025-2026 @NightMean (https://github.com/NightMean)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ollitert.llm.server.service

import com.ollitert.llm.server.runtime.ServerLlmModelHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class IncrementalDecisionTest {

  private val modelName = "test-model"

  @After
  fun cleanup() {
    ServerLlmModelHelper.invalidateCachedTurns(modelName)
  }

  private fun userMsg(text: String) = ChatMessage(role = "user", content = ChatContent(text = text))
  private fun assistantMsg(text: String) = ChatMessage(role = "assistant", content = ChatContent(text = text))
  private fun systemMsg(text: String) = ChatMessage(role = "system", content = ChatContent(text = text))

  @Test
  fun resetWhenNoCache() {
    val decision = decideIncrementalReuse(
      modelName = modelName,
      messages = listOf(userMsg("hi")),
      systemPromptHash = 0,
      toolsHash = 0,
      hasTools = false,
      hasImages = false,
      hasAudio = false,
    )
    assertEquals(IncrementalDecision.Kind.RESET, decision.kind)
    assertEquals("no_cache", decision.reason)
  }

  @Test
  fun resetWhenToolsPresent() {
    ServerLlmModelHelper.updateCachedTurns(
      modelName,
      ServerLlmModelHelper.ConversationCacheEntry(
        turns = listOf(ServerLlmModelHelper.ConversationTurn("user", "hi")),
        systemPromptHash = 0, toolsHash = 0,
      ),
    )
    val decision = decideIncrementalReuse(
      modelName = modelName,
      messages = listOf(userMsg("hi"), assistantMsg("hello"), userMsg("again")),
      systemPromptHash = 0, toolsHash = 0,
      hasTools = true, hasImages = false, hasAudio = false,
    )
    assertEquals(IncrementalDecision.Kind.RESET, decision.kind)
    assertEquals("tools_unsupported", decision.reason)
  }

  @Test
  fun resetWhenMultimodal() {
    val decision = decideIncrementalReuse(
      modelName = modelName,
      messages = listOf(userMsg("hi")),
      systemPromptHash = 0, toolsHash = 0,
      hasTools = false, hasImages = true, hasAudio = false,
    )
    assertEquals("multimodal_unsupported", decision.reason)
  }

  @Test
  fun extendWhenHistoryMatches() {
    ServerLlmModelHelper.updateCachedTurns(
      modelName,
      ServerLlmModelHelper.ConversationCacheEntry(
        turns = listOf(ServerLlmModelHelper.ConversationTurn("user", "hi")),
        systemPromptHash = 42, toolsHash = 0,
      ),
    )
    val decision = decideIncrementalReuse(
      modelName = modelName,
      messages = listOf(userMsg("hi"), assistantMsg("hello"), userMsg("how are you")),
      systemPromptHash = 42, toolsHash = 0,
      hasTools = false, hasImages = false, hasAudio = false,
    )
    assertEquals(IncrementalDecision.Kind.EXTEND, decision.kind)
    assertEquals("history_matches", decision.reason)
    assertEquals("how are you", decision.newUserText)
  }

  @Test
  fun resetWhenSystemPromptChanged() {
    ServerLlmModelHelper.updateCachedTurns(
      modelName,
      ServerLlmModelHelper.ConversationCacheEntry(
        turns = listOf(ServerLlmModelHelper.ConversationTurn("user", "hi")),
        systemPromptHash = 1, toolsHash = 0,
      ),
    )
    val decision = decideIncrementalReuse(
      modelName = modelName,
      messages = listOf(systemMsg("new sys"), userMsg("hi"), assistantMsg("hello"), userMsg("again")),
      systemPromptHash = 999, toolsHash = 0,
      hasTools = false, hasImages = false, hasAudio = false,
    )
    assertEquals("system_prompt_changed", decision.reason)
  }

  @Test
  fun resetWhenUserTurnDiverged() {
    ServerLlmModelHelper.updateCachedTurns(
      modelName,
      ServerLlmModelHelper.ConversationCacheEntry(
        turns = listOf(ServerLlmModelHelper.ConversationTurn("user", "original")),
        systemPromptHash = 0, toolsHash = 0,
      ),
    )
    val decision = decideIncrementalReuse(
      modelName = modelName,
      messages = listOf(userMsg("edited"), assistantMsg("hello"), userMsg("new")),
      systemPromptHash = 0, toolsHash = 0,
      hasTools = false, hasImages = false, hasAudio = false,
    )
    assertEquals("user_turn_diverged_at_0", decision.reason)
  }

  @Test
  fun resetWhenUserTurnCountMismatched() {
    // Cache says 2 user turns; request has only 2 (same as cache, not 3) — can't extend.
    ServerLlmModelHelper.updateCachedTurns(
      modelName,
      ServerLlmModelHelper.ConversationCacheEntry(
        turns = listOf(
          ServerLlmModelHelper.ConversationTurn("user", "a"),
          ServerLlmModelHelper.ConversationTurn("user", "b"),
        ),
        systemPromptHash = 0, toolsHash = 0,
      ),
    )
    val decision = decideIncrementalReuse(
      modelName = modelName,
      messages = listOf(userMsg("a"), userMsg("b")),
      systemPromptHash = 0, toolsHash = 0,
      hasTools = false, hasImages = false, hasAudio = false,
    )
    assertEquals(IncrementalDecision.Kind.RESET, decision.kind)
  }

  @Test
  fun resetWhenLastNotUser() {
    val decision = decideIncrementalReuse(
      modelName = modelName,
      messages = listOf(userMsg("hi"), assistantMsg("dangling")),
      systemPromptHash = 0, toolsHash = 0,
      hasTools = false, hasImages = false, hasAudio = false,
    )
    assertEquals("last_not_user_text", decision.reason)
  }

  @Test
  fun resetWhenLastUserBlank() {
    val decision = decideIncrementalReuse(
      modelName = modelName,
      messages = listOf(userMsg("hi"), assistantMsg("hi"), userMsg("")),
      systemPromptHash = 0, toolsHash = 0,
      hasTools = false, hasImages = false, hasAudio = false,
    )
    assertEquals("last_not_user_text", decision.reason)
  }
}
