package org.carpetorgaddition.periodic.fakeplayer.actiondata;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerActionInterface;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerActionManager;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerAction;
import org.carpetorgaddition.util.wheel.TextBuilder;


public class FakePlayerActionSerial {
    private final FakePlayerAction action;
    private final AbstractActionData actionData;
    public static final FakePlayerActionSerial NO_ACTION = new FakePlayerActionSerial();

    private FakePlayerActionSerial() {
        this.action = FakePlayerAction.STOP;
        this.actionData = StopData.STOP;
    }

    public FakePlayerActionSerial(EntityPlayerMPFake fakePlayer) {
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        this.action = actionManager.getAction();
        this.actionData = actionManager.getActionData();
    }

    public FakePlayerActionSerial(JsonObject json) {
        if (json.has("stop")) {
            this.action = FakePlayerAction.STOP;
            this.actionData = StopData.STOP;
        } else if (json.has("sorting")) {
            this.action = FakePlayerAction.SORTING;
            this.actionData = SortingData.load(json.get("sorting").getAsJsonObject());
        } else if (json.has("clean")) {
            this.action = FakePlayerAction.CLEAN;
            this.actionData = CleanData.load(json.get("clean").getAsJsonObject());
        } else if (json.has("fill")) {
            this.action = FakePlayerAction.FILL;
            this.actionData = FillData.load(json.get("fill").getAsJsonObject());
        } else if (json.has("inventory_crafting")) {
            this.action = FakePlayerAction.INVENTORY_CRAFT;
            this.actionData = InventoryCraftData.load(json.get("inventory_crafting").getAsJsonObject());
        } else if (json.has("crafting_table_craft")) {
            this.action = FakePlayerAction.CRAFTING_TABLE_CRAFT;
            this.actionData = CraftingTableCraftData.load(json.get("crafting_table_craft").getAsJsonObject());
        } else if (json.has("rename")) {
            this.action = FakePlayerAction.RENAME;
            this.actionData = RenameData.load(json.get("rename").getAsJsonObject());
        } else if (json.has("stonecutting")) {
            this.action = FakePlayerAction.STONECUTTING;
            this.actionData = StonecuttingData.load(json.get("stonecutting").getAsJsonObject());
        } else if (json.has("trade")) {
            this.action = FakePlayerAction.TRADE;
            this.actionData = TradeData.load(json.get("trade").getAsJsonObject());
        } else if (json.has("fishing")) {
            this.action = FakePlayerAction.FISHING;
            this.actionData = new FishingData();
        } else {
            CarpetOrgAddition.LOGGER.warn("从json中反序列化玩家动作失败");
            this.action = FakePlayerAction.STOP;
            this.actionData = StopData.STOP;
        }
    }

    /**
     * 让假玩家开始执行动作
     */
    public void startAction(EntityPlayerMPFake fakePlayer) {
        if (this == NO_ACTION) {
            return;
        }
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(fakePlayer);
        actionManager.setAction(this.action, this.actionData);
    }

    public boolean hasAction() {
        return this != NO_ACTION && this.action != FakePlayerAction.STOP;
    }

    public Text toText() {
        TextBuilder builder = new TextBuilder();
        builder.append("carpet.commands.playerManager.info.action")
                .newLine()
                .indentation()
                .append(this.action.getDisplayName());
        return builder.toLine();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        String action = switch (this.action) {
            case STOP -> "stop";
            case SORTING -> "sorting";
            case CLEAN -> "clean";
            case FILL -> "fill";
            case INVENTORY_CRAFT -> "inventory_crafting";
            case CRAFTING_TABLE_CRAFT -> "crafting_table_craft";
            case RENAME -> "rename";
            case STONECUTTING -> "stonecutting";
            case TRADE -> "trade";
            case FISHING -> "fishing";
        };
        json.add(action, this.actionData.toJson());
        return json;
    }
}
