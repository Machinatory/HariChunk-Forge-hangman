package com.hari.harichunk.compat.immersivepetroleum.mixin;

import flaxbeard.immersivepetroleum.api.reservoir.ReservoirHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.function.Function;

@Mixin(value = ReservoirHandler.class, remap = false)
public class MixinReservoirHandler {

    @Redirect(
        method = "*",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;", remap = false),
        require = 0
    )
    private static Object redirectMapComputeIfAbsent(Map<Object, Object> map, Object key, Function<Object, Object> mappingFunction) {
        synchronized (map) {
            return map.computeIfAbsent(key, mappingFunction);
        }
    }

    @Redirect(
        method = "*",
        at = @At(value = "INVOKE", target = "Ljava/util/HashMap;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;", remap = false),
        require = 0
    )
    private static Object redirectHashMapComputeIfAbsent(java.util.HashMap<Object, Object> map, Object key, Function<Object, Object> mappingFunction) {
        synchronized (map) {
            return map.computeIfAbsent(key, mappingFunction);
        }
    }

}
