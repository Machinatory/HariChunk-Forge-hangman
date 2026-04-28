package com.hari.harichunk.opts.allocs.mixin.predicates;

import com.hari.harichunk.opts.allocs.common.ducks.CombinedBlockPredicateExtension;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.CombiningPredicate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(CombiningPredicate.class)
public class MixinCombinedBlockPredicate implements CombinedBlockPredicateExtension {

    @Shadow(remap = false) @Final protected List<BlockPredicate> f_190453_;  // predicates

    @Unique
    private BlockPredicate[] harichunk$predicatesArray;

    @Override
    public BlockPredicate[] harichunk$getPredicatesArray() {
        BlockPredicate[] predicateArray = this.harichunk$predicatesArray;
        if (predicateArray == null) {
            this.harichunk$predicatesArray = predicateArray = this.f_190453_.toArray(BlockPredicate[]::new);
        }
        return predicateArray;
    }

}
