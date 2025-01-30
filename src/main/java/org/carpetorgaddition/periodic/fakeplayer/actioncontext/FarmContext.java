package org.carpetorgaddition.periodic.fakeplayer.actioncontext;

import carpet.patches.EntityPlayerMPFake;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.util.TextUtils;

import java.util.ArrayList;

public class FarmContext extends AbstractActionContext {
    @Override
    public ArrayList<MutableText> info(EntityPlayerMPFake fakePlayer) {
        return Lists.newArrayList(TextUtils.translate("carpet.commands.playerAction.info.farm", fakePlayer.getDisplayName()));
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject();
    }
}
