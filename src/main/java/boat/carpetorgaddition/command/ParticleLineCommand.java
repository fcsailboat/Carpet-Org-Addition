package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.task.DrawParticleLineTask;
import boat.carpetorgaddition.periodic.task.ServerTaskManager;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Deprecated(forRemoval = true)
public class ParticleLineCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("particleLine")
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandParticleLine))
                .then(Commands.argument("from", Vec3Argument.vec3())
                        .then(Commands.argument("to", Vec3Argument.vec3())
                                .executes(context -> draw(context, false)))
                        .then(Commands.argument("uuid", StringArgumentType.string())
                                .executes(context -> draw(context, true)))));
    }

    // 准备绘制粒子线
    public static int draw(CommandContext<CommandSourceStack> context, boolean isUuid) throws CommandSyntaxException {
        // 获取玩家对象
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        // 获取粒子线的起始和结束点
        Vec3 from = Vec3Argument.getVec3(context, "from");
        Vec3 to;
        CommandSourceStack source = context.getSource();
        if (isUuid) {
            String uuid = StringArgumentType.getString(context, "uuid");
            Entity entity = ServerUtils.getEntityFromUUID(source.getServer(), CommandUtils.parseUuidFromString(uuid));
            if (entity == null) {
                throw EntityArgument.NO_ENTITIES_FOUND.create();
            }
            to = new Vec3(entity.getX(), entity.getY(0.618), entity.getZ());
        } else {
            to = Vec3Argument.getVec3(context, "to");
        }
        // 获取粒子的效果类型
        ParticleOptions mainParticle = new DustParticleOptions(0x000000, 1);
        // 计算粒子线的长度（平方）
        double distanceTo = from.distanceToSqr(to);
        // 计算粒子线长度
        int distance = (int) Math.round(Math.sqrt(distanceTo));
        if (distance == 0) {
            return 0;
        }
        MinecraftServer server = ServerUtils.getServer(source);
        ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        // 新建绘制粒子线任务
        manager.addTask(new DrawParticleLineTask(source, ServerUtils.getWorld(player), mainParticle, from, to));
        // 发送箭头
        sendArrow(player, to);
        // 返回值为粒子线的长度
        return distance;
    }

    /**
     * 发送箭头文本用来指示方向
     *
     * @see SubtitleOverlay#extractRenderState(GuiGraphicsExtractor)
     */
    private static void sendArrow(ServerPlayer player, Vec3 to) {
        // 获取玩家眼睛的位置
        Vec3 eyePos = ServerUtils.getEyePos(player);
        Vec3 vec3d2 = new Vec3(0.0, 0.0, -1.0).xRot(-player.getXRot() * ((float) Math.PI / 180)).yRot(-player.getYRot() * ((float) Math.PI / 180));
        Vec3 vec3d3 = new Vec3(0.0, 1.0, 0.0).xRot(-player.getXRot() * ((float) Math.PI / 180)).yRot(-player.getYRot() * ((float) Math.PI / 180));
        Vec3 vec3d4 = vec3d2.cross(vec3d3);
        Vec3 vec3d5 = to.subtract(eyePos).normalize();
        // 视线与垂直方向的夹角
        double verticalAngle = -vec3d4.dot(vec3d5);
        double f = -vec3d2.dot(vec3d5);
        if (f <= 0.5) {
            if (verticalAngle > 0.0) {
                MessageUtils.sendMessageToHud(player, Component.literal("-->"));
            } else if (verticalAngle < 0.0) {
                MessageUtils.sendMessageToHud(player, Component.literal("<--"));
            }
        }
    }
}
