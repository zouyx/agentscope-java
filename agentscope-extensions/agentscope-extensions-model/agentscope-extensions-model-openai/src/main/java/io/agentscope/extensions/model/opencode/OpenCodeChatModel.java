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

import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.ModelUtils;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.model.transport.HttpTransport;
import io.agentscope.core.model.transport.HttpTransportConfig;
import io.agentscope.core.model.transport.HttpTransportFactory;
import io.agentscope.core.model.transport.OkHttpTransport;
import io.agentscope.core.model.transport.ProxyConfig;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.OpenAIClient;
import io.agentscope.extensions.model.openai.dto.OpenAIMessage;
import io.agentscope.extensions.model.openai.dto.OpenAIRequest;
import io.agentscope.extensions.model.openai.dto.OpenAIResponse;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chat Model for OpenCode Zen API.
 *
 * <p>Pre-configured with sensible defaults for the OpenCode Zen API
 * ({@code https://opencode.ai/zen/v1}). Uses {@link OpenCodeFormatter} for message formatting
 * and {@link OpenCodeResponseParser} for response parsing.
 *
 * <p>Usage:
 * <pre>{@code
 * OpenCodeChatModel model = OpenCodeChatModel.builder()
 *     .apiKey("your-opencode-zen-api-key")
 *     .modelName("deepseek-v4-flash-free")
 *     .build();
 * }</pre>
 */
public class OpenCodeChatModel extends OpenAIChatModel {

    private static final Logger log = LoggerFactory.getLogger(OpenCodeChatModel.class);

    /** Default OpenCode Zen API base URL. */
    private static final String DEFAULT_BASE_URL = "https://opencode.ai/zen/v1";

    private final OpenAIClient client;
    private final Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest> formatter;
    private final GenerateOptions configuredOptions;

    /**
     * Creates a new OpenCode chat model instance.
     *
     * @param client            the HTTP client
     * @param formatter         the message formatter
     * @param configuredOptions the pre-configured options
     */
    protected OpenCodeChatModel(
            OpenAIClient client,
            Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest> formatter,
            GenerateOptions configuredOptions) {
        super(client, formatter, configuredOptions);
        this.client = client;
        this.formatter = formatter;
        this.configuredOptions = configuredOptions;
    }

    @Override
    public boolean supportsNativeStructuredOutput() {
        return true;
    }

    @Override
    protected void customizeRequest(
            OpenAIRequest request, List<ToolSchema> tools, GenerateOptions options) {
        // OpenCode Zen providers may return structured tool call output even when no tools
        // are configured. Explicitly suppress tool calls to prevent unsolicited output.
        if (tools == null || tools.isEmpty()) {
            request.setToolChoice("none");
        }
    }

    /**
     * Creates a new builder for OpenCodeChatModel.
     *
     * @return a new Builder instance
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builder for OpenCodeChatModel.
     *
     * <p>Pre-configured with:
     * <ul>
     *   <li>Default base URL: {@code https://opencode.ai/zen/v1}</li>
     *   <li>Default formatter: {@link OpenCodeFormatter}</li>
     * </ul>
     */
    public static class Builder {
        private String apiKey;
        private String modelName;
        private boolean stream = true;
        private GenerateOptions defaultOptions;
        private String baseUrl = DEFAULT_BASE_URL;
        private String endpointPath;
        private Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest> formatter =
                new OpenCodeFormatter();
        private HttpTransport httpTransport;
        private ProxyConfig proxyConfig;
        private int contextWindowSize = -1;
        private Boolean nativeStructuredOutputWithTools;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder generateOptions(GenerateOptions options) {
            this.defaultOptions = options;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder endpointPath(String endpointPath) {
            this.endpointPath = endpointPath;
            return this;
        }

        public Builder formatter(
                Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest> formatter) {
            this.formatter = formatter;
            return this;
        }

        public Builder httpTransport(HttpTransport httpTransport) {
            this.httpTransport = httpTransport;
            return this;
        }

        public Builder proxy(ProxyConfig proxyConfig) {
            this.proxyConfig = proxyConfig;
            return this;
        }

        public Builder contextWindowSize(int contextWindowSize) {
            this.contextWindowSize = contextWindowSize;
            return this;
        }

        public Builder nativeStructuredOutputWithTools(boolean nativeStructuredOutputWithTools) {
            this.nativeStructuredOutputWithTools = nativeStructuredOutputWithTools;
            return this;
        }

        /**
         * Builds the OpenCodeChatModel instance.
         *
         * @return configured OpenCodeChatModel instance
         * @throws IllegalArgumentException if modelName is not set
         */
        public OpenCodeChatModel build() {
            Objects.requireNonNull(modelName, "modelName must be set");

            GenerateOptions.Builder optionsBuilder =
                    GenerateOptions.builder()
                            .apiKey(apiKey)
                            .baseUrl(baseUrl)
                            .modelName(modelName)
                            .stream(stream);

            if (endpointPath != null) {
                optionsBuilder.endpointPath(endpointPath);
            }

            GenerateOptions builderOptions = optionsBuilder.build();
            GenerateOptions mergedOptions =
                    GenerateOptions.mergeOptions(builderOptions, defaultOptions);
            GenerateOptions effectiveOptions =
                    ModelUtils.ensureDefaultExecutionConfig(mergedOptions);

            HttpTransport transport = resolveTransport();
            OpenAIClient client = new OpenAIClient(transport);
            Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest> fmt =
                    formatter != null ? formatter : new OpenCodeFormatter();

            OpenCodeChatModel model = new OpenCodeChatModel(client, fmt, effectiveOptions);
            model.setContextWindowSize(
                    contextWindowSize >= 0
                            ? contextWindowSize
                            : io.agentscope.core.model.ModelContextWindows.lookup(
                                    modelName,
                                    io.agentscope.core.model.ModelContextWindows.OPENAI));
            if (nativeStructuredOutputWithTools != null) {
                model.setNativeStructuredOutputWithTools(nativeStructuredOutputWithTools);
            }
            return model;
        }

        private HttpTransport resolveTransport() {
            if (httpTransport != null) {
                if (proxyConfig != null) {
                    log.warn(
                            "OpenCodeChatModel: both proxy() and httpTransport() are set. "
                                    + "httpTransport() takes precedence, proxy() is ignored.");
                }
                return httpTransport;
            }
            if (proxyConfig != null) {
                return OkHttpTransport.builder()
                        .config(HttpTransportConfig.builder().proxy(proxyConfig).build())
                        .build();
            }
            return HttpTransportFactory.getDefault();
        }
    }
}
