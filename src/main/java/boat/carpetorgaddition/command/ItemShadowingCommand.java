package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.inventory.ImmutableInventory;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemShadowingCommand extends AbstractServerCommand {
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("itemshadowing");

    public ItemShadowingCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandItemShadowing))
                .executes(this::itemShadowing));
    }

    // 制作物品分身
    private int itemShadowing(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        // 获取主副手上的物品
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        // 两只手都持有物品，或两只手都不持有物品
        if (main.isEmpty() == off.isEmpty()) {
            throw CommandUtils.createException(KEY.then("fail").translate());
        }
        ItemStack itemStack = main.isEmpty() ? off : main;
        // 制作物品分身
        player.setItemInHand(main.isEmpty() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, itemStack);
        // 广播制作物品分身的消息
        MessageUtils.sendMessage(
                context.getSource().getServer(),
                KEY.then("broadcast").translate(player.getDisplayName(), itemStack.getDisplayName())
        );
        // 将玩家制作物品分身的消息写入日志
        this.logItemShadowing(player, itemStack);
        return 1;
    }

    private void logItemShadowing(ServerPlayer player, ItemStack itemStack) {
        Level world = ServerUtils.getWorld(player);
        if (InventoryUtils.isShulkerBoxItem(itemStack)) {
            // 获取潜影盒的物品栏
            ImmutableInventory inventory = InventoryUtils.getInventory(itemStack);
            if (inventory.isEmpty()) {
                CarpetOrgAddition.LOGGER.info("{} created an empty [{}] item shadow at {} | Coordinates: [{}]",
                        PlayerUtils.getName(player), ServerUtils.getName(itemStack.getItem()).getString(),
                        ServerUtils.getWorldIdAsString(world), ServerUtils.toPosString(player.blockPosition()));
            } else {
                CarpetOrgAddition.LOGGER.info("{} created a [{}] item shadow containing {} at {} | Coordinates: [{}]",
                        PlayerUtils.getName(player), ServerUtils.getName(itemStack.getItem()).getString(),
                        inventory, ServerUtils.getWorldIdAsString(world), ServerUtils.toPosString(player.blockPosition()));
            }
        } else {
            CarpetOrgAddition.LOGGER.info("{} created a [{}] item shadow at {} | Coordinates: [{}]",
                    PlayerUtils.getName(player), ServerUtils.getName(itemStack.getItem()).getString(),
                    ServerUtils.getWorldIdAsString(world), ServerUtils.toPosString(player.blockPosition()));
        }
    }

    @Override
    public String getDefaultName() {
        return "itemshadowing";
    }
}
