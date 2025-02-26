package com.hqbans.listeners;

import com.hqbans.HQBans;
import com.hqbans.utils.Config;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.entity.Player;

public class ChatListener implements Listener {
    private final HQBans plugin;

    public ChatListener(HQBans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (plugin.isMuted(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            long expiry = plugin.getMuteExpiry(event.getPlayer().getUniqueId());
            String duration = expiry == -1 ? Config.getMessage("ban.permanent") 
                : Config.formatDuration(expiry - System.currentTimeMillis());
            
            event.getPlayer().sendMessage(Config.formatMessage("mute.chat-blocked", 
                "{duration}", duration));

            // Send attempt message to staff
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("hqbans.staff")) {
                    player.sendMessage(Config.formatMessage("mute.attempt",
                        "{player}", event.getPlayer().getName(),
                        "{message}", event.getMessage()));
                }
            }
        }
    }
} 