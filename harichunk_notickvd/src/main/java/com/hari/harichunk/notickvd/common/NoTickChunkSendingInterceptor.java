package com.hari.harichunk.notickvd.common;

import net.minecraft.server.level.ServerPlayer;

public class NoTickChunkSendingInterceptor {

    public static boolean onChunkSending(ServerPlayer player, long pos) {
        return true;
    }

}
