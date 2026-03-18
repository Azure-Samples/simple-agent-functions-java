# Simple Agent QuickStart (Java Copilot SDK)

A simple AI agent built with the GitHub Copilot SDK for Java, running as an Azure Function.

> Looking for [TypeScript](https://github.com/Azure-Samples/simple-agent-functions-typescript), [Python](https://github.com/Azure-Samples/simple-agent-functions-python), or [C#](https://github.com/Azure-Samples/simple-agent-functions-dotnet)?

## Prerequisites

- [Java 17+](https://learn.microsoft.com/en-us/java/openjdk/download)
- [Maven 3.8+](https://maven.apache.org/download.cgi)
- [Azure Functions Core Tools](https://learn.microsoft.com/en-us/azure/azure-functions/functions-run-local#install-the-azure-functions-core-tools)
- [Azure Developer CLI (azd)](https://aka.ms/azd-install) (only needed for deploying Microsoft Foundry resources)
- Access to an AI model via one of:
  - **GitHub Copilot subscription** — models are available automatically
  - **Bring Your Own Key (BYOK)** — use an API key from [Microsoft Foundry](https://ai.azure.com) (see [BYOK docs](https://github.com/github/copilot-sdk/blob/main/docs/auth/byok.md))

## Quickstart

> **Want to use your own models?** See [Deploy Microsoft Foundry Resources](#deploy-microsoft-foundry-resources) below to provision a Microsoft Foundry project instead of using GitHub Copilot models.

1. Clone the repository

2. Build the project:

   ```bash
   mvn clean package -DskipTests
   ```

3. Run the function locally:

   ```bash
   func start
   ```

4. Test the agent (in a new terminal):

   ```bash
   # Interactive chat client
   mvn exec:java -Dexec.mainClass="com.function.Chat"

   # Or use curl directly
   curl -X POST http://localhost:7071/api/ask -d "what are the laws"
   ```

   To chat with a deployed instance, grab the URL and function key from your `azd` environment:

   ```bash
   export AGENT_URL=$(azd env get-value SERVICE_API_URI)
   export FUNCTION_KEY=$(az functionapp keys list \
     -n $(azd env get-value AZURE_FUNCTION_APP_NAME) \
     -g $(azd env get-value RESOURCE_GROUP) \
     --query "functionKeys.default" -o tsv)

   mvn exec:java -Dexec.mainClass="com.function.Chat"
   ```

## Source Code

The agent logic is in [`src/main/java/com/function/Ask.java`](src/main/java/com/function/Ask.java). It creates a `CopilotClient`, configures a session with a system message (Asimov's Three Laws of Robotics), and exposes an HTTP endpoint (`/api/ask`) that accepts a prompt and returns the agent's response.

[`src/main/java/com/function/Chat.java`](src/main/java/com/function/Chat.java) is a lightweight console client that POSTs messages to the function in a loop, giving you an interactive chat experience. It defaults to `http://localhost:7071` but can be pointed at a deployed instance via the `AGENT_URL` environment variable.

## Deploy Microsoft Foundry Resources

If you prefer to use your own models via BYOK and don't already have a Microsoft Foundry project with a model deployed:

```bash
azd auth login
azd up
```

This provisions all resources and configures local development automatically.

### What Gets Deployed

- Microsoft Foundry project with GPT-5-mini model
- Azure Functions app (Java 17, Flex Consumption plan)
- Storage, monitoring, and all necessary RBAC role assignments
- Optional: Search for vector store (disabled by default)
- Optional: Cosmos DB for agent thread storage (disabled by default)

## Using Microsoft Foundry (BYOK)

By default the agent uses GitHub Copilot's models. To use your own model from Microsoft Foundry instead, set these environment variables:

```bash
export AZURE_OPENAI_ENDPOINT="https://<your-ai-services>.openai.azure.com/"
export AZURE_OPENAI_API_KEY="<your-api-key>"
export AZURE_OPENAI_MODEL="gpt-5-mini"  # optional, defaults to gpt-5-mini
```

**Getting these values:**
- If you ran `azd up`, the endpoint is already in your environment — run `azd env get-values | grep AZURE_OPENAI_ENDPOINT`
- For the API key, go to [Azure Portal](https://portal.azure.com) → your AI Services resource → **Keys and Endpoint** → select the **Azure OpenAI** tab
- Or find both in the [Microsoft Foundry portal](https://ai.azure.com) under your project settings

See the [BYOK docs](https://github.com/github/copilot-sdk/blob/main/docs/auth/byok.md) for details.

## Learn More

- [GitHub Copilot SDK for Java](https://github.com/github/copilot-sdk-java)
- [GitHub Copilot SDK](https://github.com/github/copilot-sdk)
- [BYOK (Bring Your Own Key)](https://github.com/github/copilot-sdk/blob/main/docs/auth/byok.md)
- [Azure Developer CLI](https://learn.microsoft.com/en-us/azure/developer/azure-developer-cli/)
