package org.carpetorgaddition.util.task.playerscheduletask;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.jetbrains.annotations.NotNull;

public class DelayedLogoutTask extends PlayerScheduleTask {
    private final MinecraftServer server;
    private final EntityPlayerMPFake fakePlayer;
    private long delayed;

    public DelayedLogoutTask(MinecraftServer server, EntityPlayerMPFake fakePlayer, long delayed) {
        this.server = server;
        this.fakePlayer = fakePlayer;
        this.delayed = delayed;
    }

    @Override
    public void tick() {
        if (this.delayed == 0L) {
            if (this.fakePlayer.isRemoved()) {
                // 假玩家可能被真玩家顶替，或者假玩家穿过了末地返回传送门，或者假玩家退出游戏后重新上线
                ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(this.fakePlayer.getUuid());
                if (player instanceof EntityPlayerMPFake) {
                    player.kill();
                }
            } else {
                this.fakePlayer.kill();
            }
            this.delayed = -1L;
        } else {
            this.delayed--;
        }
    }

    public void setDelayed(long delayed) {
        this.delayed = delayed;
    }

    public EntityPlayerMPFake getFakePlayer() {
        return fakePlayer;
    }

    @Override
    public String getPlayerName() {
        return this.fakePlayer.getName().getString();
    }

    @Override
    public void onCancel(CommandContext<ServerCommandSource> context) {
        MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.logout.cancel",
                this.fakePlayer.getDisplayName(), this.getDisplayTime());
    }

    private @NotNull MutableText getDisplayTime() {
        return TextUtils.hoverText(TextConstants.tickToTime(this.delayed), TextConstants.tickToRealTime(this.delayed));
    }

    @Override
    public void sendEachMessage(ServerCommandSource source) {
        MessageUtils.sendMessage(source, "carpet.commands.playerManager.schedule.logout",
                this.fakePlayer.getDisplayName(), this.getDisplayTime());
    }

    @Override
    public boolean stopped() {
        return this.delayed < 0L;
    }

    @Override
    public String getLogName() {
        return this.getPlayerName() + "延迟下线";
    }
}
