package org.carpetorgaddition.periodic.fakeplayer.actioncontext;

import carpet.patches.EntityPlayerMPFake;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerBreakBedrock;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerBreakBedrock.BedrockDestructor;
import org.carpetorgaddition.util.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class BreakBedrockContext extends AbstractActionContext implements Iterable<BedrockDestructor> {
    private final HashSet<BedrockDestructor> hashSet = new HashSet<>();

    @Override
    public ArrayList<MutableText> info(EntityPlayerMPFake fakePlayer) {
        return Lists.newArrayList(TextUtils.translate("carpet.commands.playerAction.info.bedrock", fakePlayer.getDisplayName()));
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject();
    }

    public void add(BedrockDestructor destructor) {
        this.hashSet.add(destructor);
    }

    public void remove() {
        this.hashSet.removeIf(destructor -> destructor.getState() == FakePlayerBreakBedrock.State.COMPLETE);
    }

    public boolean isEmpty() {
        return this.hashSet.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<BedrockDestructor> iterator() {
        return this.hashSet.iterator();
    }
}
