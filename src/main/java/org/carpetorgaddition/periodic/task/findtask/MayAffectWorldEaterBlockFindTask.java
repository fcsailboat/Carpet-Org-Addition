package org.carpetorgaddition.periodic.task.findtask;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.carpetorgaddition.util.wheel.SelectionArea;

public class MayAffectWorldEaterBlockFindTask extends BlockFindTask {
    public MayAffectWorldEaterBlockFindTask(ServerWorld world, BlockPos sourcePos, SelectionArea selectionArea, CommandContext<ServerCommandSource> context, FinderCommand.BlockPredicate blockPredicate) {
        super(world, sourcePos, selectionArea, context, blockPredicate);
    }

    protected MutableText getResultMessage(BlockPos sourcteBlockPos, BlockPos blockPos) {
        return TextUtils.translate(
                "carpet.commands.finder.may_affect_world_eater_block.feedback",
                MathUtils.getBlockIntegerDistance(sourcteBlockPos, blockPos),
                TextConstants.blockPos(blockPos, Formatting.GREEN),
                this.world.getBlockState(blockPos).getBlock().getName()
        );
    }
}
