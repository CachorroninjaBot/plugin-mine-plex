package com.haiz.servercore.teams;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Bridge para BetterTeams 5.x via reflexão.
 * Adapta a nova API (getID, getTeamHome, MemberSetComponent, etc).
 */
public final class TeamsBridge {

    private Plugin betterTeamsPlugin;
    private boolean available;

    // Core classes
    private Class<?> teamClass;
    private Class<?> teamPlayerClass;
    private Class<?> playerRankClass;
    private Class<?> memberSetClass;
    private Class<?> allySetClass;
    private Class<?> warpSetClass;
    private Class<?> warpClass;

    // Team static methods
    private Method getTeamByUUID;
    private Method getTeamByNameStatic;

    // Team instance methods - info
    private Method getID;
    private Method getName;
    private Method getTag;
    private Method getDescription;
    private Method getColor;
    private Method isOpen;
    private Method isPvp;
    private Method getLevel;
    private Method getScore;
    private Method getBalance;
    private Method getTeamHome;
    private Method getMembers;
    private Method getAllies;
    private Method getBannedPlayers;
    private Method getInvitedPlayers;

    // Team instance methods - mutators
    private Method setName;
    private Method setTag;
    private Method setDescription;
    private Method setColor;
    private Method setOpen;
    private Method setPvp;
    private Method setTeamHome;
    private Method deleteTeamHome;
    private Method invite;
    private Method disband;
    private Method banPlayer;
    private Method unbanPlayer;
    private Method getTeamPlayer;
    private Method addAllyMethod;
    private Method removeAllyMethod;
    private Method becomeNeutralMethod;
    private Method delWarpMethod;
    private Method setMoneyMethod;
    private Method getScoreInt;

    // MemberSetComponent
    private Method mscAsList;

    // AllySetComponent
    private Method ascAsList;

    // WarpSetComponent
    private Method wscAsList;
    private Method getWarpsMethod;

    // TeamPlayer
    private Method tpGetPlayer;
    private Method tpGetRank;
    private Method tpGetTitle;

    // PlayerRank
    private Method prName;
    private Method prOrdinal;

    // Warp
    private Method warpGetLocation;
    private Method warpGetName;

    // Rank constants
    private Map<String, Object> rankConstants;

    public TeamsBridge() {
        this.available = false;
        this.rankConstants = new HashMap<>();
    }

    public boolean initialize() {
        betterTeamsPlugin = Bukkit.getPluginManager().getPlugin("BetterTeams");
        if (betterTeamsPlugin == null) return false;

        try {
            teamClass = Class.forName("com.booksaw.betterTeams.Team");
            teamPlayerClass = Class.forName("com.booksaw.betterTeams.TeamPlayer");
            playerRankClass = Class.forName("com.booksaw.betterTeams.PlayerRank");
            memberSetClass = Class.forName("com.booksaw.betterTeams.team.MemberSetComponent");
            allySetClass = Class.forName("com.booksaw.betterTeams.team.AllySetComponent");
            warpSetClass = Class.forName("com.booksaw.betterTeams.team.WarpSetComponent");
            warpClass = Class.forName("com.booksaw.betterTeams.Warp");

            initMethods();
            initRankConstants();

            available = true;
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[HaizCore/Teams] Falha ao carregar BetterTeams v5: " + e.getMessage());
            available = false;
            return false;
        }
    }

