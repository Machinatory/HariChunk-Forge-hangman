package com.hari.harichunk.fixes.worldgen.threading_issues.mixin.threading.math;

import com.mojang.math.OctahedralGroup;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OctahedralGroup.class)
public abstract class MixinDirectionTransformation {

    @Shadow(remap = false) public abstract Direction m_56528_(Direction direction);  // rotate

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {
        m_56528_(Direction.UP); // force load mapping to prevent further issues
    }

}
