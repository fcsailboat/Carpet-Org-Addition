package boat.carpetorgaddition.wheel.nbt;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import boat.carpetorgaddition.network.event.ActionSource;
import boat.carpetorgaddition.wheel.inventory.ImmutableInventory;
import boat.carpetorgaddition.wheel.inventory.PlayerInventoryType;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtFormatException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public class NbtReader {
    private static final ProblemReporter REPORTER = new ProblemReporter.ScopedCollector(NbtReader.class::toString, CarpetOrgAddition.LOGGER);
    private final ValueInput input;

    public NbtReader(MinecraftServer server, CompoundTag tag) {
        this.input = TagValueInput.create(REPORTER, server.registryAccess(), tag);
    }

    public String getString(String key) {
        return this.getStringNullable(key).orElse("");
    }

    public String getStringOrElse(String key, String other) {
        return this.getStringNullable(key).orElse(other);
    }

    public Optional<String> getStringNullable(String key) {
        return this.input.getString(key);
    }

    public int getIntOrThrow(String key) {
        return this.getIntNullable(key).orElseThrow();
    }

    public Optional<Integer> getIntNullable(String key) {
        return this.input.getInt(key);
    }

    public boolean getBooleanOrElse(String key, boolean other) {
        return this.input.getBooleanOr(key, other);
    }

    public Identifier getIdentifierOrThrow(String key) {
        return getIdentifierNullable(key).orElseThrow();
    }

    private Optional<Identifier> getIdentifierNullable(String key) {
        return this.input.read(key, Identifier.CODEC);
    }

    public UUID getUuidOrThrow(String key) {
        return this.getUuidNullable(key).orElseThrow();
    }

    public Optional<UUID> getUuidNullable(String key) {
        return this.input.read(key, UUIDUtil.STRING_CODEC);
    }

    public PlayerInventoryType getPlayerInventoryTypeOrThrow(String key) {
        return getInventoryTypeNullable(key).orElseThrow();
    }

    private Optional<PlayerInventoryType> getInventoryTypeNullable(String key) {
        return this.input.read(key, PlayerInventoryType.CODEC);
    }

    public ActionSource getActionSource(String key) {
        return this.getActionSourceNullable(key).orElse(ActionSource.UNKNOWN);
    }

    public Optional<ActionSource> getActionSourceNullable(String key) {
        return this.input.read(key, ActionSource.CODEC);
    }

    public ItemStack getItemStack(String key) {
        return this.getItemStackNullable(key).orElse(ItemStack.EMPTY);
    }

    public Optional<ItemStack> getItemStackNullable(String key) {
        return this.input.read(key, ItemStack.CODEC);
    }

    public LocalDateTime getLocalDateTime(String key) {
        return this.getLocalDateTimeNullable(key).orElse(CodecConstants.TIME_DEFAULT_VALUE);
    }

    public Optional<LocalDateTime> getLocalDateTimeNullable(String key) {
        return this.input.read(key, CodecConstants.TIME_CODEC);
    }

    public ImmutableInventory getInventory(String key) {
        return this.getInventoryNullable(key).orElse(ImmutableInventory.EMPTY);
    }

    public Optional<ImmutableInventory> getInventoryNullable(String key) {
        Optional<List<ItemStack>> optional = this.input.read(key, Codec.list(ItemStack.CODEC));
        if (optional.isEmpty()) {
            return Optional.empty();
        }
        return optional.map(ImmutableInventory::new);
    }

    public int getVersion() {
        return this.input.getInt(DataUpdater.DATA_VERSION).orElseThrow(() -> new NbtFormatException("Missing version number information"));
    }
}
