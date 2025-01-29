package org.carpetorgaddition.periodic.fakeplayer.actioncontext;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.periodic.PeriodicTaskUtils;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerAction;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerActionManager;
import org.carpetorgaddition.util.wheel.TextBuilder;


public class FakePlayerActionSerial {
    private final FakePlayerAction action;
    private final AbstractActionContext context;
    public static final FakePlayerActionSerial NO_ACTION = new FakePlayerActionSerial();

    private FakePlayerActionSerial() {
        this.action = FakePlayerAction.STOP;
        this.context = StopContext.STOP;
    }

    public FakePlayerActionSerial(EntityPlayerMPFake fakePlayer) {
        FakePlayerActionManager actionManager = PeriodicTaskUtils.getFakePlayerActionManager(fakePlayer);
        this.action = actionManager.getAction();
        this.context = actionManager.getActionContext();
    }

    public FakePlayerActionSerial(JsonObject json) {
        if (json.has("stop")) {
            this.action = FakePlayerAction.STOP;
            this.context = StopContext.STOP;
        } else if (json.has("sorting")) {
            this.action = FakePlayerAction.SORTING;
            this.context = SortingContext.load(json.get("sorting").getAsJsonObject());
        } else if (json.has("clean")) {
            this.action = FakePlayerAction.CLEAN;
            this.context = CleanContext.load(json.get("clean").getAsJsonObject());
        } else if (json.has("fill")) {
            this.action = FakePlayerAction.FILL;
            this.context = FillContext.load(json.get("fill").getAsJsonObject());
        } else if (json.has("inventory_crafting")) {
            this.action = FakePlayerAction.INVENTORY_CRAFT;
            this.context = InventoryCraftContext.load(json.get("inventory_crafting").getAsJsonObject());
        } else if (json.has("crafting_table_craft")) {
            this.action = FakePlayerAction.CRAFTING_TABLE_CRAFT;
            this.context = CraftingTableCraftContext.load(json.get("crafting_table_craft").getAsJsonObject());
        } else if (json.has("rename")) {
            this.action = FakePlayerAction.RENAME;
            this.context = RenameContext.load(json.get("rename").getAsJsonObject());
        } else if (json.has("stonecutting")) {
            this.action = FakePlayerAction.STONECUTTING;
            this.context = StonecuttingContext.load(json.get("stonecutting").getAsJsonObject());
        } else if (json.has("trade")) {
            this.action = FakePlayerAction.TRADE;
            this.context = TradeContext.load(json.get("trade").getAsJsonObject());
        } else if (json.has("fishing")) {
            this.action = FakePlayerAction.FISHING;
            this.context = new FishingContext();
        } else {
            CarpetOrgAddition.LOGGER.warn("从json中反序列化玩家动作失败");
            this.action = FakePlayerAction.STOP;
            this.context = StopContext.STOP;
        }
    }

    /**
     * 让假玩家开始执行动作
     */
    public void startAction(EntityPlayerMPFake fakePlayer) {
        if (this == NO_ACTION) {
            return;
        }
        FakePlayerActionManager actionManager = PeriodicTaskUtils.getFakePlayerActionManager(fakePlayer);
        actionManager.setAction(this.action, this.context);
    }

    public boolean hasAction() {
        return this != NO_ACTION && this.action != FakePlayerAction.STOP;
    }

    public Text toText() {
        TextBuilder builder = new TextBuilder();
        builder.appendTranslate("carpet.commands.playerManager.info.action")
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
        json.add(action, this.context.toJson());
        return json;
    }
}
