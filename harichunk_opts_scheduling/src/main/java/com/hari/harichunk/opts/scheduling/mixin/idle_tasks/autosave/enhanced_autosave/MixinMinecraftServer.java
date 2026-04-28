package com.hari.harichunk.opts.scheduling.mixin.idle_tasks.autosave.enhanced_autosave;

import com.hari.harichunk.opts.scheduling.common.idle_tasks.IThreadedAnvilChunkStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer extends ReentrantBlockableEventLoop<TickTask> {

    @Shadow(remap = false) protected abstract boolean m_129960_(); // haveTime

    @Shadow(remap = false) public abstract Iterable<ServerLevel> m_129785_(); // getAllLevels

    public MixinMinecraftServer(String string) {
        super(string);
    }

    /**
     * @author Hari
     * @reason improve task execution when waiting for next tick
     */
    @Overwrite(remap = false)
    private boolean m_129961_() {
        if (super.pollTask()) {
            return true;
        } else {
            boolean hasWork = false;
            if (this.m_129960_()) {
                for(ServerLevel serverWorld : this.m_129785_()) {
                    if (serverWorld.getChunkSource().pollTask()) hasWork = true;
                }
            }

            if (!hasWork && this.m_129960_()) {
                for (ServerLevel serverWorld : this.m_129785_()) {
                    if (this.m_129960_()) {
                        hasWork = ((IThreadedAnvilChunkStorage) serverWorld.getChunkSource().chunkMap).runOneChunkAutoSave();
                    }
                }
            }

            return hasWork;
        }
    }

}
