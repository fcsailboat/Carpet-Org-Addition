package boat.carpetorgaddition.util;

import boat.carpetorgaddition.CarpetOrgAdditionExtension;
import boat.carpetorgaddition.wheel.FakePlayerSpawner;
import boat.carpetorgaddition.wheel.inventory.ContainerComponentInventory;
import boat.carpetorgaddition.wheel.screen.QuickShulkerScreenHandler;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import carpet.api.settings.SettingsManager;
import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import carpet.script.utils.Tracer;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PlayerUtils {
    private PlayerUtils() {
    }

    /**
     * 获取一名玩家的字符串形式的玩家名
     *
     * @param player 要获取字符串形式玩家名的玩家
     * @return 玩家名的字符串形式
     */
    public static String getName(Player player) {
        return player.getGameProfile().name();
    }

    /**
     * 打开一个GUI
     */
    public static void openScreenHandler(Player player, MenuConstructor baseFactory, Component name) {
        SimpleMenuProvider factory = new SimpleMenuProvider(baseFactory, name);
        player.openMenu(factory);
    }

    public static AbstractContainerMenu getCurrentScreen(Player player) {
        return player.containerMenu;
    }

    /**
     * 打开快捷潜影盒屏幕
     */
    public static void openShulkerScreenHandler(ServerPlayer player, ItemStack shulker) {
        if (shulker.isEmpty() || shulker.getCount() != 1) {
            return;
        }
        AbstractContainerMenu currentScreenHandler = player.containerMenu;
        if (currentScreenHandler instanceof QuickShulkerScreenHandler screenHandler && screenHandler.getShulkerBox() == shulker) {
            return;
        }
        // 玩家可能在箱子中打开快捷潜影盒，如果这时玩家离开箱子过远导致箱子所在区块被卸载，则可能导致物品复制，因此就需要在离开箱子时关闭潜影盒
        Predicate<Player> predicate = currentScreenHandler::stillValid;
        if (predicate.negate().test(player)) {
            return;
        }
        ContainerComponentInventory inventory = new ContainerComponentInventory(shulker);
        MenuConstructor factory = (syncId, playerInventory, _) ->
                new QuickShulkerScreenHandler(syncId, playerInventory, inventory, player, predicate, shulker);
        openScreenHandler(player, factory, shulker.getHoverName());
    }

    /**
     * 打开一个对话框
     */
    public static void openDialog(Player player, Dialog dialog) {
        player.openDialog(Holder.direct(dialog));
    }

    public static EntityPlayerActionPack getActionPack(ServerPlayer player) {
        return ((ServerPlayerInterface) player).getActionPack();
    }

    /**
     * 检查名称长度是否小于等于16
     */
    public static boolean verifyNameLength(String name) {
        return !playerNameTooLong(name);
    }

    public static boolean playerNameTooLong(String name) {
        return name.length() > 16;
    }

    /**
     * @return 添加名称前缀
     */
    public static String appendNamePrefix(String name) {
        List<String> list = new LinkedList<>();
        list.add(name);
        SettingsManager settingManager = CarpetOrgAdditionExtension.getSettingManager();
        Stream.of("fakePlayerNamePrefix", "fakePlayerPrefixName")
                .map(settingManager::getCarpetRule)
                .filter(Objects::nonNull)
                .filter(rule -> !RuleHelper.isInDefaultValue(rule))
                .map(CarpetRule::value)
                .filter(o -> o instanceof String)
                .map(o -> (String) o)
                .forEach(prefix -> {
                    if (list.getFirst().toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                        return;
                    }
                    list.addFirst(prefix);
                });
        StringBuilder builder = new StringBuilder();
        list.forEach(builder::append);
        return builder.toString();
    }

    /**
     * @return 添加名称后缀
     */
    public static String appendNameSuffix(String name) {
        List<String> list = new ArrayList<>();
        list.add(name);
        SettingsManager settingManager = CarpetOrgAdditionExtension.getSettingManager();
        Stream.of("fakePlayerNameSuffix", "fakePlayerSuffixName")
                .map(settingManager::getCarpetRule)
                .filter(Objects::nonNull)
                .filter(rule -> !RuleHelper.isInDefaultValue(rule))
                .map(CarpetRule::value)
                .filter(o -> o instanceof String)
                .map(o -> (String) o)
                .forEach(suffix -> {
                    if (list.getLast().toLowerCase(Locale.ROOT).startsWith(suffix.toLowerCase(Locale.ROOT))) {
                        return;
                    }
                    list.addLast(suffix);
                });
        StringBuilder builder = new StringBuilder();
        list.forEach(builder::append);
        return builder.toString();
    }

    /**
     * 在不显示退出消息的情况下退出
     */
    public static void exitGameSilently(EntityPlayerMPFake fakePlayer) {
        ScopedValue.where(FakePlayerSpawner.SILENCE, true).run(() -> exitGame(fakePlayer));
    }

    /**
     * 让一名假玩家退出游戏
     */
    public static void exitGame(EntityPlayerMPFake fakePlayer) {
        fakePlayer.kill(ServerUtils.getWorld(fakePlayer));
    }

    /**
     * 向玩家发送一个网络数据包
     */
    public static void sendNetworkPacket(ServerPlayer player, CustomPacketPayload payload) {
        if (player instanceof EntityPlayerMPFake) {
            return;
        }
        ServerPlayNetworking.send(player, payload);
    }

    /**
     * 关闭当前屏幕界面
     */
    public static void closeScreen(ServerPlayer player) {
        player.closeContainer();
    }

    /**
     * 将要丢弃的物品堆栈对象复制一份并丢出，然后将原本的物品堆栈对象删除
     *
     * @param player    当前要丢弃物品的玩家
     * @param itemStack 要丢弃的物品堆栈对象
     * @apiNote 此方法不应用于丢弃GUI中的物品，因为这不会触发{@link AbstractContainerMenu#clicked}的行为
     */
    public static void dropCopyItemAndClear(ServerPlayer player, ItemStack itemStack) {
        ServerUtils.drop(player, itemStack.copyAndClear());
    }

    /**
     * 让玩家看向某个方向
     */
    public static void look(ServerPlayer player, Direction direction) {
        EntityPlayerActionPack actionPack = getActionPack(player);
        actionPack.look(direction);
    }

    public static void click(ServerPlayer player, InteractionHand hand) {
        EntityPlayerActionPack actionPack = getActionPack(player);
        EntityPlayerActionPack.ActionType type = switch (hand) {
            case MAIN_HAND -> EntityPlayerActionPack.ActionType.ATTACK;
            case OFF_HAND -> EntityPlayerActionPack.ActionType.USE;
        };
        actionPack.start(type, EntityPlayerActionPack.Action.once());
    }

    public static void attack(ServerPlayer player) {
        click(player, InteractionHand.MAIN_HAND);
    }

    public static void use(ServerPlayer player) {
        click(player, InteractionHand.OFF_HAND);
    }

    public static boolean isSneaking(ServerPlayer player) {
        return player.isShiftKeyDown();
    }

    public static void setSneaking(ServerPlayer player, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
    }

    public static boolean isRealPlayer(ServerPlayer player) {
        return switch (player) {
            case EntityPlayerMPFake _, FakePlayer _ -> false;
            case ServerPlayer _ -> true;
        };
    }

    public static void useItemOn(ServerPlayer player, final BlockHitResult hitResult) {
        useItemOn(player, ServerUtils.getWorld(player), InteractionHand.MAIN_HAND, hitResult);
    }

    public static void useItemOn(ServerPlayer player, Level world, final InteractionHand hand, final BlockHitResult hitResult) {
        ItemStack itemStack = player.getItemInHand(hand);
        player.gameMode.useItemOn(player, world, itemStack, hand, hitResult);
    }

    @SuppressWarnings("unused")
    public static double getBlockInteractionRange(ServerPlayer player) {
        return player.blockInteractionRange();
    }

    public static double getEntityInteractionRange(ServerPlayer player) {
        return player.entityInteractionRange();
    }

    public static List<Entity> listWithinEntityInteractionRange(ServerPlayer player) {
        ServerLevel world = ServerUtils.getWorld(player);
        Vec3 pos = ServerUtils.getEyePos(player);
        double range = getEntityInteractionRange(player);
        AABB aabb = new AABB(
                pos.x() - range,
                pos.y() - range,
                pos.z() - range,
                pos.x() + range,
                pos.y() + range,
                pos.z() + range
        );
        return world.getEntities(player, aabb);
    }

    public static Optional<HitResult> getHitResult(ServerPlayer player) {
        // 使用硬编码的距离并不准确，这里是为了与Carpet的假玩家交互相兼容
        double reach = player.gameMode.isCreative() ? 5.0 : 4.5;
        return Optional.of(Tracer.rayTrace(player, 1F, reach, false));
    }
}
