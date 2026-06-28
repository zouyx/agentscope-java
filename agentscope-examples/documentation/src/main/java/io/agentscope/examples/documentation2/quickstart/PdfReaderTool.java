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

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * A simple tool that extracts text content from PDF files.
 * Registered in {@link BasicChatExample} to demonstrate agent tool calling.
 */
public class PdfReaderTool {

    @Tool(name = "read_pdf", description = "Extract text content from a PDF file. "
            + "Useful for reading PDF documents and extracting their text content.")
    public String readPdf(
            @ToolParam(name = "file_path", description = "Absolute path to the PDF file")
            String filePath,
            @ToolParam(name = "page_start", description = "Start page (0-indexed, default: 0)",
                    required = false)
            Integer pageStart,
            @ToolParam(name = "page_end", description = "End page (exclusive, default: all pages)",
                    required = false)
            Integer pageEnd) {
        Path path = Path.of(filePath);
        if (!path.toFile().exists()) {
            return "Error: File not found: " + filePath;
        }
        if (!filePath.toLowerCase().endsWith(".pdf")) {
            return "Error: Not a PDF file: " + filePath;
        }

        try (PDDocument doc = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int start = pageStart != null ? Math.max(1, pageStart + 1) : 1;
            int end = pageEnd != null ? Math.min(pageEnd, doc.getNumberOfPages())
                    : doc.getNumberOfPages();
            if (start > end) {
                return "Error: pageStart (" + start + ") > pageEnd (" + end + ")";
            }
            stripper.setStartPage(start);
            stripper.setEndPage(end);
            String text = stripper.getText(doc);

            StringBuilder sb = new StringBuilder();
            sb.append("PDF: ").append(path.getFileName()).append("\n");
            sb.append("Total pages: ").append(doc.getNumberOfPages()).append("\n");
            sb.append("Extracted pages: ").append(start).append(" - ").append(end).append("\n\n");
            sb.append(text.trim());
            return sb.toString();
        } catch (IOException e) {
            return "Error reading PDF: " + e.getMessage();
        }
    }
}
