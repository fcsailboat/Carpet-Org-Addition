package boat.carpetorgaddition.wheel.misc;

import boat.carpetorgaddition.periodic.fakeplayer.action.LibrarianTradeFindAction.Triple;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.*;

public class LibrarianTradeGroupManager {
    private final Map<String, Map<Holder.Reference<Enchantment>, Triple>> groups = new HashMap<>();

    public LibrarianTradeGroupManager() {
    }

    public boolean add(String group, Triple triple) {
        Map<Holder.Reference<Enchantment>, Triple> triples = this.groups.computeIfAbsent(group.toLowerCase(Locale.ROOT), _ -> new HashMap<>());
        return triples.putIfAbsent(triple.enchantment(), triple) == null;
    }

    public boolean remove(String group, Holder.Reference<Enchantment> enchantment) {
        String lowerCase = group.toLowerCase(Locale.ROOT);
        Map<Holder.Reference<Enchantment>, Triple> triples = this.groups.get(lowerCase);
        if (triples == null) {
            return false;
        }
        if (triples.containsKey(enchantment) && triples.remove(enchantment) != null) {
            if (triples.isEmpty()) {
                this.groups.remove(lowerCase, triples);
            }
            return true;
        }
        return false;
    }

    public boolean remove(String group) {
        String lowerCase = group.toLowerCase(Locale.ROOT);
        return this.groups.containsKey(lowerCase) && this.groups.remove(lowerCase) != null;
    }

    public List<String> listGroup() {
        return this.groups.keySet().stream().toList();
    }

    public List<Holder.Reference<Enchantment>> listEnchantment(String group) {
        return this.groups.getOrDefault(group.toLowerCase(Locale.ROOT), Map.of()).keySet().stream().toList();
    }

    public LibrarianTradeTriples getTriples(String group) {
        Map<Holder.Reference<Enchantment>, Triple> tripleMap = this.groups.get(group.toLowerCase(Locale.ROOT));
        if (tripleMap == null) {
            return LibrarianTradeTriples.EMPTY;
        }
        return new LibrarianTradeTriples() {
            @Override
            public List<Triple> getTriples() {
                return tripleMap.values().stream().toList();
            }

            @Override
            public List<Triple> testAndRemove(MerchantOffer offer) {
                Iterator<Map.Entry<Holder.Reference<Enchantment>, Triple>> it = tripleMap.entrySet().iterator();
                ArrayList<Triple> list = new ArrayList<>();
                while (it.hasNext()) {
                    Triple triple = it.next().getValue();
                    if (triple.test(offer)) {
                        Holder.Reference<Enchantment> enchantment = triple.enchantment();
                        int level = triple.getEnchantmentBookLevel(offer);
                        int price = offer.getBaseCostA().getCount();
                        list.add(new Triple(enchantment, level, price));
                        it.remove();
                    }
                }
                return list;
            }

            @Override
            public int size() {
                return tripleMap.size();
            }

            @Override
            public boolean isEmpty() {
                return tripleMap.isEmpty();
            }
        };
    }
}
