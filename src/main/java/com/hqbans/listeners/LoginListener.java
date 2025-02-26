package com.hqbans.listeners;

import com.hqbans.HQBans;
import com.hqbans.models.Ban;
import com.hqbans.utils.Config;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Date;

public class LoginListener implements Listener {
    private final HQBans plugin;

    public LoginListener(HQBans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        try {
            Ban activeBan = plugin.getDatabaseManager().getActiveBan(event.getUniqueId());
            if (activeBan != null) {
                String duration = activeBan.getExpiresAt() == null ? Config.getMessage("ban.permanent") 
                    : Config.formatDuration(activeBan.getExpiresAt().getTime() - System.currentTimeMillis());

                String banScreen = Config.formatMessage("ban.screen",
                    "{admin}", activeBan.getAdminName(),
                    "{reason}", activeBan.getReason(),
                    "{duration}", duration);
                
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, banScreen);

                // Send attempt message to staff
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("hqbans.staff")) {
                        player.sendMessage(Config.formatMessage("ban.attempt",
                            "{player}", event.getName()));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // If there's a database error, prevent login to be safe
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Database error occurred. Please try again later.");
        }
    }
} 