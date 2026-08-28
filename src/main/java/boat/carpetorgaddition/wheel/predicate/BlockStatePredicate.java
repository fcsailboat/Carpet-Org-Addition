package boat.carpetorgaddition.wheel.predicate;

import boat.carpetorgaddition.command.FinderCommand;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class BlockStatePredicate implements BiPredicate<Level, BlockPos> {
    protected final String content;
    private final BiPredicate<Level, BlockPos> biPredicate;
    @Nullable
    private final Block block;
    @Nullable
    private final Predicate<BlockState> paletteMatcher;
    public static final BlockStatePredicate EMPTY = new BlockStatePredicate();

    public BlockStatePredicate(@NonNull Block block) {
        this.content = ServerUtils.getIdAsString(block);
        this.biPredicate = (world, blockPos) -> world.getBlockState(blockPos).is(block);
        this.block = block;
        this.paletteMatcher = blockState -> blockState.is(block);
    }

    private BlockStatePredicate() {
        this.content = ServerUtils.getIdAsString(Blocks.AIR);
        this.biPredicate = (_, _) -> false;
        this.block = Blocks.AIR;
        this.paletteMatcher = _ -> false;
    }

    private BlockStatePredicate(String content, BiPredicate<Level, BlockPos> biPredicate, @Nullable Block block, @Nullable Predicate<BlockState> paletteMatcher) {
        this.content = content;
        this.biPredicate = biPredicate;
        this.block = block;
        this.paletteMatcher = paletteMatcher;
    }

    private BlockStatePredicate(LinkedHashSet<Block> blocks) {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (Block block : blocks) {
            joiner.add(ServerUtils.getIdAsString(block));
        }
        this.content = joiner.toString();
        this.biPredicate = (world, blockPos) -> blocks.contains(world.getBlockState(blockPos).getBlock());
        this.block = null;
        this.paletteMatcher = state -> blocks.stream().anyMatch(state::is);
    }

    public static BlockStatePredicate ofBlocks(Collection<Block> collection, String name) {
        LinkedHashSet<Block> blocks = new LinkedHashSet<>(collection);
        return switch (blocks.size()) {
            case 0 -> EMPTY;
            case 1 -> new BlockStatePredicate(blocks.getFirst());
            default -> new AnyOfBlockPredicate(blocks, name);
        };
    }

    public static BlockStatePredicate ofWorldEater() {
        return new WorldEaterBlockPredicate();
    }

    public static BlockStatePredicate ofPredicate(CommandContext<CommandSourceStack> context, HolderLookup<Block> blocks, String arguments) {
        for (ParsedCommandNode<CommandSourceStack> commandNode : context.getNodes()) {
            if (commandNode.getNode() instanceof ArgumentCommandNode<?, ?> node && Objects.equals(node.getName(), arguments)) {
                StringRange range = commandNode.getRange();
                String content = context.getInput().substring(range.getStart(), range.getEnd());
                Predicate<BlockInWorld> argument;
                try {
                    argument = BlockPredicateArgument.getBlockPredicate(context, arguments);
                } catch (CommandSyntaxException e) {
                    throw new IllegalArgumentException(e);
                }
                BiPredicate<Level, BlockPos> biPredicate = (world, blockPos) -> argument.test(new BlockInWorld(world, blockPos, true));
                Block block = tryConvert(content);
                Predicate<BlockState> paletteMatcher;
                try {
                    BlockStateParser.BlockResult blockResult = BlockStateParser.parseForBlock(blocks, content, false);
                    paletteMatcher = state -> {
                        BlockState blockState = blockResult.blockState();
                        if (state.is(blockState.getBlock())) {
                            for (Property<?> property : blockResult.properties().keySet()) {
                                if (state.getValue(property) == blockState.getValue(property)) {
                                    continue;
                                }
                                return false;
                            }
                            return true;
                        }
                        return false;
                    };
                } catch (CommandSyntaxException _) {
                    paletteMatcher = null;
                }
                return new BlockStatePredicate(content, biPredicate, block, paletteMatcher);
            }
        }
        throw new IllegalArgumentException();
    }

    @Nullable
    private static Block tryConvert(String id) {
        if ("air".equals(id) || "minecraft:air".equals(id)) {
            return Blocks.AIR;
        }
        try {
            Block block = ServerUtils.asBlock(id);
            // 不能使用isAir()，因为虚空空气和洞穴空气也是空气
            if (block == Blocks.AIR) {
                return null;
            }
            return block;
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public boolean test(Level world, BlockPos blockPos) {
        return this.biPredicate.test(world, blockPos);
    }

    public Component getDisplayName() {
        if (this == EMPTY) {
            return Blocks.AIR.getName();
        }
        if (this.block != null) {
            return this.block.getName();
        }
        if (this.content.length() > 30) {
            String substring = this.content.substring(0, 30);
            Component ellipsis = TextBuilder.create("...");
            Component result = TextBuilder.combineAll(substring, ellipsis);
            TextBuilder builder = TextBuilder.of(result).setGrayItalic().setHover(this.content);
            return builder.build();
        }
        return TextBuilder.create(this.content);
    }

    @Nullable
    public Predicate<BlockState> getPaletteMatcher() {
        return this.paletteMatcher;
    }

    public static class WorldEaterBlockPredicate extends BlockStatePredicate {
        private static final LocalizationKey KEY = FinderCommand.KEY.then("world_eater");

        private WorldEaterBlockPredicate() {
            super("", isUnsafeBlockPos(), null, null);
        }

        private static BiPredicate<Level, BlockPos> isUnsafeBlockPos() {
            return (world, pos) -> {
                BlockState blockState = world.getBlockState(pos);
                // 排除基岩，空气和流体
                if (blockState.is(Blocks.BEDROCK) || blockState.isAir() || blockState.getBlock() instanceof LiquidBlock) {
                    return false;
                }
                // 被活塞推动时会被破坏
                if (blockState.getPistonPushReaction() == PushReaction.DESTROY) {
                    return false;
                }
                // 高爆炸抗性
                if (blockState.getBlock().getExplosionResistance() > 17) {
                    return true;
                }
                // 不能推动（实体方块不能被推动）且含水
                boolean blockPiston = blockState.getBlock() instanceof BaseEntityBlock || blockState.getPistonPushReaction() == PushReaction.BLOCK;
                boolean hasWater = !blockState.getFluidState().isEmpty();
                if (blockPiston && hasWater) {
                    return true;
                }
                // 含水，可以被推动，但下方8格全都有方块
                return hasWater && canPush(world, pos);
            };
        }

        private static boolean canPush(Level world, BlockPos pos) {
            for (int i = 1; i <= 8; i++) {
                BlockState blockState = world.getBlockState(pos.below(i));
                // 不可被推动的方块
                PushReaction pistonBehavior = blockState.getPistonPushReaction();
                if (pistonBehavior == PushReaction.BLOCK) {
                    return true;
                }
                // 下方方块可以被推动
                if (pistonBehavior == PushReaction.DESTROY) {
                    return false;
                }
            }
            // 下方8格内都有方块
            return true;
        }

        @Override
        public Component getDisplayName() {
            return KEY.then("head").translate();
        }
    }

    public static class AnyOfBlockPredicate extends BlockStatePredicate {
        private final String name;

        private AnyOfBlockPredicate(LinkedHashSet<Block> blocks, String name) {
            super(blocks);
            this.name = name;
        }

        @Override
        public Component getDisplayName() {
            if (this.name.length() > 30) {
                String display = this.name.substring(0, 30) + "...";
                TextBuilder builder = TextBuilder.of(display).setGrayItalic().setHover(this.name);
                return builder.build();
            }
            // TODO 鼠标悬停显示方块名称
            return TextBuilder.of(this.name).setColor(ChatFormatting.GRAY).build();
        }
    }
}
