package com.hari.harichunk.opts.allocs.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ResourceLocation.class)
public class MixinIdentifier {

    @Shadow(remap = false) @Final private String f_135804_; // namespace
    @Shadow(remap = false) @Final private String f_135805_; // path
    @Unique
    private String cachedString = null;

    /**
     * @author Hari
     * @reason cache toString
     */
    @Overwrite(remap = false)
    public String toString() {
        if (this.cachedString != null) return this.cachedString;
        final String s = this.f_135804_ + ":" + this.f_135805_;
        this.cachedString = s;
        return s;
    }

}
