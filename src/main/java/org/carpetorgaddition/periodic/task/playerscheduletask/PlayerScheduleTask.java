package org.carpetorgaddition.periodic.task.playerscheduletask;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import org.carpetorgaddition.periodic.task.ServerTask;

public abstract class PlayerScheduleTask extends ServerTask {
    /**
     * @return 玩家名称
     */
    public abstract String getPlayerName();

    /**
     * 任务取消被取消时调用，可以用来发送消息
     */
    public abstract void onCancel(CommandContext<ServerCommandSource> context);

    public abstract void sendEachMessage(ServerCommandSource source);

    @Override
    public boolean equals(Object obj) {
        return this.getClass() == obj.getClass() && this.getPlayerName().equals(((PlayerScheduleTask) obj).getPlayerName());
    }

    @Override
    public boolean constantSpeed() {
        return false;
    }

    @Override
    public int hashCode() {
        return this.getPlayerName().hashCode();
    }
}
