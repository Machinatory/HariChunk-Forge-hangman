package com.hari.harichunk.opts.worldgen.general.mixin.random_instances;

import com.hari.harichunk.opts.worldgen.general.common.random_instances.SimplifiedAtomicSimpleRandom;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {
        StructurePlacement.class,
})
public class MixinRedirectAtomicSimpleRandomStatic {

    @Redirect(method = "*", at = @At(value = "NEW", target = "net/minecraft/world/level/levelgen/LegacyRandomSource"))
    private static LegacyRandomSource redirectAtomicSimpleRandom(long l) {
        return new SimplifiedAtomicSimpleRandom(l);
    }

}
