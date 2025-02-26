package com.hqbans.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;
import java.util.UUID;

@DatabaseTable(tableName = "warns")
public class Warn {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false)
    private UUID playerUUID;

    @DatabaseField(canBeNull = false)
    private String playerName;

    @DatabaseField(canBeNull = false)
    private String adminName;

    @DatabaseField
    private String reason;

    @DatabaseField(canBeNull = false)
    private Date createdAt;

    public Warn() {
        
    }

    public Warn(UUID playerUUID, String playerName, String adminName, String reason) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.adminName = adminName;
        this.reason = reason;
        this.createdAt = new Date();
    }

    
    public int getId() { return id; }
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public String getAdminName() { return adminName; }
    public String getReason() { return reason; }
    public Date getCreatedAt() { return createdAt; }

    
    public void setId(int id) { this.id = id; }
    public void setPlayerUUID(UUID playerUUID) { this.playerUUID = playerUUID; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }
    public void setReason(String reason) { this.reason = reason; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
} 