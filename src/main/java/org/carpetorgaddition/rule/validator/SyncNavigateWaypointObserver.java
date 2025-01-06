package org.carpetorgaddition.rule.validator;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.periodic.PlayerPeriodicTaskManager;
import org.carpetorgaddition.periodic.navigator.AbstractNavigator;
import org.carpetorgaddition.periodic.navigator.NavigatorManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

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
        List<AbstractNavigator> list = source.getServer().getPlayerManager().getPlayerList()
                .stream()
                .map(PlayerPeriodicTaskManager::getManager)
                .map(PlayerPeriodicTaskManager::getNavigatorManager)
                .map(NavigatorManager::getNavigator)
                .filter(Objects::nonNull)
                .toList();
        // 设置玩家路径点
        if (newValue) {
            list.forEach(AbstractNavigator::sendWaypointUpdate);
        } else {
            list.forEach(AbstractNavigator::clear);
        }
    }
}
