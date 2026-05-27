package boat.carpetorgaddition.periodic.task.schedule;

import boat.carpetorgaddition.command.PlayerManagerCommand;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerSerializer;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.NonNull;

public class DelayedLoginTask extends PlayerScheduleTask {
    private final MinecraftServer server;
    private final FakePlayerSerializer serial;
    private long delayed;
    public static final LocalizationKey KEY = PlayerManagerCommand.SCHEDULE.then("login");

    public DelayedLoginTask(MinecraftServer server, CommandSourceStack source, FakePlayerSerializer serial, long delayed) {
        super(source);
        this.server = server;
        this.serial = serial;
        this.delayed = delayed;
    }

    @Override
    public void tick() {
        if (this.delayed == 0L) {
            this.serial.spawn(this.server, true);
        }
        this.delayed--;
    }

    @Override
    public String getPlayerName() {
        return serial.getName();
    }

    @Override
    public void onCancel(CommandContext<CommandSourceStack> context) {
        this.markRemove();
        Component time = getDisplayTime();
        Component displayName = this.serial.getDisplayName();
        MessageUtils.sendMessage(context, KEY.then("cancel").translate(displayName, time));
    }

    // 获取带有悬停提示的时间
    private @NonNull Component getDisplayTime() {
        TextBuilder builder = TextBuilder.of(TextProvider.tickToTime(this.delayed));
        builder.setHover(TextProvider.tickToRealTime(this.delayed));
        return builder.build();
    }

    @Override
    public void sendEachMessage(CommandSourceStack source) {
        MessageUtils.sendMessage(source, KEY.translate(this.serial.getDisplayName(), this.getDisplayTime()));
    }

    public void setDelayed(long delayed) {
        this.delayed = delayed;
    }

    public Component getInfo() {
        return this.serial.info();
    }

    @Override
    public boolean stopped() {
        return this.delayed < 0L;
    }
}
