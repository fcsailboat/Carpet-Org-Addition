package boat.carpetorgaddition.wheel.nbt;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.dataupdate.nbt.NbtDataUpdater;
import boat.carpetorgaddition.network.event.ActionSource;
import boat.carpetorgaddition.wheel.inventory.PlayerInventoryType;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@SuppressWarnings("unused")
public class NbtWriter {
    private final TagValueOutput output;
    private static final ProblemReporter REPORTER = new ProblemReporter.ScopedCollector(NbtWriter.class::toString, CarpetOrgAddition.LOGGER);

    public NbtWriter(MinecraftServer server, int version) {
        this.output = TagValueOutput.createWithContext(REPORTER, server.registryAccess());
        this.output.putInt(NbtDataUpdater.DATA_VERSION, version);
        this.output.putInt(NbtDataUpdater.VANILLA_DATA_VERSION, NbtDataUpdater.CURRENT_VANILLA_DATA_VERSION);
    }

    public void putInt(String key, int value) {
        this.output.putInt(key, value);
    }

    public void putBoolean(String key, boolean value) {
        this.output.putBoolean(key, value);
    }

    public void putString(String key, String value) {
        this.output.putString(key, value);
    }

    public void putIdentifier(String key, Identifier identifier) {
        this.output.store(key, Identifier.CODEC, identifier);
    }

    public void putUuid(String key, UUID uuid) {
        this.output.store(key, UUIDUtil.STRING_CODEC, uuid);
    }

    public void putPlayerInventoryType(String key, PlayerInventoryType type) {
        this.output.store(key, PlayerInventoryType.CODEC, type);
    }

    public void putActionSource(ActionSource source) {
        this.output.store("action_source", ActionSource.CODEC, source);
    }

    public void putItemStack(String key, ItemStack itemStack) {
        this.output.store(key, ItemStack.CODEC, itemStack);
    }

    public void putLocalDateTime(String key, LocalDateTime time) {
        this.output.store(key, CodecConstants.TIME_CODEC, time);
    }

    public void putInventory(String key, Container container) {
        ArrayList<ItemStack> list = new ArrayList<>(container.getContainerSize());
        container.forEach(list::add);
        this.output.store(key, Codec.list(ItemStack.CODEC), list);
    }

    public CompoundTag toNbt() {
        return this.output.buildResult();
    }
}
