package com.hari.harichunk.tfthreadsafetyaddon.mixin;

import com.hari.harichunk.tfthreadsafetyaddon.common.ducks.LazyAreaExtension;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.world.components.layer.vanillalegacy.area.LazyArea;
import twilightforest.world.components.layer.vanillalegacy.context.LazyAreaContext;

@Mixin(value = LazyAreaContext.class, remap = false)
public class MixinLazyAreaContext {

    private Long2ObjectLinkedOpenHashMap<ResourceKey<Biome>> tfthreadsafetyaddon$cache;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(int maxCache, long salt, CallbackInfo ci) {
        this.tfthreadsafetyaddon$cache = new Long2ObjectLinkedOpenHashMap<>(maxCache, 0.5F);
    }

    @ModifyReturnValue(
            method = {
                    "createResult(Ltwilightforest/world/components/layer/vanillalegacy/Area;)Ltwilightforest/world/components/layer/vanillalegacy/area/LazyArea;",
                    "createResult(Ltwilightforest/world/components/layer/vanillalegacy/Area;Ltwilightforest/world/components/layer/vanillalegacy/area/LazyArea;)Ltwilightforest/world/components/layer/vanillalegacy/area/LazyArea;",
                    "createResult(Ltwilightforest/world/components/layer/vanillalegacy/Area;Ltwilightforest/world/components/layer/vanillalegacy/area/LazyArea;Ltwilightforest/world/components/layer/vanillalegacy/area/LazyArea;)Ltwilightforest/world/components/layer/vanillalegacy/area/LazyArea;"
            },
            at = @At("RETURN"),
            require = 3,
            remap = false
    )
    private LazyArea injectCache(LazyArea original) {
        ((LazyAreaExtension) original).tfthreadsafetyaddon$setCachedSamples(this.tfthreadsafetyaddon$cache);
        return original;
    }

}
