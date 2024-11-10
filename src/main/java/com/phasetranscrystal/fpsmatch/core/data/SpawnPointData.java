package com.phasetranscrystal.fpsmatch.core.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class SpawnPointData extends SavedData {
    ResourceKey<Level> dimension;BlockPos position; float pYaw; float pPitch;
    public SpawnPointData(ResourceKey<Level> pDimension, @Nullable BlockPos pPosition, float pYaw, float pPitch){
        this.dimension = pDimension;
        this.position = pPosition;
        this.pYaw = pYaw;
        this.pPitch = pPitch;
    }
    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public BlockPos getPosition() {
        return position;
    }

    public float getPitch() {
        return pPitch;
    }

    public float getYaw() {
        return pYaw;
    }

    @Override
    public String toString() {
        return dimension.location().getPath() +" "+ position.toString();
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag pCompoundTag) {
        pCompoundTag.putString("Dimension", this.dimension.location().toString());

        if (this.position != null) {
            pCompoundTag.putLong("Position", this.position.asLong());
        }

        pCompoundTag.putFloat("Yaw", this.pYaw);
        pCompoundTag.putFloat("Pitch", this.pPitch);

        return pCompoundTag;
    }
}
