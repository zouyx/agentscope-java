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
package io.agentscope.examples.documentation2.quickstart;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BasicChatExample - The simplest Agent conversation example.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Creating an agent with {@code model("dashscope:qwen-plus")} (ModelRegistry auto-resolves
 *       the provider and reads API key from env)</li>
 *   <li>Interactive streaming chat via {@code streamEvents()}</li>
 *   <li>Incremental text output using {@link TextBlockDeltaEvent}</li>
 * </ul>
 *
 * <p><b>Run:</b>
 * <pre>
 *   export DASHSCOPE_API_KEY=your_key
 *   mvn exec:java -pl agentscope-examples/documentation \
 *       -Dexec.mainClass=io.agentscope.examples.documentation2.quickstart.BasicChatExample
 * </pre>
 */
public class BasicChatExample {

    private static final Pattern AT_PATH = Pattern.compile("@(?<path>~?[\\w./\\\\\\-~]+)");

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Error: DASHSCOPE_API_KEY environment variable not set.");
            System.err.println("Get your API key from: https://dashscope.aliyun.com");
            System.err.println("Then set it with: export DASHSCOPE_API_KEY=your_api_key");
            System.exit(1);
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("Basic Chat Example");
        System.out.println("=".repeat(60));
        System.out.println("Use @path/to/file to upload files for the AI to read.");
        System.out.println("Type 'exit' to quit.\n");

        //        Toolkit toolkit = new Toolkit();
        //        toolkit.registerTool(new PdfReaderTool());

        ReActAgent agent =
                ReActAgent.builder()
                        .name("Assistant")
                        .sysPrompt("You are a helpful AI assistant. Be friendly and concise.")
                        .model(opencodeModel())
                        //                        .toolkit(toolkit)
                        .build();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("You: ");
            String input = reader.readLine();

            if (input == null || input.trim().equalsIgnoreCase("exit")) {
                System.out.println("\nGoodbye!");
                break;
            }
            if (input.isBlank()) {
                continue;
            }

            //            String processed = expandAtPaths(input);
            Msg userMsg = new UserMessage(input);

            System.out.print("\nAssistant: ");
            agent.streamEvents(userMsg)
                    .doOnNext(
                            event -> {
                                if (event instanceof TextBlockDeltaEvent e) {
                                    System.out.print(e.getDelta());
                                }
                            })
                    .blockLast();
            System.out.println("\n");
        }
    }

    private static String expandAtPaths(String input) {
        Matcher m = AT_PATH.matcher(input);
        if (!m.find()) {
            return input;
        }
        m.reset();
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            String ref = m.group("path");
            sb.append(input, last, m.start());
            String content = readFile(ref);
            if (content != null) {
                sb.append(ref)
                        .append("\n\n<attached_file path=\"")
                        .append(ref)
                        .append("\">\n")
                        .append(content)
                        .append("\n</attached_file>");
            } else {
                sb.append('@').append(ref);
            }
            last = m.end();
        }
        sb.append(input.substring(last));
        return sb.toString();
    }

    private static String readFile(String path) {
        try {
            Path p =
                    Path.of(
                            path.startsWith("~/")
                                    ? System.getProperty("user.home") + path.substring(1)
                                    : path);
            if (!Files.exists(p)) {
                return null;
            }
            if (path.toLowerCase().endsWith(".pdf")) {
                return new PdfReaderTool().readPdf(p.toAbsolutePath().toString(), null, null);
            }
            String text = Files.readString(p);
            return text.length() > 100_000
                    ? text.substring(0, 100_000) + "\n... [truncated]"
                    : text;
        } catch (Exception e) {
            return null;
        }
    }

    private static Model model() {
        return OllamaChatModel.builder().modelName("deepseek-r1:1.5b").build();
    }

    private static Model opencodeModel() {
        return OpenAIChatModel.builder().baseUrl("https://opencode.ai/zen/v1").stream(true)
                .modelName("deepseek-v4-flash-free")
                .formatter(new OpenAIChatFormatter())
                .build();
    }

    private static Model kModel() {
        Model model = opencodeModel();

        model.stream(
                        List.of(new UserMessage("Count from 1 to 5.")),
                        /* tools= */ List.of(),
                        GenerateOptions.builder().build())
                .doOnNext(chunk -> System.out.println("Delta: " + chunk.getContent()))
                .doOnComplete(() -> System.out.println("Stream completed"))
                .blockLast();
        return model;
    }
}
