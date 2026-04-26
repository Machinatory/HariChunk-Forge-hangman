package com.hari.harichunk.tfthreadsafetyaddon.mixin;

import com.hari.harichunk.tfthreadsafetyaddon.common.ducks.TFBiomeProviderExtension;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import twilightforest.world.components.biomesources.TFBiomeProvider;
import twilightforest.world.components.chunkgenerators.warp.TerrainColumn;
import twilightforest.world.components.layer.vanillalegacy.BiomeLayerFactory;

import java.util.Map;

@Mixin(value = TFBiomeProvider.class, remap = false)
public class MixinTFBiomeProvider implements TFBiomeProviderExtension {

    @Shadow @Final private Map<ResourceKey<Biome>, TerrainColumn> biomeList;
    @Shadow @Final private float baseOffset;
    @Shadow @Final private float baseFactor;
    @Shadow @Final private Holder<BiomeLayerFactory> genBiomeConfig;

    @Override
    public TFBiomeProvider tfthreadsafetyaddon$recreate() {
        return new TFBiomeProvider(this.biomeList, this.baseOffset, this.baseFactor, this.genBiomeConfig);
    }

}
