package boat.carpetorgaddition.periodic.task.schedule;

import boat.carpetorgaddition.periodic.task.ServerTask;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

public abstract class PlayerScheduleTask extends ServerTask {
    public PlayerScheduleTask(CommandSourceStack source) {
        super(source);
    }

    /**
     * @return 玩家名称
     */
    public abstract String getPlayerName();

    /**
     * 任务取消被取消时调用，可以用来发送消息
     */
    public abstract void onCancel(CommandContext<CommandSourceStack> context);

    public abstract void sendEachMessage(CommandSourceStack source);

    @Override
    public Object getIdentityKey() {
        return this.getPlayerName();
    }

    @Override
    public boolean constantSpeed() {
        return false;
    }
}
