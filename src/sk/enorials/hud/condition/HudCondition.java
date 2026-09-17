/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.condition;

import org.bukkit.entity.Player;
import sk.enorials.hud.condition.ConditionAction;

public interface HudCondition {
    public ConditionAction getAction();

    public String getVariable();

    public boolean result(Player var1);
}

