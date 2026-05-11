package boat.carpetorgaddition.periodic.fakeplayer;

import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.ServerUtils;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class AStarPathfinder implements FakePlayerPathfinder {
    private final Supplier<EntityPlayerMPFake> supplier;
    private final Supplier<Optional<BlockPos>> target;
    private final List<Vec3> nodes = new ArrayList<>();

    public AStarPathfinder(Supplier<EntityPlayerMPFake> supplier, Supplier<Optional<BlockPos>> target) {
        this.supplier = supplier;
        this.target = target;
    }

    @Override
    public void tick() {
        this.pathfinding();
    }

    @Override
    public double length() {
        return this.nodes.size();
    }

    @Override
    public void pathfinding() {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        Optional<BlockPos> optional = this.target.get();
        if (optional.isEmpty()) {
            return;
        }
        BlockPos startPos = fakePlayer.blockPosition();
        BlockPos targetPos = optional.get();
        TreeSet<SearchNode> opens = new TreeSet<>();
        HashSet<BlockPos> clones = new HashSet<>();
        HashMap<BlockPos, SearchNode> openCaches = new HashMap<>();
        SearchNode node = new SearchNode(null, startPos, 0, estimatedCost(startPos, targetPos));
        opens.add(node);
        openCaches.put(startPos, node);
        this.nodes.clear();
        this.pathfinding(opens, openCaches, clones, targetPos);
    }

    private void pathfinding(TreeSet<SearchNode> opens, HashMap<BlockPos, SearchNode> openCaches, HashSet<BlockPos> clones, BlockPos target) {
        while (true) {
            if (opens.isEmpty()) {
                return;
            }
            SearchNode first = opens.getFirst();
            if (first.gCost > 100000) {
                return;
            }
            if (first.blockPos.equals(target)) {
                this.fillNodes(first);
                return;
            }
            opens.remove(first);
            openCaches.remove(first.blockPos);
            clones.add(first.blockPos);
            Level world = ServerUtils.getWorld(this.getFakePlayer());
            this.addSearchNode(world, first, target, opens, openCaches, clones);
        }
    }

    private void addSearchNode(Level world, SearchNode current, BlockPos target, TreeSet<SearchNode> opens, HashMap<BlockPos, SearchNode> openCaches, HashSet<BlockPos> clones) {
        BlockPos center = current.blockPos;
        for (BlockPos blockPos : around(center)) {
            if (clones.contains(blockPos)) {
                continue;
            }
            Relationship relationship = relationship(center, blockPos);
            SearchNode node = new SearchNode(current, blockPos, current.gCost + relationship.getCost(), this.estimatedCost(blockPos, target));
            if (isValidBlockPos(world, blockPos)) {
                SearchNode existing = openCaches.get(blockPos);
                if (existing == null) {
                    opens.add(node);
                    openCaches.put(node.blockPos, node);
                } else {
                    if (node.gCost < existing.gCost) {
                        opens.remove(existing);
                        opens.add(node);
                        openCaches.put(node.blockPos, node);
                    }
                }
            }
        }
    }

    private boolean isValidBlockPos(Level world, BlockPos blockPos) {
        if (world.getBlockState(blockPos.below()).isFaceSturdy(world, blockPos, Direction.UP)) {
            for (int i : List.of(0, 1)) {
                BlockState up = world.getBlockState(blockPos.above(i));
                if (up.isPathfindable(PathComputationType.LAND)) {
                    continue;
                }
                return false;
            }
            return true;
        }
        return false;
    }

    private int estimatedCost(BlockPos startPos, BlockPos targetPos) {
        return MathUtils.calculateManhattanDistance(startPos, targetPos) * 10;
    }

    private List<BlockPos> around(BlockPos blockPos) {
        ArrayList<BlockPos> list = new ArrayList<>();
        for (int i : List.of(-1, 0, 1)) {
            list.add(blockPos.offset(1, i, -1));
            list.add(blockPos.offset(1, i, 0));
            list.add(blockPos.offset(1, i, 1));
            list.add(blockPos.offset(0, i, 1));
            list.add(blockPos.offset(-1, i, 1));
            list.add(blockPos.offset(-1, i, 0));
            list.add(blockPos.offset(-1, i, -1));
            list.add(blockPos.offset(0, i, -1));
        }
        return list;
    }

    private Relationship relationship(BlockPos center, BlockPos around) {
        int distance = MathUtils.calculateHorizontalManhattanDistance(center, around);
        if (center.getY() > around.getY()) {
            if (distance == 1) {
                return Relationship.SIDE_DOWN;
            } else {
                return Relationship.DIAGONALLY_BELOW;
            }
        } else if (center.getY() < around.getY()) {
            if (distance == 1) {
                return Relationship.SIDE_TOP;
            } else {
                return Relationship.DIAGONALLY_ABOVE;
            }
        } else {
            if (distance == 1) {
                return Relationship.ADJACENT;
            } else {
                return Relationship.DIAGONAL;
            }
        }
    }

    private void fillNodes(SearchNode end) {
        SearchNode node;
        while ((node = end.parent) != null) {
            end = node;
            this.nodes.add(ServerUtils.getBlockBottomCenter(node.blockPos));
        }
    }

    @Override
    public Vec3 getCurrentNode() {
        return this.nodes.getFirst();
    }

    @Override
    public boolean arrivedAtAnyNode() {
        return false;
    }

    @Override
    public boolean backToBeforeNode() {
        return false;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public void pause(int time) {
    }

    @Override
    public void stop() {
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public boolean isInvalid() {
        return false;
    }

    @Override
    public boolean isInaccessible() {
        return false;
    }

    private EntityPlayerMPFake getFakePlayer() {
        return this.supplier.get();
    }

    public enum Relationship {
        /**
         * 相邻
         */
        ADJACENT(10),
        /**
         * 对角
         */
        DIAGONAL(12),
        /**
         * 侧上方
         */
        SIDE_TOP(15),
        /**
         * 侧下方
         */
        SIDE_DOWN(15),
        /**
         * 斜上方
         */
        DIAGONALLY_ABOVE(20),
        /**
         * 斜下方
         */
        DIAGONALLY_BELOW(20);
        private final int cost;

        Relationship(int cost) {
            this.cost = cost;
        }

        public int getCost() {
            return cost;
        }
    }

    public static class SearchNode implements Comparable<SearchNode> {
        /**
         * 上一个节点
         */
        private final SearchNode parent;
        /**
         * 当前位置
         */
        private final BlockPos blockPos;
        /**
         * 已经走过的实际距离
         */
        private final int gCost;
        /**
         * 到达终点的预估距离
         */
        @SuppressWarnings("FieldCanBeLocal")
        private final int hCost;
        /**
         * 预估总代价
         */
        private final int fCost;

        public SearchNode(SearchNode parent, BlockPos blockPos, int gCost, int hCost) {
            this.parent = parent;
            this.blockPos = blockPos;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
        }

        @Override
        public int compareTo(@NotNull AStarPathfinder.SearchNode o) {
            int fCompare = Integer.compare(this.fCost, o.fCost);
            if (fCompare != 0) {
                return fCompare;
            }
            int gCompare = Integer.compare(this.gCost, o.gCost);
            if (gCompare != 0) {
                return gCompare;
            }
            // 确保不同位置的节点不会因为成本相同而被TreeMap认为相同
            return this.blockPos.compareTo(o.blockPos);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (this.getClass() == obj.getClass()) {
                SearchNode other = (SearchNode) obj;
                return this.blockPos.equals(other.blockPos);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.blockPos.hashCode();
        }
    }
}
