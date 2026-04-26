package com.hari.harichunk.base.mixin.access;

import net.minecraft.world.level.levelgen.Aquifer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Aquifer.FluidStatus.class)
public interface IAquiferSamplerFluidLevel {

}
