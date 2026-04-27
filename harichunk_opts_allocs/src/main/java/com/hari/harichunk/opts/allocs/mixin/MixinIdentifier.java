package com.hari.harichunk.opts.allocs.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ResourceLocation.class)
public class MixinIdentifier {

    @Shadow(remap = false) @Final protected String f_135820_; // namespace
    @Shadow(remap = false) @Final protected String f_135821_; // path
    @Unique
    private String cachedString = null;

    /**
     * @author Hari
     * @reason cache toString
     */
    @Overwrite
    public String toString() {
        if (this.cachedString != null) return this.cachedString;
        final String s = this.f_135820_ + ":" + this.f_135821_;
        this.cachedString = s;
        return s;
    }

}
