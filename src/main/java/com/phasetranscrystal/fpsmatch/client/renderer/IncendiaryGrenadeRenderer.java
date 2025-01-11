package com.phasetranscrystal.fpsmatch.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.phasetranscrystal.fpsmatch.entity.IncendiaryGrenadeEntity;
import com.phasetranscrystal.fpsmatch.entity.SmokeShellEntity;
import com.phasetranscrystal.fpsmatch.item.FPSMItemRegister;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class IncendiaryGrenadeRenderer implements EntityRendererProvider<IncendiaryGrenadeEntity> {

    @Override
    public @NotNull EntityRenderer<IncendiaryGrenadeEntity> create(Context pContext) {
        return new EntityRenderer<>(pContext) {
            ItemEntity item = null;
            ItemEntityRenderer itemRender = null;

            @Override
            public @NotNull ResourceLocation getTextureLocation(IncendiaryGrenadeEntity pEntity) {
                return TextureAtlas.LOCATION_BLOCKS;
            }
            @Override
            public void render(IncendiaryGrenadeEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
                super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
                if(pEntity.getState() == 2) return;
                pPoseStack.pushPose();
                pPoseStack.translate(0.0F, -0.25F, 0.0F);
                if(item == null){
                    item = new ItemEntity(pEntity.level(), pEntity.getX(), pEntity.getY(), pEntity.getZ(), new ItemStack(FPSMItemRegister.SMOKE_SHELL.get()));
                    itemRender = new ItemEntityRenderer(pContext);
                }
                item.setXRot(pEntity.getXRot());
                item.setYRot(pEntity.getYRot());
                itemRender.render(item, pEntityYaw, 0, pPoseStack, pBuffer, pPackedLight);
                pPoseStack.popPose();
            }

        };
    }
}