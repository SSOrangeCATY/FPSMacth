package com.phasetranscrystal.fpsmatch.entity;

import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.core.BaseMap;
import com.phasetranscrystal.fpsmatch.core.BaseTeam;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.map.BlastModeMap;
import com.phasetranscrystal.fpsmatch.cs.CSGameMap;
import com.phasetranscrystal.fpsmatch.item.FPSMItemRegister;
import com.phasetranscrystal.fpsmatch.net.BombDemolitionProgressS2CPacket;
import com.phasetranscrystal.fpsmatch.net.ShopDataSlotS2CPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class CompositionC4Entity extends Entity implements TraceableEntity {
    private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(CompositionC4Entity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EXPLOSION_RADIUS = SynchedEntityData.defineId(CompositionC4Entity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DEMOLITION_STATE = SynchedEntityData.defineId(CompositionC4Entity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DEMOLITION_PROGRESS = SynchedEntityData.defineId(CompositionC4Entity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DELETE_TIME = SynchedEntityData.defineId(CompositionC4Entity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EXPLOSION_INTERACTION = SynchedEntityData.defineId(CompositionC4Entity.class, EntityDataSerializers.INT);
    private static final int DEFAULT_FUSE_TIME = 900; // 45秒
    private static final int DEFAULT_EXPLOSION_RADIUS = 60;
    @Nullable
    private Player owner;
    @Nullable
    private Player demolisher;
    private BlastModeMap<?> map;
    private boolean deleting = false;

    public CompositionC4Entity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.blocksBuilding = true;
        this.setDemolitionProgress(0);
        this.setDeleteTime(0);
    }

    public CompositionC4Entity(Level pLevel, double pX, double pY, double pZ, @Nullable Player pOwner, @NotNull BlastModeMap<?> map) {
        this(EntityRegister.C4.get(), pLevel);
        this.setFuse(DEFAULT_FUSE_TIME);
        this.setExplosionRadius(DEFAULT_EXPLOSION_RADIUS);
        this.setPos(pX, pY, pZ);
        this.owner = pOwner;
        this.map = map;
        map.setBlasting(1);
    }

    public CompositionC4Entity(Level pLevel, Vec3 pos, @Nullable Player pOwner, @NotNull BlastModeMap<?> map, int fuseTime, int explosionRadius) {
        this(EntityRegister.C4.get(), pLevel);
        this.setFuse(fuseTime);
        this.setExplosionRadius(explosionRadius);
        this.setPos(pos);
        this.owner = pOwner;
        this.map = map;
        map.setBlasting(1);
    }
    public CompositionC4Entity(Level pLevel, Vec3 pos, @Nullable Player pOwner, @NotNull BlastModeMap<?> map, int fuseTime, int explosionRadius, Level.ExplosionInteraction explosionInteraction) {
        this(EntityRegister.C4.get(), pLevel);
        this.setExplosionInteraction(explosionInteraction);
        this.setFuse(fuseTime);
        this.setExplosionRadius(explosionRadius);
        this.setPos(pos);
        this.owner = pOwner;
        this.map = map;
        map.setBlasting(1);
    }
    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_FUSE_ID, DEFAULT_FUSE_TIME);
        this.entityData.define(DATA_EXPLOSION_RADIUS, DEFAULT_EXPLOSION_RADIUS);
        this.entityData.define(DATA_DEMOLITION_PROGRESS,0);
        this.entityData.define(DATA_DELETE_TIME,0);
        this.entityData.define(DATA_DEMOLITION_STATE, 0);
        this.entityData.define(DATA_EXPLOSION_INTERACTION, Level.ExplosionInteraction.NONE.ordinal());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        this.setFuse(pCompound.getInt("Fuse"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putInt("Fuse", this.getFuse());
    }

    public void tick() {
        if(this.getDeleteTime() >= 140){
            this.discard();
        }

        if(!this.level().isClientSide){
            if(this.deleting){
                int d = this.getDeleteTime() + 1;
                this.setDeleteTime(d);
                return;
            }

            if(this.map == null) {
                this.discard();
                return;
            }

            int i = this.getFuse() - 1;
            this.setFuse(i);
            if(demolisher == null){
                this.setDemolitionProgress(0);
            }else{
                this.setDemolitionProgress(this.getDemolitionProgress() + 1);
            }

            int j = 200;
            if(this.demolitionStates() == 2){
                j = 100;
            }

            float k = (float) this.getDemolitionProgress() / j;
            if(k > 0 && demolisher != null){
                this.syncDemolitionProgress(k);
            }else {
                this.syncDemolitionProgress(0);
            }

            if(this.getDemolitionProgress() >= j){
                this.deleting = true;
                map.setBlasting(2);
                return;
            }

            if (i <= 0) {
                if (!this.level().isClientSide) {
                    this.explode();
                }
            }

            if(i < 200){
                if(i < 100){
                    if(i <= 20) this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);

                    if(i % 5 == 0){
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                    }
                }else if( i % 10 == 0){
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            } else{
                if(i % 20 == 0){
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        }

        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D)); // 重力
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98D)); // 空气阻力

        if (this.level().isClientSide && this.getDeleteTime() <= 0) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private void explode() {
        float explosionRadius = this.getExplosionRadius(); // 爆炸半径
        this.deleting = true;
        this.map.setExploded(true);
        this.syncDemolitionProgress(0);
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), explosionRadius, this.explosionInteraction());
    }

    public void syncDemolitionProgress(float progress){
        BaseMap map = (BaseMap) this.map;
        if(map != null){
            map.getMapTeams().getJoinedPlayers().forEach((pUUID)->{
                ServerPlayer receiver = (ServerPlayer) this.level().getPlayerByUUID(pUUID);
                if(receiver != null){
                    FPSMatch.INSTANCE.send(PacketDistributor.PLAYER.with(() -> receiver), new BombDemolitionProgressS2CPacket(progress));
                }
            });
        }
    }


    @Nullable
    public LivingEntity getOwner() {
        return this.owner;
    }

    protected float getEyeHeight(@NotNull Pose pPose, @NotNull EntityDimensions pSize) {
        return 0.15F;
    }

    public void setFuse(int pLife) {
        this.entityData.set(DATA_FUSE_ID, pLife);
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE_ID);
    }

    public void setExplosionRadius(int radius){
        this.entityData.set(DATA_EXPLOSION_RADIUS, radius);
    }

    public int getExplosionRadius() {
        return this.entityData.get(DATA_EXPLOSION_RADIUS);
    }

    public int getDemolitionProgress(){
        return this.entityData.get(DATA_DEMOLITION_PROGRESS);
    }

    public void setDemolitionProgress(int progress){
        this.entityData.set(DATA_DEMOLITION_PROGRESS, progress);
    }

    public int getDeleteTime(){
        return this.entityData.get(DATA_DELETE_TIME);
    }

    public void setDeleteTime(int progress){
        this.entityData.set(DATA_DELETE_TIME, progress);
    }

    public BlastModeMap<?> getMap() {
        return map;
    }

    public void setDemolisher(@org.jetbrains.annotations.Nullable Player player){
        this.demolisher = player;
    }

    @Nullable
    public Player getDemolisher() {
        return demolisher;
    }

    public int demolitionStates() {
        return this.entityData.get(DATA_DEMOLITION_STATE);
    }

    public void setDemolitionStates(int demolitionStates) {
        this.entityData.set(DATA_DEMOLITION_STATE, demolitionStates);
    }

    public void setExplosionInteraction(Level.ExplosionInteraction explosionInteraction) {
        this.entityData.set(DATA_DEMOLITION_STATE, explosionInteraction.ordinal());
    }
    public Level.ExplosionInteraction explosionInteraction() {
        return Level.ExplosionInteraction.values()[this.entityData.get(DATA_EXPLOSION_INTERACTION)];
    }


}