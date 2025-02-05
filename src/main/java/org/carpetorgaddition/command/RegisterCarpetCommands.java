package org.carpetorgaddition.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;

public class RegisterCarpetCommands {
    // 注册Carpet命令
    public static void registerCarpetCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess) {
        // 物品分身命令
        ItemShadowingCommand.register(dispatcher);
        // 假玩家工具命令
        PlayerToolsCommand.register(dispatcher);
        // 发送消息命令
        SendMessageCommand.register(dispatcher, commandRegistryAccess);
        // 苦力怕音效命令
        CreeperCommand.register(dispatcher);
        // 经验转移命令
        XpTransferCommand.register(dispatcher);
        // 生存旁观切换命令
        SpectatorCommand.register(dispatcher);
        // 查找器命令
        FinderCommand.register(dispatcher, commandRegistryAccess);
        // 自杀命令
        KillMeCommand.register(dispatcher);
        // 路径点管理器命令
        LocationsCommand.register(dispatcher);
        // 绘制粒子线命令
        // ParticleLineCommand.register(dispatcher);
        // 假玩家动作命令
        PlayerActionCommand.register(dispatcher, commandRegistryAccess);
        // 规则搜索命令
        RuleSearchCommand.register(dispatcher);
        // 玩家管理器命令
        PlayerManagerCommand.register(dispatcher);
        // 导航器命令
        NavigatorCommand.register(dispatcher);
        // 快递命令
        MailCommand.register(dispatcher);
    }
}
