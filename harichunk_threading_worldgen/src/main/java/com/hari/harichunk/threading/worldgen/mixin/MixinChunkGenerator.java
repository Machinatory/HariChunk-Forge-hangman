package com.hari.harichunk.threading.worldgen.mixin;

import com.hari.harichunk.base.common.util.InvokingExecutorService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ExecutorService;
import net.minecraft.world.level.chunk.ChunkGenerator;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator {

    @Redirect(method = "createBiomes", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;backgroundExecutor()Ljava/util/concurrent/ExecutorService;"))
    private ExecutorService redirectBiomeExecutor() {
        return InvokingExecutorService.INSTANCE;
    }

}
