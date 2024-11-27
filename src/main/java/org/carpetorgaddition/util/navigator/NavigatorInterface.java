package org.carpetorgaddition.util.navigator;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.util.wheel.Waypoint;
import org.jetbrains.annotations.Nullable;

public interface NavigatorInterface {
    @Nullable
    AbstractNavigator getNavigator();

    void setNavigator(Entity entity, boolean isContinue);

    void setNavigator(Waypoint waypoint);

    void setNavigator(BlockPos blockPos, World world);

    void setNavigator(BlockPos blockPos, World world, Text name);

    void clearNavigator();

    static NavigatorInterface getInstance(ServerPlayerEntity player) {
        return (NavigatorInterface) player;
    }
}
