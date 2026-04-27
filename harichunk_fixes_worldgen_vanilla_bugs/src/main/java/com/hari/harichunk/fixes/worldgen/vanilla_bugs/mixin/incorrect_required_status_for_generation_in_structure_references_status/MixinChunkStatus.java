package com.hari.harichunk.fixes.worldgen.vanilla_bugs.mixin.incorrect_required_status_for_generation_in_structure_references_status;

import com.google.common.collect.ImmutableList;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.chunk.ChunkStatus;

@Mixin(ChunkStatus.class)
public class MixinChunkStatus {

    @Mutable
    @Shadow(remap = false) @Final private static List<ChunkStatus> f_62330_; // STATUS_BY_RANGE

    @Dynamic
    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void appendStructureStarts(CallbackInfo info) {
        final List<ChunkStatus> distanceToStatus = new ArrayList<>(f_62330_);
        distanceToStatus.add(ChunkStatus.STRUCTURE_STARTS);
        f_62330_ = ImmutableList.copyOf(distanceToStatus);
    }

}
