package com.phasetranscrystal.fpsmatch.cs;

import com.phasetranscrystal.fpsmatch.core.BaseMap;
import com.phasetranscrystal.fpsmatch.core.data.SpawnPointData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class CSGameMap extends BaseMap {
    public static final int WINNER_ROUND = 13;
    public static final int PAUSE_TIME = 2400;
    public static final int WINNER_WAITING_TIME = 160;
    public static final int WARM_UP_TIME = 1200;
    private int waittingTime = 20;
    private int currentPauseTime = 0;
    private int roundTimeLimit = 115; // 回合时间限制，单位为秒
    private int currentRoundTime = 0; // 当前回合已过时间
    private boolean isDebug = true;
    private boolean isStart = false;
    private boolean isError = false;
    private boolean isPause = false;
    private boolean isWaiting = false;
    private boolean isWarmTime = false;
    private boolean isWaitingWinner = false;
    private final Map<String,Integer> teamScores = new HashMap<>();

    public CSGameMap(ServerLevel serverLevel, SpawnPointData spawnPoint) {
        super(serverLevel, List.of("ct","t"), spawnPoint);
        this.getMapTeams().setTeamPlayerLimit("ct",10);
        this.getMapTeams().setTeamPlayerLimit("t",10);
    }


    public void JoinCTTeam(Player player){
        this.getMapTeams().joinTeam("ct",player);
    }

    public void JoinTTeam(Player player){
        this.getMapTeams().joinTeam("t",player);
    }

    @Override
    public void tick() {
        if (!checkPauseTime() && !checkWarmUpTime() && !checkWaitingTime() && !checkWinnerTime() && isStart) {
            if(!isRoundTimeEnd() && !this.isDebug){
                currentRoundTime++;
                Map<String, List<UUID>> teamsLiving = this.getMapTeams().getTeamsLiving();
                for (String teamName : teamsLiving.keySet()){
                    if(teamsLiving.get(teamName).isEmpty()){
                        teamsLiving.remove(teamName);
                    }
                }

                if(teamsLiving.keySet().size() == 1) {
                    String winnerTeam = teamsLiving.keySet().stream().findFirst().get();
                    this.roundVictory(winnerTeam);
                }

            }else{
                this.roundVictory("ct");
            }
        }
    }

    public void startGame(){
        AtomicBoolean checkFlag = new AtomicBoolean(true);
        this.getMapTeams().getJoinedPlayers().forEach((uuid -> {
            Player player = this.getServerLevel().getServer().getPlayerList().getPlayer(uuid);
            if (player != null){
                String team = this.getMapTeams().getTeamByPlayer(player);
                if(team == null) checkFlag.set(false);
            }else{
                this.getMapTeams().playerLeave(uuid);
                checkFlag.set(false);
            }
        }));

        if (!checkFlag.get() && !this.isError) return;

        this.getMapTeams().setTeamsSpawnPoints();
        this.cleanupMap();
        this.isWaiting = true;
    }

    public boolean checkPauseTime(){
        if(this.isPause && currentPauseTime < PAUSE_TIME){
            this.currentPauseTime++;
        }else isPause = false;
        return this.isPause;
    }

    public boolean checkWarmUpTime(){
        if(this.isWarmTime && currentPauseTime < WARM_UP_TIME){
            this.currentPauseTime++;
        }else isWarmTime = false;
        return this.isWarmTime;
    }

    public boolean checkWaitingTime(){
        if(this.isWaiting && currentPauseTime < waittingTime){
            this.currentPauseTime++;
        }else isWaiting = false;
        return this.isWaiting;
    }

    public boolean checkWinnerTime(){
        if(!this.victoryGoal()){
            if(this.isWaitingWinner && currentRoundTime < WINNER_WAITING_TIME){
                this.currentRoundTime++;
            }else{
                isWaitingWinner = false;
                this.startNewRound();
            }
        }

        return this.isWaitingWinner;
    }

    public boolean isRoundTimeEnd(){
        return this.currentRoundTime >= this.roundTimeLimit * 20;
    }

    private void endCurrentRound() {
        currentRoundTime = 0;
    }

    private void roundVictory(String teamName) {
        this.teamScores.put(teamName,this.teamScores.getOrDefault(teamName,0) + 1);
    }

    private void startNewRound() {
        this.getMapTeams().startNewRound();
        currentRoundTime = 0;
        this.isWaiting = true;
        // 可以在这里添加更多的逻辑，比如重置得分板、装备武器等
    }

    @Override
    public void victory() {
        // 显示胜利信息

        // 重置游戏状态
        resetGame();
    }

    @Override
    public boolean victoryGoal() {
        AtomicBoolean isVictory = new AtomicBoolean(false);
        teamScores.values().forEach((integer -> {
            isVictory.set(integer >= WINNER_ROUND);
        }));
        return isVictory.get() && !this.isDebug;
    }

    @Override
    public void initializeMap() {
       if(!this.getMapTeams().checkSpawnPoints()) {
           this.getServerLevel().getServer().sendSystemMessage(Component.literal("队伍出生点与队伍人数上限不一致"));
           this.isError = true;
       }
    }

    @Override
    public void cleanupMap() {
        // 清理地图，重置重生点，清除武器等
        this.getMapTeams().setTeamsSpawnPoints();
        this.getMapTeams().getJoinedPlayers().forEach((uuid -> {
            ServerPlayer player =  this.getServerLevel().getServer().getPlayerList().getPlayer(uuid);
            if(player != null){
                float angle = player.getRespawnAngle();
                ResourceKey<Level> dimension = player.getRespawnDimension();
                ServerLevel serverLevel = this.getServerLevel().getServer().getLevel(dimension);
                BlockPos pos =  player.getRespawnPosition();
                player.heal(player.getMaxHealth());
                if (serverLevel != null && pos != null) {
                    player.teleportTo(serverLevel, pos.getX(), pos.getY(), pos.getZ(), angle, 0F);
                }
                this.clearPlayerInventory(player);
            }
        }));
    }

    public void clearPlayerInventory(ServerPlayer player){
        player.getInventory().clearOrCountMatchingItems((p_180029_) -> true, -1, player.inventoryMenu.getCraftSlots());
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.slotsChanged(player.getInventory());
    }

    private void resetGame() {
        this.isError = false;
        this.isStart = false;
        this.currentRoundTime = 0;
    }

}