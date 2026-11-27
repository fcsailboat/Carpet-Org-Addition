package boat.carpetorgaddition.network.codec;

import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.util.IdentityUtils;
import boat.carpetorgaddition.util.ServerUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
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
                    .map(IdentityUtils::asItem)
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
                    .map(IdentityUtils::asItem)
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
                String id = IdentityUtils.getIdAsString(block);
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
                    .map(IdentityUtils::asBlock)
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
                    .map(IdentityUtils::asItem)
                    .toList();
            return new TradeItemSearchContext(range, list);
        }
    };

    public static final JsonObjectCodec<TradeEnchantedBookSearchContext> TRADE_ENCHANTED_BOOK_SEARCH_CODEC = new JsonObjectCodec<>() {
        @Override
        public JsonObject encode(TradeEnchantedBookSearchContext value) {
            JsonObject json = new JsonObject();
            json.addProperty("range", value.range());
            JsonArray array = new JsonArray();
            RegistryAccess registryAccess = ClientUtils.getRegistryAccess();
            int len = 0;
            for (Holder<Enchantment> holder : value.list()) {
                String id = IdentityUtils.getIdAsString(registryAccess, holder);
                len += id.length();
                if (len >= 2000) {
                    break;
                }
                array.add(id);
            }
            json.add("enchantments", array);
            return json;
        }

        @Override
        public TradeEnchantedBookSearchContext decode(JsonObject json) {
            int range = json.get("range").getAsInt();
            List<Holder<Enchantment>> list = json.get("enchantments")
                    .getAsJsonArray()
                    .asList()
                    .stream()
                    .map(JsonElement::getAsString)
                    .map(Identifier::parse)
                    .map(id -> IdentityUtils.asEnchantment(ServerUtils.getCurrentServerOrThrow().registryAccess(), id))
                    .toList();
            return new TradeEnchantedBookSearchContext(range, list);
        }
    };

    private static @NonNull JsonArray createItemArray(List<Item> list) {
        JsonArray array = new JsonArray();
        int len = 0;
        for (Item item : list) {
            String id = IdentityUtils.getIdAsString(item);
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

    public record TradeEnchantedBookSearchContext(int range, List<Holder<Enchantment>> list) {
    }
}
