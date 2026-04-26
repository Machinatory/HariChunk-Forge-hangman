package com.hari.harichunk.client.uncapvd.mixin;

import com.hari.harichunk.client.uncapvd.common.Config;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo
@Mixin(targets = {
        "me.jellysquid.mods.sodium.client.gui.SodiumGameOptionPages",
        "net.caffeinemc.sodium.config.user.UserConfigCategories",
})
public class MixinSodiumUserConfigCategories {

    @Dynamic // TODO?: check byte code
    @ModifyConstant(method = "lambda$general$0", constant = @Constant(intValue = 32), remap = false)
    private static int modifyMaxViewDistance(int value) {
        return Config.maxViewDistance;
    }

}
