package boat.carpetorgaddition.wheel.misc;

import boat.carpetorgaddition.periodic.fakeplayer.action.LibrarianTradeFindAction.Triple;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public interface LibrarianTradeTriples extends Iterable<Triple> {
    LibrarianTradeTriples EMPTY = new LibrarianTradeTriples() {
        @Override
        public List<Triple> getTriples() {
            return List.of();
        }

        @Override
        public List<Triple> testAndRemove(MerchantOffer offer) {
            return List.of();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    };

    List<Triple> getTriples();

    List<Triple> testAndRemove(MerchantOffer offer);

    int size();

    boolean isEmpty();

    @Override
    default @NonNull Iterator<Triple> iterator() {
        return this.getTriples().iterator();
    }

    static LibrarianTradeTriples of(Triple triple) {
        ArrayList<Triple> list = new ArrayList<>();
        list.add(triple);
        return new LibrarianTradeTriples() {
            @Override
            public List<Triple> getTriples() {
                return list;
            }

            @Override
            public List<Triple> testAndRemove(MerchantOffer offer) {
                ArrayList<Triple> result = new ArrayList<>();
                Iterator<Triple> it = list.iterator();
                while (it.hasNext()) {
                    Triple triple = it.next();
                    if (triple.test(offer)) {
                        Holder.Reference<Enchantment> enchantment = triple.enchantment();
                        int level = triple.getEnchantmentBookLevel(offer);
                        int price = offer.getBaseCostA().getCount();
                        result.add(new Triple(enchantment, level, price));
                        it.remove();
                    }
                }
                return result;
            }

            @Override
            public int size() {
                return list.size();
            }

            @Override
            public boolean isEmpty() {
                return list.isEmpty();
            }
        };
    }
}