    private void initMethods() throws Exception {
        // Static lookups
        getTeamByUUID = teamClass.getMethod("getTeam", UUID.class);
        getTeamByNameStatic = teamClass.getMethod("getTeam", String.class);

        // Info methods
        getID = teamClass.getMethod("getID");
        getName = teamClass.getMethod("getName");
        getTag = teamClass.getMethod("getTag");
        getDescription = teamClass.getMethod("getDescription");
        getColor = teamClass.getMethod("getColor");
        isOpen = teamClass.getMethod("isOpen");
        isPvp = teamClass.getMethod("isPvp");
        getLevel = teamClass.getMethod("getLevel");
        getScore = teamClass.getMethod("getScore");
        getBalance = teamClass.getMethod("getBalance");
        getTeamHome = teamClass.getMethod("getTeamHome");
        getMembers = teamClass.getMethod("getMembers");
        getAllies = teamClass.getMethod("getAllies");
        getWarpsMethod = findMethod(teamClass, "getWarps");
        getBannedPlayers = teamClass.getMethod("getBannedPlayers");
        getInvitedPlayers = teamClass.getMethod("getInvitedPlayers");

        // Mutators - v5 signatures
        setName = teamClass.getMethod("setName", String.class, Player.class);
        setTag = teamClass.getMethod("setTag", String.class);
        setDescription = teamClass.getMethod("setDescription", String.class);
        setColor = teamClass.getMethod("setColor", org.bukkit.ChatColor.class);
        setOpen = teamClass.getMethod("setOpen", boolean.class);
        setPvp = teamClass.getMethod("setPvp", boolean.class);
        setTeamHome = teamClass.getMethod("setTeamHome", Location.class);
        deleteTeamHome = teamClass.getMethod("deleteTeamHome");
        invite = teamClass.getMethod("invite", UUID.class);
        disband = teamClass.getMethod("disband");
        banPlayer = teamClass.getMethod("banPlayer", OfflinePlayer.class);
        unbanPlayer = teamClass.getMethod("unbanPlayer", OfflinePlayer.class);
        getTeamPlayer = teamClass.getMethod("getTeamPlayer", OfflinePlayer.class);

        // Ally/warp/money methods
        addAllyMethod = findMethod(teamClass, "addAlly", UUID.class);
        removeAllyMethod = findMethod(teamClass, "becomeNeutral", UUID.class, boolean.class);
        if (removeAllyMethod == null) {
            removeAllyMethod = findMethod(teamClass, "removeAlly", UUID.class);
        }
        delWarpMethod = findMethod(teamClass, "delWarp", String.class);
        setMoneyMethod = findMethod(teamClass, "setMoney", double.class);
        getScoreInt = findMethod(teamClass, "getScore");

        // Component methods
        mscAsList = memberSetClass.getMethod("asList");
        ascAsList = allySetClass.getMethod("asList");
        wscAsList = warpSetClass.getMethod("asList");

        // TeamPlayer methods
        tpGetPlayer = teamPlayerClass.getMethod("getPlayer");
        tpGetRank = teamPlayerClass.getMethod("getRank");
        tpGetTitle = teamPlayerClass.getMethod("getTitle");

        // PlayerRank
        prName = playerRankClass.getMethod("name");
        prOrdinal = playerRankClass.getMethod("ordinal");

        // Warp
        warpGetLocation = warpClass.getMethod("getLocation");
        warpGetName = warpClass.getMethod("getName");
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
        return invokeStatic(getTeamByUUID, playerUUID);
    }

    public Object getTeamByName(String name) {
        return invokeStatic(getTeamByNameStatic, name);
    }

    public Object getTeam(OfflinePlayer player) {
        return invokeStatic(getTeamByUUID, player);
    }

    // ── Team Info ──────────────────────────────────────────────────────────

    public UUID getTeamId(Object team) {
        return (UUID) invoke(team, getID);
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
        return (boolean) invokeBool(team, isOpen);
    }

    public boolean isTeamPvp(Object team) {
        return (boolean) invokeBool(team, isPvp);
    }

    public int getTeamLevel(Object team) {
        return (int) invokeInt(team, getLevel);
    }

    public double getTeamScore(Object team) {
        return invokeInt(team, getScore);
    }

    public String getTeamBalance(Object team) {
        Object result = invoke(team, getBalance);
        return result != null ? result.toString() : "0";
    }

    public Location getTeamHome(Object team) {
        return (Location) invoke(team, getTeamHome);
    }

    // ── Members ────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Object> getMembers(Object team) {
        Object memberSet = invoke(team, getMembers);
        if (memberSet == null) return Collections.emptyList();
        try {
            Object list = mscAsList.invoke(memberSet);
            if (list instanceof Collection<?> col) {
                return new ArrayList<>(col);
            }
        } catch (Exception ignored) {}
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

    public Object getTeamPlayerFor(Object team, OfflinePlayer player) {
        return invoke(team, getTeamPlayer, player);
    }

