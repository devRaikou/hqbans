package com.hqbans;

import com.hqbans.database.DatabaseManager;
import com.hqbans.listeners.ChatListener;
import com.hqbans.listeners.LoginListener;
import com.hqbans.models.Ban;
import com.hqbans.models.Mute;
import com.hqbans.models.Warn;
import com.hqbans.models.Kick;
import com.hqbans.utils.Config;
import com.hqbans.utils.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.ArrayList;

public class HQBans extends JavaPlugin {
    private DatabaseManager databaseManager;
    private DiscordWebhook discordWebhook;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Initialize config and webhook
        Config.init(getConfig());
        initializeWebhook();
        
        // Initialize database
        databaseManager = new DatabaseManager(this);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new LoginListener(this), this);

        getLogger().info("HQBans has been enabled successfully!");
    }

    private void initializeWebhook() {
        discordWebhook = new DiscordWebhook(getConfig());
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("HQBans has been disabled!");
    }

    private long parseDuration(String duration) {
        if (duration.equalsIgnoreCase("permanent") || duration.equalsIgnoreCase("perm") || duration.equals("-1")) {
            return -1; // Use -1 for permanent instead of Long.MAX_VALUE
        }

        try {
            char unit = duration.charAt(duration.length() - 1);
            long value = Long.parseLong(duration.substring(0, duration.length() - 1));

            if (value <= 0) {
                throw new IllegalArgumentException("Duration must be positive");
            }

            switch (Character.toLowerCase(unit)) {
                case 's':
                    return value * 1000;
                case 'm':
                    return value * 1000 * 60;
                case 'h':
                    return value * 1000 * 60 * 60;
                case 'd':
                    return value * 1000 * 60 * 60 * 24;
                default:
                    throw new IllegalArgumentException("Invalid duration unit");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid duration format");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("hqbans")) {
            if (!sender.hasPermission("hqbans.reload")) {
                sender.sendMessage(Config.formatMessage("error.no-permission"));
                return true;
            }
            
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                Config.init(getConfig());
                initializeWebhook();
                sender.sendMessage(Config.formatMessage("reload.success"));
                return true;
            }
        }

        if (args.length == 0) {
            sender.sendMessage(Config.formatMessage("error.usage." + command.getName().toLowerCase()));
            return false;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        switch (command.getName().toLowerCase()) {
            case "ban":
                if (!sender.hasPermission("hqbans.ban")) {
                    sender.sendMessage(Config.formatMessage("error.no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Config.formatMessage("error.usage.ban"));
                    return false;
                }
                if (target == null) {
                    sender.sendMessage(Config.formatMessage("error.player-not-found"));
                    return false;
                }
                handleBan(sender, target, args);
                break;

            case "unban":
                if (!sender.hasPermission("hqbans.unban")) {
                    sender.sendMessage(Config.formatMessage("error.no-permission"));
                    return true;
                }
                handleUnban(sender, targetName);
                break;

            case "mute":
                if (!sender.hasPermission("hqbans.mute")) {
                    sender.sendMessage(Config.formatMessage("error.no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Config.formatMessage("error.usage.mute"));
                    return false;
                }
                if (target == null) {
                    sender.sendMessage(Config.formatMessage("error.player-not-found"));
                    return false;
                }
                handleMute(sender, target, args);
                break;

            case "unmute":
                if (!sender.hasPermission("hqbans.unmute")) {
                    sender.sendMessage(Config.formatMessage("error.no-permission"));
                    return true;
                }
                if (target == null) {
                    sender.sendMessage(Config.formatMessage("error.player-not-found"));
                    return false;
                }
                handleUnmute(sender, target);
                break;

            case "warn":
                if (!sender.hasPermission("hqbans.warn")) {
                    sender.sendMessage(Config.formatMessage("error.no-permission"));
                    return true;
                }
                if (target == null) {
                    sender.sendMessage(Config.formatMessage("error.player-not-found"));
                    return false;
                }
                handleWarn(sender, target, args);
                break;

            case "kick":
                if (!sender.hasPermission("hqbans.kick")) {
                    sender.sendMessage(Config.formatMessage("error.no-permission"));
                    return true;
                }
                if (target == null) {
                    sender.sendMessage(Config.formatMessage("error.player-not-found"));
                    return false;
                }
                handleKick(sender, target, args);
                break;

            case "history":
                if (!sender.hasPermission("hqbans.history")) {
                    sender.sendMessage(Config.formatMessage("error.no-permission"));
                    return true;
                }
                handleHistory(sender, targetName, args);
                break;
        }

        return true;
    }

    private void handleBan(CommandSender sender, Player target, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Config.formatMessage("error.usage.ban"));
            return;
        }

        String reason = buildReason(args, 2);
        long duration = parseDuration(args[1]);
        Date expiresAt = duration == -1 ? null : new Date(System.currentTimeMillis() + duration);

        try {
            databaseManager.addBan(target.getUniqueId(), target.getName(), sender.getName(), reason, expiresAt);
            target.kickPlayer(Config.formatMessage("ban.screen", 
                "{admin}", sender.getName(),
                "{reason}", reason,
                "{duration}", Config.formatDuration(duration)));

            String broadcastMsg = Config.formatMessage("ban.broadcast",
                "{player}", target.getName(),
                "{admin}", sender.getName(),
                "{reason}", reason,
                "{duration}", Config.formatDuration(duration));

            Bukkit.broadcastMessage(broadcastMsg);

            // Send to Discord
            String discordMessage = getConfig().getString("discord.messages.ban", "")
                .replace("{player}", target.getName())
                .replace("{admin}", sender.getName())
                .replace("{reason}", reason)
                .replace("{duration}", Config.formatDuration(duration)) + "\nDuration: " + Config.formatDuration(duration) + "\nReason: " + reason;
            discordWebhook.sendMessage(discordMessage);

        } catch (SQLException e) {
            sender.sendMessage(Config.formatMessage("error.database"));
            e.printStackTrace();
        }
    }

    private void handleUnban(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Config.formatMessage("unban.not-found"));
            return;
        }

        try {
            Ban activeBan = databaseManager.getActiveBan(target.getUniqueId());
            if (activeBan == null) {
                sender.sendMessage(Config.formatMessage("unban.not-banned"));
                return;
            }

            databaseManager.removeBan(target.getUniqueId());
            
            String broadcastMsg = Config.formatMessage("unban.success",
                "{player}", targetName,
                "{admin}", sender.getName());
            
            Bukkit.broadcastMessage(broadcastMsg);

            // Send to Discord
            String discordMessage = getConfig().getString("discord.messages.unban", "")
                .replace("{player}", targetName)
                .replace("{admin}", sender.getName());
            discordWebhook.sendMessage(discordMessage);

        } catch (SQLException e) {
            sender.sendMessage(Config.formatMessage("error.database-error"));
            e.printStackTrace();
        }
    }

    private void handleMute(CommandSender sender, Player target, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Config.formatMessage("error.usage.mute"));
            return;
        }

        String reason = buildReason(args, 2);
        long duration = parseDuration(args[1]);
        Date expiresAt = duration == -1 ? null : new Date(System.currentTimeMillis() + duration);

        try {
            databaseManager.addMute(target.getUniqueId(), target.getName(), sender.getName(), reason, expiresAt);
            target.sendMessage(Config.formatMessage("mute.message",
                "{admin}", sender.getName(),
                "{reason}", reason,
                "{duration}", Config.formatDuration(duration)));

            String broadcastMsg = Config.formatMessage("mute.broadcast",
                "{player}", target.getName(),
                "{admin}", sender.getName(),
                "{reason}", reason,
                "{duration}", Config.formatDuration(duration));

            Bukkit.broadcastMessage(broadcastMsg);

            // Send to Discord
            String discordMessage = getConfig().getString("discord.messages.mute", "")
                .replace("{player}", target.getName())
                .replace("{admin}", sender.getName())
                .replace("{reason}", reason)
                .replace("{duration}", Config.formatDuration(duration)) + "\nDuration: " + Config.formatDuration(duration) + "\nReason: " + reason;
            discordWebhook.sendMessage(discordMessage);

        } catch (SQLException e) {
            sender.sendMessage(Config.formatMessage("error.database"));
            e.printStackTrace();
        }
    }

    private void handleUnmute(CommandSender sender, Player target) {
        try {
            Mute activeMute = databaseManager.getActiveMute(target.getUniqueId());
            if (activeMute == null) {
                sender.sendMessage(Config.formatMessage("unmute.not-muted"));
                return;
            }

            databaseManager.removeMute(target.getUniqueId());
            
            String broadcastMsg = Config.formatMessage("unmute.success",
                "{player}", target.getName(),
                "{admin}", sender.getName());
            
            Bukkit.broadcastMessage(broadcastMsg);

            // Send to Discord
            String discordMessage = getConfig().getString("discord.messages.unmute", "")
                .replace("{player}", target.getName())
                .replace("{admin}", sender.getName());
            discordWebhook.sendMessage(discordMessage);

        } catch (SQLException e) {
            sender.sendMessage(Config.formatMessage("error.database-error"));
            e.printStackTrace();
        }
    }

    private void handleWarn(CommandSender sender, Player target, String[] args) {
        String reason = buildReason(args, 1);

        try {
            databaseManager.addWarn(target.getUniqueId(), target.getName(), sender.getName(), reason);

            target.sendMessage(Config.formatMessage("warn.message",
                "{admin}", sender.getName(),
                "{reason}", reason));
            
            String broadcastMsg = Config.formatMessage("warn.broadcast",
                "{player}", target.getName(),
                "{admin}", sender.getName(),
                "{reason}", reason);
            
            Bukkit.broadcastMessage(broadcastMsg);

            // Send to Discord
            String discordMessage = getConfig().getString("discord.messages.warn", "")
                .replace("{player}", target.getName())
                .replace("{admin}", sender.getName())
                .replace("{reason}", reason) + "\nReason: " + reason;
            discordWebhook.sendMessage(discordMessage);

        } catch (SQLException e) {
            sender.sendMessage(Config.formatMessage("error.database-error"));
            e.printStackTrace();
        }
    }

    private void handleKick(CommandSender sender, Player target, String[] args) {
        String reason = buildReason(args, 1);

        try {
            databaseManager.addKick(target.getUniqueId(), target.getName(), sender.getName(), reason);

            String kickScreen = Config.formatMessage("kick.screen",
                "{admin}", sender.getName(),
                "{reason}", reason);
            
            target.kickPlayer(kickScreen);
            
            String broadcastMsg = Config.formatMessage("kick.broadcast",
                "{player}", target.getName(),
                "{admin}", sender.getName(),
                "{reason}", reason);
            
            Bukkit.broadcastMessage(broadcastMsg);

            // Send to Discord
            String discordMessage = getConfig().getString("discord.messages.kick", "")
                .replace("{player}", target.getName())
                .replace("{admin}", sender.getName())
                .replace("{reason}", reason) + "\nReason: " + reason;
            discordWebhook.sendMessage(discordMessage);

        } catch (SQLException e) {
            sender.sendMessage(Config.formatMessage("error.database-error"));
            e.printStackTrace();
        }
    }

    private void handleHistory(CommandSender sender, String targetName, String[] args) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Config.formatMessage("error.player-not-found"));
            return;
        }

        try {
            // First get all histories to count total
            List<Ban> allBans = databaseManager.getBanHistory(target.getUniqueId(), Integer.MAX_VALUE);
            List<Mute> allMutes = databaseManager.getMuteHistory(target.getUniqueId(), Integer.MAX_VALUE);
            List<Warn> allWarns = databaseManager.getWarnHistory(target.getUniqueId(), Integer.MAX_VALUE);
            List<Kick> allKicks = databaseManager.getKickHistory(target.getUniqueId(), Integer.MAX_VALUE);
            
            int totalPunishments = allBans.size() + allMutes.size() + allWarns.size() + allKicks.size();

            if (totalPunishments == 0) {
                sender.sendMessage(Config.formatMessage("history.no-history", "{player}", targetName));
                return;
            }

            // Default limit is 10, or use specified limit
            int requestedLimit = 10;
            if (args.length > 1) {
                try {
                    requestedLimit = Integer.parseInt(args[1]);
                    if (requestedLimit <= 0) {
                        sender.sendMessage(Config.formatMessage("error.invalid-limit"));
                        return;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(Config.formatMessage("history.usage"));
                    return;
                }
            }

            // Use the smaller of requested limit and total punishments
            int effectiveLimit = Math.min(requestedLimit, totalPunishments);

            // Get histories with the effective limit
            List<Ban> bans = databaseManager.getBanHistory(target.getUniqueId(), effectiveLimit);
            List<Mute> mutes = databaseManager.getMuteHistory(target.getUniqueId(), effectiveLimit);
            List<Warn> warns = databaseManager.getWarnHistory(target.getUniqueId(), effectiveLimit);
            List<Kick> kicks = databaseManager.getKickHistory(target.getUniqueId(), effectiveLimit);

            // Combine all histories into a single list for sorting
            List<Object> allPunishments = new ArrayList<>();
            allPunishments.addAll(bans);
            allPunishments.addAll(mutes);
            allPunishments.addAll(warns);
            allPunishments.addAll(kicks);

            // Sort all punishments by date (newest first)
            Collections.sort(allPunishments, (a, b) -> {
                Date dateA = getCreatedAt(a);
                Date dateB = getCreatedAt(b);
                return dateB.compareTo(dateA);
            });

            // Take only the effective limit number of most recent punishments
            allPunishments = allPunishments.stream()
                .limit(effectiveLimit)
                .collect(Collectors.toList());

            // Send header with count info
            sender.sendMessage(Config.formatMessage("history.header", 
                "{player}", targetName,
                "{count}", String.valueOf(allPunishments.size()),
                "{allHistory}", String.valueOf(totalPunishments)));

            // Display all punishments in chronological order
            for (Object punishment : allPunishments) {
                if (punishment instanceof Ban) {
                    Ban ban = (Ban) punishment;
                    String duration = ban.getExpiresAt() == null ? Config.getMessage("ban.permanent") 
                        : Config.formatDuration(ban.getExpiresAt().getTime() - ban.getCreatedAt().getTime());
                    
                    String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(ban.getCreatedAt());
                    
                    String entry = Config.formatMessage("history.ban-entry",
                        "{admin}", ban.getAdminName(),
                        "{date}", date,
                        "{duration}", duration);

                    if (ban.isActive()) {
                        if (ban.getExpiresAt() == null || ban.getExpiresAt().after(new Date())) {
                            entry += Config.getMessage("history.active-suffix");
                        } else {
                            entry += Config.getMessage("history.expired-suffix");
                        }
                    } else {
                        entry += Config.getMessage("history.expired-suffix");
                    }

                    sender.sendMessage(entry);
                    sender.sendMessage(Config.formatMessage("history.reason", "{reason}", ban.getReason()));
                } else if (punishment instanceof Mute) {
                    Mute mute = (Mute) punishment;
                    String duration = mute.getExpiresAt() == null ? Config.getMessage("ban.permanent")
                        : Config.formatDuration(mute.getExpiresAt().getTime() - mute.getCreatedAt().getTime());
                    
                    String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(mute.getCreatedAt());
                    
                    String entry = Config.formatMessage("history.mute-entry",
                        "{admin}", mute.getAdminName(),
                        "{date}", date,
                        "{duration}", duration);

                    if (mute.isActive()) {
                        if (mute.getExpiresAt() == null || mute.getExpiresAt().after(new Date())) {
                            entry += Config.getMessage("history.active-suffix");
                        } else {
                            entry += Config.getMessage("history.expired-suffix");
                        }
                    } else {
                        entry += Config.getMessage("history.expired-suffix");
                    }

                    sender.sendMessage(entry);
                    sender.sendMessage(Config.formatMessage("history.reason", "{reason}", mute.getReason()));
                } else if (punishment instanceof Warn) {
                    Warn warn = (Warn) punishment;
                    String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(warn.getCreatedAt());
                    
                    String entry = Config.formatMessage("history.warn-entry",
                        "{admin}", warn.getAdminName(),
                        "{date}", date);

                    sender.sendMessage(entry);
                    sender.sendMessage(Config.formatMessage("history.reason", "{reason}", warn.getReason()));
                } else if (punishment instanceof Kick) {
                    Kick kick = (Kick) punishment;
                    String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(kick.getCreatedAt());
                    
                    String entry = Config.formatMessage("history.kick-entry",
                        "{admin}", kick.getAdminName(),
                        "{date}", date);

                    sender.sendMessage(entry);
                    sender.sendMessage(Config.formatMessage("history.reason", "{reason}", kick.getReason()));
                }
            }

            // Send footer
            sender.sendMessage(Config.getMessage("history.footer"));

        } catch (SQLException e) {
            sender.sendMessage(Config.formatMessage("error.database-error"));
            e.printStackTrace();
        }
    }

    private Date getCreatedAt(Object punishment) {
        if (punishment instanceof Ban) return ((Ban) punishment).getCreatedAt();
        if (punishment instanceof Mute) return ((Mute) punishment).getCreatedAt();
        if (punishment instanceof Warn) return ((Warn) punishment).getCreatedAt();
        if (punishment instanceof Kick) return ((Kick) punishment).getCreatedAt();
        return new Date(0);
    }

    public boolean isBanned(UUID uuid) {
        try {
            Ban ban = databaseManager.getActiveBan(uuid);
            return ban != null;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isMuted(UUID uuid) {
        try {
            Mute mute = databaseManager.getActiveMute(uuid);
            return mute != null;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public long getMuteExpiry(UUID uuid) {
        try {
            Mute mute = databaseManager.getActiveMute(uuid);
            if (mute == null) return 0L;
            if (mute.getExpiresAt() == null) return -1L;
            return mute.getExpiresAt().getTime();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    private String buildReason(String[] args, int startIndex) {
        if (args.length <= startIndex) {
            return "No reason provided";
        }
        StringBuilder reason = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            reason.append(args[i]).append(" ");
        }
        return reason.toString().trim();
    }
} 