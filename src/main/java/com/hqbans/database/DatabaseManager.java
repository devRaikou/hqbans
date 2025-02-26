package com.hqbans.database;

import com.hqbans.HQBans;
import com.hqbans.models.Ban;
import com.hqbans.models.Kick;
import com.hqbans.models.Mute;
import com.hqbans.models.Warn;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.io.File;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
    private final HQBans plugin;
    private ConnectionSource connectionSource;
    private Dao<Ban, Integer> banDao;
    private Dao<Mute, Integer> muteDao;
    private Dao<Warn, Integer> warnDao;
    private Dao<Kick, Integer> kickDao;

    public DatabaseManager(HQBans plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        try {
            
            String databaseUrl = "jdbc:sqlite:" + plugin.getDataFolder() + File.separator + "database.db";
            connectionSource = new JdbcConnectionSource(databaseUrl);

            
            banDao = DaoManager.createDao(connectionSource, Ban.class);
            muteDao = DaoManager.createDao(connectionSource, Mute.class);
            warnDao = DaoManager.createDao(connectionSource, Warn.class);
            kickDao = DaoManager.createDao(connectionSource, Kick.class);

            
            TableUtils.createTableIfNotExists(connectionSource, Ban.class);
            TableUtils.createTableIfNotExists(connectionSource, Mute.class);
            TableUtils.createTableIfNotExists(connectionSource, Warn.class);
            TableUtils.createTableIfNotExists(connectionSource, Kick.class);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            connectionSource.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    public void addBan(UUID playerUUID, String playerName, String adminName, String reason, Date expiresAt) throws SQLException {
        Ban ban = new Ban(playerUUID, playerName, adminName, reason, expiresAt);
        banDao.create(ban);
    }

    public void removeBan(UUID playerUUID) throws SQLException {
        List<Ban> bans = banDao.queryBuilder()
            .where()
            .eq("playerUUID", playerUUID)
            .and()
            .eq("active", true)
            .query();

        for (Ban ban : bans) {
            ban.setActive(false);
            banDao.update(ban);
        }
    }

    public Ban getActiveBan(UUID playerUUID) throws SQLException {
        List<Ban> bans = banDao.queryBuilder()
            .where()
            .eq("playerUUID", playerUUID)
            .and()
            .eq("active", true)
            .query();

        if (bans.isEmpty()) return null;

        Ban ban = bans.get(0);
        if (ban.getExpiresAt() != null && ban.getExpiresAt().before(new Date())) {
            ban.setActive(false);
            banDao.update(ban);
            return null;
        }

        return ban;
    }

    // Mute methods
    public void addMute(UUID playerUUID, String playerName, String adminName, String reason, Date expiresAt) throws SQLException {
        Mute mute = new Mute(playerUUID, playerName, adminName, reason, expiresAt);
        muteDao.create(mute);
    }

    public void removeMute(UUID playerUUID) throws SQLException {
        List<Mute> mutes = muteDao.queryBuilder()
            .where()
            .eq("playerUUID", playerUUID)
            .and()
            .eq("active", true)
            .query();

        for (Mute mute : mutes) {
            mute.setActive(false);
            muteDao.update(mute);
        }
    }

    public Mute getActiveMute(UUID playerUUID) throws SQLException {
        List<Mute> mutes = muteDao.queryBuilder()
            .where()
            .eq("playerUUID", playerUUID)
            .and()
            .eq("active", true)
            .query();

        if (mutes.isEmpty()) return null;

        Mute mute = mutes.get(0);
        if (mute.getExpiresAt() != null && mute.getExpiresAt().before(new Date())) {
            mute.setActive(false);
            muteDao.update(mute);
            return null;
        }

        return mute;
    }

    // Warn methods
    public void addWarn(UUID playerUUID, String playerName, String adminName, String reason) throws SQLException {
        Warn warn = new Warn(playerUUID, playerName, adminName, reason);
        warnDao.create(warn);
    }

    // Kick methods
    public void addKick(UUID playerUUID, String playerName, String adminName, String reason) throws SQLException {
        Kick kick = new Kick(playerUUID, playerName, adminName, reason);
        kickDao.create(kick);
    }

    // History methods with limit
    public List<Ban> getBanHistory(UUID playerUUID, int limit) throws SQLException {
        return banDao.queryBuilder()
            .orderBy("createdAt", false)
            .where()
            .eq("playerUUID", playerUUID)
            .query()
            .stream()
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }

    public List<Mute> getMuteHistory(UUID playerUUID, int limit) throws SQLException {
        return muteDao.queryBuilder()
            .orderBy("createdAt", false)
            .where()
            .eq("playerUUID", playerUUID)
            .query()
            .stream()
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }

    public List<Warn> getWarnHistory(UUID playerUUID, int limit) throws SQLException {
        return warnDao.queryBuilder()
            .orderBy("createdAt", false)
            .where()
            .eq("playerUUID", playerUUID)
            .query()
            .stream()
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }

    public List<Kick> getKickHistory(UUID playerUUID, int limit) throws SQLException {
        return kickDao.queryBuilder()
            .orderBy("createdAt", false)
            .where()
            .eq("playerUUID", playerUUID)
            .query()
            .stream()
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
} 