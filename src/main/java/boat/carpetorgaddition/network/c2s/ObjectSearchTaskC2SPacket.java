package boat.carpetorgaddition.network.c2s;

import boat.carpetorgaddition.network.NetworkPacketRegister;
import boat.carpetorgaddition.util.IOUtils;
import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record ObjectSearchTaskC2SPacket(ObjectSearchTaskC2SPacket.Type key, String name, JsonObject json) implements CustomPacketPayload {
    public static final int CURRENT_VERSION = 1;
    public static final CustomPacketPayload.Type<ObjectSearchTaskC2SPacket> ID = NetworkPacketRegister.ofType("object_search_task");
    public static final StreamCodec<RegistryFriendlyByteBuf, ObjectSearchTaskC2SPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, ObjectSearchTaskC2SPacket value) {
            // 数据包版本
            buf.writeInt(CURRENT_VERSION);
            // 搜索类型
            buf.writeUtf(value.key.toString());
            buf.writeUtf(value.name);
            buf.writeUtf(IOUtils.jsonAsString(value.json));
        }

        @Override
        public ObjectSearchTaskC2SPacket decode(RegistryFriendlyByteBuf buf) {
            int version = buf.readInt();
            if (version > CURRENT_VERSION) {
                return new ObjectSearchTaskC2SPacket(ObjectSearchTaskC2SPacket.Type.INVALID, "Invalid", new JsonObject());
            }
            ObjectSearchTaskC2SPacket.Type type = switch (buf.readUtf()) {
                case "item" -> ObjectSearchTaskC2SPacket.Type.ITEM;
                case "offline_player_item" -> ObjectSearchTaskC2SPacket.Type.OFFLINE_PLAYER_ITEM;
                case "block" -> ObjectSearchTaskC2SPacket.Type.BLOCK;
                case "trade_item" -> ObjectSearchTaskC2SPacket.Type.TRADE_ITEM;
                case "trade_enchanted_book" -> ObjectSearchTaskC2SPacket.Type.TRADE_ENCHANTED_BOOK;
                default -> ObjectSearchTaskC2SPacket.Type.INVALID;
            };
            String name = buf.readUtf();
            String jsonString = buf.readUtf();
            JsonObject json = IOUtils.stringAsJson(jsonString);
            return new ObjectSearchTaskC2SPacket(type, name, json);
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public enum Type {
        ITEM,
        OFFLINE_PLAYER_ITEM,
        BLOCK,
        TRADE_ITEM,
        TRADE_ENCHANTED_BOOK,
        INVALID;

        @Override
        public String toString() {
            return switch (this) {
                case ITEM -> "item";
                case OFFLINE_PLAYER_ITEM -> "offline_player_item";
                case BLOCK -> "block";
                case TRADE_ITEM -> "trade_item";
                case TRADE_ENCHANTED_BOOK -> "trade_enchanted_book";
                case INVALID -> "invalid";
            };
        }
    }
}