    // ── Warps ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Object> getWarps(Object team) {
        Object warpSet = invoke(team, getWarpsMethod);
        if (warpSet == null) return Collections.emptyList();
        try {
            Object list = wscAsList.invoke(warpSet);
            if (list instanceof Collection<?> col) {
                return new ArrayList<>(col);
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    public Location getWarpLocation(Object warp) {
        return (Location) invoke(warp, warpGetLocation);
    }

    public String getWarpName(Object warp) {
        return (String) invoke(warp, warpGetName);
    }

    // ── Allies ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Object> getAllies(Object team) {
        Object allySet = invoke(team, getAllies);
        if (allySet == null) return Collections.emptyList();
        try {
            Object list = ascAsList.invoke(allySet);
            if (list instanceof Collection<?> col) {
                return new ArrayList<>(col);
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    public Set<UUID> getAllyIds(Object team) {
        Set<UUID> ids = new HashSet<>();
        for (Object ally : getAllies(team)) {
            UUID id = getTeamId(ally);
            if (id != null) ids.add(id);
        }
        return ids;
    }

    public String getAllyName(UUID allyTeamId) {
        for (Object team : getAllTeams()) {
            if (getTeamId(team).equals(allyTeamId)) {
                return getTeamName(team);
            }
        }
        return allyTeamId.toString().substring(0, 8);
    }

    // ── Mutations (v5 signatures) ──────────────────────────────────────────

    public boolean setTeamName(Object team, String name, Player source) {
        return invokeVoid(team, setName, name, source);
    }

    public boolean setTeamTag(Object team, String tag) {
        return invokeVoid(team, setTag, tag);
    }

    public boolean setTeamDescription(Object team, String desc) {
        return invokeVoid(team, setDescription, desc);
    }

    public boolean setTeamColor(Object team, org.bukkit.ChatColor color) {
        return invokeVoid(team, setColor, color);
    }

    public boolean setTeamColorByName(Object team, String colorName) {
        try {
            org.bukkit.ChatColor color = org.bukkit.ChatColor.valueOf(colorName.toUpperCase());
            return setTeamColor(team, color);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean setTeamOpen(Object team, boolean open) {
        return invokeVoid(team, setOpen, open);
    }

    public boolean setTeamPvp(Object team, boolean pvp) {
        return invokeVoid(team, setPvp, pvp);
    }

    public boolean setTeamHome(Object team, Location loc) {
        return invokeVoid(team, setTeamHome, loc);
    }

    public boolean deleteTeamHome(Object team) {
        return invokeVoid(team, deleteTeamHome);
    }

    public boolean invitePlayer(Object team, UUID playerUUID) {
        return invokeVoid(team, invite, playerUUID);
    }

    public boolean kickPlayer(Object team, UUID playerUUID) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerUUID);
        Object teamPlayer = invoke(team, getTeamPlayer, op);
        if (teamPlayer == null) return false;
        try {
            Method removePlayer = teamClass.getMethod("removePlayer", teamPlayerClass);
            return (boolean) invokeBool(team, removePlayer, teamPlayer);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean promotePlayer(Object team, UUID playerUUID) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerUUID);
        Object teamPlayer = invoke(team, getTeamPlayer, op);
        if (teamPlayer == null) return false;
        try {
            Method promote = teamClass.getMethod("promotePlayer", teamPlayerClass);
            invoke(team, promote, teamPlayer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean demotePlayer(Object team, UUID playerUUID) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerUUID);
        Object teamPlayer = invoke(team, getTeamPlayer, op);
        if (teamPlayer == null) return false;
        try {
            Method demote = teamClass.getMethod("demotePlayer", teamPlayerClass);
            invoke(team, demote, teamPlayer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean banPlayer(Object team, UUID playerUUID) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerUUID);
        return invokeVoid(team, banPlayer, op);
    }

    public boolean unbanPlayer(Object team, UUID playerUUID) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerUUID);
        return invokeVoid(team, unbanPlayer, op);
    }

    public boolean disbandTeam(Object team) {
        return invokeVoid(team, disband);
    }

    public boolean addAlly(Object team, UUID allyTeamId) {
        return invokeVoid(team, addAllyMethod, allyTeamId);
    }

    public boolean removeAlly(Object team, UUID allyTeamId) {
        return invokeVoid(team, removeAllyMethod, allyTeamId);
    }

    public boolean deleteWarp(Object team, String name) {
        return invokeVoid(team, delWarpMethod, name);
    }

    public boolean depositMoney(Object team, double amount) {
        String balanceStr = getTeamBalance(team);
        try {
            double current = Double.parseDouble(balanceStr.replace(",", "."));
            invoke(team, setMoneyMethod, current + amount);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean withdrawMoney(Object team, double amount) {
        String balanceStr = getTeamBalance(team);
        try {
            double current = Double.parseDouble(balanceStr.replace(",", "."));
            if (current < amount) return false;
            invoke(team, setMoneyMethod, current - amount);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean levelupTeam(Object team) {
        try {
            Method levelup = teamClass.getMethod("levelup");
            invoke(team, levelup);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Utility ────────────────────────────────────────────────────────────

    public List<Object> getAllTeams() {
        List<Object> teams = new ArrayList<>();
        try {
            Class<?> teamManagerClass = Class.forName("com.booksaw.betterTeams.team.TeamManager");
            Method getTeams = teamManagerClass.getMethod("getTeams");
            Object result = getTeams.invoke(null);
            if (result instanceof Collection<?> col) {
                teams.addAll(col);
            }
        } catch (Exception ignored) {}
        return teams;
    }

    public String resolvePlayerName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        String name = op.getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    // ── Reflection helpers ─────────────────────────────────────────────────

    private Object invoke(Object target, Method method, Object... args) {
        if (method == null) return null;
        try {
            return method.invoke(target, args);
        } catch (Exception e) {
            return null;
        }
    }

    private Object invokeStatic(Method method, Object... args) {
        if (method == null) return null;
        try {
            return method.invoke(null, args);
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
            return false;
        }
    }

    private boolean invokeVoid(Object target, Method method, Object... args) {
        if (method == null) return false;
        try {
            method.invoke(target, args);
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[HaizCore/Teams] Erro ao executar " + method.getName() + ": " + e.getMessage());
            return false;
        }
    }

    private int invokeInt(Object target, Method method, Object... args) {
        if (method == null) return 0;
        try {
            Object result = method.invoke(target, args);
            if (result instanceof Number n) return n.intValue();
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
