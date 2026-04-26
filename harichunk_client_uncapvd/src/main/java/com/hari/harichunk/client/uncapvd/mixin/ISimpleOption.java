package com.hari.harichunk.client.uncapvd.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OptionInstance.class)
public interface ISimpleOption<T> {

    @Accessor("values")
    @Mutable
    void setCallbacks(OptionInstance.ValueSet<T> callbacks);

    @Accessor
    @Mutable
    void setCodec(Codec<T> codec);

}
