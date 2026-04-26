package com.hari.harichunk.rewrites.chunk_serializer.mixin;

import com.hari.harichunk.rewrites.chunk_serializer.common.HeightMapTypeAccessor;
import com.hari.harichunk.rewrites.chunk_serializer.common.NbtWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;
import net.minecraft.world.level.levelgen.Heightmap;

@Mixin(Heightmap.Types.class)
public class HeightMapTypeMixin implements HeightMapTypeAccessor {
    private byte[] nameBytes;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void postInit(String enum$name, int enum$ordinal, String name, Heightmap.Usage purpose, Predicate<?> blockPredicate, CallbackInfo ci) {
        this.nameBytes = NbtWriter.getStringBytes(name);
    }

    @Override
    public byte[] getNameBytes() {
        return this.nameBytes;
    }

}

