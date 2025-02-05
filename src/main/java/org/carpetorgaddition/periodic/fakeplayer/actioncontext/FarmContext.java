package org.carpetorgaddition.periodic.fakeplayer.actioncontext;

import carpet.patches.EntityPlayerMPFake;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.util.TextUtils;

import java.util.ArrayList;

public class FarmContext extends AbstractActionContext {
    /**
     * 当前正在采集的农作物
     */
    private BlockPos CropPos;

    @Override
    public ArrayList<MutableText> info(EntityPlayerMPFake fakePlayer) {
        return Lists.newArrayList(TextUtils.translate("carpet.commands.playerAction.info.farm", fakePlayer.getDisplayName()));
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject();
    }

    public BlockPos getCropPos() {
        return this.CropPos;
    }

    public void setCropPos(BlockPos cropPos) {
        this.CropPos = cropPos;
    }
}
