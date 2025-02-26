package com.hqbans.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class Config {
    private static FileConfiguration config;
    private static String prefix;

    public static void init(FileConfiguration configuration) {
        config = configuration;
        prefix = ChatColor.translateAlternateColorCodes('&', config.getString("prefix", "&7[&6SGHQ&7] "));
    }

    public static String getMessage(String path) {
        String message = config.getString("messages." + path, "Message not found: " + path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String formatMessage(String path, String... replacements) {
        String message = getMessage(path);
        message = message.replace("{prefix}", prefix);
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        
        return message;
    }

    public static String formatDuration(long duration) {
        if (duration == -1) {
            return getMessage("ban.permanent");
        }

        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        StringBuilder formatted = new StringBuilder();
        
        if (days > 0) {
            formatted.append(days).append(days == 1 ? " day" : " days");
            hours %= 24;
            if (hours > 0 || minutes > 0 || seconds > 0) formatted.append(", ");
        }
        if (hours > 0) {
            formatted.append(hours).append(hours == 1 ? " hour" : " hours");
            minutes %= 60;
            if (minutes > 0 || seconds > 0) formatted.append(", ");
        }
        if (minutes > 0) {
            formatted.append(minutes).append(minutes == 1 ? " minute" : " minutes");
            seconds %= 60;
            if (seconds > 0) formatted.append(", ");
        }
        if (seconds > 0 || formatted.length() == 0) {
            formatted.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return formatted.toString();
    }

    public static String getPrefix() {
        return prefix;
    }
} 