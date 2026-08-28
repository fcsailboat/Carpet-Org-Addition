package boat.carpetorgaddition.network.handler;

import boat.carpetorgaddition.command.FinderCommand;
import boat.carpetorgaddition.network.c2s.ObjectSearchTaskC2SPacket;
import boat.carpetorgaddition.network.codec.ObjectSearchTaskCodecs;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.task.ServerTask;
import boat.carpetorgaddition.periodic.task.ServerTaskManager;
import boat.carpetorgaddition.periodic.task.search.BlockSearchTask;
import boat.carpetorgaddition.periodic.task.search.ItemSearchTask;
import boat.carpetorgaddition.periodic.task.search.OfflinePlayerInventorySearchTask;
import boat.carpetorgaddition.periodic.task.search.TradeItemSearchTask;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.permission.CommandPermission;
import boat.carpetorgaddition.wheel.permission.PermissionManager;
import boat.carpetorgaddition.wheel.predicate.BlockStatePredicate;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.traverser.BlockEntityTraverser;
import boat.carpetorgaddition.wheel.traverser.BlockPosTraverser;
import boat.carpetorgaddition.wheel.traverser.QuickBlockPosTraverser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ObjectSearchTaskPacketHandler implements ServerPlayNetworking.PlayPayloadHandler<ObjectSearchTaskC2SPacket> {
    @Override
    public void receive(ObjectSearchTaskC2SPacket packet, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        MinecraftServer server = ServerUtils.getServer(player);
        ServerTaskManager taskManager = ServerComponentCoordinator.of(server).getServerTaskManager();
        ServerLevel world = ServerUtils.getWorld(player);
        CommandSourceStack source = player.createCommandSourceStack();
        BlockPos blockPos = player.blockPosition();
        try {
            checkPermission(packet.key(), source);
        } catch (CommandSyntaxException e) {
            CommandUtils.handlingException(e, source);
            return;
        }
        ServerTask serverTask = switch (packet.key()) {
            case ITEM -> {
                ObjectSearchTaskCodecs.ItemSearchContext decode = ObjectSearchTaskCodecs.ITEM_SEARCH_CODEC.decode(packet.json());
                ItemStackPredicate predicate = ItemStackPredicate.of(decode.list(), packet.name());
                BlockEntityTraverser traverser = new BlockEntityTraverser(world, blockPos, decode.range());
                yield new ItemSearchTask(world, predicate, traverser, source, player);
            }
            case OFFLINE_PLAYER_ITEM -> {
                ObjectSearchTaskCodecs.OfflinePlayerItemSearchContext decode = ObjectSearchTaskCodecs.OFFLINE_PLAYER_SEARCH_CODEC.decode(packet.json());
                ItemStackPredicate predicate = ItemStackPredicate.of(decode.list(), packet.name());
                yield new OfflinePlayerInventorySearchTask(source, predicate, player);
            }
            case BLOCK -> {
                ObjectSearchTaskCodecs.BlockSearchContext decode = ObjectSearchTaskCodecs.BLOCK_SEARCH_CODEC.decode(packet.json());
                BlockStatePredicate predicate = BlockStatePredicate.ofBlocks(decode.list(), packet.name());
                QuickBlockPosTraverser traverser = new QuickBlockPosTraverser(world, blockPos, decode.range(), predicate.getPaletteMatcher());
                yield new BlockSearchTask(world, blockPos, traverser, source, predicate, player);
            }
            case TRADE_ITEM -> {
                ObjectSearchTaskCodecs.TradeItemSearchContext decode = ObjectSearchTaskCodecs.TRADE_ITEM_SEARCH_CODEC.decode(packet.json());
                BlockPosTraverser traverser = new BlockPosTraverser(world, blockPos, decode.range());
                ItemStackPredicate predicate = ItemStackPredicate.of(decode.list(), packet.name());
                yield new TradeItemSearchTask(world, traverser, blockPos, predicate, source, player);
            }
            default -> null;
        };
        if (serverTask == null) {
            return;
        }
        CommandUtils.handlingException(() -> taskManager.addTask(serverTask), source);
    }

    private void checkPermission(ObjectSearchTaskC2SPacket.Type type, CommandSourceStack source) throws CommandSyntaxException {
        CommandPermission permission = switch (type) {
            case ITEM -> PermissionManager.getPermission(FinderCommand.FINDER_ITEM);
            case OFFLINE_PLAYER_ITEM -> PermissionManager.getPermission(FinderCommand.FINDER_ITEM_FROM_OFFLINE_PLAYER);
            case BLOCK -> PermissionManager.getPermission(FinderCommand.FINDER_BLOCK);
            default -> null;
        };
        if (permission == null || permission.test(source)) {
            return;
        }
        throw CommandUtils.createException(LocalizationKeys.Operation.INSUFFICIENT_PERMISSIONS.translate());
    }
}
