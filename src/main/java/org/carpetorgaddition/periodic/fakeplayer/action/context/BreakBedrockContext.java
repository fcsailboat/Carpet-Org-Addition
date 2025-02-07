package org.carpetorgaddition.periodic.fakeplayer.action.context;

import carpet.patches.EntityPlayerMPFake;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.periodic.fakeplayer.action.FakePlayerBreakBedrock.BedrockDestructor;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.wheel.SelectionArea;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Predicate;

public class BreakBedrockContext extends AbstractActionContext implements Iterable<BedrockDestructor> {
    private final HashSet<BedrockDestructor> hashSet = new HashSet<>();
    private final SelectionArea selectionArea;

    public BreakBedrockContext(BlockPos from, BlockPos to) {
        this.selectionArea = new SelectionArea(from, to);
    }

    public BreakBedrockContext(JsonObject json) {
        JsonArray from = json.getAsJsonArray("from");
        JsonArray to = json.getAsJsonArray("to");
        BlockPos minPos = new BlockPos(from.get(0).getAsInt(), from.get(1).getAsInt(), from.get(2).getAsInt());
        BlockPos maxPos = new BlockPos(to.get(0).getAsInt(), to.get(1).getAsInt(), to.get(2).getAsInt());
        this.selectionArea = new SelectionArea(minPos, maxPos);
    }

    @Override
    public ArrayList<MutableText> info(EntityPlayerMPFake fakePlayer) {
        return Lists.newArrayList(TextUtils.translate("carpet.commands.playerAction.info.bedrock", fakePlayer.getDisplayName()));
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        BlockPos minBlockPos = this.selectionArea.getMinBlockPos();
        JsonArray from = new JsonArray();
        from.add(minBlockPos.getX());
        from.add(minBlockPos.getY());
        from.add(minBlockPos.getZ());
        json.add("from", from);
        BlockPos maxBlockPos = this.selectionArea.getMaxBlockPos();
        JsonArray to = new JsonArray();
        to.add(maxBlockPos.getX());
        to.add(maxBlockPos.getY());
        to.add(maxBlockPos.getZ());
        json.add("to", to);
        return json;
    }

    public void add(BedrockDestructor destructor) {
        this.hashSet.add(destructor);
    }

    public void removeIf(Predicate<BedrockDestructor> predicate) {
        this.hashSet.removeIf(predicate);
    }

    public boolean contains(BlockPos blockPos) {
        return this.selectionArea.contains(blockPos);
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
