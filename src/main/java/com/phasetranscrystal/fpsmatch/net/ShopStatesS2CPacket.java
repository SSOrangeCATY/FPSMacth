package com.phasetranscrystal.fpsmatch.net;

import com.phasetranscrystal.fpsmatch.client.data.ClientData;
import com.phasetranscrystal.fpsmatch.client.screen.CSGameShopScreen;
import icyllis.modernui.ModernUI;
import icyllis.modernui.mc.forge.ModernUIForge;
import icyllis.modernui.mc.forge.MuiForgeApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShopStatesS2CPacket {
    boolean canOpenShop;

    public ShopStatesS2CPacket(boolean canOpenShop){
        this.canOpenShop = canOpenShop;
    }
    public static void encode(ShopStatesS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.canOpenShop);

    }

    public static ShopStatesS2CPacket decode(FriendlyByteBuf buf) {
        return new ShopStatesS2CPacket(
                buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ClientData.canOpenShop != this.canOpenShop && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().setScreen(null);
            }
            ClientData.canOpenShop = this.canOpenShop;
        });
        ctx.get().setPacketHandled(true);
    }
}
