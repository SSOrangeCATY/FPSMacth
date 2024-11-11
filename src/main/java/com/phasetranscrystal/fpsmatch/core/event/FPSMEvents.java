package com.phasetranscrystal.fpsmatch.core.event;

import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.core.*;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.phasetranscrystal.fpsmatch.core.data.ShopData;
import com.phasetranscrystal.fpsmatch.net.ShopActionS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;


@Mod.EventBusSubscriber(modid = FPSMatch.MODID)
public class FPSMEvents {
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            BaseMap map = FPSMCore.getMapByPlayer(player);
            if(map != null && map.isStart){
                MapTeams teams = map.getMapTeams();
                BaseTeam deadPlayerTeam = teams.getTeamByPlayer(player);
                if(deadPlayerTeam != null){
                    PlayerData data = deadPlayerTeam.getPlayerData(player.getUUID());
                    if(data == null) return;
                    data.getTabData().addDeaths();
                    data.setLiving(false);
                    player.heal(player.getMaxHealth());
                    player.setGameMode(GameType.SPECTATOR);
                    List<UUID> uuids = teams.getSameTeamPlayerUUIDs(player);
                    Entity entity = null;
                    if(uuids.size() > 1){
                        Random random = new Random();
                        entity = map.getServerLevel().getEntity(uuids.get(random.nextInt(0,uuids.size())));
                    }else if(!uuids.isEmpty()){
                        entity = map.getServerLevel().getEntity(uuids.get(0));
                    }
                    if(entity != null) player.setCamera(entity);
                    player.setRespawnPosition(player.level().dimension(),player.getOnPos().above(),0f,true,false);
                    event.setCanceled(true);
                }

                if(event.getSource().getEntity() instanceof ServerPlayer killer){
                    BaseTeam killerPlayerTeam = teams.getTeamByPlayer(killer);
                    if(killerPlayerTeam != null){
                        PlayerData data = killerPlayerTeam.getPlayerData(player.getUUID());
                        if(data == null) return;
                        data.getTabData().addKills();

                        Map<UUID, Float> hurtDataMap = teams.getLivingHurtData().get(player.getUUID());
                        if(hurtDataMap != null && !hurtDataMap.isEmpty()){

                            List<Map.Entry<UUID, Float>> sortedDamageEntries = hurtDataMap.entrySet().stream()
                                    .filter(entry -> !entry.getKey().equals(killer.getUUID()) && entry.getValue() > 4)
                                    .sorted(Map.Entry.<UUID, Float>comparingByValue().reversed())
                                    .limit(2)
                                    .toList();

                            for (Map.Entry<UUID, Float> sortedDamageEntry : sortedDamageEntries) {
                                UUID assistId = sortedDamageEntry.getKey();
                                Player assist = map.getServerLevel().getPlayerByUUID(assistId);
                                if (assist != null && teams.getJoinedPlayers().contains(assistId)) {
                                    BaseTeam assistPlayerTeam = teams.getTeamByPlayer(killer);
                                    if(assistPlayerTeam != null){
                                        PlayerData assistData = assistPlayerTeam.getPlayerData(assistId);
                                        if(assistData == null) return;
                                        assistData.getTabData().addAssist();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerOffline(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            BaseMap map = FPSMCore.getMapByPlayer(player);
            if(map != null && map.isStart){
                MapTeams teams = map.getMapTeams();
                BaseTeam playerTeam = teams.getTeamByPlayer(player);
                if(playerTeam != null) {
                    PlayerData data = playerTeam.getPlayerData(player.getUUID());
                    if(data == null) return;
                    data.setOffline(false);
                    data.setDirty();
                    //TODO
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerOffline(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            BaseMap map = FPSMCore.getMapByPlayer(player);
            if(map != null && map.isStart){
                MapTeams teams = map.getMapTeams();
                BaseTeam playerTeam = teams.getTeamByPlayer(player);
                if(playerTeam != null) {
                    PlayerData data = playerTeam.getPlayerData(player.getUUID());
                    if(data == null) return;
                    data.setOffline(true);
                    data.getTabDataTemp().addDeaths();
                    player.heal(player.getMaxHealth());
                    player.setGameMode(GameType.SPECTATOR);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurtEvent(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DamageSource damageSource = event.getSource();
            if(damageSource.getEntity() instanceof ServerPlayer from){
                BaseMap map = FPSMCore.getMapByPlayer(player);
                if (map != null && map.checkGameHasPlayer(player) && map.checkGameHasPlayer(from)) {
                    float damage = event.getAmount();
                    map.getMapTeams().addHurtData(player,from.getUUID(),damage);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDropItem(ItemTossEvent event){
        if(event.getEntity().level().isClientSide) return;
        BaseMap map = FPSMCore.getMapByPlayer((ServerPlayer) event.getPlayer());
        if (map == null) return;
        FPSMShop shop = FPSMShop.getShopByMapName(map.getMapName());
        if (shop == null) return;
        ShopData.ShopSlot slot = shop.getPlayerShopData(event.getPlayer().getUUID()).checkItemStackIsInData(event.getEntity().getItem());
        if(slot != null){
            if (event.getEntity().getItem().getCount() > 1 && slot.canReturn()){
                shop.resetSlot((ServerPlayer) event.getPlayer(),slot.type(),slot.index());
                FPSMatch.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getPlayer()), new ShopActionS2CPacket(map.getMapName(),slot,2,shop.getPlayerShopData(event.getPlayer().getUUID()).getMoney()));
            }else{
                slot.returnGoods();
                FPSMatch.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getPlayer()), new ShopActionS2CPacket(map.getMapName(),slot,0,shop.getPlayerShopData(event.getPlayer().getUUID()).getMoney()));
            }
        }
    }


    @SubscribeEvent
    public static void onPlayerPickupItem(PlayerEvent.ItemPickupEvent event){
        if(event.getEntity().level().isClientSide) return;
        BaseMap map = FPSMCore.getMapByPlayer((ServerPlayer) event.getEntity());
        if (map == null) return;
        FPSMShop shop = FPSMShop.getShopByMapName(map.getMapName());
        if (shop == null) return;

        // 如果不是Tacz的枪就是根据名称判断的！！！ 他会先遍历装备到投掷物 index 0 到 4 所以尽量不要重名!
        ShopData.ShopSlot slot = shop.getPlayerShopData(event.getEntity().getUUID()).checkItemStackIsInData(event.getStack());
        if(slot != null){
            if (event.getStack().getCount() > 1 && slot.type() == ShopData.ItemType.THROWABLE){
                if(slot.boughtCount() < 2 && slot.index() == 0){
                    slot.bought(slot.boughtCount() != 1);
                }else{
                    slot.bought();
                }
                FPSMatch.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new ShopActionS2CPacket(map.getMapName(),slot,1,shop.getPlayerShopData(event.getEntity().getUUID()).getMoney()));
            }else{
                slot.bought();
                FPSMatch.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new ShopActionS2CPacket(map.getMapName(),slot,1,shop.getPlayerShopData(event.getEntity().getUUID()).getMoney()));
            }
        }
    }

    @SubscribeEvent
    public static void onServerStoppingEvent(ServerStoppingEvent event){
        FPSMCore.getAllMaps().forEach((type,gameList)->{
            gameList.forEach(SavedData::setDirty);
        });
    }


}
