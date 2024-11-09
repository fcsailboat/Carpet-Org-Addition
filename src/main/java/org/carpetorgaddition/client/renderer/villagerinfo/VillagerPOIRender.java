package org.carpetorgaddition.client.renderer.villagerinfo;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.carpetorgaddition.client.renderer.BlockOutlineRender;
import org.carpetorgaddition.client.renderer.Color;
import org.carpetorgaddition.client.renderer.LineRender;

import java.util.Objects;

public class VillagerPOIRender {
    private final VillagerEntity villagerEntity;
    private final GlobalPos bedPos;
    private final GlobalPos jobSitePos;
    private final GlobalPos potentialJobSite;

    public VillagerPOIRender(VillagerEntity villagerEntity, GlobalPos bedPos, GlobalPos jobSitePos, GlobalPos potentialJobSite) {
        this.villagerEntity = villagerEntity;
        this.bedPos = bedPos;
        this.jobSitePos = jobSitePos;
        this.potentialJobSite = potentialJobSite;
    }

    public void render(MatrixStack matrixStack, RenderTickCounter tickCounter) {
        float tickDelta = tickCounter.getTickDelta(true);
        Vec3d leashPos = this.villagerEntity
                .getLerpedPos(tickDelta)
                .add(new Vec3d(0.0, this.villagerEntity.getHeight() * 0.6, 0.0));
        if (this.bedPos != null) {
            // 渲染床位置
            World world = this.villagerEntity.getWorld();
            BlockState blockState = world.getBlockState(this.bedPos.pos());
            // TODO 床的渲染不明显
            new LineRender(leashPos, bedPos.pos().toCenterPos()).render(matrixStack);
            // 渲染床轮廓
            if (blockState.getBlock() instanceof BedBlock && blockState.get(BedBlock.PART) == BedPart.HEAD) {
                // 渲染床头轮廓
                new BlockOutlineRender(this.bedPos.pos()).render(matrixStack);
                // 渲染床尾轮廓
                Direction direction = blockState.get(HorizontalFacingBlock.FACING).getOpposite();
                BlockPos offset = this.bedPos.pos().offset(direction);
                BlockState bedTailBlockState = world.getBlockState(offset);
                if (bedTailBlockState.getBlock() instanceof BedBlock && bedTailBlockState.get(BedBlock.PART) == BedPart.FOOT) {
                    new BlockOutlineRender(offset).render(matrixStack);
                }
            } else {
                this.getBlockOutlineRender(this.bedPos.pos()).render(matrixStack);
            }
        }
        if (this.jobSitePos != null) {
            // 渲染工作方块位置
            LineRender lineRender = new LineRender(leashPos, this.jobSitePos.pos().toCenterPos());
            lineRender.setColor(new Color(0.1F, 0.75F, 0.4F, 1F));
            lineRender.render(matrixStack);
            this.getBlockOutlineRender(this.jobSitePos.pos()).render(matrixStack);
        } else if (this.potentialJobSite != null) {
            // 渲染正在绑定的工作方块位置
            LineRender lineRender = new LineRender(leashPos, this.potentialJobSite.pos().toCenterPos());
            lineRender.setColor(new Color(0.8F, 0.4F, 0.9F, 1F));
            lineRender.render(matrixStack);
            this.getBlockOutlineRender(this.potentialJobSite.pos()).render(matrixStack);
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
            return this.villagerEntity.equals(((VillagerPOIRender) obj).villagerEntity);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.villagerEntity.hashCode();
    }
}
