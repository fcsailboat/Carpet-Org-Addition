package boat.carpetorgaddition.client.render.hud;

import boat.carpetorgaddition.client.render.Tooltip;
import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.network.c2s.LibrarianCommodityQueryC2SPacket;
import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.misc.LibrarianCommodityEntry;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NullMarked
public class LibrarianCommodityTooltip implements HudElement {
    private static final LibrarianCommodityTooltip INSTANCE = new LibrarianCommodityTooltip();
    private static final LocalizationKey KEY = LocalizationKeys.LOGGER.then("librarian");
    private LibrarianCommodityEntry offer = LibrarianCommodityEntry.EMPTY;
    @Nullable
    private BlockPos previous = null;
    private boolean enable = false;

    private LibrarianCommodityTooltip() {
    }

    public static void init() {
        HudElementRegistry.addFirst(ServerUtils.ofIdentifier("librarian_commodity_tooltip"), INSTANCE);
    }

    public static LibrarianCommodityTooltip getInstance() {
        return INSTANCE;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (this.enable) {
            if (ClientUtils.getCrosshairTarget() instanceof BlockHitResult hitResult) {
                ClientLevel world = ClientUtils.getWorld();
                BlockPos blockPos = hitResult.getBlockPos();
                BlockPos prev = this.previous;
                this.previous = blockPos;
                if (world.getBlockState(blockPos).is(Blocks.LECTERN)) {
                    if (Objects.equals(this.offer.blockPos(), blockPos)) {
                        this.renderTooltip(graphics);
                    } else {
                        // 确保在改变目标方块前只发送一次网络数据包
                        if (!Objects.equals(prev, blockPos)) {
                            ClientPlayNetworking.send(new LibrarianCommodityQueryC2SPacket(new GlobalPos(world.dimension(), blockPos)));
                        }
                    }
                    return;
                }
            } else {
                this.previous = null;
            }
            this.setOffer(LibrarianCommodityEntry.EMPTY);
        }
    }

    private void renderTooltip(GuiGraphicsExtractor graphics) {
        ItemStack commodity = this.offer.commodity();
        if (commodity.isEmpty()) {
            Tooltip.drawTooltip(graphics, KEY.then("no_enchanted_book").translate());
        } else {
            ItemEnchantments enchantments = commodity.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments != null) {
                enchantments.entrySet().stream().findFirst().ifPresent(entry -> renderTooltip(graphics, entry, commodity));
            }
        }
    }

    private void renderTooltip(GuiGraphicsExtractor graphics, Object2IntMap.Entry<Holder<Enchantment>> entry, ItemStack commodity) {
        Minecraft client = ClientUtils.getClient();
        Font font = ClientUtils.getTextRenderer();
        Holder<Enchantment> holder = entry.getKey();
        int level = entry.getIntValue();
        Component name = EnchantmentUtils.getName(holder, level);
        int height = client.getWindow().getGuiScaledHeight();
        int width = client.getWindow().getGuiScaledWidth();
        graphics.item(commodity, width / 2 + 8, height / 2);
        ArrayList<Component> list = new ArrayList<>();
        list.add(name);
        Int2IntMap.Entry range = getPriceRange(holder, level);
        PriceLevel priceLevel = this.getPriceLevel(this.offer.price(), range.getIntKey(), range.getIntValue());
        list.add(
                KEY.then("price")
                        .builder(this.offer.price(), range.getIntKey(), range.getIntValue())
                        .setColor(priceLevel.getColor())
                        .build()
        );
        Enchantment enchantment = holder.value();
        int maxLevel = enchantment.getMaxLevel();
        list.add(
                KEY.then("level")
                        .builder(level, maxLevel)
                        .setColor(level >= maxLevel ? ChatFormatting.GOLD : ChatFormatting.GRAY)
                        .build()
        );
        List<ClientTooltipComponent> components = list.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).toList();
        graphics.tooltip(font, components, width / 2 + 20, height / 2 + 25, DefaultTooltipPositioner.INSTANCE, null);
    }

    private Int2IntMap.Entry getPriceRange(Holder<Enchantment> enchantment, int level) {
        int min = 2 + level * 3;
        int max = 6 + level * 13;
        if (enchantment.is(EnchantmentTags.DOUBLE_TRADE_PRICE)) {
            return Int2IntMap.entry(min * 2, max * 2);
        } else {
            return Int2IntMap.entry(min, max);
        }
    }

    private PriceLevel getPriceLevel(int price, int min, int max) {
        int total = max - min + 1;
        if (price < min + total / 3) {
            return PriceLevel.LOW;
        } else if (price < min + (2 * total) / 3) {
            return PriceLevel.MEDIUM;
        } else {
            return PriceLevel.HIGH;
        }
    }

    public void setOffer(LibrarianCommodityEntry offer) {
        this.offer = offer;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public void failure() {
        this.previous = null;
    }

    public enum PriceLevel {
        LOW,
        MEDIUM,
        HIGH;

        private ChatFormatting getColor() {
            return switch (this) {
                case LOW -> ChatFormatting.GREEN;
                case MEDIUM -> ChatFormatting.YELLOW;
                case HIGH -> ChatFormatting.DARK_RED;
            };
        }
    }
}
