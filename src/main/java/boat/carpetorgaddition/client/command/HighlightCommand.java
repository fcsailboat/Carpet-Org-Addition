package boat.carpetorgaddition.client.command;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.client.CarpetOrgAdditionClient;
import boat.carpetorgaddition.client.command.argument.ClientBlockPosArgumentType;
import boat.carpetorgaddition.client.render.waypoint.HighlightWaypoint;
import boat.carpetorgaddition.client.render.waypoint.Waypoint;
import boat.carpetorgaddition.client.render.waypoint.WaypointRenderer;
import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.util.ServerUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ObjectShare;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class HighlightCommand extends AbstractClientCommand {
    public static final String DEFAULT_COMMAND_NAME = "highlight";

    public HighlightCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
        ObjectShare share = FabricLoader.getInstance().getObjectShare();
        Supplier<String> supplier = () -> {
            try {
                return getCustomNames()[0];
            } catch (RuntimeException e) {
                return DEFAULT_COMMAND_NAME;
            }
        };
        share.put("%s:%s".formatted(CarpetOrgAdditionConstants.MOD_ID, "highlight"), supplier);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(ClientCommands.literal(name)
                .then(ClientCommands.argument("blockPos", ClientBlockPosArgumentType.blockPos())
                        .executes(context -> highlight(context, 60 * 20L, !CarpetOrgAdditionClient.CLEAR_WAYPOINT.isUnbound()))
                        .then(ClientCommands.argument("second", IntegerArgumentType.integer(1))
                                .suggests((_, builder) -> SharedSuggestionProvider.suggest(new String[]{"30", "60", "120"}, builder))
                                .executes(context -> highlight(context, IntegerArgumentType.getInteger(context, "second") * 20L, false)))
                        .then(ClientCommands.literal("continue")
                                .executes(context -> highlight(context, 1L, true))))
                .then(ClientCommands.literal("clear")
                        .executes(_ -> clear())));
    }

    // 高亮路径点
    private int highlight(CommandContext<FabricClientCommandSource> context, long duration, boolean persistent) {
        BlockPos blockPos = ClientBlockPosArgumentType.getBlockPos(context, "blockPos");
        Vec3 vec3d = ServerUtils.getBlockCenter(blockPos);
        ClientLevel world = ClientUtils.getWorld();
        WaypointRenderer instance = WaypointRenderer.getInstance();
        List<Waypoint> list = instance.listRenderers(Waypoint.HIGHLIGHT);
        Waypoint newWaypoint = new HighlightWaypoint(world, vec3d, duration, persistent);
        // 创建新路径点
        boolean update = false;
        for (Waypoint waypoint : list) {
            // 如果两个路径点指向同一个位置，就让玩家看向该路径点
            if (waypoint.equals(newWaypoint)) {
                update = true;
                // 看向路径点
                LocalPlayer player = context.getSource().getPlayer();
                ServerUtils.lookAt(player, vec3d);
                break;
            }
        }
        // 设置新的路径点
        if (update) {
            instance.addOrUpdate(newWaypoint);
        } else {
            Optional<Waypoint> optional = instance.addOrModify(newWaypoint);
            optional.ifPresent(Waypoint::stop);
        }
        return 1;
    }

    // 取消高亮路径点
    private int clear() {
        WaypointRenderer instance = WaypointRenderer.getInstance();
        List<Waypoint> list = instance.listRenderers(Waypoint.HIGHLIGHT);
        int result = list.size();
        list.forEach(Waypoint::stop);
        return result;
    }

    @Override
    public String getDefaultName() {
        return DEFAULT_COMMAND_NAME;
    }
}
