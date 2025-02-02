package org.carpetorgaddition.client.renderer.villagerpoi;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.carpetorgaddition.client.renderer.*;

import java.util.Objects;

public class VillagerPoiRenderer implements WorldRenderer {
    private final VillagerEntity villagerEntity;
    private final GlobalPos bedPos;
    private final GlobalPos jobSitePos;
    private final GlobalPos potentialJobSite;

    public VillagerPoiRenderer(VillagerEntity villagerEntity, GlobalPos bedPos, GlobalPos jobSitePos, GlobalPos potentialJobSite) {
        this.villagerEntity = villagerEntity;
        this.bedPos = bedPos;
        this.jobSitePos = jobSitePos;
        this.potentialJobSite = potentialJobSite;
    }

    @Override
    public void render(WorldRenderContext context) {
        MatrixStack matrixStack = context.matrixStack();
        if (matrixStack == null) {
            return;
        }
        // 相机距离村民过远时不渲染
        if (MinecraftClient.getInstance().gameRenderer.getCamera().getPos().distanceTo(villagerEntity.getPos()) > 96) {
            return;
        }
        float tickDelta = context.tickCounter().getTickProgress(true);
        Vec3d leashPos = this.villagerEntity
                .getLerpedPos(tickDelta)
                .add(new Vec3d(0.0, this.villagerEntity.getHeight() * 0.6, 0.0));
        Frustum frustum = context.frustum();
        if (frustum == null) {
            return;
        }
        // 渲染床位置
        if (this.bedPos != null) {
            new LineRenderer(leashPos, bedPos.pos().toCenterPos()).render(matrixStack);
            if (canRender(this.bedPos, frustum)) {
                BoxRenderer renderer = createBedRenderer();
                renderer.setSeeThroughLine(false);
                renderer.setFaceColor(1F, 0.9F, 0.2F, 0.4F);
                renderer.render(matrixStack);
            }
        }
        if (this.jobSitePos != null) {
            // 渲染工作方块位置
            LineRenderer lineRenderer = new LineRenderer(leashPos, this.jobSitePos.pos().toCenterPos());
            lineRenderer.setColor(new Color(0.1F, 0.75F, 0.4F, 1F));
            lineRenderer.render(matrixStack);
            if (this.canRender(this.jobSitePos, frustum)) {
                this.getBlockOutlineRender(this.jobSitePos.pos()).render(matrixStack);
            }
        } else if (this.potentialJobSite != null) {
            // 渲染正在绑定的工作方块位置
            LineRenderer lineRenderer = new LineRenderer(leashPos, this.potentialJobSite.pos().toCenterPos());
            lineRenderer.setColor(new Color(0.8F, 0.4F, 0.9F, 1F));
            lineRenderer.render(matrixStack);
            if (this.canRender(this.potentialJobSite, frustum)) {
                this.getBlockOutlineRender(this.potentialJobSite.pos()).render(matrixStack);
            }
        }
    }

    // 兴趣点是否在渲染范围内
    private boolean canRender(GlobalPos globalPos, Frustum frustum) {
        return frustum.isVisible(new Box(globalPos.pos()));
    }

    // 创建床渲染器
    private BoxRenderer createBedRenderer() {
        // 渲染床位置
        World world = this.villagerEntity.getWorld();
        BlockPos bedPos = this.bedPos.pos();
        BlockState blockState = world.getBlockState(bedPos);
        // 渲染床轮廓
        if (blockState.getBlock() instanceof BedBlock && blockState.get(BedBlock.PART) == BedPart.HEAD) {
            // 检查是否有床尾
            Direction direction = blockState.get(HorizontalFacingBlock.FACING).getOpposite();
            BlockPos offset = bedPos.offset(direction);
            BlockState bedTailBlockState = world.getBlockState(offset);
            Box box;
            if (bedTailBlockState.getBlock() instanceof BedBlock && bedTailBlockState.get(BedBlock.PART) == BedPart.FOOT) {
                // 有床尾
                box = new Box(
                        Math.min(bedPos.getX(), offset.getX()) + 0.0,
                        Math.min(bedPos.getY(), offset.getY()) + 0.18,
                        Math.min(bedPos.getZ(), offset.getZ()) + 0.0,
                        Math.max(bedPos.getX(), offset.getX()) + 1.0,
                        Math.max(bedPos.getY(), offset.getY()) + 0.625,
                        Math.max(bedPos.getZ(), offset.getZ()) + 1.0
                );
            } else {
                // 无床尾
                box = new Box(
                        bedPos.getX() + 0.0,
                        bedPos.getY() + 0.18,
                        bedPos.getZ() + 0.0,
                        bedPos.getX() + 1.0,
                        bedPos.getY() + 0.625,
                        bedPos.getZ() + 1.0
                );
            }
            return new BoxRenderer(box.expand(0.001));
        } else {
            return new BoxRenderer(new Box(bedPos).expand(-0.001));
        }
    }

    // 生成方块轮廓渲染器
    private BlockOutlineRender getBlockOutlineRender(BlockPos blockPos) {
        ClientWorld world = Objects.requireNonNull(MinecraftClient.getInstance().world);
        BlockState blockState = world.getBlockState(blockPos);
        if (blockState.isAir()) {
            return new BlockOutlineRender(blockPos, VoxelShapes.fullCube());
        } else {
            return new BlockOutlineRender(blockPos);
        }
    }

    public boolean shouldStop() {
        return this.villagerEntity.isRemoved();
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass() == obj.getClass()) {
            return this.villagerEntity.equals(((VillagerPoiRenderer) obj).villagerEntity);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.villagerEntity.hashCode();
    }
}
