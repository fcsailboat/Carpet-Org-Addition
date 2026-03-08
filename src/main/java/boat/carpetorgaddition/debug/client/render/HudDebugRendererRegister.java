package boat.carpetorgaddition.debug.client.render;

import boat.carpetorgaddition.client.render.Tooltip;
import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.debug.DebugSettings;
import boat.carpetorgaddition.exception.ProductionEnvironmentError;
import boat.carpetorgaddition.mixin.debug.accessor.ExperienceOrbEntityAccessor;
import boat.carpetorgaddition.mixin.debug.accessor.HandledScreenAccessor;
import boat.carpetorgaddition.mixin.debug.accessor.ScreenAccessor;
import boat.carpetorgaddition.util.IdentifierUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.Counter;
import boat.carpetorgaddition.wheel.SimpleCounter;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Contract;

import java.util.*;

public class HudDebugRendererRegister {
    private static final Map<Identifier, HudElement> renders = new HashMap<>();

    static {
        // 断言开发环境
        ProductionEnvironmentError.assertDevelopmentEnvironment();
    }

    static {
        // 显示方块挖掘速度
        renders.put(IdentifierUtils.ofIdentifier("block_destroy_speed"), (context, _) -> {
            if (DebugSettings.showBlockBreakingSpeed.get()) {
                HitResult hitResult = ClientUtils.getCrosshairTarget();
                if (hitResult == null) {
                    return;
                }
                if (hitResult instanceof BlockHitResult blockHitResult) {
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    LocalPlayer player = ClientUtils.getPlayer();
                    ServerLevel world = getServer().getLevel(ServerUtils.getWorld(player).dimension());
                    if (world == null) {
                        return;
                    }
                    BlockState blockState = world.getBlockState(blockPos);
                    if (blockState.isAir()) {
                        return;
                    }
                    float speed = player.getDestroySpeed(blockState);
                    String formatted = "%.2f".formatted(speed);
                    if (formatted.endsWith(".00")) {
                        formatted = formatted.substring(0, formatted.length() - 3);
                    } else if (formatted.endsWith("0")) {
                        formatted = formatted.substring(0, formatted.length() - 1);
                    }
                    Tooltip.drawTooltip(context, TextBuilder.create("挖掘速度：" + formatted));
                }
            }
        });
        // 渲染比较器强度
        renders.put(IdentifierUtils.ofIdentifier("comparator_level"), (context, _) -> {
            if (DebugSettings.showComparatorLevel.get()) {
                HitResult hitResult = ClientUtils.getCrosshairTarget();
                if (hitResult == null) {
                    return;
                }
                if (hitResult instanceof BlockHitResult blockHitResult) {
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    LocalPlayer player = ClientUtils.getPlayer();
                    ServerLevel world = getServer().getLevel(ServerUtils.getWorld(player).dimension());
                    if (world == null) {
                        return;
                    }
                    BlockEntity blockEntity = world.getChunkAt(blockPos).getBlockEntity(blockPos, LevelChunk.EntityCreationType.IMMEDIATE);
                    if (blockEntity instanceof ComparatorBlockEntity comparator) {
                        int level = comparator.getOutputSignal();
                        if (level == 0) {
                            return;
                        }
                        Tooltip.drawTooltip(context, TextBuilder.create("红石信号等级：" + level));
                    }
                }
            }
        });
        // 渲染灵魂沙物品数量
        renders.put(IdentifierUtils.ofIdentifier("soul_sand_item_count"), (context, _) -> {
            if (showSoulSandItemCount()) {
                HitResult hitResult = ClientUtils.getCrosshairTarget();
                if (hitResult == null) {
                    return;
                }
                if (hitResult instanceof BlockHitResult blockHitResult) {
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    ServerLevel world = getPlayerWorld();
                    BlockState blockState = world.getBlockState(blockPos);
                    if (blockState.isAir()) {
                        return;
                    }
                    if (blockState.is(Blocks.SOUL_SAND)) {
                        AABB box = new AABB(blockPos.above());
                        List<Entity> itemEntities;
                        try {
                            // 方法可能被多个线程调用
                            itemEntities = world.getEntitiesOfClass(Entity.class, box, EntitySelector.ENTITY_STILL_ALIVE);
                        } catch (ConcurrentModificationException e) {
                            return;
                        }
                        if (itemEntities.isEmpty()) {
                            return;
                        }
                        Counter<Item> counter = new SimpleCounter<>();
                        int experienceOrbEntityCount = 0;
                        int experienceOrbTotalValue = 0;
                        for (Entity entity : itemEntities) {
                            switch (entity) {
                                case ItemEntity itemEntity -> {
                                    ItemStack itemStack = itemEntity.getItem();
                                    counter.add(itemStack.getItem(), itemStack.getCount());
                                }
                                case ExperienceOrb experienceOrb -> {
                                    experienceOrbEntityCount++;
                                    ExperienceOrbEntityAccessor accessor = (ExperienceOrbEntityAccessor) experienceOrb;
                                    experienceOrbTotalValue += accessor.getPickingCount() * experienceOrb.getValue();
                                }
                                default -> {
                                }
                            }
                        }
                        List<Component> list = new ArrayList<>();
                        for (Item item : counter) {
                            int count = counter.getCount(item);
                            list.add(TextBuilder.combineAll(ServerUtils.getName(item), "*", count));
                        }
                        if (experienceOrbEntityCount != 0) {
                            list.add(TextBuilder.combineAll("经验球实体数量：", experienceOrbEntityCount));
                            list.add(TextBuilder.combineAll("经验球总价值：", experienceOrbTotalValue));
                        }
                        Tooltip.drawTooltip(context, list);
                    }
                }
            }
        });
        // 渲染当前HUD信息
        renders.put(IdentifierUtils.ofIdentifier("hud_information_display"), (context, _) -> {
            if (DebugSettings.HUDInformationDisplay.get()) {
                Minecraft client = ClientUtils.getClient();
                Screen screen = ClientUtils.getCurrentScreen();
                if (screen == null) {
                    return;
                }
                ArrayList<String> list = new ArrayList<>();
                String name = screen.getClass().getSimpleName();
                list.add("类名：" + name);
                MouseHandler mouse = ClientUtils.getMouse();
                double mouseX = mouse.xpos();
                double mouseY = mouse.ypos();
                list.add("鼠标X：" + (int) mouseX);
                list.add("鼠标Y：" + (int) mouseY);
                if (screen instanceof AbstractContainerScreen<?> handledScreen) {
                    try {
                        MenuType<?> type = handledScreen.getMenu().getType();
                        Identifier id = BuiltInRegistries.MENU.getKey(type);
                        if (id != null) {
                            list.add("屏幕类型id：" + id);
                        }
                    } catch (UnsupportedOperationException e) {
                        list.add("屏幕类型id：null");
                    }
                    HandledScreenAccessor accessor = (HandledScreenAccessor) handledScreen;
                    Window window = client.getWindow();
                    Slot slot = accessor.invokerGetSlotAt(
                            mouseX * (double) window.getGuiScaledWidth() / (double) window.getScreenWidth(),
                            mouseY * (double) window.getGuiScaledHeight() / (double) window.getScreenHeight()
                    );
                    if (slot != null) {
                        list.add("槽位索引：" + slot.index);
                        list.add("槽位物品栏：" + slot.container.getClass().getSimpleName());
                    }
                }
                context.setComponentTooltipForNextFrame(ClientUtils.getTextRenderer(), list.stream().map(Component::nullToEmpty).toList(), 3, 25);
            }
        });
        // 渲染当前HUD信息
        renders.put(IdentifierUtils.ofIdentifier("show_player_experience"), (context, _) -> {
            if (DebugSettings.showPlayerExperience.get() && ClientUtils.getCrosshairTarget() instanceof EntityHitResult hitResult) {
                Entity entity = hitResult.getEntity();
                if (entity instanceof Player) {
                    IntegratedServer server = ClientUtils.getServer();
                    if (server == null) {
                        return;
                    }
                    Player player = (Player) ClientUtils.getServerWorld().getEntity(entity.getId());
                    if (player == null) {
                        return;
                    }
                    Tooltip.drawTooltip(context, TextBuilder.create("经验等级：" + player.experienceLevel));
                }
            }
        });
    }

