package boat.carpetorgaddition.periodic.task.schedule;

import boat.carpetorgaddition.command.PlayerManagerCommand;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

public class DelayedLogoutTask extends PlayerScheduleTask {
    private final MinecraftServer server;
    private final EntityPlayerMPFake fakePlayer;
    private long delayed;
    public static final LocalizationKey KEY = PlayerManagerCommand.SCHEDULE.then("logout");

    public DelayedLogoutTask(MinecraftServer server, CommandSourceStack source, EntityPlayerMPFake fakePlayer, long delayed) {
        super(source);
        this.server = server;
        this.fakePlayer = fakePlayer;
        this.delayed = delayed;
    }

    @Override
    public void tick() {
        if (this.delayed == 0L) {
            if (this.fakePlayer.isRemoved()) {
                // 假玩家可能被真玩家顶替，或者假玩家穿过了末地返回传送门，或者假玩家退出游戏后重新上线
                ServerPlayer player = this.server.getPlayerList().getPlayer(this.fakePlayer.getUUID());
                if (player instanceof EntityPlayerMPFake) {
                    player.kill(player.level());
                }
            } else {
                this.fakePlayer.kill(fakePlayer.level());
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
        return PlayerUtils.getName(this.fakePlayer);
    }

    @Override
    public void onCancel(CommandContext<CommandSourceStack> context) {
        this.markRemove();
        MessageUtils.sendMessage(context, KEY.then("cancel").translate(this.fakePlayer.getDisplayName(), this.getDisplayTime()));
    }

    private @NonNull Component getDisplayTime() {
        TextBuilder builder = TextBuilder.of(TextProvider.tickToTime(this.delayed));
        builder.setHover(TextProvider.tickToRealTime(this.delayed));
        return builder.build();
    }

    @Override
    public void sendEachMessage(CommandSourceStack source) {
        MessageUtils.sendMessage(source, KEY.translate(this.fakePlayer.getDisplayName(), this.getDisplayTime()));
    }

    @Override
    public boolean stopped() {
        return this.delayed < 0L;
    }

}
