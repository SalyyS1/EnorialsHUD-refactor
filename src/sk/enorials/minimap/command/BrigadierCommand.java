/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  net.minecraft.commands.CommandSourceStack
 */
package sk.enorials.minimap.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import sk.enorials.minimap.EnorialsMinimap;

public abstract class BrigadierCommand {
    private final EnorialsMinimap plugin;

    public abstract void register(CommandDispatcher<CommandSourceStack> var1);

    public EnorialsMinimap getPlugin() {
        return this.plugin;
    }

    public BrigadierCommand(EnorialsMinimap enorialsMinimap) {
        this.plugin = enorialsMinimap;
    }
}

