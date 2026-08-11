package boat.carpetorgaddition.wheel.traverser;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CylinderBlockPosTraverserTest {
    @Test
    public void testRandomBlockPos() {
        CylinderBlockPosTraverser region = new CylinderBlockPosTraverser(new BlockPos(0, 0, 0), 1, 1);
        System.out.println(region.randomBlockPos());
    }

    @Test
    public void testIterator() {
        BlockPos center = new BlockPos(0, 0, 0);
        Assertions.assertEquals(0L, new CylinderBlockPosTraverser(center, 15, 0).size());
        Assertions.assertEquals(9L, new CylinderBlockPosTraverser(center, 1, 1).size());
        Assertions.assertEquals(21L, new CylinderBlockPosTraverser(center, 2, 1).size());
        Assertions.assertEquals(37L, new CylinderBlockPosTraverser(center, 3, 1).size());
        Assertions.assertEquals(74L, new CylinderBlockPosTraverser(center, 3, 2).size());
        Assertions.assertEquals(111L, new CylinderBlockPosTraverser(center, 3, 3).size());
    }
}
