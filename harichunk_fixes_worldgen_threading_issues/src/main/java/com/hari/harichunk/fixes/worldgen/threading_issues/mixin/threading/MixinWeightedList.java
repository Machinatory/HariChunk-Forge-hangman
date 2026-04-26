package com.hari.harichunk.fixes.worldgen.threading_issues.mixin.threading;

import com.hari.harichunk.base.mixin.access.IWeightedListEntry;
import com.hari.harichunk.fixes.worldgen.threading_issues.common.IWeightedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.world.entity.ai.behavior.ShufflingList;

@Mixin(ShufflingList.class)
public class MixinWeightedList<U> implements IWeightedList<U> {

    @Shadow @Final public List<ShufflingList.WeightedEntry<U>> entries;

    @Shadow @Final private net.minecraft.util.RandomSource random;

    /**
     * @author Hari
     * @reason create new instance on shuffling
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    public ShufflingList<U> shuffle() {
        // TODO [VanillaCopy]
        final ShufflingList<U> newList = new ShufflingList<>(entries); // HariChunk - use new instance
        final Random random = new Random(); // HariChunk - use new instance
        ((com.hari.harichunk.base.mixin.access.IWeightedList<U>) newList).getEntries().forEach((entry) -> { // HariChunk - use new instance
            ((IWeightedListEntry) entry).invokeSetShuffledOrder(random.nextFloat());
        });
        ((com.hari.harichunk.base.mixin.access.IWeightedList<U>) newList).getEntries().sort(Comparator.comparingDouble((object) -> { // HariChunk - use new instance
            return ((IWeightedListEntry)object).invokeGetShuffledOrder();
        }));
        return newList; // HariChunk - use new instance
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public ShufflingList<U> shuffleVanilla() {
        // TODO [VanillaCopy]
        this.entries.forEach((entry) -> {
            ((IWeightedListEntry) entry).invokeSetShuffledOrder(this.random.nextFloat());
        });
        this.entries.sort(Comparator.comparingDouble(uEntry -> ((IWeightedListEntry) uEntry).invokeGetShuffledOrder()));
        return (ShufflingList<U>) (Object) this;
    }
}
