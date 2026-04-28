package com.hari.harichunk.fixes.worldgen.threading_issues.mixin.threading;

import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;

@Mixin(StructureCheck.class)
public class MixinStructureChecker {

    @Mutable
    @Shadow(remap = false) @Final private Long2ObjectMap<Object2IntMap<Structure>> f_197247_;  // loadedChunks

    @Mutable
    @Shadow(remap = false) @Final private Map<Structure, Long2BooleanMap> f_197248_;  // featureChecks

    @Unique
    private Object mapMutex = new Object();

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onInit(CallbackInfo info) {
        this.mapMutex = new Object();
        this.f_197247_ = Long2ObjectMaps.synchronize(this.f_197247_);
        this.f_197248_ = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>(), this.mapMutex);
    }

    @Redirect(method = "storeFullResults", at = @At(value = "INVOKE", target = "Ljava/util/Collection;forEach(Ljava/util/function/Consumer;)V"))
    private void redirectForEach(Collection<Long2BooleanMap> instance, Consumer<Long2BooleanMap> consumer) {
        //noinspection SynchronizeOnNonFinalField
        synchronized (this.mapMutex) {
            final Iterator<Long2BooleanMap> iterator = instance.iterator();
            while (iterator.hasNext()) {
                final Long2BooleanMap next = iterator.next();
                consumer.accept(next);
                if (next.isEmpty()) iterator.remove();
            }
        }
    }

}
