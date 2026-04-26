package com.hari.harichunk.server.utils.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// SJhub - register in AdvancementCommands
@Mixin(Commands.class)
public class MixinCommandManager {

    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;

}
