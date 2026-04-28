package com.hari.harichunk.opts.scheduling.mixin.task_scheduling;

import com.hari.harichunk.opts.scheduling.common.DuckChunkHolder;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;

@Mixin(ChunkHolder.class)
public abstract class MixinChunkHolder implements DuckChunkHolder {

    @Shadow(remap = false) public abstract void m_140036_(LightLayer lightType, int y); // sectionLightChanged

    @Shadow(remap = false) @Final private LevelHeightAccessor f_142983_; // levelHeightAccessor

    private AtomicIntegerArray[] harichunk$dirtyLightSections;
    private final AtomicBoolean harichunk$scheduledLightUndirty = new AtomicBoolean(false);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        int bottomSection = this.f_142983_.getMinSection() - 1;
        int topSection = this.f_142983_.getMinSection() + this.f_142983_.getSectionsCount();
        int range = topSection - bottomSection + 1;
        harichunk$dirtyLightSections = new AtomicIntegerArray[LightLayer.values().length];
        for (int i = 0; i < harichunk$dirtyLightSections.length; i++) {
            harichunk$dirtyLightSections[i] = new AtomicIntegerArray(range);
        }
    }

    @Override
    public void harichunk$queueLightSectionDirty(LightLayer lightType, int sectionY) {
        int bottomSection = this.f_142983_.getMinSection() - 1;
        int topSection = this.f_142983_.getMinSection() + this.f_142983_.getSectionsCount();
        if (sectionY >= bottomSection && sectionY <= topSection)
            this.harichunk$dirtyLightSections[lightType.ordinal()].set(sectionY - bottomSection, 1);
    }

    @Override
    public boolean harichunk$shouldScheduleUndirty() {
        return this.harichunk$scheduledLightUndirty.compareAndSet(false, true);
    }

    @Override
    public boolean harichunk$undirtyLight() {
        if (!this.harichunk$scheduledLightUndirty.compareAndSet(true, false)) {
            return false;
        }
        boolean hasDirtyLight = false;
        AtomicIntegerArray[] sections = this.harichunk$dirtyLightSections;
        final int bottomSection = this.f_142983_.getMinSection() - 1;
        for (int i = 0, length = sections.length; i < length; i++) {
            AtomicIntegerArray section = sections[i];
            LightLayer lightType = LightLayer.values()[i];
            for (int j = 0; j < section.length(); j++) {
                if (section.compareAndSet(j, 1, 0)) {
                    hasDirtyLight = true;
                    this.m_140036_(lightType, j + bottomSection);
                }
            }
        }
        return hasDirtyLight;
    }

}
