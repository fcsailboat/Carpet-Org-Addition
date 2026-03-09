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
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PlayerUtils {
    private PlayerUtils() {
    }

    /**
     * 打开一个GUI
     */
    public static void openScreenHandler(Player player, MenuConstructor baseFactory, Component name) {
        SimpleMenuProvider factory = new SimpleMenuProvider(baseFactory, name);
        player.openMenu(factory);
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
    public static void silenceLogout(EntityPlayerMPFake fakePlayer) {
        ScopedValue.where(FakePlayerSpawner.SILENCE, true).run(() -> logout(fakePlayer));
    }

    public static void logout(EntityPlayerMPFake fakePlayer) {
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
}
