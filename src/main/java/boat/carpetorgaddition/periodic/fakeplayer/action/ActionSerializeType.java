package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.periodic.fakeplayer.action.LibrarianTradeFindAction.PriceLevel;
import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;

public enum ActionSerializeType {
    /**
     * 停止操作
     */
    STOP((server, _) -> new StopAction(server)),
    /**
     * 物品分拣
     */
    CATEGORIZE((server, json) -> {
        JsonElement element = json.get(ItemCategorizeAction.ITEM);
        List<ItemStackPredicate> predicates;
        if (element.isJsonPrimitive()) {
            predicates = List.of(ItemStackPredicate.parse(element.getAsString()));
        } else {
            predicates = element.getAsJsonArray()
                    .asList()
                    .stream()
                    .map(JsonElement::getAsString)
                    .map(ItemStackPredicate::parse)
                    .toList();
        }
        JsonArray thisVecArray = json.get(ItemCategorizeAction.THIS_VEC).getAsJsonArray();
        Vec3 thisVec = new Vec3(
                thisVecArray.get(0).getAsDouble(),
                thisVecArray.get(1).getAsDouble(),
                thisVecArray.get(2).getAsDouble()
        );
        JsonArray otherVecArray = json.get(ItemCategorizeAction.OTHER_VEC).getAsJsonArray();
        Vec3 otherVec = new Vec3(
                otherVecArray.get(0).getAsDouble(),
                otherVecArray.get(1).getAsDouble(),
                otherVecArray.get(2).getAsDouble()
        );
        return new ItemCategorizeAction(server, predicates, thisVec, otherVec);
    }),
    /**
     * 清空潜影盒
     */
    EMPTY_THE_CONTAINER((server, json) -> {
        String item = json.get(EmptyTheContainerAction.ITEM).getAsString();
        ItemStackPredicate predicate = ItemStackPredicate.parse(item);
        return new EmptyTheContainerAction(server, predicate);
    }),
    /**
     * 填充潜影盒
     */
    FILL_THE_CONTAINER((server, json) -> {
        boolean dropOther = !json.has(FillTheContainerAction.DROP_OTHER) || json.get(FillTheContainerAction.DROP_OTHER).getAsBoolean();
        String item = json.get(EmptyTheContainerAction.ITEM).getAsString();
        ItemStackPredicate predicate = ItemStackPredicate.parse(item);
        boolean moreContainer = json.has(FillTheContainerAction.MORE_CONTAINER) && json.get(FillTheContainerAction.MORE_CONTAINER).getAsBoolean();
        return new FillTheContainerAction(server, predicate, dropOther, moreContainer);
    }),
    /**
     * 在工作台合成物品
     */
    CRAFTING_TABLE_CRAFT((server, json) -> {
        ItemStackPredicate[] predicates = new ItemStackPredicate[9];
        for (int i = 0; i < predicates.length; i++) {
            String item = json.get(String.valueOf(i)).getAsString();
            predicates[i] = ItemStackPredicate.parse(item);
        }
        return new CraftingTableCraftAction(server, predicates);
    }),
    /**
     * 在生存模式物品栏合成物品
     */
    INVENTORY_CRAFT((server, json) -> {
        ItemStackPredicate[] predicates = new ItemStackPredicate[4];
        for (int i = 0; i < predicates.length; i++) {
            String item = json.get(String.valueOf(i)).getAsString();
            predicates[i] = ItemStackPredicate.parse(item);
        }
        return new InventoryCraftAction(server, predicates);
    }),
    /**
     * 自动重命名物品
     */
    RENAME((server, json) -> {
        ItemStackPredicate predicate = ItemStackPredicate.parse(json.get(RenameAction.ITEM).getAsString());
        String newName = json.get(RenameAction.NAME).getAsString();
        return new RenameAction(server, predicate, newName);
    }),
    /**
     * 自动使用切石机
     */
    STONECUTTING((server, json) -> {
        String item = json.get(StonecuttingAction.ITEM).getAsString();
        ItemStackPredicate predicate = ItemStackPredicate.parse(item);
        int index = json.get(StonecuttingAction.BUTTON).getAsInt();
        return new StonecuttingAction(server, predicate, index);
    }),
    /**
     * 自动交易
     */
    TRADE((server, json) -> {
        int index = json.get(TradeAction.INDEX).getAsInt();
        boolean voidTrade = json.get(TradeAction.VOID_TRADE).getAsBoolean();
        return new TradeAction(server, index, voidTrade);
    }),
    /**
     * 自动钓鱼
     */
    FISHING((server, _) -> new FishingAction(server)),
    /**
     * 自动种植
     */
    PLANT((server, _) -> new PlantAction(server)),
    /**
     * 自动破基岩
     */
    BEDROCK((server, json) -> {
        String regionType = Optional.ofNullable(json.get("region_type")).map(JsonElement::getAsString).orElse("cuboid");
        boolean ai = Optional.ofNullable(json.get("ai")).map(JsonElement::getAsBoolean).orElse(false);
        boolean timedMaterialRecycling = Optional.ofNullable(json.get("timed_material_recycling")).map(JsonElement::getAsBoolean).orElse(false);
        switch (regionType) {
            case "cuboid" -> {
                JsonArray from = json.getAsJsonArray("from");
                JsonArray to = json.getAsJsonArray("to");
                return new BedrockAction(server, toBlockPos(from), toBlockPos(to), ai, timedMaterialRecycling);
            }
            case "cylinder" -> {
                JsonArray center = json.getAsJsonArray("center");
                int radius = json.get("radius").getAsInt();
                int height = json.get("height").getAsInt();
                return new BedrockAction(server, toBlockPos(center), radius, height, ai, timedMaterialRecycling);
            }
            default -> {
                return new StopAction(server);
            }
        }
    }),
    GOTO((server, _) -> new StopAction(server)),
    LIBRARIAN((server, json) -> {
        BlockPos lecternPos = AbstractPlayerAction.fromJson(json.get("lectern_pos").getAsJsonObject());
        long startTime = json.get("start_time").getAsLong();
        String type = json.get("type").getAsString();
        LibrarianTradeFindAction action = switch (type.toLowerCase(Locale.ROOT)) {
            case "specific" -> {
                Identifier id = Identifier.parse(json.get("enchantment").getAsString());
                Holder.Reference<Enchantment> enchantment = EnchantmentUtils.parse(server, id).orElseThrow(() -> new IllegalStateException("Unable to parse the enchantment: " + id));
                int minLevel = json.get("min_level").getAsInt();
                int maxPrice = json.get("max_price").getAsInt();
                yield LibrarianTradeFindAction.of(server, lecternPos, enchantment, minLevel, maxPrice, startTime);
            }
            case "any" -> {
                PriceLevel price = PriceLevel.valueOf(json.get("price").getAsString().toUpperCase(Locale.ROOT));
                yield LibrarianTradeFindAction.of(server, lecternPos, price, startTime);
            }
            case "missing" -> {
                PriceLevel price = PriceLevel.valueOf(json.get("price").getAsString().toUpperCase(Locale.ROOT));
                BlockPos from = AbstractPlayerAction.fromJson(json.get("from").getAsJsonObject());
                BlockPos to = AbstractPlayerAction.fromJson(json.get("to").getAsJsonObject());
                yield LibrarianTradeFindAction.of(server, lecternPos, from, to, price, startTime);
            }
            default -> throw new JsonSyntaxException(
                    "Invalid 'type' for %s: '%s'. Expected one of: specific, any, missing"
                            .formatted(LibrarianTradeFindAction.class.getSimpleName(), type)
            );
        };
        int refreshCount = json.get("refresh_count").getAsInt();
        action.setRefreshCount(refreshCount);
        return action;
    }),
    ENCHANTING((server, json) -> {
        Identifier id = Identifier.parse(json.get("enchantment").getAsString());
        ItemStackPredicate predicate = ItemStackPredicate.parse(json.get("item").getAsString());
        Holder.Reference<Enchantment> enchantment = EnchantmentUtils.parse(server, id).orElseThrow(() -> new IllegalStateException("Unable to parse the enchantment: " + id));
        return new EnchantingAction(server, predicate, enchantment);
    });

    private final String serializedName;
    private final BiFunction<MinecraftServer, JsonObject, AbstractPlayerAction> deserializer;

    ActionSerializeType(BiFunction<MinecraftServer, JsonObject, AbstractPlayerAction> deserializer) {
        this.deserializer = deserializer;
        this.serializedName = this.name().toLowerCase(Locale.ROOT);
    }

    public AbstractPlayerAction deserialize(MinecraftServer server, JsonObject json) {
        return this.deserializer.apply(server, json);
    }

    /**
     * 获取序列化名称
     */
    public String getSerializedName() {
        return this.serializedName;
    }

    /**
     * 将一个方块坐标转换为json数组
     */
    public static JsonArray toJson(BlockPos blockPos) {
        JsonArray array = new JsonArray();
        array.add(blockPos.getX());
        array.add(blockPos.getY());
        array.add(blockPos.getZ());
        return array;
    }

    /**
     * 将一个json数组转换成一个方块坐标
     */
    public static BlockPos toBlockPos(JsonArray array) {
        int x = array.get(0).getAsInt();
        int y = array.get(1).getAsInt();
        int z = array.get(2).getAsInt();
        return new BlockPos(x, y, z);
    }
}
