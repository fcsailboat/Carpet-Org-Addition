package boat.carpetorgaddition.wheel.permission;

import boat.carpetorgaddition.util.CommandUtils;
import net.minecraft.server.permissions.PermissionSet;

import java.util.Arrays;
import java.util.function.Predicate;

public enum PermissionLevel implements Predicate<PermissionSet> {
    /**
     * 允许任何玩家执行
     */
    PASS,
    /**
     * 允许命令权限为1的玩家执行
     */
    MODERATORS,
    /**
     * 允许命令权限为2的玩家执行
     */
    OPS,
    /**
     * 允许命令权限为3的玩家执行
     */
    ADMINS,
    /**
     * 允许命令权限为4的玩家执行
     */
    OWNERS,
    /**
     * 拒绝任何玩家执行
     */
    REJECT;

    @Override
    public boolean test(PermissionSet source) {
        return switch (this) {
            case REJECT -> false;
            case MODERATORS, OPS, ADMINS, OWNERS -> source.hasPermission(CommandUtils.parsePermission(this.ordinal()));
            case PASS -> true;
        };
    }

    public String asString() {
        return switch (this) {
            case REJECT -> "false";
            case MODERATORS -> "1";
            case OPS -> "2";
            case ADMINS -> "3";
            case OWNERS -> "4";
            case PASS -> "true";
        };
    }

    public static String[] listPermission() {
        return new String[]{"true", "false", "ops", "0", "1", "2", "3", "4"};
    }

    /**
     * 从字符串加载权限等级
     */
    public static PermissionLevel fromString(String value) {
        return switch (value) {
            case "true", "0" -> PASS;
            case "false" -> REJECT;
            case "1" -> MODERATORS;
            case "ops", "2" -> OPS;
            case "3" -> ADMINS;
            case "4" -> OWNERS;
            default ->
                    throw new IllegalArgumentException("Unknown permission level: '" + value + "', expected one of " + Arrays.toString(listPermission()));
        };
    }
}
