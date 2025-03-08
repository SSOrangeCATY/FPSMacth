package com.phasetranscrystal.fpsmatch.net;

import com.phasetranscrystal.fpsmatch.core.item.IThrowEntityAble;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ThrowEntityC2SPacket {
    float velocity;
    float inaccuracy;

    public ThrowEntityC2SPacket(float velocity, float inaccuracy) {
        this.velocity = velocity;
        this.inaccuracy = inaccuracy;
    }
    public static void encode(ThrowEntityC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.velocity);
        buf.writeFloat(packet.inaccuracy);

    }

    public static ThrowEntityC2SPacket decode(FriendlyByteBuf buf) {
        return new ThrowEntityC2SPacket(
                buf.readFloat(),
                buf.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if(player == null) {
                ctx.get().setPacketHandled(true);
                return;
            }
            if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof IThrowEntityAble throwEntityAble) {
                throwEntityAble.shoot(player, player.level(), InteractionHand.MAIN_HAND, 1.5F, 1.0F);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
