package com.phasetranscrystal.fpsmatch.core;

import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.phasetranscrystal.fpsmatch.core.data.SpawnPointData;
import com.phasetranscrystal.fpsmatch.core.data.TabData;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MapTeams {
    protected final ServerLevel level;
    protected final BaseMap map;
    private final Map<String,BaseTeam> teams = new HashMap<>();
    private final BaseTeam spectatorTeam = this.addTeam("spectator",-1);
    private final Map<String,List<UUID>> unableToSwitch = new HashMap<>();
    public final Map<UUID,Component> playerName = new HashMap<>();

    /**
     * 构造函数，用于创建 MapTeams 对象
     * @param level 服务器级别。
     * @param team 包含团队名称和玩家限制的映射。
     * @param map 地图对象。
     */
    public MapTeams(ServerLevel level, Map<String, Integer> team, BaseMap map) {
        this.level = level;
        this.map = map;
        team.forEach(this::addTeam);
    }

    /**
     * 构造函数，用于创建 MapTeams 对象
     * @param level 服务器级别。
     * @param map 地图对象。
     */
    public MapTeams(ServerLevel level,BaseMap map){
        this.level = level;
        this.map = map;
    }

    /**
     * 根据队伍名称获取指定队伍的出生点数据。
     * <p>
     * 如果指定的队伍不存在，则返回 null。
     *
     * @param team 队伍名称
     * @return 指定队伍的出生点数据列表，如果队伍不存在则返回 null
     */
    @Nullable
    public List<SpawnPointData> getSpawnPointsByTeam(String team){
        BaseTeam t = this.teams.getOrDefault(team,null);
        if(t == null) return null;
        return t.getSpawnPointsData();
    }

    /**
     * 获取所有队伍的出生点数据。
     * <p>
     * 返回一个 Map，其中键为队伍名称，值为对应的出生点数据列表。
     *
     * @return 包含所有队伍出生点数据的 Map
     */
    public Map<String,List<SpawnPointData>> getAllSpawnPoints(){
        Map<String,List<SpawnPointData>> data = new HashMap<>();
        this.teams.forEach((n,t)-> data.put(n,t.getSpawnPointsData()));
        return data;
    }

    /**
     * 交换两个队伍的攻击方和防守方状态。
     * <p>
     * 该方法会交换两个队伍的玩家数据、得分、连败次数、补偿因子以及暂停次数和状态。
     * 如果指定的攻击方或防守方队伍不存在，则不会执行任何操作。
     *
     * @param attackTeamName 攻击方队伍名称
     * @param defendTeamName 防守方队伍名称
     */
    public void switchAttackAndDefend(String attackTeamName, String defendTeamName) {
        BaseTeam attackTeam = this.getTeamByName(attackTeamName);
        BaseTeam defendTeam = this.getTeamByName(defendTeamName);
        if(attackTeam == null || defendTeam == null) return;

        //交换玩家
        Map<UUID, PlayerData> tempPlayers = new HashMap<>(attackTeam.getPlayers());
        attackTeam.resetAllPlayers(this.level, defendTeam.getPlayers());
        defendTeam.resetAllPlayers(this.level, tempPlayers);

        // 交换得分
        int tempScore = attackTeam.getScores();
        attackTeam.setScores(defendTeam.getScores());
        defendTeam.setScores(tempScore);

        attackTeam.setLoseStreak(0);
        attackTeam.setCompensationFactor(0);
        defendTeam.setCompensationFactor(0);
        defendTeam.setLoseStreak(0);

        // 交换暂停次数
        int tempP = attackTeam.getPauseTime();
        boolean tempN = attackTeam.needPause();
        attackTeam.setPauseTime(defendTeam.getPauseTime());
        attackTeam.setNeedPause(defendTeam.needPause());
        defendTeam.setPauseTime(tempP);
        defendTeam.setNeedPause(tempN);
    }

    /**
     * 获取无法切换的队伍列表。
     * <p>
     * 返回一个 Map，键为队伍名称，值为无法切换的玩家 UUID 列表。
     *
     * @return 无法切换的队伍列表
     */
    public Map<String, List<UUID>> getUnableToSwitch() {
        return unableToSwitch;
    }

    /**
     * 将提供的出生点数据批量添加到对应队伍中。
     * <p>
     * 如果队伍存在，则将提供的出生点数据列表添加到队伍的出生点数据中。
     *
     * @param data 包含队伍名称和出生点数据列表的 Map
     */
    public void putAllSpawnPoints(Map<String,List<SpawnPointData>> data){
        data.forEach((n,list)->{
            if (teams.containsKey(n)){
                teams.get(n).addAllSpawnPointData(list);
            }
        });
    }

    /**
     * 为所有队伍随机分配出生点。
     * <p>
     * 遍历所有队伍，并调用队伍的随机出生点分配方法。
     */
    public void setTeamsSpawnPoints(){
            this.teams.forEach(((s, t) -> t.randomSpawnPoints()));
    }

    /**
     * 为指定队伍添加出生点数据。
     * <p>
     * 如果指定的队伍不存在，则不会执行任何操作。
     *
     * @param teamName 队伍名称
     * @param data 出生点数据
     */
    public void defineSpawnPoint(String teamName, SpawnPointData data) {
        BaseTeam team = this.teams.getOrDefault(teamName, null);
        if (team == null) return;
        team.addSpawnPointData(data);
    }

    /**
     * 重置指定队伍的出生点数据。
     * <p>
     * 如果指定的队伍不存在，则不会执行任何操作。
     *
     * @param teamName 队伍名称
     */
    public void resetSpawnPoints(String teamName){
        BaseTeam team = this.teams.getOrDefault(teamName, null);
        if (team == null) return;
        team.resetSpawnPointData();
    }

    /**
     * 重置所有队伍的出生点数据。
     * <p>
     * 遍历所有队伍，并调用队伍的出生点数据重置方法。
     */
    public void resetAllSpawnPoints(){
        this.teams.forEach((s,t)-> t.resetSpawnPointData());
    }

    /**
     * 添加一个新队伍。
     * <p>
     * 创建一个新的队伍，并设置队伍的名称、颜色、是否允许友军伤害等属性。
     * 队伍名称会根据游戏类型、地图名称和队伍名称进行固定格式化。
     *
     * @param teamName 队伍名称
     * @param limit 队伍人数上限
     */
    public BaseTeam addTeam(String teamName,int limit){
        String fixedName = map.getGameType()+"_"+map.getMapName()+"_"+teamName;
        PlayerTeam playerteam = Objects.requireNonNullElseGet(this.level.getScoreboard().getPlayersTeam(fixedName), () -> this.level.getScoreboard().addPlayerTeam(fixedName));
        BaseTeam team = new BaseTeam(map.getGameType(),map.getMapName(),teamName,limit,playerteam);
        this.teams.put(teamName, team);
        return team;
    }

    /**
     * 设置队伍的名称颜色。
     * <p>
     * 根据游戏类型、地图名称和队伍名称获取或创建队伍，并设置其颜色。
     *
     * @param map 地图信息
     * @param teamName 队伍名称
     * @param color 队伍名称颜色
     */
    public void setTeamNameColor(BaseMap map, String teamName, ChatFormatting color){
        String fixedName = map.getGameType()+"_"+map.getMapName()+"_"+teamName;
        PlayerTeam playerteam = Objects.requireNonNullElseGet(this.level.getScoreboard().getPlayersTeam(fixedName), () -> this.level.getScoreboard().addPlayerTeam(fixedName));
        playerteam.setColor(color);
    }

    /**
     * 删除一个队伍。
     * <p>
     * 如果指定的队伍不存在，则不会执行任何操作。
     *
     * @param team 要删除的队伍
     */
    public void delTeam(PlayerTeam team){
        if(!checkTeam(team.getName())) return;
        this.teams.remove(team.getName());
        this.level.getScoreboard().removePlayerTeam(team);
    }

    /**
     * 根据玩家对象获取其所属的队伍。
     * <p>
     * 遍历所有队伍，检查是否有队伍包含该玩家的 UUID，返回对应的队伍。
     * 如果玩家未加入任何队伍，则返回 null。
     *
     * @param player 玩家对象
     * @return 玩家所属的队伍，如果未找到则返回 null
     */
    @Nullable
    public BaseTeam getTeamByPlayer(Player player) {
        AtomicReference<BaseTeam> baseTeamAtomicReference = new AtomicReference<>();
        this.teams.forEach(((s, team) -> {
            if (team.hasPlayer(player.getUUID())) {
                baseTeamAtomicReference.set(team);
            };
        }));
        return baseTeamAtomicReference.get();
    }

    /**
     * 根据玩家 UUID 获取其所属的队伍。
     * <p>
     * 遍历所有队伍，检查是否有队伍包含该玩家的 UUID，返回对应的队伍。
     * 如果玩家未加入任何队伍，则返回 null。
     *
     * @param player 玩家 UUID
     * @return 玩家所属的队伍，如果未找到则返回 null
     */
    @Nullable
    public BaseTeam getTeamByPlayer(UUID player) {
        AtomicReference<BaseTeam> baseTeamAtomicReference = new AtomicReference<>();
        this.teams.forEach(((s, team) -> {
            if (team.hasPlayer(player)) {
                baseTeamAtomicReference.set(team);
            };
        }));
        return baseTeamAtomicReference.get();
    }

    /**
     * 获取所有已加入队伍的玩家 UUID 列表。
     * <p>
     * 遍历所有队伍，收集所有队伍中的玩家 UUID。
     *
     * @return 包含所有已加入队伍的玩家 UUID 的列表
     */
    public List<UUID> getJoinedPlayers() {
        List<UUID> uuids = new ArrayList<>();
        this.teams.values().forEach((t) -> uuids.addAll(t.getPlayerList()));
        return uuids;
    }

    public List<UUID> getSpecPlayers(){
        return this.spectatorTeam.getPlayerList();
    }

    /**
     * 重置所有队伍的“存活状态”。
     * <p>
     * 遍历所有队伍，调用队伍的重置存活状态方法。
     */
    public void resetLivingPlayers() {
        this.teams.values().forEach(BaseTeam::resetLiving);
    }

    /**
     * 让玩家加入指定的队伍。
     * <p>
     * 如果指定的队伍不存在，则不会执行任何操作。
     *
     * @param player 玩家对象
     * @param teamName 队伍名称
     */
    private void playerJoin(ServerPlayer player, String teamName) {
        this.teams.getOrDefault(teamName, this.spectatorTeam).join(player);
    }

    /**
     * 让玩家加入指定的队伍，并离开当前队伍。
     * <p>
     * 如果指定的队伍不存在或队伍已满，则发送提示信息并让玩家离开当前队伍。
     *
     * @param teamName 队伍名称
     * @param player 玩家对象
     */
    public void joinTeam(String teamName, ServerPlayer player) {
        leaveTeam(player);
        if (checkTeam(teamName) && !this.testTeamIsFull(teamName)) {
            this.playerJoin(player, teamName);
            this.playerName.put(player.getUUID(), player.getDisplayName());
            player.displayClientMessage(Component.translatable("fpsm.map.cs.join.team", teamName).withStyle(ChatFormatting.GREEN), false);
        } else {
            player.sendSystemMessage(Component.literal("[FPSM] 队伍已满或未找到目标队伍，当前队伍已离队!"));
        }
    }

    /**
     * 检查指定的队伍是否存在。
     * <p>
     * 如果队伍不存在，则发送提示信息。
     *
     * @param teamName 队伍名称
     * @return 如果队伍存在返回 true，否则返回 false
     */
    public boolean checkTeam(String teamName) {
        if (this.teams.containsKey(teamName)) {
            return true;
        } else {
            this.level.getServer().sendSystemMessage(Component.literal("[FPSM] 不合法的队伍名 -?>" + teamName + " 检查队伍名是否在FPSM中被定义。"));
            return false;
        }
    }

    /**
     * 检查指定的队伍是否已满。
     * <p>
     * 如果队伍不存在，则返回 false。
     *
     * @param teamName 队伍名称
     * @return 如果队伍已满返回 true，否则返回 false
     */
    public boolean testTeamIsFull(String teamName) {
        BaseTeam team = teams.get(teamName);
        if (team == null) return false;
        return team.getPlayerLimit() < team.getPlayerList().size() || team.getPlayerLimit() == -1;
    }

    /**
     * 获取所有队伍的列表。
     * <p>
     * 返回一个包含所有队伍对象的列表。
     *
     * @return 所有队伍的列表
     */
    public List<BaseTeam> getTeams() {
        return new ArrayList<>(teams.values().stream().toList());
    }

    public BaseTeam getSpectatorTeam() {
        return spectatorTeam;
    }
    /**
     * 获取所有队伍的名称列表。
     * <p>
     * 返回一个包含所有队伍名称的列表。
     *
     * @return 所有队伍的名称列表
     */
    public List<String> getTeamsName() {
        return teams.keySet().stream().toList();
    }

    /**
     * 根据队伍名称获取队伍对象。
     * <p>
     * 如果队伍不存在，则返回 null。
     *
     * @param teamName 队伍名称
     * @return 队伍对象，如果未找到则返回 null
     */
    @Nullable
    public BaseTeam getTeamByName(String teamName) {
        if (checkTeam(teamName)) return this.teams.get(teamName);
        return null;
    }

    /**
     * 根据队伍的完整名称（固定格式名称）获取队伍对象。
     * <p>
     * 遍历所有队伍，检查是否有队伍的固定名称与指定名称匹配。
     * 如果未找到，则返回 null。
     *
     * @param teamName 队伍的完整名称
     * @return 队伍对象，如果未找到则返回 null
     */
    @Nullable
    public BaseTeam getTeamByComplexName(String teamName) {
        AtomicReference<BaseTeam> team = new AtomicReference<>();
        teams.forEach((s, t) -> {
            if (t.getFixedName().equals(teamName)) {
                team.set(t);
            }
        });
        return team.get();
    }

    /**
     * 重置所有队伍的状态。
     * <p>
     * 包括重置所有队伍的伤害数据、存活状态、得分、玩家列表、连败次数、补偿因子和暂停时间。
     */
    public void reset() {
        this.resetAllHurtData();
        this.resetLivingPlayers();
        this.teams.forEach((name, team) -> {
            team.setScores(0);
            team.getPlayers().clear();
            team.setLoseStreak(0);
            team.setCompensationFactor(0);
            team.setPauseTime(0);
        });
        this.unableToSwitch.clear();
        this.playerName.clear();
    }

    /**
     * 让玩家离开当前所在的队伍。
     * <p>
     * 遍历所有队伍，调用队伍的离开方法移除玩家，并从玩家名称映射中移除该玩家的 UUID。
     *
     * @param player 玩家对象
     */
    public void leaveTeam(ServerPlayer player) {
        this.spectatorTeam.leave(player);
        this.teams.values().forEach((t) -> t.leave(player));
        this.playerName.remove(player.getUUID());
    }

    /**
     * 获取所有队伍的存活玩家数据。
     * <p>
     * 返回一个 Map，键为队伍名称，值为该队伍存活玩家的 UUID 列表。
     * 如果某个队伍没有存活玩家，则不会将其加入返回的 Map 中。
     *
     * @return 包含所有队伍存活玩家数据的 Map
     */
    public Map<BaseTeam, List<UUID>> getTeamsLiving() {
        Map<BaseTeam, List<UUID>> teamsLiving = new HashMap<>();
        teams.forEach((s, t) -> {
            List<UUID> list = t.getLivingPlayers();
            if (!list.isEmpty()) {
                teamsLiving.put(t, list);
            }
        });
        return teamsLiving;
    }

    /**
     * 根据玩家 UUID 获取其 Tab 数据。
     * <p>
     * 遍历所有队伍，查找包含该玩家的队伍并获取其 Tab 数据。
     * 如果玩家未加入任何队伍，则返回 null。
     *
     * @param player 玩家 UUID
     * @return 玩家的 Tab 数据，如果未找到则返回 null
     */
    @Nullable
    public TabData getTabData(UUID player) {
        AtomicReference<TabData> data = new AtomicReference<>();
        teams.values().forEach((team) -> {
            if (team.hasPlayer(player)) {
                data.set(team.getPlayerTabData(player));
            }
        });
        return data.get();
    }

    /**
     * 根据玩家对象获取其 Tab 数据。
     * <p>
     * 通过玩家 UUID 获取其所属队伍的 Tab 数据。
     * 如果玩家未加入任何队伍，则返回 null。
     *
     * @param player 玩家对象
     * @return 玩家的 Tab 数据，如果未找到则返回 null
     */
    @Nullable
    public TabData getTabData(Player player) {
        BaseTeam team = getTeamByPlayer(player);
        if (team != null) {
            if (team.hasPlayer(player.getUUID())) {
                return team.getPlayerTabData(player.getUUID());
            }
        }
        return null;
    }

    /**
     * 获取与玩家同队的所有玩家 UUID 列表。
     * <p>
     * 如果玩家未加入任何队伍，则返回空列表。
     *
     * @param player 玩家对象
     * @return 同队玩家的 UUID 列表
     */
    public List<UUID> getSameTeamPlayerUUIDs(Player player) {
        BaseTeam team = getTeamByPlayer(player);
        List<UUID> uuids = new ArrayList<>();
        if (team != null) {
            if (team.hasPlayer(player.getUUID())) {
                uuids.addAll(team.getPlayerList());
            }
        }
        return uuids;
    }

    /**
     * 添加玩家的伤害数据。
     * <p>
     * 根据攻击者和目标玩家的 UUID，记录伤害值。
     * 如果攻击者未加入任何队伍，则不会记录任何数据。
     *
     * @param attackerId 攻击者玩家对象
     * @param targetId 目标玩家的 UUID
     * @param damage 伤害值
     */
    public void addHurtData(Player attackerId, UUID targetId, float damage) {
        BaseTeam team = getTeamByPlayer(attackerId);
        if (team != null) {
            TabData data = team.getPlayerTabData(attackerId.getUUID());
            if (data != null) {
                data.addDamageData(targetId, damage);
            }
        }
    }

    /**
     * 获取当前游戏中伤害输出最高的玩家 UUID。
     * <p>
     * 遍历所有队伍的伤害数据，计算每个玩家的总伤害输出，返回最高伤害输出的玩家 UUID。
     * 如果没有玩家造成伤害，则返回 null。
     *
     * @return 伤害输出最高的玩家 UUID，如果没有则返回 null
     */
    @Nullable
    public UUID getDamageMvp() {
        Map<UUID, Float> damageMap = new HashMap<>();

        this.getLivingHurtData().forEach((attackerId, attackerDamageMap) -> attackerDamageMap.forEach((targetId, damage) -> damageMap.merge(attackerId, damage, Float::sum)));

        UUID mvpId = null;
        float highestDamage = 0;

        for (Map.Entry<UUID, Float> entry : damageMap.entrySet()) {
            if (mvpId == null || entry.getValue() > highestDamage) {
                mvpId = entry.getKey();
                highestDamage = entry.getValue();
            }
        }
        return mvpId;
    }

    /**
     * 获取所有玩家的 Tab 数据。
     * <p>
     * 返回一个 Map，键为玩家 UUID，值为对应的 Tab 数据。
     *
     * @return 包含所有玩家 Tab 数据的 Map
     */
    public Map<UUID, TabData> getAllTabData() {
        Map<UUID, TabData> data = new HashMap<>();
        this.teams.forEach((s, t) -> t.getPlayersTabData().forEach((tab) -> data.put(tab.getOwner(), tab)));
        return data;
    }

    /**
     * 获取游戏的 MVP 玩家数据。
     * <p>
     * 根据队伍的得分、击杀数、助攻数和伤害输出计算 MVP 玩家。
     * 如果没有玩家符合条件，则返回 null。
     *
     * @param winnerTeam 获胜队伍
     * @return MVP 玩家数据，如果没有则返回 null
     */
    public RawMVPData getGameMvp(BaseTeam winnerTeam) {
        UUID mvpId = null;
        int highestScore = 0;
        UUID damageMvpId = this.getDamageMvp();
        for (TabData tabData : winnerTeam.getPlayerDataTemp()) {
            int kills = tabData.getKills() * 2;
            int assists = tabData.getAssists();
            int score = kills + assists;
            if (tabData.getOwner().equals(damageMvpId)) {
                score += 2;
            }

            if (mvpId == null || score > highestScore) {
                mvpId = tabData.getOwner();
                highestScore = score;
            }
        }

        return mvpId == null ? null : new RawMVPData(mvpId, "MVP");
    }

    /**
     * 开始新一轮游戏。
     * <p>
     * 重置所有玩家的伤害数据和存活状态，并保存队伍的临时数据。
     */
    public void startNewRound() {
        this.resetAllHurtData();
        this.resetLivingPlayers();
        this.teams.values().forEach(BaseTeam::saveTemp);
    }

    /**
     * 检查当前是否是第一轮游戏。
     * <p>
     * 如果所有队伍的得分总和为 0，则认为是第一轮。
     *
     * @return 如果是第一轮返回 true，否则返回 false
     */
    public boolean isFirstRound() {
        AtomicInteger flag = new AtomicInteger();
        teams.values().forEach((team) -> flag.addAndGet(team.getScores()));
        return flag.get() == 0;
    }

    /**
     * 获取当前轮次的 MVP 玩家数据。
     * <p>
     * 根据击杀数、助攻数和伤害输出计算 MVP 玩家。
     * 如果是第一轮，则直接调用 {@link #getGameMvp(BaseTeam)} 方法。
     *
     * @param winnerTeam 获胜队伍
     * @return MVP 玩家数据
     */
    public RawMVPData getRoundMvpPlayer(BaseTeam winnerTeam) {
        RawMVPData mvpId = null;
        int highestScore = 0;
        UUID damageMvpId = this.getDamageMvp();
        if (!teams.containsValue(winnerTeam)) return null;

        if (isFirstRound()) {
            mvpId = this.getGameMvp(winnerTeam);
        } else {
            for (PlayerData data : winnerTeam.getPlayersData()) {
                TabData temp = winnerTeam.getPlayerTabTemp(data.getOwner());
                int kills = data.getTabData().getKills() - temp.getKills();
                int assists = data.getTabData().getAssists() - temp.getAssists();
                int score = kills * 2 + assists;
                if (data.getOwner().equals(damageMvpId)) {
                    score += 2;
                }

                if (mvpId == null || score > highestScore) {
                    mvpId = new RawMVPData(data.getOwner(), "MVP");
                    highestScore = score;
                }
            }
        }

        if (mvpId != null) {
            PlayerData data = winnerTeam.getPlayerData(mvpId.uuid());
            if (data != null) {
                data.getTabData().addMvpCount(1);
            } else {
                System.out.println("error : MVP Player Data is null -> " + mvpId.uuid.toString());
            }
        }

        return mvpId;
    }

    /**
     * 获取所有存活玩家的伤害数据。
     * <p>
     * 返回一个 Map，键为玩家 UUID，值为该玩家对其他玩家造成的伤害数据。
     *
     * @return 包含所有存活玩家伤害数据的 Map
     */
    public Map<UUID, Map<UUID, Float>> getLivingHurtData() {
        Map<UUID,Map<UUID,Float>> hurtData = new HashMap<>();
        teams.values().forEach((t)-> t.getPlayersTabData().forEach((data)-> hurtData.put(data.getOwner(),data.getDamageData())));
        return hurtData;
    }

    /**
     * 重置所有玩家的伤害数据。
     * <p>
     * 遍历所有队伍，调用队伍的 Tab 数据清理方法，清空玩家的伤害记录。
     */
    public void resetAllHurtData() {
        this.teams.values().forEach((t) -> t.getPlayersTabData().forEach(TabData::clearDamageData));
    }

    /**
     * 用于表示 MVP 数据的记录类。
     * <p>
     * 包含玩家的 UUID 和获得 MVP 的原因。
     *
     * @param uuid 玩家的 UUID
     * @param reason 获得 MVP 的原因
     */
    public record RawMVPData(UUID uuid, String reason) {
    }

}
