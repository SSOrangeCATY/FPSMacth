package com.phasetranscrystal.fpsmatch.net;

import com.phasetranscrystal.fpsmatch.client.data.ClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CSGameSettingsS2CPacket {
    private final int cTWinnerRounds;
    private final int tWinnerRounds;
    private final int pauseTime;
    private final int roundTime;
    private final boolean isDebug;
    private final boolean isStart;
    private final boolean isError;
    private final boolean isPause;
    private final boolean isWaiting;
    private boolean isWarmTime;
    private final boolean isWaitingWinner;

    public CSGameSettingsS2CPacket(int cTWinnerRounds,
                                   int tWinnerRounds,
                                   int pauseTime,
                                   int roundTime,
                                   boolean isDebug,
                                   boolean isStart,
                                   boolean isError,
                                   boolean isPause,
                                   boolean isWaiting,
                                   boolean isWaitingWinner) {
        this.cTWinnerRounds = cTWinnerRounds;
        this.tWinnerRounds = tWinnerRounds;
        this.pauseTime = pauseTime;
        this.roundTime = roundTime;
        this.isDebug = isDebug;
        this.isStart = isStart;
        this.isError = isError;
        this.isPause = isPause;
        this.isWaiting = isWaiting;
        this.isWaitingWinner = isWaitingWinner;
    }

    public static void encode(CSGameSettingsS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.cTWinnerRounds);
        buf.writeInt(packet.tWinnerRounds);
        buf.writeInt(packet.pauseTime);
        buf.writeInt(packet.roundTime);
        buf.writeBoolean(packet.isDebug);
        buf.writeBoolean(packet.isStart);
        buf.writeBoolean(packet.isError);
        buf.writeBoolean(packet.isPause);
        buf.writeBoolean(packet.isWaiting);
        buf.writeBoolean(packet.isWaitingWinner);
    }

    public static CSGameSettingsS2CPacket decode(FriendlyByteBuf buf) {
        return new CSGameSettingsS2CPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientData.cTWinnerRounds = this.cTWinnerRounds;
            ClientData.tWinnerRounds = this.tWinnerRounds;
            ClientData.pauseTime = this.pauseTime;
            ClientData.roundTime = this.roundTime;
            ClientData.isDebug = this.isDebug;
            ClientData.isStart = this.isStart;
            ClientData.isError = this.isError;
            ClientData.isPause = this.isPause;
            ClientData.isWaiting = this.isWaiting;
            ClientData.isWarmTime = this.isWarmTime;
            ClientData.isWaitingWinner = this.isWaitingWinner;
        });
        ctx.get().setPacketHandled(true);
    }
}