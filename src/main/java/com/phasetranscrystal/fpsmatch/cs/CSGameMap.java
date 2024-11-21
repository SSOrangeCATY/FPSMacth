package com.phasetranscrystal.fpsmatch.cs;

import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.core.*;
import com.phasetranscrystal.fpsmatch.core.data.AreaData;
import com.phasetranscrystal.fpsmatch.core.data.ShopData;
import com.phasetranscrystal.fpsmatch.core.data.SpawnPointData;
import com.phasetranscrystal.fpsmatch.core.data.save.FileHelper;
import com.phasetranscrystal.fpsmatch.core.event.PlayerKillOnMapEvent;
import com.phasetranscrystal.fpsmatch.core.map.BlastModeMap;
import com.phasetranscrystal.fpsmatch.core.map.ShopMap;
import com.phasetranscrystal.fpsmatch.net.BombDemolitionProgressS2CPacket;
import com.phasetranscrystal.fpsmatch.net.CSGameSettingsS2CPacket;
import com.phasetranscrystal.fpsmatch.net.ShopStatesS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


@Mod.EventBusSubscriber(modid = FPSMatch.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CSGameMap extends BaseMap implements BlastModeMap , ShopMap {
    public static final int WINNER_ROUND = 13;
    public static final int PAUSE_TIME = 2400;
    public static final int WINNER_WAITING_TIME = 160;
    public static final int WARM_UP_TIME = 1200;
    private final int waittingTime = 400;
    private int currentPauseTime = 0;
    private final int roundTimeLimit = 115 * 20;
    private int currentRoundTime = 0;
    private boolean isError = false;
    private boolean isPause = false;
    private boolean isWaiting = false;
    private boolean isWarmTime = false;
    private boolean isWaitingWinner = false;
    private final Map<String,Integer> teamScores = new HashMap<>();
    private int isBlasting = 0; // 是否放置炸弹 0 = 未放置 | 1 = 已放置 | 2 = 已拆除
    private boolean isExploded = false; // 炸弹是否爆炸
    private final List<AreaData> bombAreaData = new ArrayList<>();
    private String blastTeam;
    private final FPSMShop shop;

    public CSGameMap(ServerLevel serverLevel,String mapName) {
        super(serverLevel,mapName);
        this.shop = new FPSMShop(mapName);
    }

    public Map<String,Integer> getTeams(){
        Map<String,Integer> teams = new HashMap<>();
        teams.put("ct",5);
        teams.put("t",5);
        this.setBlastTeam("t");
        return teams;
    }

    @Override
    public FPSMShop getShop() {
        return shop;
    }

    @Override
    public @Nullable ShopData defineShopData() {
        return null;
    }

    @Override
    public void tick() {
        if(isStart){
            if (!checkPauseTime() & !checkWarmUpTime() & !checkWaitingTime()) {
                if(!isRoundTimeEnd()){
                    if(!this.isDebug()){
                        switch (this.isBlasting()){
                            case 1 : this.checkBlastingVictory(); break;
                            case 2 : if(!isWaitingWinner) this.roundVictory("ct",WinnerReason.DEFUSE_BOMB); break;
                            default : this.checkRoundVictory(); break;
                        }

                        if(this.isWaitingWinner){
                            checkWinnerTime();

                            if(this.currentPauseTime >= WINNER_WAITING_TIME){
                                this.startNewRound();
                            }
                        }
                    }
                }else{
                    if(!checkWinnerTime()){
                        this.roundVictory("ct",WinnerReason.TIME_OUT);
                    }else if(this.currentPauseTime >= WINNER_WAITING_TIME){
                        this.startNewRound();
                    }
                }
            }
        }
    }

    public void startGame(){
        AtomicBoolean checkFlag = new AtomicBoolean(true);
        this.getMapTeams().getJoinedPlayers().forEach((uuid -> {
            ServerPlayer player = this.getServerLevel().getServer().getPlayerList().getPlayer(uuid);
            if (player != null){
                BaseTeam team = this.getMapTeams().getTeamByPlayer(player);
                if(team == null) checkFlag.set(false);
            }else{
                checkFlag.set(false);
            }
        }));

        if (!checkFlag.get() && !this.isError) return;
        this.getServerLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true,null);
        this.getServerLevel().getGameRules().getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(true,null);
        this.getServerLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,null);
        this.getServerLevel().getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,null);
        this.getServerLevel().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
        this.getServerLevel().getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false,null);
        this.getServerLevel().getServer().setDifficulty(Difficulty.HARD,true);
        this.getMapTeams().getJoinedPlayers().forEach((uuid -> {
            this.setPlayerMoney(uuid,800);
        }));
        startNewRound();
    }

    public boolean canRestTime(){
        return !this.isPause && !this.isWarmTime && !this.isWaiting && !this.isWaitingWinner;
    }
    public boolean checkPauseTime(){
        if(this.isPause && currentPauseTime < PAUSE_TIME){
            this.currentPauseTime++;
        }else{
            if(this.canRestTime()) currentPauseTime = 0;
            isPause = false;
        }
        return this.isPause;
    }

    public boolean checkWarmUpTime(){
        if(this.isWarmTime && currentPauseTime < WARM_UP_TIME){
            this.currentPauseTime++;
        }else {
            if(this.canRestTime()) currentPauseTime = 0;
            isWarmTime = false;
        }
        return this.isWarmTime;
    }

    public boolean checkWaitingTime(){
        if(this.isWaiting && currentPauseTime < waittingTime){
            this.currentPauseTime++;
        }else {
            if(this.canRestTime()) currentPauseTime = 0;
            isWaiting = false;
            this.getMapTeams().getJoinedPlayers().forEach((uuid -> {
                ServerPlayer serverPlayer = this.getServerLevel().getServer().getPlayerList().getPlayer(uuid);
                if(serverPlayer != null){
                    FPSMatch.INSTANCE.send(PacketDistributor.PLAYER.with(()-> serverPlayer), new ShopStatesS2CPacket(false));
                }
            }));

        }
        return this.isWaiting;
    }

    public boolean checkWinnerTime(){
        if(this.isWaitingWinner && currentPauseTime < WINNER_WAITING_TIME){
            this.currentPauseTime++;
        }else{
            if(this.canRestTime()) currentPauseTime = 0;
        }
        return this.isWaitingWinner;
    }

    public void checkRoundVictory(){
        if(isWaitingWinner) return;
        Map<String, List<UUID>> teamsLiving = this.getMapTeams().getTeamsLiving();
        if(teamsLiving.size() == 1){
            String winnerTeam = teamsLiving.keySet().stream().findFirst().get();
            this.roundVictory(winnerTeam,WinnerReason.ACED);
        }
    }
    public void checkBlastingVictory(){
        if(isWaitingWinner) return;
        if(this.isExploded()) {
            this.roundVictory("t",WinnerReason.DETONATE_BOMB);
        }
    }
    public boolean isRoundTimeEnd(){
        if(this.isBlasting() > 0){
            this.currentRoundTime = -1;
            return false;
        }
        if(this.currentRoundTime < this.roundTimeLimit){
            this.currentRoundTime++;
        }
        return this.currentRoundTime >= this.roundTimeLimit;
    }

    private void roundVictory(String winnerTeamName, WinnerReason reason) {
        if(this.getMapTeams().checkTeam(winnerTeamName)){
            if(isWaitingWinner) return;
            this.isWaitingWinner = true;

            // 获取当前队伍分数
            int currentScore = this.teamScores.getOrDefault(winnerTeamName, 0);
            // 更新分数
            this.teamScores.put(winnerTeamName, currentScore + 1);

            // 获取胜利队伍和失败队伍列表
            BaseTeam winnerTeam = this.getMapTeams().getTeamByName(winnerTeamName);
            List<BaseTeam> lostTeams = this.getMapTeams().getTeams();
            lostTeams.remove(winnerTeam);

            // 处理胜利经济奖励
            int reward = reason.winMoney;

            // 对于拆除炸弹的胜利，额外奖励800
            if (reason == WinnerReason.DEFUSE_BOMB) {
                reward += 800;
            }

            if (winnerTeam == null) return;
            // 遍历所有玩家，更新经济
            int finalReward = reward;
            this.getMapTeams().getJoinedPlayers().forEach(uuid -> {
                // 如果是胜利队伍的玩家
                if (winnerTeam.getPlayers().contains(uuid)) {
                    this.addPlayerMoney(uuid, finalReward);
                } else { // 失败队伍的玩家
                    lostTeams.forEach((lostTeam)->{
                        if (lostTeam.getPlayers().contains(uuid)) {
                            int defaultEconomy = 1400;
                            int compensation = 500;
                            int compensationFactor = lostTeam.getCompensationFactor();
                            // 计算失败补偿
                            int loss = defaultEconomy + compensation * compensationFactor;
                            if(!Objects.requireNonNull(lostTeam.getPlayerData(uuid)).getTabData().isLiving()){
                                this.addPlayerMoney(uuid, loss);
                            }
                        }
                    });
                }
            });
            checkLoseStreaks(winnerTeamName);
            this.getShop().syncShopMoneyData();
        }
    }

    private void checkLoseStreaks(String winnerTeam) {
        // 遍历所有队伍，检查连败情况
        this.getMapTeams().getTeams().forEach(team -> {
            if (team.getName().equals(winnerTeam)) {
                // 胜利，连败次数重置
                team.setLoseStreak(0);
            } else {
                // 失败，连败次数加1
                team.setLoseStreak(team.getLoseStreak() + 1);
            }

            // 更新补偿因数
            int compensationFactor = team.getCompensationFactor();
            if (team.getLoseStreak() > 0) {
                // 连败，补偿因数加1
                compensationFactor = Math.min(compensationFactor + 1, 4);
            }
            team.setCompensationFactor(compensationFactor);
        });
    }


    public void startNewRound() {
        this.isStart = true;
        this.isWaiting = true;
        this.isWaitingWinner = false;
        this.cleanupMap();
        this.getMapTeams().setTeamsSpawnPoints();
        this.getMapTeams().resetLivingPlayers();
        this.getMapTeams().getJoinedPlayers().forEach((uuid -> {
            ServerPlayer serverPlayer = this.getServerLevel().getServer().getPlayerList().getPlayer(uuid);
            if(serverPlayer != null){
                FPSMatch.INSTANCE.send(PacketDistributor.PLAYER.with(()-> serverPlayer), new ShopStatesS2CPacket(true));
                serverPlayer.addEffect(new MobEffectInstance(MobEffects.SATURATION,-1,2,false,false,false));
                FPSMatch.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new BombDemolitionProgressS2CPacket(0));
            }
        }));

    }

    @Override
    public void victory() {
        resetGame();
    }
    @Override
    public boolean victoryGoal() {
        AtomicBoolean isVictory = new AtomicBoolean(false);
        teamScores.values().forEach((integer -> {
            isVictory.set(integer >= WINNER_ROUND);
        }));
        return isVictory.get() && !this.isDebug();
    }

    @Override
    public void cleanupMap() {
        this.setBlasting(0);
        this.setExploded(false);
        this.currentRoundTime = 0;
        this.currentPauseTime = 0;
        this.getMapTeams().setTeamsSpawnPoints();
        this.getMapTeams().getJoinedPlayers().forEach((uuid -> {
            ServerPlayer player =  this.getServerLevel().getServer().getPlayerList().getPlayer(uuid);
            if(player != null){
                player.heal(player.getMaxHealth());
                player.setGameMode(GameType.ADVENTURE);
                this.clearPlayerInventory(player);
                this.teleportPlayerToReSpawnPoint(player);
            }
        }));

    }

    public void teleportPlayerToReSpawnPoint(ServerPlayer player){
        BaseTeam team = this.getMapTeams().getTeamByPlayer(player);
        if (team == null) return;
        SpawnPointData data = Objects.requireNonNull(team.getPlayerData(player.getUUID())).getSpawnPointsData();
        BlockPos pos = data.getPosition();
        float f = Mth.wrapDegrees(data.getYaw());
        float f1 = Mth.wrapDegrees(data.getPitch());
        player.teleportTo(Objects.requireNonNullElse(this.getServerLevel().getServer().getLevel(data.getDimension()),this.getServerLevel()),pos.getX(),pos.getY(),pos.getZ(),f, f1);
    }

    public void clearPlayerInventory(ServerPlayer player){
        if(!Objects.requireNonNull(Objects.requireNonNull(this.getMapTeams().getTeamByPlayer(player)).getPlayerTabData(player.getUUID())).isLiving()){
            player.getInventory().clearOrCountMatchingItems((p_180029_) -> true, -1, player.inventoryMenu.getCraftSlots());
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.slotsChanged(player.getInventory());
        }
    }

    public void resetGame() {
        this.getMapTeams().getJoinedPlayers().forEach((uuid -> {
            ServerPlayer player =  this.getServerLevel().getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.removeAllEffects();
            }
        }));
        this.teamScores.clear();
        this.isError = false;
        this.isStart = false;
        this.isWaiting = false;
        this.isWaitingWinner = false;
        this.isWarmTime = false;
        this.currentRoundTime = 0;
        this.currentPauseTime = 0;
        this.isBlasting = 0;
        this.isExploded = false;
        this.getMapTeams().reset();
    }


    public final void setBlastTeam(String team){
        this.blastTeam = this.getGameType()+"_"+this.getMapName()+"_"+team;
    }

    public boolean checkCanPlacingBombs(String team){
        if(this.blastTeam == null) return false;
        return this.blastTeam.equals(team);
    }

    public boolean checkPlayerIsInBombArea(Player player){
        AtomicBoolean a = new AtomicBoolean(false);
        this.bombAreaData.forEach(area->{
            if(!a.get()) a.set(area.isPlayerInArea(player));
        });
        return a.get();
    }

    public void addBombArea(AreaData area){
        this.bombAreaData.add(area);
    }

    public List<AreaData> getBombAreaData() {
        return bombAreaData;
    }

    public void setBlasting(int blasting) {
        isBlasting = blasting;
    }
    public void setExploded(boolean exploded) {
        isExploded = exploded;
    }

    public int isBlasting() {
        return isBlasting;
    }

    public boolean isExploded() {
        return isExploded;
    }
    public void syncToClient() {
        CSGameSettingsS2CPacket packet = new CSGameSettingsS2CPacket(this.teamScores.getOrDefault("ct",0),this.teamScores.getOrDefault("t",0), this.currentPauseTime,this.currentRoundTime,this.isDebug(),this.isStart,this.isError,this.isPause,this.isWaiting,this.isWaitingWinner);
        this.getMapTeams().getJoinedPlayers().forEach((uuid -> {
            ServerPlayer player = (ServerPlayer) this.getServerLevel().getPlayerByUUID(uuid);
            if(player != null){
                FPSMatch.INSTANCE.send(PacketDistributor.PLAYER.with(()-> player), packet);
            }
        }));
    }

    @SubscribeEvent
    public static void onPlayerKillOnMap(PlayerKillOnMapEvent event){
        if(event.getBaseMap() instanceof CSGameMap csGameMap){
            BaseTeam killerTeam = csGameMap.getMapTeams().getTeamByPlayer(event.getKiller());
            BaseTeam deadTeam = csGameMap.getMapTeams().getTeamByPlayer(event.getDead());
            if(killerTeam == null || deadTeam == null) return;
            if (killerTeam.getName().equals(deadTeam.getName())){
                csGameMap.removePlayerMoney(event.getKiller().getUUID(),300);
                csGameMap.getShop().syncShopMoneyData(event.getKiller().getUUID());
            }else{
                csGameMap.addPlayerMoney(event.getKiller().getUUID(),300);
                csGameMap.getShop().syncShopMoneyData(event.getKiller().getUUID());
            }
        }
    }

    public enum WinnerReason{
        TIME_OUT(3250),
        ACED(3250),
        DEFUSE_BOMB(3500),
        DETONATE_BOMB(3500);
        public final int winMoney;

        WinnerReason(int winMoney) {
            this.winMoney = winMoney;
        }
    }
}
