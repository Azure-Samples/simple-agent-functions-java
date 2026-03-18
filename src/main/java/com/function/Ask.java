package com.function;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.json.AzureOptions;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.ProviderConfig;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SystemMessageConfig;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class Ask {

    private static final String INSTRUCTIONS = """
            1. A robot may not injure a human being...
            2. A robot must obey orders given it by human beings...
            3. A robot must protect its own existence...

            Objective: Give me the TLDR in exactly 5 words.
            """;

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

        try (var client = new CopilotClient()) {
            client.start().get();

            var sessionConfig = getSessionConfig();
            var session = client.createSession(sessionConfig).get();

            var responseText = new AtomicReference<String>("No response");

            session.on(AssistantMessageEvent.class, msg -> {
                var content = msg.getData().content();
                if (content != null && !content.isEmpty()) {
                    responseText.set(content);
                }
            });

            session.sendAndWait(new MessageOptions().setPrompt(prompt)).get();

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "text/plain")
                    .body(responseText.get())
                    .build();

        } catch (Exception e) {
            context.getLogger().severe("Error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "text/plain")
                    .body("Error: " + e.getMessage())
                    .build();
        }
    }

    private SessionConfig getSessionConfig() {
        var config = new SessionConfig()
                .setSystemMessage(new SystemMessageConfig().setContent(INSTRUCTIONS))
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);

        String baseUrl = System.getenv("AZURE_OPENAI_ENDPOINT");
        String apiKey = System.getenv("AZURE_OPENAI_API_KEY");
        String model = System.getenv("AZURE_OPENAI_DEPLOYMENT_NAME");
        if (model == null || model.isEmpty()) {
            model = System.getenv("AZURE_OPENAI_MODEL");
        }
        if (model == null || model.isEmpty()) {
            model = "gpt-5-mini";
        }

        if (baseUrl != null && !baseUrl.isEmpty()) {
            config.setModel(model);

            var provider = new ProviderConfig()
                    .setType("azure")
                    .setBaseUrl(baseUrl);

            if (apiKey != null && !apiKey.isEmpty()) {
                provider.setApiKey(apiKey);
            } else {
                var credential = new DefaultAzureCredentialBuilder().build();
                var tokenResponse = credential.getTokenSync(
                        new com.azure.core.credential.TokenRequestContext()
                                .addScopes("https://cognitiveservices.azure.com/.default"));
                provider.setBearerToken(tokenResponse.getToken());
            }

            config.setProvider(provider);
        }

        return config;
    }
}
