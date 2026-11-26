package boat.carpetorgaddition.util;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class EntityUtils {
    /**
     * 让实体看向指定位置
     */
    public static void lookAt(Entity entity, Vec3 pos) {
        lookAt(entity, EntityAnchorArgument.Anchor.EYES, pos);
    }

    public static void lookAt(Entity entity, EntityAnchorArgument.Anchor anchor, Vec3 pos) {
        entity.lookAt(anchor, pos);
    }

    /**
     * 丢弃物品
     */
    public static Optional<ItemEntity> drop(LivingEntity entity, ItemStack itemStack) {
        return Optional.ofNullable(entity.drop(itemStack, false, Prediction.PREDICTED));
    }

    /**
     * 摆动手
     */
    public static void swing(LivingEntity entity, InteractionHand hand) {
        entity.swing(hand, SwingAnimation.DEFAULT, entity instanceof ServerPlayer player && PlayerUtils.isRealPlayer(player));
    }

    public static Vec3 getFootPos(Entity entity) {
        return entity.position();
    }

    public static Vec3 getEyePos(Entity entity) {
        return entity.getEyePosition();
    }

    public static BlockPos getBlockPos(Entity entity) {
        return entity.blockPosition();
    }

    public static ChunkPos getChunkPos(Entity entity) {
        return entity.chunkPosition();
    }
}
