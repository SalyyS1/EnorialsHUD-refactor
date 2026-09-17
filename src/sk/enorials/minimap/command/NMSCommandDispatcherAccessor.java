/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  net.minecraft.commands.CommandSourceStack
 *  org.bukkit.Bukkit
 *  org.bukkit.craftbukkit.CraftServer
 */
package sk.enorials.minimap.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;

public class NMSCommandDispatcherAccessor {
    public static CommandDispatcher<CommandSourceStack> vanillaDispatcher() {
        return ((CraftServer)Bukkit.getServer()).getServer().getCommands().getDispatcher();
    }
}

