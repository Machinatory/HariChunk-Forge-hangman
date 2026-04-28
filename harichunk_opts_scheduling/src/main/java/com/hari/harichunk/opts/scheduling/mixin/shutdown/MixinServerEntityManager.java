package com.hari.harichunk.opts.scheduling.mixin.shutdown;

import com.hari.harichunk.opts.scheduling.common.ITryFlushable;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Consumer;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;

@Mixin(PersistentEntitySectionManager.class)
public abstract class MixinServerEntityManager<T> implements ITryFlushable {

    @Shadow(remap = false) protected abstract LongSet m_157587_(); // getAllChunksToSave

    @Shadow(remap = false) @Final private EntityPersistentStorage<T> f_157493_; // permanentStorage

    @Shadow(remap = false) protected abstract void m_157582_(); // processPendingLoads

    @Shadow(remap = false) @Final private Long2ObjectMap<Visibility> f_90979_; // chunkVisibility

    @Shadow(remap = false) protected abstract boolean m_157568_(long chunkPos); // processChunkUnload

    @Shadow(remap = false) protected abstract boolean m_157512_(long chunkPos, Consumer<T> action); // storeChunkSections

    public boolean harichunk$tryFlush() {
        LongSet longSet = this.m_157587_();

        if(!longSet.isEmpty()) {
            this.f_157493_.flush(false);
            this.m_157582_();
            longSet.removeIf((pos) -> {
                boolean bl = this.f_90979_.get(pos) == Visibility.HIDDEN;
                return bl ? this.m_157568_(pos) : this.m_157512_(pos, (entity) -> {
                });
            });
        }

        this.f_157493_.flush(true);
        return longSet.isEmpty();
    }

}
