package boat.carpetorgaddition.wheel.provider;

import boat.carpetorgaddition.client.command.AbstractClientCommand;
import boat.carpetorgaddition.client.command.ClientCommandRegister;
import boat.carpetorgaddition.client.command.HighlightCommand;
import boat.carpetorgaddition.command.*;
import boat.carpetorgaddition.util.PlayerUtils;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.StringJoiner;
import java.util.UUID;

public class CommandProvider {
    private CommandProvider() {
    }

    /**
     * 接收一件快递
     */
    public static String collectParcel(int id, boolean force) {
        StringJoiner joiner = new StringJoiner(" ", "/", "");
        joiner.add(getCommandName(MailCommand.class))
                .add("collect")
                .add(String.valueOf(id));
        if (force) {
            joiner.add("true");
        }
        return joiner.toString();
    }

    /**
     * 撤回一件快递
     */
    public static String recallParcel(int id, boolean force) {
        StringJoiner joiner = new StringJoiner(" ", "/", "");
        joiner.add(getCommandName(MailCommand.class))
                .add("recall")
                .add(String.valueOf(id));
        if (force) {
            joiner.add("true");
        }
        return joiner.toString();
    }

    /**
     * 拦截一件快递
     */
    public static String interceptParcel(int id, boolean force) {
        StringJoiner joiner = new StringJoiner(" ", "/", "");
        joiner.add(getCommandName(MailCommand.class))
                .add("intercept")
                .add(String.valueOf(id));
        if (force) {
            joiner.add("true");
        }
        return joiner.toString();
    }

    /**
     * 导航到指定UUID的实体
     */
    public static String navigateToUuidEntity(UUID uuid) {
        return "/%s uuid %s".formatted(getCommandName(NavigatorCommand.class), uuid.toString());
    }

    /**
     * 通过玩家管理器生成玩家
     */
    public static String playerManagerSpawn(String playerName) {
        return "/%s spawn %s".formatted(getCommandName(PlayerManagerCommand.class), playerName);
    }

    public static String playerManagerSpawnGroup(String group) {
        return "/%s group spawn %s".formatted(getCommandName(PlayerManagerCommand.class), StringArgumentType.escapeIfRequired(group));
    }

    public static String playerManagerKillGroup(String group) {
        return "/%s group kill %s".formatted(getCommandName(PlayerManagerCommand.class), StringArgumentType.escapeIfRequired(group));
    }

    /**
     * 将一名玩家重新保存到玩家管理器
     */
    public static String playerManagerResave(String playerName) {
        return "/%s modify %s resave".formatted(getCommandName(PlayerManagerCommand.class), playerName);
    }

    public static String playerManagerAutologin(String name, boolean autologin) {
        return "/%s modify %s autologin %s".formatted(getCommandName(PlayerManagerCommand.class), name, autologin);
    }

    /**
     * 永久更改假玩家安全挂机阈值
     */
    public static String setupSafeAfkPermanentlyChange(EntityPlayerMPFake player, float threshold) {
        String commandName = getCommandName(PlayerManagerCommand.class);
        String playerName = PlayerUtils.getName(player);
        return "/%s safeafk set %s %s true".formatted(commandName, playerName, threshold);
    }

    /**
     * 取消设置假玩家安全挂机阈值
     */
    public static String cancelSafeAfkPermanentlyChange(EntityPlayerMPFake player) {
        String commandName = getCommandName(PlayerManagerCommand.class);
        String playerName = PlayerUtils.getName(player);
        return "/%s safeafk set %s -1 true".formatted(commandName, playerName);
    }

    public static String listGroupPlayer(String group) {
        StringJoiner joiner = new StringJoiner(" ", "/", "");
        joiner.add(getCommandName(PlayerManagerCommand.class));
        joiner.add("group list group");
        joiner.add(StringArgumentType.escapeIfRequired(group));
        return joiner.toString();
    }

    public static String listUngroupedPlayer() {
        StringJoiner joiner = new StringJoiner(" ", "/", "");
        joiner.add(getCommandName(PlayerManagerCommand.class));
        joiner.add("group list ungrouped");
        return joiner.toString();
    }

    public static String listAllPlayer() {
        StringJoiner joiner = new StringJoiner(" ", "/", "");
        joiner.add(getCommandName(PlayerManagerCommand.class));
        joiner.add("group list all");
        return joiner.toString();
    }

    /**
     * 生成一名假玩家
     */
    public static String spawnFakePlayer(String playerName) {
        return "/player %s spawn".formatted(playerName);
    }

    /**
     * 让一名假玩家退出游戏
     */
    public static String killFakePlayer(String playerName) {
        return "/player %s kill".formatted(playerName);
    }

    /**
     * 高亮路径点
     */
    public static String highlightWaypoint(BlockPos blockPos) {
        String name = getClientCommandName(HighlightCommand.class, HighlightCommand.DEFAULT_COMMAND_NAME);
        return "/%s %s %s %s".formatted(name, blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    /**
     * 使用OMMC高亮路径点
     */
    public static String highlightWaypointByOmmc(BlockPos blockPos) {
        return "/highlightWaypoint %s %s %s".formatted(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    /**
     * 停止导航
     */
    public static String stopNavigate() {
        return "%s stop".formatted(getCommandName(NavigatorCommand.class));
    }

    /**
     * 绘制粒子线
     */
    public static String drawParticleLine(double x1, double y1, double z1, double x2, double y2, double z2) {
        return "particleLine %s %s %s %s %s %s".formatted(x1, y1, z1, x2, y2, z2);
    }

    /**
     * 打开玩家物品栏
     */
    public static String openPlayerInventory(Player player) {
        return openPlayerInventory(PlayerUtils.getName(player));
    }

    public static String openPlayerInventory(String name) {
        return "/player %s inventory".formatted(name);
    }

    @SuppressWarnings("unused")
    public static String openPlayerEnderChest(Player player) {
        return openPlayerEnderChest(PlayerUtils.getName(player));
    }

    public static String openPlayerEnderChest(String name) {
        return "/player %s enderChest".formatted(name);
    }

    public static String setCarpetRule(String rule, String value) {
        return "/carpet %s %s".formatted(rule, value);
    }

    /**
     * 打开玩家设置合成GUI
     */
    public static String openPlayerCraftGui(EntityPlayerMPFake fakePlayer) {
        return "/playerAction %s craft gui".formatted(PlayerUtils.getName(fakePlayer));
    }

    /**
     * 打开玩家设置切石机GUI
     */
    public static String openPlayerStonecuttingGui(EntityPlayerMPFake fakePlayer) {
        return "/playerAction %s stonecutting gui".formatted(PlayerUtils.getName(fakePlayer));
    }

    private static <T extends AbstractServerCommand> String getCommandName(Class<T> clazz) {
        return CommandRegister.getCommandInstance(clazz).getAvailableName();
    }

    @SuppressWarnings("SameParameterValue")
    private static <T extends AbstractClientCommand> String getClientCommandName(Class<T> clazz, String other) {
        T instance = ClientCommandRegister.getCommandInstance(clazz);
        if (instance == null) {
            return other;
        }
        return instance.getAvailableName();
    }
}
