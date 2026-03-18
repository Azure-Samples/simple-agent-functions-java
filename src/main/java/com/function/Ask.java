package com.function;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;

/**
 * AI Agent function with two runtime modes:
 *
 *   1. Azure OpenAI (default when AZURE_OPENAI_ENDPOINT is set)
 *      Calls the Azure OpenAI Chat Completions REST API directly.
 *      Auth: API key or managed identity via DefaultAzureCredential.
 *
 *   2. GitHub Copilot SDK (local dev when copilot CLI is in PATH)
 *      Uses CopilotClient which wraps the copilot CLI binary.
 *      Falls back to this when no Azure OpenAI endpoint is configured.
 */
public class Ask {

    private static final String INSTRUCTIONS = """
            1. A robot may not injure a human being...
            2. A robot must obey orders given it by human beings...
            3. A robot must protect its own existence...

            Objective: Give me the TLDR in exactly 5 words.
            """;

    private static final String API_VERSION = "2024-08-01-preview";

    @FunctionName("ask")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = "ask")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        String prompt = request.getBody().orElse("What are the laws?");
        context.getLogger().info("Prompt: " + prompt);

        String endpoint = System.getenv("AZURE_OPENAI_ENDPOINT");

        try {
            String response;

            if (endpoint != null && !endpoint.isEmpty()) {
                context.getLogger().info("Using Azure OpenAI direct mode");
                response = callAzureOpenAI(prompt, context);
            } else {
                context.getLogger().info("Using Copilot SDK mode (requires copilot CLI)");
                response = callCopilotSDK(prompt, context);
            }

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "text/plain")
                    .body(response)
                    .build();

        } catch (Exception e) {
            context.getLogger().severe("Error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "text/plain")
                    .body("Error: " + e.getMessage())
                    .build();
        }
    }

    // --- Azure OpenAI direct REST path (no CLI binary needed) ---

    private String callAzureOpenAI(String prompt, ExecutionContext context) throws IOException {
        String endpoint = System.getenv("AZURE_OPENAI_ENDPOINT");
        String deployment = System.getenv("AZURE_OPENAI_DEPLOYMENT_NAME");
        if (deployment == null || deployment.isEmpty()) {
            deployment = System.getenv("AZURE_OPENAI_MODEL");
        }
        if (deployment == null || deployment.isEmpty()) {
            deployment = "gpt-5-mini";
        }

        // Normalize endpoint: remove trailing slash
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }

        String url = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s",
                endpoint, deployment, API_VERSION);

        // Build JSON payload (no extra JSON library needed)
        String systemMsg = escapeJson(INSTRUCTIONS);
        String userMsg = escapeJson(prompt);
        String payload = String.format("""
                {"messages":[{"role":"system","content":"%s"},{"role":"user","content":"%s"}],"max_completion_tokens":800}""",
                systemMsg, userMsg);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        // Auth: API key or managed identity
        String apiKey = System.getenv("AZURE_OPENAI_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            conn.setRequestProperty("api-key", apiKey);
        } else {
            var credential = new DefaultAzureCredentialBuilder().build();
            var tokenResponse = credential.getTokenSync(
                    new TokenRequestContext().addScopes("https://cognitiveservices.azure.com/.default"));
            conn.setRequestProperty("Authorization", "Bearer " + tokenResponse.getToken());
            context.getLogger().info("Authenticated with managed identity");
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        String body;
        try (var stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
             var scanner = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A")) {
            body = scanner.hasNext() ? scanner.next() : "";
        }

        if (status >= 200 && status < 300) {
            return extractContent(body);
        } else {
            throw new IOException("Azure OpenAI returned " + status + ": " + body);
        }
    }

    // Simple JSON content extractor — avoids Jackson dependency for this small response
    private String extractContent(String json) {
        // Find "content":"..." in the response
        String marker = "\"content\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            marker = "\"content\": \"";
            start = json.indexOf(marker);
        }
        if (start < 0) return json;

        start += marker.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"' -> { sb.append('"'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case 'n' -> { sb.append('\n'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    default -> sb.append(c);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // --- Copilot SDK path (local dev with copilot CLI in PATH) ---

    private String callCopilotSDK(String prompt, ExecutionContext context) throws Exception {
        var client = new com.github.copilot.sdk.CopilotClient();
        try {
            client.start().get();

            var config = new com.github.copilot.sdk.json.SessionConfig()
                    .setSystemMessage(new com.github.copilot.sdk.json.SystemMessageConfig().setContent(INSTRUCTIONS))
                    .setOnPermissionRequest(com.github.copilot.sdk.json.PermissionHandler.APPROVE_ALL);

            var session = client.createSession(config).get();

            var responseText = new java.util.concurrent.atomic.AtomicReference<String>("No response");
            session.on(com.github.copilot.sdk.events.AssistantMessageEvent.class, msg -> {
                var content = msg.getData().content();
                if (content != null && !content.isEmpty()) {
                    responseText.set(content);
                }
            });

            session.sendAndWait(new com.github.copilot.sdk.json.MessageOptions().setPrompt(prompt)).get();
            return responseText.get();
        } finally {
            client.close();
        }
    }
}
