package com.hqbans.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Bukkit;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {
    private final String webhookUrl;
    private final String serverName;
    private final int embedColor;
    private final boolean enabled;

    public DiscordWebhook(FileConfiguration config) {
        this.enabled = config.getBoolean("discord.enabled", false);
        this.webhookUrl = config.getString("discord.webhook-url", "");
        this.serverName = config.getString("discord.server-name", "Minecraft Server");
        String colorStr = config.getString("discord.embed-color", "#ff0000").replace("#", "");
        this.embedColor = Integer.parseInt(colorStr, 16);
    }

    public void sendMessage(String content) {
        if (!enabled || webhookUrl.isEmpty()) return;

        try {
            URL url = new URL(webhookUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Extract player name and action info
            String playerName = "Steve";
            String description = "";
            String footerText = "";
            
            // First, get the action type
            String actionType = "";
            if (content.contains("banned")) actionType = "ban";
            else if (content.contains("muted")) actionType = "mute";
            else if (content.contains("warned")) actionType = "warn";
            else if (content.contains("kicked")) actionType = "kick";
            
            // Extract player name and create description
            String[] parts = content.split("\\*\\*");
            if (parts.length > 1) {
                playerName = parts[1].trim();
                description = parts[0] + "**" + parts[1] + "**" + parts[2];
                if (parts.length > 3) {
                    description += "**" + parts[3] + "**";
                }
            }

            // Extract duration and reason based on action type
            if (actionType.equals("ban") || actionType.equals("mute")) {
                String duration = "";
                String reason = "";
                
                // Find duration and reason in the content
                for (String line : content.split("\\n")) {
                    if (line.contains("Duration:")) {
                        duration = line.substring(line.indexOf("Duration:") + 9).trim();
                    }
                    if (line.contains("Reason:")) {
                        reason = line.substring(line.indexOf("Reason:") + 7).trim();
                    }
                }
                
                // Create footer text with both duration and reason
                if (!duration.isEmpty() && !reason.isEmpty()) {
                    footerText = String.format("Duration: %s | Reason: %s", duration, reason);
                }
            } else if (actionType.equals("warn") || actionType.equals("kick")) {
                // For warn and kick, only include reason
                for (String line : content.split("\\n")) {
                    if (line.contains("Reason:")) {
                        String reason = line.substring(line.indexOf("Reason:") + 7).trim();
                        footerText = "Reason: " + reason;
                        break;
                    }
                }
            }

            // Create embed with clean formatting
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{")
                .append("\"embeds\": [{")
                .append("\"title\": \"").append(escapeJson(serverName)).append("\",")
                .append("\"description\": \"").append(escapeJson(description)).append("\",")
                .append("\"color\": ").append(embedColor).append(",")
                .append("\"footer\": {")
                .append("\"text\": \"").append(escapeJson(footerText)).append("\"")
                .append("},")
                .append("\"thumbnail\": {")
                .append("\"url\": \"https://minotar.net/avatar/").append(escapeJson(playerName)).append("/64.png\"")
                .append("}")
                .append("}]")
                .append("}");

            String json = jsonBuilder.toString();
            Bukkit.getLogger().info("Sending webhook JSON: " + json);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
                Bukkit.getLogger().warning("Discord webhook failed with response code: " + responseCode);
                Bukkit.getLogger().warning("Error response: " + response.toString());
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t")
                  .replace("'", "\\'");
    }
} 