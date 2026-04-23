package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RenameAction extends AbstractPlayerAction {
    /**
     * 要进行重命名的物品
     */
    private final Item item;
    /**
     * 物品的新名称
     */
    private final String newName;
    private boolean canSendMessage = true;
    public static final String ITEM = "item";
    public static final String NEW_NAME = "new_name";
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("rename");

    public RenameAction(EntityPlayerMPFake fakePlayer, Item item, String newName) {
        super(fakePlayer);
        this.item = item;
        this.newName = newName;
    }

    @Override
    protected void tick() {
        // 如果假玩家对铁砧持续按住右键，就会一直打开新的铁砧界面，同时旧的铁砧界面会自动关闭，关闭旧的铁砧界面时，铁砧内的物品会回到玩家物品栏
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        if (fakePlayer.containerMenu instanceof AnvilMenu anvilScreenHandler) {
            // 如果假玩家没有足够的经验，直接结束方法，创造玩家给物品重命名不需要消耗经验
            if (fakePlayer.experienceLevel < 1 && !fakePlayer.isCreative()) {
                if (this.canSendMessage) {
                    LocalizationKey key = KEY.then("wait");
                    MessageUtils.sendMessage(this.getServer(), key.translate(fakePlayer.getDisplayName(), this.getDisplayName()));
                    this.canSendMessage = false;
                }
                return;
            }
            Slot oneSlot = anvilScreenHandler.getSlot(0);
            // 第一个槽位的物品是否正确：是指定物品，没有被正确重命名，已经最大堆叠
            boolean oneSlotCorrect = false;
            // 判断第一个槽位是否有物品
            if (oneSlot.hasItem()) {
                ItemStack itemStack = oneSlot.getItem();
                // 判断该槽位的物品是否已经正确重命名
                if (itemStack.getHoverName().getString().equals(newName) || !itemStack.is(item)) {
                    // 如果已经重命名，或者当前槽位不是指定物品，丢出该槽位的物品
                    // 因为该槽位的物品被丢弃，所以该槽位已经没有物品，没有必要继续判断，直接结束方法
                    FakePlayerUtils.pickupAndThrow(anvilScreenHandler, 0, fakePlayer);
                    return;
                } else {
                    // 判断当前物品堆栈对象是否为指定物品
                    // 让物品最大堆叠后才能重命名，节省经验
                    if (itemStack.getCount() == itemStack.getMaxStackSize()) {
                        oneSlotCorrect = true;
                    }
                }
            }
            // 遍历玩家物品栏，找到指定需要重命名的物品
            // 第一个槽位的物品必须是正确的
            for (int index = 3; !oneSlotCorrect && index < anvilScreenHandler.slots.size(); index++) {
                // 这里遍历的是玩家物品栏
                if (anvilScreenHandler.getSlot(index).hasItem()
                    && anvilScreenHandler.getSlot(index).getItem().is(item)) {
                    // 找到指定物品后，模拟按住Shift键将物品移动到铁砧输入槽，然后跳出for循环
                    FakePlayerUtils.quickMove(anvilScreenHandler, index, fakePlayer);
                    break;
                }
                // 如果遍历完物品栏还是没有找到指定物品，认为玩家物品栏中已经没有指定物品，结束方法
                if (index == anvilScreenHandler.slots.size() - 1) {
                    return;
                }
            }
            // 获取铁砧第二个输入槽
            Slot twoSlot = anvilScreenHandler.getSlot(1);
            // 判断该槽位是否有物品
            if (twoSlot.hasItem()) {
                // 如果有，移动到物品栏，如果不能移动，直接丢出
                FakePlayerUtils.quickMove(anvilScreenHandler, 1, fakePlayer);
                if (twoSlot.hasItem()) {
                    FakePlayerUtils.pickupAndThrow(anvilScreenHandler, 1, fakePlayer);
                }
            }
            // 判断第一个输入槽是否正确，第二个格子是否没有物品
            if (oneSlotCorrect && !twoSlot.hasItem()) {
                // 设置物品名称
                anvilScreenHandler.setItemName(newName);
                // 判断是否可以取出输出槽的物品
                if (anvilScreenHandler.getSlot(2).hasItem() && canTakeOutput(anvilScreenHandler)) {
                    // 丢出输出槽的物品
                    FakePlayerUtils.pickupAndThrow(anvilScreenHandler, 2, fakePlayer);
                }
            }
        }
    }

    // 判断是否可以输出物品
    private boolean canTakeOutput(AnvilMenu screenHandler) {
        if (this.getFakePlayer().getAbilities().instabuild || this.getFakePlayer().experienceLevel >= screenHandler.getCost()) {
            return screenHandler.getCost() > 0;
        }
        return false;
    }

    @Override
    public void onFakePlayerLogout() {
        this.getFakePlayerNullable().ifPresent(PlayerUtils::closeScreen);
    }

    @Override
    public List<Component> info() {
        ArrayList<Component> list = new ArrayList<>();
        // 获取假玩家的显示名称
        Component playerName = getFakePlayer().getDisplayName();
        // 将假玩家要重命名的物品和物品新名称的信息添加到集合
        LocalizationKey key = this.getInfoLocalizationKey();
        list.add(key.translate(playerName, this.item.getDefaultInstance().getDisplayName(), newName));
        // 将假玩家剩余经验的信息添加到集合
        list.add(key.then("xp").translate(getFakePlayer().experienceLevel));
        if (getFakePlayer().containerMenu instanceof AnvilMenu anvilScreenHandler) {
            // 将铁砧GUI上的物品信息添加到集合
            list.add(TextBuilder.combineAll("    ",
                    FakePlayerUtils.getWithCountHoverText(anvilScreenHandler.getSlot(0).getItem()), " ",
                    FakePlayerUtils.getWithCountHoverText(anvilScreenHandler.getSlot(1).getItem()), " -> ",
                    FakePlayerUtils.getWithCountHoverText(anvilScreenHandler.getSlot(2).getItem())));
        } else {
            // 将假玩家没有打开铁砧的信息添加到集合
            list.add(key.then("no_anvil").translate(playerName, Blocks.ANVIL.getName()));
        }
        return list;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(ITEM, BuiltInRegistries.ITEM.getKey(item).toString());
        json.addProperty(NEW_NAME, this.newName);
        return json;
    }

    @Override
    public LocalizationKey getLocalizationKey() {
        return KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.RENAME;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RenameAction that = (RenameAction) o;
        return canSendMessage == that.canSendMessage && Objects.equals(item, that.item) && Objects.equals(newName, that.newName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, newName, canSendMessage);
    }
}
