package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.mixin.accessor.FishingBobberEntityAccessor;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class FishingAction extends AbstractPlayerAction {
    private int timer = 0;
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("fishing");

    public FishingAction(MinecraftServer server) {
        super(server);
    }

    @Override
    protected void tick() {
        // 检查玩家是否持有钓鱼竿
        if (pickFishingRod()) {
            // 检查玩家是否抛出钓竿
            FishingHook fishHook = this.getFakePlayer().fishing;
            if (fishHook == null) {
                // 检查计时器是否清零
                if (this.timer == 0) {
                    // 右键抛出钓鱼竿
                    this.use();
                } else {
                    this.timer--;
                }
            } else {
                // 如果钓鱼竿钩到方块或其它实体，通过切换物品收杆，防止额外的耐久损耗
                Entity entity = fishHook.getHookedIn();
                if (fishHook.onGround() || (entity != null && !(entity instanceof ItemEntity))) {
                    this.switchInventory();
                }
                // 检查鱼是否上钩
                if (this.canReelInTheFishingPole(fishHook)) {
                    // 右键收杆
                    this.use();
                    // 设置10个游戏刻后重新抛竿
                    this.timer = 10;
                }
            }
        }
    }

    /**
     * 将钓鱼竿拿到手上
     *
     * @return 物品栏是否有钓鱼竿
     */
    private boolean pickFishingRod() {
        // 如果玩家手上有钓鱼竿，无需切换
        if (this.getFakePlayer().getMainHandItem().is(Items.FISHING_ROD) || this.getFakePlayer().getOffhandItem().is(Items.FISHING_ROD)) {
            return true;
        }
        // 从物品栏拿取钓鱼竿
        Inventory inventory = this.getFakePlayer().getInventory();
        for (int i = 0; i < inventory.getNonEquipmentItems().size(); i++) {
            if (inventory.getItem(i).is(Items.FISHING_ROD)) {
                // 将非钓鱼竿物品放入主手
                inventory.pickSlot(i);
                if (this.getFakePlayer().getMainHandItem().is(Items.FISHING_ROD)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 通过切换物品栏收起钓鱼竿
     */
    private void switchInventory() {
        if (this.getFakePlayer().getOffhandItem().is(Items.FISHING_ROD)) {
            swapHands();
        }
        Inventory inventory = this.getFakePlayer().getInventory();
        // 查找物品栏内的非钓鱼竿物品
        for (int i = 0; i < inventory.getNonEquipmentItems().size(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            // 其它钓鱼竿物品不能与主手物品切换
            if (itemStack.is(Items.FISHING_ROD)) {
                continue;
            }
            // 将非钓鱼竿物品放入主手
            inventory.pickSlot(i);
            // 检查钓鱼竿是否切换成功（钓鱼竿不能与盔甲槽中的物品切换），如果成功，结束方法
            if (this.getFakePlayer().getMainHandItem().is(Items.FISHING_ROD)) {
                // 主手是钓鱼竿，切换失败
                continue;
            }
            return;
        }
    }

    /**
     * @return 是否可以收杆
     */
    private boolean canReelInTheFishingPole(FishingHook fishHook) {
        return ((FishingBobberEntityAccessor) fishHook).getHookCountdown() > 0;
    }

    /**
     * 右键
     */
    private void use() {
        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) this.getFakePlayer()).getActionPack();
        actionPack.start(EntityPlayerActionPack.ActionType.USE, EntityPlayerActionPack.Action.once());
    }

    /**
     * 交换主副手物品
     */
    private void swapHands() {
        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) this.getFakePlayer()).getActionPack();
        actionPack.start(EntityPlayerActionPack.ActionType.SWAP_HANDS, EntityPlayerActionPack.Action.once());
    }

    @Override
    public List<Component> info() {
        ArrayList<Component> list = new ArrayList<>();
        list.add(this.getInfoLocalizationKey().translate(this.getFakePlayer().getDisplayName()));
        return list;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject();
    }

    @Override
    public LocalizationKey getLocalizationKey() {
        return KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.FISHING;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        return this.getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 1;
    }
}