    public static void register() {
        for (Map.Entry<Identifier, HudElement> entry : renders.entrySet()) {
            HudElementRegistry.addLast(entry.getKey(), entry.getValue());
        }
        ScreenEvents.AFTER_INIT.register((_, screen, _, _) -> {
            if (DebugSettings.HUDInformationDisplay.get() && screen instanceof AbstractContainerScreen<?> handledScreen) {
                NonNullList<Slot> slots = handledScreen.getMenu().slots;
                slots.forEach(slot -> ((ScreenAccessor) screen).putDrawable((context, _, _, _) -> {
                    HandledScreenAccessor accessor = (HandledScreenAccessor) handledScreen;
                    int x = accessor.getX();
                    int y = accessor.getY();
                    context.drawString(
                            screen.getFont(),
                            String.valueOf(slot.index),
                            slot.x + x + 1,
                            slot.y + y,
                            0xFFFE9900,
                            true
                    );
                }));
            }
        });
    }

    // 是否应该显示灵魂沙物品计数
    private static boolean showSoulSandItemCount() {
        if (DebugSettings.showSoulSandItemCount.get()) {
            if (ClientUtils.getServer() == null) {
                return false;
            }
            if (ClientUtils.getCurrentScreen() == null) {
                return true;
            }
            return ClientUtils.getCurrentScreen() instanceof ChatScreen;
        }
        return false;
    }

    @Contract("-> !null")
    private static ClientLevel getClientWorld() {
        return ClientUtils.getWorld();
    }

    @Contract("-> !null")
    private static ServerLevel getPlayerWorld() {
        return getServer().getLevel(getClientWorld().dimension());
    }

    @Contract("-> !null")
    private static IntegratedServer getServer() {
        return ClientUtils.getServer();
    }
}
