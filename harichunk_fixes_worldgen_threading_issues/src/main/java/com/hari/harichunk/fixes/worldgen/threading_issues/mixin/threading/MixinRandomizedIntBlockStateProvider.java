package com.hari.harichunk.fixes.worldgen.threading_issues.mixin.threading;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RandomizedIntStateProvider.class)
public class MixinRandomizedIntBlockStateProvider {

    @Shadow(remap = false) @Nullable private IntegerProperty f_161558_; // property

    @Redirect(method = "getState", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/feature/stateproviders/RandomizedIntStateProvider;f_161558_:Lnet/minecraft/world/level/block/state/properties/IntegerProperty;", opcode = Opcodes.PUTFIELD, remap = false))
    private void redirectGetProperty(RandomizedIntStateProvider randomizedIntBlockStateProvider, IntegerProperty value) {
        if (this.f_161558_ != null) System.err.println("Detected different property settings in RandomizedIntBlockStateProvider! Expected " + this.f_161558_ + " but got " + value);
        synchronized (this) {
            this.f_161558_ = value;
        }
    }

}
