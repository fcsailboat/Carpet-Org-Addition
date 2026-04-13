package boat.carpetorgaddition.network.codec;

import boat.carpetorgaddition.util.ServerUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ObjectSearchTaskCodecs {
    public static final JsonObjectCodec<ItemSearchContext> ITEM_SEARCH_CODEC = new JsonObjectCodec<>() {
        @Override
        public JsonObject encode(ItemSearchContext value) {
            JsonObject json = new JsonObject();
            json.addProperty("range", value.range);
            JsonArray array = createItemArray(value.list());
            json.add("items", array);
            return json;
        }

        @Override
        public ItemSearchContext decode(JsonObject json) {
            int range = json.get("range").getAsInt();
            JsonArray array = json.get("items").getAsJsonArray();
            List<Item> list = array.asList().stream()
                    .map(JsonElement::getAsString)
                    .map(ServerUtils::asItem)
                    .toList();
            return new ItemSearchContext(range, list);
        }
    };

    public static final JsonObjectCodec<OfflinePlayerItemSearchContext> OFFLINE_PLAYER_SEARCH_CODEC = new JsonObjectCodec<>() {
        @Override
        public JsonObject encode(OfflinePlayerItemSearchContext value) {
            JsonObject json = new JsonObject();
            JsonArray array = createItemArray(value.list());
            json.add("items", array);
            return json;
        }

        @Override
        public OfflinePlayerItemSearchContext decode(JsonObject json) {
            List<Item> list = json.get("items")
                    .getAsJsonArray()
                    .asList()
                    .stream()
                    .map(JsonElement::getAsString)
                    .map(ServerUtils::asItem)
                    .toList();
            return new OfflinePlayerItemSearchContext(list);
        }
    };

    public static final JsonObjectCodec<BlockSearchContext> BLOCK_SEARCH_CODEC = new JsonObjectCodec<>() {
        @Override
        public JsonObject encode(BlockSearchContext value) {
            JsonObject json = new JsonObject();
            json.addProperty("range", value.range);
            JsonArray array = new JsonArray();
            List<Block> list = value.list;
            int len = 0;
            for (Block block : list) {
                String id = ServerUtils.getIdAsString(block);
                len += id.length();
                if (len >= 2000) {
                    break;
                }
                array.add(id);
            }
            json.add("blocks", array);
            return json;
        }

        @Override
        public BlockSearchContext decode(JsonObject json) {
            int range = json.get("range").getAsInt();
            List<Block> list = json.get("blocks")
                    .getAsJsonArray()
                    .asList()
                    .stream()
                    .map(JsonElement::getAsString)
                    .map(ServerUtils::asBlock)
                    .toList();
            return new BlockSearchContext(range, list);
        }
    };

    public static final JsonObjectCodec<TradeItemSearchContext> TRADE_ITEM_SEARCH_CODEC = new JsonObjectCodec<>() {
        @Override
        public JsonObject encode(TradeItemSearchContext value) {
            JsonObject json = new JsonObject();
            json.addProperty("range", value.range());
            JsonArray array = createItemArray(value.list());
            json.add("items", array);
            return json;
        }

        @Override
        public TradeItemSearchContext decode(JsonObject json) {
            int range = json.get("range").getAsInt();
            List<Item> list = json.get("items")
                    .getAsJsonArray()
                    .asList()
                    .stream()
                    .map(JsonElement::getAsString)
                    .map(ServerUtils::asItem)
                    .toList();
            return new TradeItemSearchContext(range, list);
        }
    };

    private static @NonNull JsonArray createItemArray(List<Item> list) {
        JsonArray array = new JsonArray();
        int len = 0;
        for (Item item : list) {
            String id = ServerUtils.getIdAsString(item);
            len += id.length();
            if (len >= 2000) {
                break;
            }
            array.add(id);
        }
        return array;
    }

    public record ItemSearchContext(int range, List<Item> list) {
    }

    public record OfflinePlayerItemSearchContext(List<Item> list) {
    }

    public record BlockSearchContext(int range, List<Block> list) {
    }

    public record TradeItemSearchContext(int range, List<Item> list) {
    }
}
