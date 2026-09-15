package application;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small REST client for an Azure AI Foundry prompt agent. */
public class FoundryAgentClient {
    private static final String API_VERSION = "2025-05-01-preview";
    private static final Pattern ID_PATTERN = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern TEXT_PATTERN = Pattern.compile("\\\"text\\\"\\s*:\\s*\\{\\s*\\\"value\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final String projectEndpoint;
    private final String agentId;
    private final String credential;
    private String threadId;

    public FoundryAgentClient() {
        projectEndpoint = requiredEnvironment("AZURE_AI_PROJECT_ENDPOINT").replaceAll("/$", "");
        agentId = requiredEnvironment("AZURE_AI_AGENT_ID");
        credential = firstEnvironment("AZURE_AI_TOKEN", "AZURE_AI_API_KEY");
    }

    public String sendMessage(String message) throws IOException, InterruptedException {
        if (threadId == null) {
            threadId = extractId(request("POST", "/threads", "{}"));
        }

        request("POST", "/threads/" + threadId + "/messages",
                "{\"role\":\"user\",\"content\":" + quote(message) + "}");
        String run = request("POST", "/threads/" + threadId + "/runs",
                "{\"assistant_id\":" + quote(agentId) + "}");
        String runId = extractId(run);

        for (int attempt = 0; attempt < 30; attempt++) {
            Thread.sleep(1000);
            String runStatus = request("GET", "/threads/" + threadId + "/runs/" + runId, null);
            String status = extractString(runStatus, "status");
            if ("completed".equals(status)) {
                return extractLatestText(request("GET", "/threads/" + threadId + "/messages", null));
            }
            if ("failed".equals(status) || "cancelled".equals(status) || "expired".equals(status)) {
                throw new IOException("Azure agent run " + status + ".");
            }
        }
        throw new IOException("Timed out waiting for the Azure agent.");
    }

    private String request(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(projectEndpoint + path + "?api-version=" + API_VERSION))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json");
        if (credential.startsWith("Bearer ")) {
            builder.header("Authorization", credential);
        } else {
            builder.header("api-key", credential);
        }
        HttpRequest request = builder.method(method,
                body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Azure agent request failed (HTTP " + response.statusCode() + "): " + response.body());
        }
        return response.body();
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set " + name + " before using the chat agent.");
        }
        return value.trim();
    }

    private static String firstEnvironment(String... names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (value != null && !value.isBlank()) {
                return value.startsWith("Bearer ") ? value : value.trim();
            }
        }
        throw new IllegalStateException("Set AZURE_AI_TOKEN or AZURE_AI_API_KEY before using the chat agent.");
    }

    private static String extractId(String json) throws IOException {
        String id = extractString(json, "id");
        if (id == null) {
            throw new IOException("Azure agent response did not contain an id.");
        }
        return id;
    }

    private static String extractString(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String extractLatestText(String json) throws IOException {
        Matcher matcher = TEXT_PATTERN.matcher(json);
        String latest = null;
        while (matcher.find()) {
            latest = matcher.group(1).replace("\\\"", "\"").replace("\\n", "\n");
        }
        if (latest == null) {
            throw new IOException("Azure agent response did not contain message text.");
        }
        return latest;
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}