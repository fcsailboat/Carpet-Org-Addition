package boat.carpetorgaddition.rule.value;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.PlayerCommandExtension;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.ServerUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;

public enum OpenPlayerInventoryCommandOption {
    /**
     * 允许打开假玩家物品栏<br>
     * 尝试打开真玩家物品栏时，提示该玩家不是假玩家<br>
     * 尝试打开离线或不存在的玩家的物品栏时，提示未找到玩家
     */
    FAKE_PLAYER(false, false),
    /**
     * 允许打开在线玩家物品栏<br>
     * 尝试打开离线或不存在的玩家的物品栏时，提示未找到玩家
     */
    ONLINE_PLAYER(true, false),
    /**
     * 允许打开离线玩家和假玩家的物品栏<br>
     * 尝试打开真玩家物品栏时，提示该玩家不是假玩家<br>
     * 尝试打开白名单或管理员玩家的物品栏且自己不是管理员时，提示打开白名单玩家物品栏需要OP权限<br>
     * 尝试打开不存在玩家的物品栏时，提示未找到该玩家的存储文件
     */
    NON_WHITELIST(false, true),
    /**
     * 允许打开包括离线玩家在内的任意玩家的物品栏<br>
     * 尝试打开不存在玩家的物品栏时，提示未找到该玩家的存储文件
     */
    ALL_PLAYER(true, true);

    private final boolean real;
    private final boolean offline;

    OpenPlayerInventoryCommandOption(boolean real, boolean offline) {
        this.real = real;
        this.offline = offline;
    }

    /**
     * @return 是否允许打开假玩家的物品栏，总是为{@code true}
     */
    public boolean canOpenFakePlayer() {
        return true;
    }

    /**
     * @return 是否可以打开真玩家的物品栏
     */
    public boolean canOpenRealPlayer() {
        return this.real;
    }

    /**
     * @return 是否允许打开离线玩家的物品栏
     */
    public boolean canOpenOfflinePlayer() {
        return this.offline;
    }

    /**
     * 检查玩家打开物品栏的权限
     */
    public void checkPermission(ServerPlayer player, GameProfile gameProfile) throws CommandSyntaxException {
        if (this != NON_WHITELIST) {
            return;
        }
        if (player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) {
            return;
        }
        PlayerList playerManager = ServerUtils.getServer(player).getPlayerList();
        NameAndId entry = new NameAndId(gameProfile);
        if (playerManager.isWhiteListed(entry) || playerManager.isOp(entry)) {
            throw CommandUtils.createException(PlayerCommandExtension.INVENTORY.then("permission").translate());
        }
    }

    public static boolean isEnable(CommandSourceStack source) {
        return CommandUtils.canUseCommand(source, CarpetOrgAdditionSettings.playerCommandOpenPlayerInventory);
    }
}
