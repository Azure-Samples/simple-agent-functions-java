package com.function;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * Console chat client for the Simple Agent function app.
 * Run with: mvn exec:java -Dexec.mainClass="com.function.Chat"
 */
public class Chat {

    public static void main(String[] args) throws Exception {
        String baseUrl = System.getenv("AGENT_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:7071";
        }
        baseUrl = baseUrl.replaceAll("/$", "");

        String functionKey = System.getenv("FUNCTION_KEY");
        if (functionKey == null) {
            functionKey = "";
        }

        System.out.println("=== Simple Agent Chat ===");
        System.out.println("Endpoint: " + baseUrl + "/api/ask");
        System.out.println("Type 'exit' or 'quit' to end.\n");

        var reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("You: ");
            String message = reader.readLine();
            if (message == null) break;

            String trimmed = message.trim();
            if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("exit") || trimmed.equalsIgnoreCase("quit")) {
                System.out.println("Goodbye!");
                break;
            }

            String url = baseUrl + "/api/ask";
            if (!functionKey.isEmpty()) {
                url += "?code=" + functionKey;
            }

            try {
                var connection = (HttpURLConnection) new URI(url).toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.getOutputStream().write(trimmed.getBytes());

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("\nError: HTTP " + responseCode + "\n");
                    continue;
                }

                var responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                var sb = new StringBuilder();
                String line;
                while ((line = responseReader.readLine()) != null) {
                    sb.append(line);
                }
                responseReader.close();

                System.out.println("\nAgent: " + sb + "\n");
            } catch (Exception e) {
                System.out.println("\nError: " + e.getMessage() + "\n");
            }
        }
    }
}
