package com.haiz.servercore.teams;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public final class TeamsBridge {

    private Plugin betterTeamsPlugin;
    private Class<?> teamClass;
    private Class<?> teamPlayerClass;
    private Class<?> playerRankClass;
    private Class<?> mainClass;
    private Class<?> warpClass;

    private boolean available;

    private Method getTeamByUUID;
    private Method getTeamByName;
    private Method getMembers;
    private Method getName;
    private Method getTag;
    private Method getDescription;
    private Method getColor;
    private Method isOpen;
    private Method isPvp;
    private Method getLevel;
    private Method getScore;
    private Method getMoney;
    private Method getHome;
    private Method getWarps;
    private Method getAllies;
    private Method getBannedPlayers;
    private Method getInvitedPlayers;
    private Method getTeamId;

    private Method tpGetPlayer;
    private Method tpGetRank;
    private Method tpGetTitle;

    private Method prName;
    private Method prOrdinal;

    private Method warpGetLocation;
    private Method warpGetName;

    private Method teamSetName;
    private Method teamSetTag;
    private Method teamSetDescription;
    private Method teamSetColor;
    private Method teamSetOpen;
    private Method teamSetPvp;
    private Method teamSetHome;
    private Method teamDelHome;

    private Method teamInvite;
    private Method teamKick;
    private Method teamPromote;
    private Method teamDemote;
    private Method teamBan;
    private Method teamUnban;
    private Method teamDeposit;
    private Method teamWithdraw;
    private Method teamSetWarp;
    private Method teamDelWarp;
    private Method teamAddAlly;
    private Method teamRemoveAlly;
    private Method teamDisband;
    private Method teamLevelup;

    private Object getTeamManager;
    private Method tmGetTeam;

    private Map<String, Object> rankConstants;

    public TeamsBridge() {
        this.available = false;
        this.rankConstants = new HashMap<>();
    }

    public boolean initialize() {
        betterTeamsPlugin = Bukkit.getPluginManager().getPlugin("BetterTeams");
        if (betterTeamsPlugin == null) {
            return false;
        }
        try {
            teamClass = Class.forName("com.booksaw.betterTeams.Team");
            teamPlayerClass = Class.forName("com.booksaw.betterTeams.TeamPlayer");
            playerRankClass = Class.forName("com.booksaw.betterTeams.PlayerRank");
            mainClass = Class.forName("com.booksaw.betterTeams.Main");
            warpClass = Class.forName("com.booksaw.betterTeams.Warp");

            initModelMethods();
            initMutatorMethods();
            initRankConstants();

            available = true;
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[HaizCore/Teams] Falha ao carregar BetterTeams via reflexão: " + e.getMessage());
            available = false;
            return false;
        }
    }

    private void initModelMethods() throws Exception {
        getTeamByUUID = teamClass.getMethod("getTeam", UUID.class);
        getTeamByName = teamClass.getMethod("getTeam", String.class);
        getMembers = teamClass.getMethod("getMembers");
        getName = teamClass.getMethod("getName");
        getTag = teamClass.getMethod("getTag");
        getDescription = teamClass.getMethod("getDescription");
        getColor = teamClass.getMethod("getColor");
        isOpen = teamClass.getMethod("isOpen");
        isPvp = teamClass.getMethod("isPvp");
        getLevel = teamClass.getMethod("getLevel");
        getScore = teamClass.getMethod("getScore");
        getMoney = teamClass.getMethod("getMoney");
        getHome = teamClass.getMethod("getHome");
        getWarps = teamClass.getMethod("getWarps");
        getAllies = teamClass.getMethod("getAllies");
        getTeamId = teamClass.getMethod("getId");

        try { getBannedPlayers = teamClass.getMethod("getBannedPlayers"); } catch (NoSuchMethodException ignored) {}
        try { getInvitedPlayers = teamClass.getMethod("getInvitedPlayers"); } catch (NoSuchMethodException ignored) {}

        tpGetPlayer = teamPlayerClass.getMethod("getPlayer");
        tpGetRank = teamPlayerClass.getMethod("getRank");
        try { tpGetTitle = teamPlayerClass.getMethod("getTitle"); } catch (NoSuchMethodException ignored) {}

        prName = playerRankClass.getMethod("name");
        prOrdinal = playerRankClass.getMethod("ordinal");

        warpGetLocation = warpClass.getMethod("getLocation");
        warpGetName = warpClass.getMethod("getName");
    }

    private void initMutatorMethods() throws Exception {
        teamSetName = findMethod(teamClass, "setName", String.class);
        teamSetTag = findMethod(teamClass, "setTag", String.class);
        teamSetDescription = findMethod(teamClass, "setDescription", String.class);
        teamSetColor = findMethod(teamClass, "setColor", Class.forName("org.bukkit.ChatColor"));
        teamSetOpen = findMethod(teamClass, "setOpen", boolean.class);
        teamSetPvp = findMethod(teamClass, "setPvp", boolean.class);
        teamSetHome = findMethod(teamClass, "setHome", Location.class);
        teamDelHome = findMethod(teamClass, "delHome");

        teamInvite = findMethod(teamClass, "invite", UUID.class);
        teamKick = findMethod(teamClass, "kick", UUID.class);
        teamPromote = findMethod(teamClass, "promote", UUID.class);
        teamDemote = findMethod(teamClass, "demote", UUID.class);
        teamBan = findMethod(teamClass, "ban", UUID.class);
        teamUnban = findMethod(teamClass, "unban", UUID.class);
        teamDeposit = findMethod(teamClass, "deposit", double.class);
        teamWithdraw = findMethod(teamClass, "withdraw", double.class);
        teamSetWarp = findMethod(teamClass, "setWarp", warpClass);
        teamDelWarp = findMethod(teamClass, "delWarp", String.class);
        teamAddAlly = findMethod(teamClass, "addAlly", UUID.class);
        teamRemoveAlly = findMethod(teamClass, "removeAlly", UUID.class);
        teamDisband = findMethod(teamClass, "disband");
        teamLevelup = findMethod(teamClass, "levelup");
    }

    private Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        try {
            return clazz.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private void initRankConstants() throws Exception {
        for (Object constant : playerRankClass.getEnumConstants()) {
            String name = (String) prName.invoke(constant);
            rankConstants.put(name, constant);
        }
    }

    public boolean isAvailable() { return available; }

    // ── Get Team ───────────────────────────────────────────────────────────

    public Object getTeam(UUID playerUUID) {
        if (!available) return null;
        try {
            return getTeamByUUID.invoke(null, playerUUID);
        } catch (Exception e) {
            return null;
        }
    }

    public Object getTeamByName(String name) {
        if (!available) return null;
        try {
            return getTeamByName.invoke(null, name);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Team Info ──────────────────────────────────────────────────────────

    public UUID getTeamId(Object team) {
        return (UUID) invoke(team, getTeamId);
    }

    public String getTeamName(Object team) {
        return (String) invoke(team, getName);
    }

    public String getTeamTag(Object team) {
        return (String) invoke(team, getTag);
    }

    public String getTeamDescription(Object team) {
        return (String) invoke(team, getDescription);
    }

    public String getTeamColor(Object team) {
        Object color = invoke(team, getColor);
        return color != null ? color.toString() : "WHITE";
    }

    public boolean isTeamOpen(Object team) {
        return (boolean) invoke(team, isOpen);
    }

    public boolean isTeamPvp(Object team) {
        return (boolean) invoke(team, isPvp);
    }

    public int getTeamLevel(Object team) {
        return (int) invoke(team, getLevel);
    }

    public double getTeamScore(Object team) {
        return (double) invoke(team, getScore);
    }

    public double getTeamMoney(Object team) {
        return (double) invoke(team, getMoney);
    }

    public Location getTeamHome(Object team) {
        return (Location) invoke(team, getHome);
    }

    // ── Members ────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Object> getMembers(Object team) {
        Object result = invoke(team, getMembers);
        if (result instanceof Collection<?> col) {
            return new ArrayList<>(col);
        }
        return Collections.emptyList();
    }

    public UUID getPlayerUUID(Object teamPlayer) {
        return (UUID) invoke(teamPlayer, tpGetPlayer);
    }

    public String getPlayerRank(Object teamPlayer) {
        Object rank = invoke(teamPlayer, tpGetRank);
        if (rank == null) return "DEFAULT";
        try {
            return (String) prName.invoke(rank);
        } catch (Exception e) {
            return "DEFAULT";
        }
    }

    public int getPlayerRankOrdinal(Object teamPlayer) {
        Object rank = invoke(teamPlayer, tpGetRank);
        if (rank == null) return 0;
        try {
            return (int) prOrdinal.invoke(rank);
        } catch (Exception e) {
            return 0;
        }
    }

    public String getPlayerTitle(Object teamPlayer) {
        if (tpGetTitle == null) return "";
        Object result = invoke(teamPlayer, tpGetTitle);
        return result != null ? result.toString() : "";
    }

    public Object findMember(Object team, UUID playerUUID) {
        for (Object member : getMembers(team)) {
            UUID memberUUID = getPlayerUUID(member);
            if (playerUUID.equals(memberUUID)) {
                return member;
            }
        }
        return null;
    }

    public boolean isOwner(Object team, UUID playerUUID) {
        Object member = findMember(team, playerUUID);
        return member != null && getPlayerRankOrdinal(member) == 2;
    }

    public boolean isAdmin(Object team, UUID playerUUID) {
        Object member = findMember(team, playerUUID);
        return member != null && getPlayerRankOrdinal(member) >= 1;
    }

    // ── Warps ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getWarps(Object team) {
        Object result = invoke(team, getWarps);
        if (result instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }

    public Location getWarpLocation(Object warp) {
        return (Location) invoke(warp, warpGetLocation);
    }

    public String getWarpName(Object warp) {
        return (String) invoke(warp, warpGetName);
    }

    // ── Allies ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Set<UUID> getAllies(Object team) {
        Object result = invoke(team, getAllies);
        if (result instanceof Set<?> set) {
            return (Set<UUID>) set;
        }
        return Collections.emptySet();
    }

    public String getAllyName(UUID allyTeamId) {
        Object allyTeam = getTeamByName("");
        for (Object team : getAllTeams()) {
            if (getTeamId(team).equals(allyTeamId)) {
                return getTeamName(team);
            }
        }
        return allyTeamId.toString().substring(0, 8);
    }

    // ── Mutations ──────────────────────────────────────────────────────────

    public boolean setTeamName(Object team, String name) {
        return invokeBool(team, teamSetName, name);
    }

    public boolean setTeamTag(Object team, String tag) {
        return invokeBool(team, teamSetTag, tag);
    }

    public boolean setTeamDescription(Object team, String desc) {
        return invokeBool(team, teamSetDescription, desc);
    }

    public boolean setTeamColor(Object team, String colorName) {
        try {
            Object color = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.ChatColor"), colorName.toUpperCase());
            return invokeBool(team, teamSetColor, color);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean setTeamOpen(Object team, boolean open) {
        return invokeBool(team, teamSetOpen, open);
    }

    public boolean setTeamPvp(Object team, boolean pvp) {
        return invokeBool(team, teamSetPvp, pvp);
    }

    public boolean setTeamHome(Object team, Location loc) {
        return invokeBool(team, teamSetHome, loc);
    }

    public boolean deleteTeamHome(Object team) {
        return invokeBool(team, teamDelHome);
    }

    public boolean invitePlayer(Object team, UUID playerUUID) {
        return invokeBool(team, teamInvite, playerUUID);
    }

    public boolean kickPlayer(Object team, UUID playerUUID) {
        return invokeBool(team, teamKick, playerUUID);
    }

    public boolean promotePlayer(Object team, UUID playerUUID) {
        return invokeBool(team, teamPromote, playerUUID);
    }

    public boolean demotePlayer(Object team, UUID playerUUID) {
        return invokeBool(team, teamDemote, playerUUID);
    }

    public boolean banPlayer(Object team, UUID playerUUID) {
        return invokeBool(team, teamBan, playerUUID);
    }

    public boolean unbanPlayer(Object team, UUID playerUUID) {
        return invokeBool(team, teamUnban, playerUUID);
    }

    public boolean depositMoney(Object team, double amount) {
        return invokeBool(team, teamDeposit, amount);
    }

    public boolean withdrawMoney(Object team, double amount) {
        return invokeBool(team, teamWithdraw, amount);
    }

    public boolean setWarp(Object team, Object warp) {
        return invokeBool(team, teamSetWarp, warp);
    }

    public boolean deleteWarp(Object team, String name) {
        return invokeBool(team, teamDelWarp, name);
    }

    public boolean addAlly(Object team, UUID allyTeamId) {
        return invokeBool(team, teamAddAlly, allyTeamId);
    }

    public boolean removeAlly(Object team, UUID allyTeamId) {
        return invokeBool(team, teamRemoveAlly, allyTeamId);
    }

    public boolean disbandTeam(Object team) {
        return invokeBool(team, teamDisband);
    }

    public boolean levelupTeam(Object team) {
        return invokeBool(team, teamLevelup);
    }

    // ── Utility ────────────────────────────────────────────────────────────

    public List<Object> getAllTeams() {
        List<Object> teams = new ArrayList<>();
        try {
            Class<?> teamManagerClass = Class.forName("com.booksaw.betterTeams.TeamManager");
            Method getTeams = teamManagerClass.getMethod("getTeams");
            Object result = getTeams.invoke(null);
            if (result instanceof Collection<?> col) {
                teams.addAll(col);
            }
        } catch (Exception e) {
            // fallback: não é possível listar todos os times
        }
        return teams;
    }

    public String resolvePlayerName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        String name = op.getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    private Object invoke(Object target, Method method, Object... args) {
        if (method == null) return null;
        try {
            return method.invoke(target, args);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean invokeBool(Object target, Method method, Object... args) {
        if (method == null) return false;
        try {
            Object result = method.invoke(target, args);
            return result instanceof Boolean b ? b : true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[HaizCore/Teams] Erro ao executar: " + e.getMessage());
            return false;
        }
    }
}
