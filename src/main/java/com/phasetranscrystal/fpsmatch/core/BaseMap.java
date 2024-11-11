package com.phasetranscrystal.fpsmatch.core;

import com.phasetranscrystal.fpsmatch.core.data.SpawnPointData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class BaseMap extends SavedData {
    public final String mapName;
    public String gameType = "error";
    public boolean isStart = false;
    private boolean isDebug = false;
    private final ServerLevel serverLevel;
    private MapTeams mapTeams;

    public BaseMap(ServerLevel serverLevel, String mapName) {
        this.serverLevel = serverLevel;
        this.mapName = mapName;
        this.setMapTeams(new MapTeams(this.getServerLevel(),this.getTeams()));
    }

    public Map<String,Integer> getTeams(){
        Map<String,Integer> teams = new HashMap<>();
        teams.put("teamA",5);
        teams.put("teamB",5);
        return teams;
    }

    public final void setMapTeams(MapTeams teams){
        this.mapTeams = teams;
    }

    public final void mapTick(){
        checkForVictory();
        tick();
        syncToClient();
    }

   public abstract void syncToClient();

    public void tick(){
    }

    // 检查胜利条件
    public final void checkForVictory() {
        if (this.victoryGoal()) {
            this.victory();
        }
    }

    public abstract void startGame();

    public boolean checkGameHasPlayer(ServerPlayer player){
        boolean flag = false;
        if(!this.getMapTeams().getJoinedPlayers().contains(player.getUUID()) || getMapTeams().getTeamByPlayer(player) != null){
            flag = true;
        }else if(this.getMapTeams().getJoinedPlayers().contains(player.getUUID())){
            flag = true;
        }
        return flag;
    }
    public  void startNewRound(){}
    public abstract void victory();
    public abstract boolean victoryGoal();
    public abstract void cleanupMap();
    public abstract void resetGame();

    public MapTeams getMapTeams() {
        return mapTeams;
    }

    public ServerLevel getServerLevel() {
        return serverLevel;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public boolean switchDebugMode(){
        this.isDebug = !this.isDebug;
        return this.isDebug;
    }

    public void setShopData(){
    }

    public String getMapName(){
        return mapName;
    }

    public static void syncShopDataToClient(String mapName, ServerPlayer player){
        FPSMShop.syncShopData(mapName,player);
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public String getGameType() {
        return gameType;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag compound) {
        compound.putString("MapName", this.mapName);
        compound.putString("GameType", this.gameType);
        compound.putBoolean("IsStart", this.isStart);
        compound.putBoolean("IsDebug", this.isDebug);

        // 保存 mapTeams 数据
        MapTeams mapTeams = this.getMapTeams();
        if (mapTeams != null) {
            CompoundTag teamsCompound = new CompoundTag();
            // 假设 MapTeams 类有方法来获取其 CompoundTag 表示
            CompoundTag mapTeamsTag = mapTeams.save(new CompoundTag());
            teamsCompound.put("MapTeams", mapTeamsTag);
            compound.put("MapTeams", teamsCompound);
        }

        return compound;
    }
}