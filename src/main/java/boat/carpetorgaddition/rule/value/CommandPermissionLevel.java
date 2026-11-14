package boat.carpetorgaddition.rule.value;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// 选项可以是数字0-4的的字符串形式，但枚举字段名称不能为数字，因此该类不是枚举类
public final class CommandPermissionLevel {
    private static final Map<String, CommandPermissionLevel> LEVELS = new HashMap<>();
    public static final CommandPermissionLevel TRUE = of("true");
    public static final CommandPermissionLevel FALSE = of("false");
    public static final CommandPermissionLevel OPS = of("ops");
    private final String option;
    private final Permission permission;

    private CommandPermissionLevel(String option, int level) {
        this.option = option;
        this.permission = new Permission.HasCommandLevel(PermissionLevel.byId(level));
    }

    public static CommandPermissionLevel of(String option) {
        return LEVELS.computeIfAbsent(option.toLowerCase(Locale.ROOT), key -> switch (key) {
            case "true", "0" -> new CommandPermissionLevel(key, 0);
            case "false" -> new CommandPermissionLevel(key, Integer.MAX_VALUE);
            case "1" -> new CommandPermissionLevel(key, 1);
            case "ops", "2" -> new CommandPermissionLevel(key, 2);
            case "3" -> new CommandPermissionLevel(key, 3);
            case "4" -> new CommandPermissionLevel(key, 4);
            default -> throw new IllegalArgumentException("Invalid command permission level: " + option);
        });
    }

    public boolean hasPermission(CommandSourceStack source) {
        return this.hasPermission(source.permissions());
    }

    public boolean hasPermission(Player player) {
        return this.hasPermission(player.permissions());
    }

    public boolean hasPermission(PermissionSet permissions) {
        if (this == TRUE) {
            return true;
        }
        if (this == FALSE) {
            return false;
        }
        return permissions.hasPermission(this.permission);
    }

    @Override
    public String toString() {
        return this.option;
    }
}
