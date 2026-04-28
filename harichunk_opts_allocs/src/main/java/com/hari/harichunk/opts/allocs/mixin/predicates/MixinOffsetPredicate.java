package com.hari.harichunk.opts.allocs.mixin.predicates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.StateTestingPredicate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(StateTestingPredicate.class)
public abstract class MixinOffsetPredicate {

    @Shadow(remap = false) protected abstract boolean test(BlockState state);  // test

    @Shadow(remap = false) @Final protected Vec3i f_190539_;  // offset

    /**
     * @author Hari
     * @reason reduce allocs
     */
    @Overwrite
    public final boolean test(WorldGenLevel worldGenLevel, BlockPos blockPos) {
        if (blockPos instanceof BlockPos.MutableBlockPos mutable) {
            int savedX = mutable.getX();
            int savedY = mutable.getY();
            int savedZ = mutable.getZ();
            boolean res = this.test(worldGenLevel.getBlockState(mutable.set(savedX + this.f_190539_.getX(), savedY + this.f_190539_.getY(), savedZ + this.f_190539_.getZ())));
            mutable.set(savedX, savedY, savedZ);
            return res;
        } else {
            return this.test(worldGenLevel.getBlockState(blockPos.f_190539_(this.f_190539_)));
        }
    }

}
