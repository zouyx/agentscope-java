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

import io.agentscope.extensions.model.openai.formatter.OpenAIResponseParser;

/**
 * Response parser for OpenCode Zen API.
 *
 * <p>Handles response parsing for the OpenCode Zen API endpoint
 * ({@code https://opencode.ai/zen/v1/chat/completions}).
 */
public class OpenCodeResponseParser extends OpenAIResponseParser {

    public OpenCodeResponseParser() {
        super();
    }
}
