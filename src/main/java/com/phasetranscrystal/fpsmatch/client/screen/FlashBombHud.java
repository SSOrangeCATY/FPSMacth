package com.phasetranscrystal.fpsmatch.client.screen;

import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.effect.FPSMEffectRegister;
import com.phasetranscrystal.fpsmatch.effect.FlashBlindnessMobEffect;
import com.phasetranscrystal.fpsmatch.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

public class FlashBombHud implements IGuiOverlay {

    public static final FlashBombHud INSTANCE = new FlashBombHud();
    public final Minecraft minecraft;

    public FlashBombHud() {
        minecraft = Minecraft.getInstance();
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        LocalPlayer player = minecraft.player;
        if (player != null && player.hasEffect(FPSMEffectRegister.FLASH_BLINDNESS.get())) {
            MobEffectInstance effectInstance = player.getEffect(FPSMEffectRegister.FLASH_BLINDNESS.get());
            if (effectInstance != null && effectInstance.getEffect() instanceof FlashBlindnessMobEffect flashBlindnessMobEffect) {
                float fullBlindnessTime = flashBlindnessMobEffect.getFullBlindnessTime();
                if(fullBlindnessTime > 0){
                    int colorWithAlpha = RenderUtil.color(255, 255, 255, 255);
                    guiGraphics.fill(0, 0, screenWidth, screenHeight, colorWithAlpha);
                }else{
                    float ticker = flashBlindnessMobEffect.getTicker();
                    if (ticker > 0) {
                        int alpha = (int) (ticker / flashBlindnessMobEffect.getTotalBlindnessTime() * 255);
                        int colorWithAlpha = RenderUtil.color(255, 255, 255, alpha);
                        guiGraphics.fill(0, 0, screenWidth, screenHeight, colorWithAlpha);
                    }
                }
            };
        }
    }

}