package boat.carpetorgaddition.wheel.misc;

import boat.carpetorgaddition.periodic.fakeplayer.action.LibrarianTradeFindAction.Triple;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibrarianTradeGroupManager {
    private final Map<String, Map<Holder.Reference<Enchantment>, Triple>> groups = new HashMap<>();

    public LibrarianTradeGroupManager() {
    }

    public boolean add(String group, Triple triple) {
        Map<Holder.Reference<Enchantment>, Triple> triples = this.groups.computeIfAbsent(group, _ -> new HashMap<>());
        return triples.putIfAbsent(triple.enchantment(), triple) == null;
    }

    public boolean remove(String group, Holder.Reference<Enchantment> enchantment) {
        Map<Holder.Reference<Enchantment>, Triple> triples = this.groups.get(group);
        if (triples == null) {
            return false;
        }
        if (triples.containsKey(enchantment) && triples.remove(enchantment) != null) {
            if (triples.isEmpty()) {
                this.groups.remove(group, triples);
            }
            return true;
        }
        return false;
    }

    public boolean remove(String group) {
        return this.groups.containsKey(group) && this.groups.remove(group) != null;
    }

    public List<String> listGroup() {
        return this.groups.keySet().stream().toList();
    }

    public List<Holder.Reference<Enchantment>> listEnchantment(String group) {
        return this.groups.getOrDefault(group, Map.of()).keySet().stream().toList();
    }
}
