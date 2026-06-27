/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.extensions.model.opencode;

import io.agentscope.extensions.model.openai.formatter.DeepSeekFormatter;

/**
 * Formatter for OpenCode Zen API models.
 *
 * <p>OpenCode Zen hosts multiple model families (DeepSeek, GPT, Claude, Gemini, etc.)
 * through OpenAI-compatible endpoints. This formatter extends {@link DeepSeekFormatter}
 * to inherit the DeepSeek-specific message fixes (e.g., no {@code name} field,
 * system-to-user conversion), which are commonly needed by models served on OpenCode Zen.
 */
public class OpenCodeFormatter extends DeepSeekFormatter {

    public OpenCodeFormatter() {
        super();
    }
}
