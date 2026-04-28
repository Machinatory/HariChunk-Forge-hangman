package com.hari.harichunk.opts.allocs.mixin.predicates;

import com.hari.harichunk.opts.allocs.common.ducks.CombinedBlockPredicateExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.blockpredicates.AllOfPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(AllOfPredicate.class)
public abstract class MixinAllOfBlockPredicate implements CombinedBlockPredicateExtension {

    /**
     * @author Hari
     * @reason reduce alloc
     */
    @Overwrite(remap = false)
    public boolean test(WorldGenLevel worldGenLevel, BlockPos blockPos) {
        for (BlockPredicate blockPredicate : this.harichunk$getPredicatesArray()) {
            if (!blockPredicate.test(worldGenLevel, blockPos)) {
                return false;
            }
        }

        return true;
    }

}
