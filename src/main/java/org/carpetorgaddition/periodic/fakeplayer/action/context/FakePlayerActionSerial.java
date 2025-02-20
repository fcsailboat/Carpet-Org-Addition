package org.carpetorgaddition.periodic.fakeplayer.action.context;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.periodic.fakeplayer.action.FakePlayerAction;
import org.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import org.carpetorgaddition.util.GenericFetcherUtils;
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
        FakePlayerActionManager actionManager = GenericFetcherUtils.getFakePlayerActionManager(fakePlayer);
        this.action = actionManager.getAction();
        this.context = actionManager.getActionContext();
    }

    public FakePlayerActionSerial(JsonObject json) {
        for (FakePlayerAction value : FakePlayerAction.values()) {
            String serializedName = value.getSerializedName();
            if (json.has(serializedName)) {
                if (value.isHidden()) {
                    this.action = FakePlayerAction.STOP;
                    this.context = StopContext.STOP;
                } else {
                    this.action = value;
                    final JsonObject jsonObject = json.get(serializedName).getAsJsonObject();
                    this.context = switch (value) {
                        case STOP -> StopContext.STOP;
                        case SORTING -> SortingContext.load(jsonObject);
                        case CLEAN -> CleanContext.load(jsonObject);
                        case FILL -> FillContext.load(jsonObject);
                        case CRAFTING_TABLE_CRAFT -> CraftingTableCraftContext.load(jsonObject);
                        case INVENTORY_CRAFTING -> InventoryCraftContext.load(jsonObject);
                        case RENAME -> RenameContext.load(jsonObject);
                        case STONECUTTING -> StonecuttingContext.load(jsonObject);
                        case TRADE -> TradeContext.load(jsonObject);
                        case FISHING -> new FishingContext();
                        case FARM -> new FarmContext();
                        case BEDROCK -> new BreakBedrockContext(jsonObject);
                    };
                }
                return;
            }
        }
        CarpetOrgAddition.LOGGER.warn("从json中反序列化玩家动作失败");
        this.action = FakePlayerAction.STOP;
        this.context = StopContext.STOP;
    }

    /**
     * 让假玩家开始执行动作
     */
    public void startAction(EntityPlayerMPFake fakePlayer) {
        if (this == NO_ACTION) {
            return;
        }
        FakePlayerActionManager actionManager = GenericFetcherUtils.getFakePlayerActionManager(fakePlayer);
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
        if (this.action.isHidden()) {
            json.add(FakePlayerAction.STOP.getSerializedName(), StopContext.STOP.toJson());
        } else {
            String action = this.action.getSerializedName();
            json.add(action, this.context.toJson());
        }
        return json;
    }
}
