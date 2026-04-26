package com.hari.harichunk.opts.scheduling.mixin.idle_tasks.autosave.enhanced_autosave;

import com.hari.harichunk.opts.scheduling.common.idle_tasks.IThreadedAnvilChunkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class MixinWorldChunk extends ChunkAccess {

    public MixinWorldChunk(ChunkPos pos, UpgradeData upgradeData, LevelHeightAccessor heightLimitView, Registry<Biome> biome, long inhabitedTime, @Nullable LevelChunkSection[] sectionArrayInitializer, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, heightLimitView, biome, inhabitedTime, sectionArrayInitializer, blendingData);
    }

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V", at = @At(value = "TAIL"))
    private void onSetShouldSave(CallbackInfo ci) {
        //noinspection ConstantConditions
        if (this.unsaved && (Object) this instanceof LevelChunk worldChunk) {
            if (worldChunk.getLevel() instanceof ServerLevel serverWorld) {
                ((IThreadedAnvilChunkStorage) serverWorld.getChunkSource().chunkMap).enqueueDirtyChunkPosForAutoSave(this.getPos());
            }
        }
    }

    @Inject(method = "setBlockState", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/chunk/LevelChunk;unsaved:Z"))
    private void onSetShouldSave0(BlockPos p_62865_, BlockState p_62866_, boolean p_62867_, CallbackInfoReturnable<BlockState> cir) {
        //noinspection ConstantConditions
        if (this.unsaved && (Object) this instanceof LevelChunk worldChunk) {
            if (worldChunk.getLevel() instanceof ServerLevel serverWorld) {
                ((IThreadedAnvilChunkStorage) serverWorld.getChunkSource().chunkMap).enqueueDirtyChunkPosForAutoSave(this.getPos());
            }
        }
    }
}
