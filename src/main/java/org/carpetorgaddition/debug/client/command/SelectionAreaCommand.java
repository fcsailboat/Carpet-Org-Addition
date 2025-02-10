package org.carpetorgaddition.debug.client.command;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.carpetorgaddition.client.command.argument.ClientBlockPosArgumentType;
import org.carpetorgaddition.client.renderer.BoxRenderer;
import org.carpetorgaddition.exception.ProductionEnvironmentError;
import org.carpetorgaddition.util.wheel.SelectionArea;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;

public class SelectionAreaCommand {
    private static final ArrayList<SelectionAreaDebugRenderer> RENDERERS = new ArrayList<>();

    public static void register() {
        ProductionEnvironmentError.assertDevelopmentEnvironment();
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> RENDERERS.forEach(renderer -> renderer.render(context.matrixStack())));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RENDERERS.clear());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, commandRegistryAccess) ->
                dispatcher.register(
                        ClientCommandManager.literal("selectionArea")
                                .then(ClientCommandManager.argument("from", ClientBlockPosArgumentType.blockPos())
                                        .then(ClientCommandManager.argument("to", ClientBlockPosArgumentType.blockPos())
                                                .executes(SelectionAreaCommand::render)))));
    }

    private static int render(CommandContext<FabricClientCommandSource> context) {
        ProductionEnvironmentError.assertDevelopmentEnvironment();
        BlockPos from = ClientBlockPosArgumentType.getBlockPos(context, "from");
        BlockPos to = ClientBlockPosArgumentType.getBlockPos(context, "to");
        RENDERERS.add(new SelectionAreaDebugRenderer(new SelectionArea(from, to)));
        return 0;
    }

    private static class SelectionAreaDebugRenderer extends BoxRenderer {
        private final ArrayDeque<Box> deque = new ArrayDeque<>();
        private long previousTick = getGameTime();

        public SelectionAreaDebugRenderer(SelectionArea area) {
            super(area.toBox());
            for (BlockPos blockPos : area) {
                deque.push(new Box(blockPos));
            }
        }

        @Override
        public void render(MatrixStack matrixStack) {
            if (this.deque.isEmpty()) {
                return;
            }
            Box box = this.deque.peek();
            this.setBox(box);
            if (this.previousTick != this.getGameTime()) {
                this.previousTick = this.getGameTime();
                this.deque.pop();
            }
            super.render(matrixStack);
        }

        public boolean isStop() {
            return this.deque.isEmpty();
        }

        private long getGameTime() {
            return Objects.requireNonNull(MinecraftClient.getInstance().world).getTime();
        }
    }
}
