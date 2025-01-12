package org.carpetorgaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.command.PlayerManagerCommand;
import org.carpetorgaddition.command.RegisterCarpetCommands;
import org.carpetorgaddition.logger.LoggerRegister;
import org.carpetorgaddition.periodic.ServerPeriodicTaskManager;
import org.carpetorgaddition.periodic.express.ExpressManager;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerSerial;
import org.carpetorgaddition.translate.Translate;
import org.carpetorgaddition.util.wheel.Waypoint;

import java.util.Map;

public class CarpetOrgAdditionExtension implements CarpetExtension {
    // 在游戏开始时
    @Override
    public void onGameStarted() {
        // 解析Carpet设置
        CarpetServer.settingsManager.parseSettingsClass(CarpetOrgAdditionSettings.class);
    }

    // 当玩家登录时
    @Override
    public void onPlayerLoggedIn(ServerPlayerEntity player) {
        // 假玩家生成时不保留上一次的击退，着火时间，摔落高度
        if (CarpetOrgAdditionSettings.fakePlayerSpawnNoKnockback && player instanceof EntityPlayerMPFake) {
            // 清除速度
            player.setVelocity(Vec3d.ZERO);
            // 清除着火时间
            player.setFireTicks(0);
            // 清除摔落高度
            player.fallDistance = 0;
            // 清除负面效果
            player.getStatusEffects().removeIf(effect -> effect.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL);
        }
        // 提示玩家接收快递
        ExpressManager expressManager = ServerPeriodicTaskManager.getManager(player.server).getExpressManager();
        expressManager.promptToReceive(player);
        // 加载假玩家安全挂机
        PlayerManagerCommand.loadSafeAfk(player);
    }

    // 服务器启动时调用
    @Override
    public void onServerLoaded(MinecraftServer server) {
        // 服务器启动时自动将旧的路径点替换成新的
        Waypoint.replaceWaypoint(server);
    }

    @Override
    public void onServerLoadedWorlds(MinecraftServer server) {
        // 玩家自动登录
        FakePlayerSerial.autoLogin(server);
    }

    // 设置模组翻译
    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return Translate.getTranslate();
    }

    // 注册记录器
    @Override
    public void registerLoggers() {
        LoggerRegister.register();
    }

    // 注册命令
    @Override
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess) {
        RegisterCarpetCommands.registerCarpetCommands(dispatcher, commandRegistryAccess);
    }
}
