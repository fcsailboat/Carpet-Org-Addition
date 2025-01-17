package org.carpetorgaddition.periodic.fakeplayer.actiondata;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.text.MutableText;
import org.apache.commons.lang3.mutable.MutableInt;
import org.carpetorgaddition.util.TextUtils;

import java.util.ArrayList;

public class FishingData extends AbstractActionData {
    private final MutableInt timer = new MutableInt();

    public FishingData() {
    }

    @Override
    public ArrayList<MutableText> info(EntityPlayerMPFake fakePlayer) {
        ArrayList<MutableText> list = new ArrayList<>();
        list.add(TextUtils.translate("carpet.commands.playerAction.info.fishing", fakePlayer.getDisplayName()));
        return list;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject();
    }

    public MutableInt getTimer() {
        return timer;
    }
}
