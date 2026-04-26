package com.hari.harichunk.opts.chunkio.common;

import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import org.spongepowered.asm.mixin.MixinEnvironment;

// Don't load this too early
public class ConfigConstants {

    public static final RegionFileVersion CHUNK_STREAM_VERSION;

    static {
        if (MixinEnvironment.getCurrentEnvironment().getPhase() != MixinEnvironment.Phase.DEFAULT) throw new IllegalStateException("Mixins not initialized yet");

        if (Config.chunkStreamVersion == -1) {
            CHUNK_STREAM_VERSION = RegionFileVersion.VERSION_DEFLATE;
        } else {
            final RegionFileVersion chunkStreamVersion = RegionFileVersion.fromId((int) Config.chunkStreamVersion);
            if (chunkStreamVersion == null) {
                Config.LOGGER.warn("Unknown compression {}, using vanilla default instead", Config.chunkStreamVersion);
                CHUNK_STREAM_VERSION = RegionFileVersion.VERSION_DEFLATE;
            } else {
                CHUNK_STREAM_VERSION = chunkStreamVersion;
            }
        }
    }

}
