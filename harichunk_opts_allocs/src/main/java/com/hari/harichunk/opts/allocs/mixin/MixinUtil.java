package com.hari.harichunk.opts.allocs.mixin;

import com.ibm.asyncutil.util.Combinators;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.Util;

@Mixin(Util.class)
public abstract class MixinUtil {

    /**
     * @author Hari
     * @reason use another impl
     */
    @Overwrite
    public static <V> CompletableFuture<List<V>> sequence(List<CompletableFuture<V>> futures) {
        return Combinators.collect(futures, Collectors.toList()).toCompletableFuture();
    }

    /**
     * @author Hari
     * @reason use another impl
     */
    @Overwrite
    public static <V> CompletableFuture<List<V>> sequenceFailFast(List<CompletableFuture<V>> futures) {
        final CompletableFuture<List<V>> future = Combinators.collect(futures, Collectors.toList()).toCompletableFuture();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).exceptionally(e -> {
            future.completeExceptionally(e);
            return null;
        });
        return future;
    }

    /**
     * @author Hari
     * @reason use another impl
     */
    @Overwrite
    public static <V> CompletableFuture<List<V>> sequenceFailFastAndCancel(List<CompletableFuture<V>> futures) {
        final CompletableFuture<List<V>> future = Combinators.collect(futures, Collectors.toList()).toCompletableFuture();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).exceptionally(e -> {
            future.completeExceptionally(e);
            futures.forEach(f -> f.cancel(false));
            return null;
        });
        return future;
    }

}
