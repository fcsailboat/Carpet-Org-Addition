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
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BlockStatePredicate implements BiPredicate<Level, BlockPos> {
    protected final String content;
    private final BiPredicate<Level, BlockPos> biPredicate;
    @Nullable
    private final Block block;
    public static final BlockStatePredicate EMPTY = new BlockStatePredicate();

    public BlockStatePredicate(@NotNull Block block) {
        this.content = ServerUtils.getIdAsString(block);
        this.biPredicate = (world, blockPos) -> world.getBlockState(blockPos).is(block);
        this.block = block;
    }

    @SuppressWarnings("unused")
    public BlockStatePredicate(BlockState blockState) {
        StringBuilder builder = new StringBuilder();
        Block block = blockState.getBlock();
        builder.append(ServerUtils.getIdAsString(block));
        Map<? extends Property<?>, ?> defaultEntries = block.defaultBlockState().getValues().collect(Collectors.toMap(Property.Value::property, Property.Value::value));
        List<String> list = blockState.getValues()
                // 过滤默认方块状态
                .filter(entry -> !entry.value().equals(defaultEntries.get(entry.property())))
                .map(Property.Value::property)
                .map(Property::getName)
                .toList();
        if (list.isEmpty()) {
            this.block = block;
        } else {
            this.block = null;
            builder.append("[");
            list.forEach(builder::append);
            builder.append("]");
        }
        this.content = builder.toString();
        this.biPredicate = (world, blockPos) -> world.getBlockState(blockPos).equals(blockState);
    }

    private BlockStatePredicate() {
        this.content = ServerUtils.getIdAsString(Blocks.AIR);
        this.biPredicate = (_, _) -> false;
        this.block = Blocks.AIR;
    }

    private BlockStatePredicate(String content, BiPredicate<Level, BlockPos> biPredicate, @Nullable Block block) {
        this.content = content;
        this.biPredicate = biPredicate;
        this.block = block;
    }

    private BlockStatePredicate(String content, BlockInput argument) {
        this.content = content;
        this.biPredicate = (world, blockPos) -> argument.test(new BlockInWorld(world, blockPos, false));
        Set<Property<?>> set = argument.getDefinedProperties();
        this.block = set.isEmpty() ? argument.getState().getBlock() : null;
    }

    private BlockStatePredicate(LinkedHashSet<Block> blocks) {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (Block block : blocks) {
            joiner.add(ServerUtils.getIdAsString(block));
        }
        this.content = joiner.toString();
        this.biPredicate = (world, blockPos) -> blocks.contains(world.getBlockState(blockPos).getBlock());
        this.block = null;
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

    public static BlockStatePredicate ofPredicate(CommandContext<CommandSourceStack> context, String arguments) {
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
                BiPredicate<Level, BlockPos> biPredicate = (world, blockPos) -> argument.test(new BlockInWorld(world, blockPos, false));
                Block block = tryConvert(content);
                return new BlockStatePredicate(content, biPredicate, block);
            }
        }
        throw new IllegalArgumentException();
    }

    @SuppressWarnings("unused")
    public static BlockStatePredicate ofState(CommandContext<CommandSourceStack> context, String arguments) {
        for (ParsedCommandNode<CommandSourceStack> commandNode : context.getNodes()) {
            if (commandNode.getNode() instanceof ArgumentCommandNode<?, ?> node && Objects.equals(node.getName(), arguments)) {
                StringRange range = commandNode.getRange();
                String content = context.getInput().substring(range.getStart(), range.getEnd());
                BlockInput argument = BlockStateArgument.getBlock(context, arguments);
                return new BlockStatePredicate(content, argument);
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
        // 获取区块XZ坐标
        int chunkX = SectionPos.blockToSectionCoord(blockPos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(blockPos.getZ());
        // 判断区块是否已加载
        if (world.hasChunk(chunkX, chunkZ)) {
            return this.biPredicate.test(world, blockPos);
        }
        return false;
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

    public static class WorldEaterBlockPredicate extends BlockStatePredicate {
        private static final LocalizationKey KEY = FinderCommand.KEY.then("world_eater");

        private WorldEaterBlockPredicate() {
            super();
        }

        @Override
        public boolean test(Level world, BlockPos pos) {
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
        }

        private boolean canPush(Level world, BlockPos pos) {
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
            return TextBuilder.of(this.name).setColor(ChatFormatting.GRAY).build();
        }
    }
}
