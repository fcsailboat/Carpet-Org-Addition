package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.util.IdentifierUtils;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public enum ActionSerializeType {
    /**
     * 停止操作
     */
    STOP(_ -> new StopAction(null)),
    /**
     * 物品分拣
     */
    CATEGORIZE(json -> {
        String item = json.get(ItemCategorizeAction.ITEM).getAsString();
        ItemStackPredicate predicate = ItemStackPredicate.parse(item);
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
        return new ItemCategorizeAction(null, predicate, thisVec, otherVec);
    }),
    /**
     * 清空潜影盒
     */
    EMPTY_THE_CONTAINER(json -> {
        String item = json.get(EmptyTheContainerAction.ITEM).getAsString();
        ItemStackPredicate predicate = ItemStackPredicate.parse(item);
        return new EmptyTheContainerAction(null, predicate);
    }),
    /**
     * 填充潜影盒
     */
    FILL_THE_CONTAINER(json -> {
        boolean dropOther = !json.has(FillTheContainerAction.DROP_OTHER) || json.get(FillTheContainerAction.DROP_OTHER).getAsBoolean();
        String item = json.get(EmptyTheContainerAction.ITEM).getAsString();
        ItemStackPredicate predicate = ItemStackPredicate.parse(item);
        boolean moreContainer = json.has(FillTheContainerAction.MORE_CONTAINER) && json.get(FillTheContainerAction.MORE_CONTAINER).getAsBoolean();
        return new FillTheContainerAction(null, predicate, dropOther, moreContainer);
    }),
    /**
     * 在工作台合成物品
     */
    CRAFTING_TABLE_CRAFT(json -> {
        ItemStackPredicate[] predicates = new ItemStackPredicate[9];
        for (int i = 0; i < predicates.length; i++) {
            String item = json.get(String.valueOf(i)).getAsString();
            predicates[i] = ItemStackPredicate.parse(item);
        }
        return new CraftingTableCraftAction(null, predicates);
    }),
    /**
     * 在生存模式物品栏合成物品
     */
    INVENTORY_CRAFT(json -> {
        ItemStackPredicate[] predicates = new ItemStackPredicate[4];
        for (int i = 0; i < predicates.length; i++) {
            String item = json.get(String.valueOf(i)).getAsString();
            predicates[i] = ItemStackPredicate.parse(item);
        }
        return new InventoryCraftAction(null, predicates);
    }),
    /**
     * 自动重命名物品
     */
    RENAME(json -> {
        Item item = IdentifierUtils.getItem(json.get(RenameAction.ITEM).getAsString());
        String newName = json.get(RenameAction.NEW_NAME).getAsString();
        return new RenameAction(null, item, newName);
    }),
    /**
     * 自动使用切石机
     */
    STONECUTTING(json -> {
        String item = json.get(StonecuttingAction.ITEM).getAsString();
        ItemStackPredicate predicate = ItemStackPredicate.parse(item);
        int index = json.get(StonecuttingAction.BUTTON).getAsInt();
        return new StonecuttingAction(null, predicate, index);
    }),
    /**
     * 自动交易
     */
    TRADE(json -> {
        int index = json.get(TradeAction.INDEX).getAsInt();
        boolean voidTrade = json.get(TradeAction.VOID_TRADE).getAsBoolean();
        return new TradeAction(null, index, voidTrade);
    }),
    /**
     * 自动钓鱼
     */
    FISHING(_ -> new FishingAction(null)),
    /**
     * 自动种植
     */
    PLANT(_ -> new PlantAction(null)),
    /**
     * 自动破基岩
     */
    BEDROCK(json -> {
        String regionType = Optional.ofNullable(json.get("region_type")).map(JsonElement::getAsString).orElse("cuboid");
        boolean ai = Optional.ofNullable(json.get("ai")).map(JsonElement::getAsBoolean).orElse(false);
        boolean timedMaterialRecycling = Optional.ofNullable(json.get("timed_material_recycling")).map(JsonElement::getAsBoolean).orElse(false);
        switch (regionType) {
            case "cuboid" -> {
                JsonArray from = json.getAsJsonArray("from");
                JsonArray to = json.getAsJsonArray("to");
                return new BedrockAction(null, toBlockPos(from), toBlockPos(to), ai, timedMaterialRecycling);
            }
            case "cylinder" -> {
                JsonArray center = json.getAsJsonArray("center");
                int radius = json.get("radius").getAsInt();
                int height = json.get("height").getAsInt();
                return new BedrockAction(null, toBlockPos(center), radius, height, ai, timedMaterialRecycling);
            }
            default -> {
                return new StopAction(null);
            }
        }
    }),
    GOTO(_ -> new StopAction(null));

    private final String serializedName;
    private final Function<JsonObject, AbstractPlayerAction> deserializer;

    ActionSerializeType(Function<JsonObject, AbstractPlayerAction> deserializer) {
        this.deserializer = deserializer;
        this.serializedName = this.name().toLowerCase(Locale.ROOT);
    }

    public AbstractPlayerAction deserialize(JsonObject json) {
        return this.deserializer.apply(json);
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
