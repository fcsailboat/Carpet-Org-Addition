package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.wheel.MenuController;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class FillTheContainerAction extends AbstractPlayerAction {
    public static final String ITEM = "item";
    public static final String DROP_OTHER = "dropOther";
    public static final String MORE_CONTAINER = "moreContainer";
    /**
     * 要向容器填充的物品
     */
    private final ItemStackPredicate predicate;
    private final boolean dropOther;
    private final boolean moreContainer;
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("fill");

    public FillTheContainerAction(EntityPlayerMPFake fakePlayer, ItemStackPredicate predicate, boolean dropOther, boolean moreContainer) {
        super(fakePlayer);
        this.predicate = predicate;
        this.dropOther = dropOther;
        this.moreContainer = moreContainer;
    }

    @Override
    protected void tick() {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        AbstractContainerMenu screenHandler = fakePlayer.containerMenu;
        if (screenHandler instanceof InventoryMenu) {
            return;
        }
        MenuController<AbstractContainerMenu> controller = new MenuController<>(screenHandler, fakePlayer);
        // 获取要装在潜影盒的物品
        for (int index : getRange(screenHandler)) {
            Slot slot = screenHandler.getSlot(index);
            ItemStack itemStack = slot.getItem();
            if (itemStack.isEmpty()) {
                continue;
            }
            if (this.predicate.test(itemStack)) {
                if (screenHandler instanceof ShulkerBoxMenu && !itemStack.getItem().canFitInsideContainerItems()) {
                    // 丢弃不能放入潜影盒的物品
                    if (this.dropOther) {
                        controller.dropAll(index);
                    }
                    continue;
                }
                // 模拟按住Shift键移动物品
                if (controller.quickMove(index).isEmpty()) {
                    PlayerUtils.closeScreen(fakePlayer);
                    return;
                }
            } else if (this.dropOther) {
                controller.dropAll(index);
            }
        }
    }

    private int[] getRange(AbstractContainerMenu screenHandler) {
        IntStream stream;
        if (this.moreContainer) {
            stream = switch (screenHandler) {
                case ShulkerBoxMenu _ -> IntStream.rangeClosed(27, 62);
                // 箱子，末影箱，木桶等容器
                case ChestMenu handler
                        when handler.getType() == MenuType.GENERIC_9x3 -> IntStream.rangeClosed(27, 62);
                // 大箱子，GCA假人背包
                case ChestMenu handler
                        when handler.getType() == MenuType.GENERIC_9x6 -> IntStream.rangeClosed(54, 89);
                // 漏斗
                case HopperMenu _ -> IntStream.rangeClosed(5, 40);
                // 发射器，投掷器
                case DispenserMenu _ -> IntStream.rangeClosed(9, 44);
                // 合成器
                case CrafterMenu _ -> IntStream.rangeClosed(9, 44);
                case null, default -> IntStream.of();
            };
        } else if (screenHandler instanceof ShulkerBoxMenu) {
            stream = IntStream.rangeClosed(27, 62);
        } else {
            stream = IntStream.of();
        }
        return stream.toArray();
    }

    @Override
    public List<Component> info() {
        ArrayList<Component> list = new ArrayList<>();
        Component translate = this.getInfoLocalizationKey().translate(this.getFakePlayer().getDisplayName(), this.predicate.getDisplayName());
        list.add(translate);
        return list;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        // 要清空的物品
        json.addProperty(ITEM, this.predicate.toString());
        // 是否丢弃其他物品
        json.addProperty(DROP_OTHER, this.dropOther);
        // 是否支持潜影盒以外的其它容器
        json.addProperty(MORE_CONTAINER, this.moreContainer);
        return json;
    }

    @Override
    public LocalizationKey getLocalizationKey() {
        return KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.FILL_THE_CONTAINER;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FillTheContainerAction that = (FillTheContainerAction) o;
        return dropOther == that.dropOther && moreContainer == that.moreContainer && Objects.equals(predicate, that.predicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, dropOther, moreContainer);
    }
}
