package boat.carpetorgaddition.client.render.hud;

import boat.carpetorgaddition.client.render.Tooltip;
import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.network.c2s.LibrarianCommodityQueryC2SPacket;
import boat.carpetorgaddition.periodic.fakeplayer.action.LibrarianTradeFindAction;
import boat.carpetorgaddition.periodic.fakeplayer.action.LibrarianTradeFindAction.PriceLevel;
import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.misc.LibrarianCommodityEntry;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@NullMarked
public class LibrarianCommodityTooltip implements HudElement {
    private static final LibrarianCommodityTooltip INSTANCE = new LibrarianCommodityTooltip();
    private static final LocalizationKey KEY = LocalizationKeys.LOGGER.then("librarian");
    private LibrarianCommodityEntry offers = LibrarianCommodityEntry.EMPTY;
    @Nullable
    private BlockPos previous = null;
    private boolean enable = false;

    private LibrarianCommodityTooltip() {
    }

    public static void init() {
        HudElementRegistry.addFirst(ServerUtils.ofIdentifier("librarian_commodity_tooltip"), INSTANCE);
        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> INSTANCE.setEnable(false));
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
                    if (Objects.equals(this.offers.blockPos(), blockPos)) {
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
            this.setOffers(LibrarianCommodityEntry.EMPTY);
        }
    }

    private void renderTooltip(GuiGraphicsExtractor graphics) {
        List<Map.Entry<Integer, ItemStack>> offers = this.offers.offers();
        if (offers.isEmpty()) {
            Tooltip.drawTooltip(graphics, KEY.then("no_enchanted_book").translate());
            return;
        }
        Minecraft client = ClientUtils.getClient();
        int height = client.getWindow().getGuiScaledHeight();
        int width = client.getWindow().getGuiScaledWidth();
        ArrayList<Component> list = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) {
            Map.Entry<Integer, ItemStack> offer = offers.get(i);
            ItemStack commodity = offer.getValue();
            ItemEnchantments enchantments = commodity.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments == null) {
                continue;
            }
            int offset = i;
            enchantments.entrySet().stream().findFirst().ifPresent(entry -> {
                graphics.item(commodity, width / 2 + 8, (height / 2) + offset * 18);
                Holder<Enchantment> holder = entry.getKey();
                int level = entry.getIntValue();
                Component name = EnchantmentUtils.getName(holder, level);
                list.add(name);
                Int2IntMap.Entry range = LibrarianTradeFindAction.getPriceRange(holder, level);
                int price = offer.getKey();
                PriceLevel priceLevel = PriceLevel.getPriceLevel(price, range.getIntKey(), range.getIntValue());
                list.add(
                        KEY.then("price")
                                .builder(price, range.getIntKey(), range.getIntValue())
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
                if (offset < offers.size() - 1) {
                    list.add(TextBuilder.empty());
                }
            });
        }
        Font font = ClientUtils.getTextRenderer();
        List<ClientTooltipComponent> components = list.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).toList();
        graphics.tooltip(font, components, width / 2 + 20, height / 2 + 25 - ((offers.size() - 1) * 20), DefaultTooltipPositioner.INSTANCE, null);
    }

    public void setOffers(LibrarianCommodityEntry offers) {
        this.offers = offers;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public void failure() {
        this.previous = null;
    }
}
