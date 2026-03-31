package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.exception.InfiniteLoopException;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemCategorizeAction extends AbstractPlayerAction {
    public static final String ITEM = "item";
    public static final String THIS_VEC = "thisVec";
    public static final String OTHER_VEC = "otherVec";
    /**
     * 要分拣的物品，元素已排序，用于在{@link ItemCategorizeAction#equals(Object)}方法中忽略元素顺序
     */
    private final List<ItemStackPredicate> predicates;
    /**
     * 如果当前物品是要分拣的物品，则将该物品向这个方向丢出
     */
    private final Vec3 thisVec;
    /**
     * 如果当前物品不是要分拣的物品，则将该物品向这个方向丢出
     */
    private final Vec3 otherVec;
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("sorting");

    public ItemCategorizeAction(EntityPlayerMPFake fakePlayer, List<ItemStackPredicate> predicates, Vec3 thisVec, Vec3 otherVec) {
        if (predicates.isEmpty()) {
            throw new IllegalArgumentException("At least one item predicate is required");
        }
        super(fakePlayer);
        this.predicates = predicates.stream().distinct().sorted().toList();
        this.thisVec = thisVec;
        this.otherVec = otherVec;
    }

    @Override
    protected void tick() {
        // 获取玩家物品栏对象
        Inventory inventory = this.getFakePlayer().getInventory();
        // 遍历玩家物品栏，找到要丢出的物品
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            // 定义变量记录当前槽位的物品堆栈对象
            ItemStack itemStack = inventory.getItem(index);
            if (itemStack.isEmpty()) {
                continue;
            }
            // 如果是要分拣的物品，就转向一边，否则转身向另一边
            if (this.test(itemStack)) {
                this.getFakePlayer().lookAt(EntityAnchorArgument.Anchor.EYES, thisVec);
            } else {
                // 丢弃潜影盒内的物品
                // 判断当前物品是不是潜影盒
                if (InventoryUtils.isShulkerBoxItem(itemStack)) {
                    itemStack = pickItemFromShulkerBox(inventory, index);
                } else {
                    //设置当前朝向为丢出非指定物品朝向
                    this.getFakePlayer().lookAt(EntityAnchorArgument.Anchor.EYES, otherVec);
                }
            }
            // 丢弃该物品堆栈
            FakePlayerUtils.dropItem(this.getFakePlayer(), itemStack);
        }
    }

    private boolean test(ItemStack itemStack) {
        return this.predicates.stream().anyMatch(predicate -> predicate.test(itemStack));
    }

    // 从潜影盒中拿取并分拣物品
    private ItemStack pickItemFromShulkerBox(Inventory inventory, int index) {
        ItemStack itemStack;
        int loopCount = 0;
        while (true) {
            loopCount++;
            if (loopCount > 100) {
                throw new InfiniteLoopException();
            }
            // 一轮循环结束后，再重新将当前物品设置为物品栏中的潜影盒
            itemStack = inventory.getItem(index);
            //判断潜影盒是否为空
            if (InventoryUtils.isEmptyShulkerBox(itemStack)) {
                // 如果为空，将朝向设置为丢出非指定物品的方向，然后结束循环
                // 设置当前朝向为丢出非指定物品朝向
                this.getFakePlayer().lookAt(EntityAnchorArgument.Anchor.EYES, this.otherVec);
                break;
            } else {
                // 获取潜影盒内第一个非空气物品，获取后，该物品会在潜影盒内删除
                // 设置当前物品为潜影盒内容物的第一个非空物品
                itemStack = InventoryUtils.pickItemFromShulkerBox(itemStack, stack -> !stack.isEmpty());
                if (itemStack.isEmpty()) {
                    itemStack = inventory.getItem(index);
                    // 设置当前朝向为丢出非指定物品朝向，然后丢弃这个潜影盒
                    this.getFakePlayer().lookAt(EntityAnchorArgument.Anchor.EYES, this.otherVec);
                    break;
                }
                // 根据当前物品设置朝向
                this.getFakePlayer().lookAt(EntityAnchorArgument.Anchor.EYES, this.test(itemStack) ? this.thisVec : this.otherVec);
            }
            // 丢弃潜影盒内物品堆栈
            FakePlayerUtils.dropItem(this.getFakePlayer(), itemStack);
        }
        return itemStack;
    }

    @Override
    public List<Component> info() {
        ArrayList<Component> list = new ArrayList<>();
        // 获取要分拣的物品名称
        Component itemName;
        if (this.predicates.size() == 1) {
            itemName = this.predicates.getFirst().getDisplayName();
        } else {
            TextJoiner joiner = new TextJoiner();
            for (ItemStackPredicate predicate : this.predicates) {
                joiner.newline(predicate.getDisplayName());
            }
            itemName = LocalizationKeys.Item.ITEM.builder().setItalic().setBold().setHover(joiner.join()).build();
        }
        // 获取假玩家的显示名称
        Component fakeName = this.getFakePlayer().getDisplayName();
        // 将假玩家正在分拣物品的消息添加到集合中
        LocalizationKey key = this.getInfoLocalizationKey();
        list.add(key.translate(fakeName, itemName));
        // 获取分拣物品要丢出的方向
        Component thisPos = posText(this.thisVec.x(), this.thisVec.y(), this.thisVec.z());
        // 获取非分拣物品要丢出的方向
        Component otherPos = posText(this.otherVec.x(), this.otherVec.y(), this.otherVec.z());
        // 将丢要分拣物品的方向的信息添加到集合
        list.add(key.then("this").translate(itemName, thisPos));
        // 将丢其他物品的方向的信息添加到集合
        list.add(key.then("other").translate(otherPos));
        return list;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (this.predicates.size() == 1) {
            // 要分拣的物品
            json.addProperty(ITEM, this.predicates.getFirst().toString());
        } else {
            JsonArray array = new JsonArray();
            for (ItemStackPredicate predicate : this.predicates) {
                array.add(predicate.toString());
            }
            json.add(ITEM, array);
        }
        // 当前物品要丢弃的位置
        JsonArray thisVecJson = new JsonArray();
        thisVecJson.add(this.thisVec.x);
        thisVecJson.add(this.thisVec.y);
        thisVecJson.add(this.thisVec.z);
        json.add(THIS_VEC, thisVecJson);
        // 其它物品要丢弃的位置
        JsonArray otherVecJson = new JsonArray();
        otherVecJson.add(this.otherVec.x);
        otherVecJson.add(this.otherVec.y);
        otherVecJson.add(this.otherVec.z);
        json.add(OTHER_VEC, otherVecJson);
        return json;
    }

    private Component posText(double x, double y, double z) {
        return TextBuilder.create(String.format("%.2f %.2f %.2f", x, y, z));
    }

    @Override
    public LocalizationKey getLocalizationKey() {
        return KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.CATEGORIZE;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ItemCategorizeAction that = (ItemCategorizeAction) o;
        return Objects.equals(predicates, that.predicates) && Objects.equals(thisVec, that.thisVec) && Objects.equals(otherVec, that.otherVec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicates, thisVec, otherVec);
    }
}
