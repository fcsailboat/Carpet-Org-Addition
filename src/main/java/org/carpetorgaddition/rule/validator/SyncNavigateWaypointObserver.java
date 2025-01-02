package org.carpetorgaddition.rule.validator;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.network.s2c.WaypointClearS2CPacket;
import org.carpetorgaddition.periodic.navigator.NavigatorInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SyncNavigateWaypointObserver extends AbstractValidator<Boolean> {
    @Override
    public boolean validate(Boolean newValue) {
        return true;
    }

    @Override
    public @NotNull MutableText errorMessage() {
        throw new IllegalStateException();
    }

    @Override
    public void onChange(@Nullable ServerCommandSource source, @Nullable Boolean newValue) {
        if (source == null || newValue == null) {
            return;
        }
        List<ServerPlayerEntity> list = source.getServer().getPlayerManager().getPlayerList().stream()
                .filter(player -> NavigatorInterface.getInstance(player).getNavigator() != null).toList();
        // 设置玩家路径点
        if (newValue) {
            // noinspection DataFlowIssue getNavigator()方法不会为null，上面的stream流已经过滤了null值
            list.forEach(player -> NavigatorInterface.getInstance(player).getNavigator().sendWaypointUpdate());
        } else {
            list.forEach(player -> ServerPlayNetworking.send(player, new WaypointClearS2CPacket()));
        }
    }
}
