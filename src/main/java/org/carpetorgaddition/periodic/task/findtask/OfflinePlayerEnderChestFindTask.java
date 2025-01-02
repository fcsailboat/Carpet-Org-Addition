package org.carpetorgaddition.periodic.task.findtask;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.UserCache;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.inventory.ImmutableInventory;

import java.io.File;

@SuppressWarnings("JavadocReference")
public class OfflinePlayerEnderChestFindTask extends OfflinePlayerFindTask {
    public OfflinePlayerEnderChestFindTask(CommandContext<ServerCommandSource> context, UserCache userCache, ServerPlayerEntity player, File[] files) throws CommandSyntaxException {
        super(context, userCache, player, files);
    }

    /**
     * 从NBT读取玩家末影箱
     *
     * @see PlayerEntity#readCustomDataFromNbt(NbtCompound)
     */
    @Override
    protected Inventory getInventory(NbtCompound nbt) {
        EnderChestInventory inventory = new EnderChestInventory();
        if (nbt.contains("EnderItems", NbtElement.LIST_TYPE)) {
            inventory.readNbtList(nbt.getList("EnderItems", NbtElement.COMPOUND_TYPE), this.player.getRegistryManager());
            return inventory;
        } else {
            return ImmutableInventory.EMPTY;
        }
    }

    @Override
    protected Text getInventoryName() {
        return TextUtils.translate("container.enderchest");
    }
}
