/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.condition;

import org.bukkit.entity.Player;
import sk.enorials.hud.condition.ConditionAction;
import sk.enorials.hud.condition.HudCondition;
import sk.enorials.hud.condition.HudLineConfig;

public abstract class AbstractHudCondition
implements HudCondition {
    private final boolean match;
    private final ConditionAction action;
    private final String variable;

    protected AbstractHudCondition(HudLineConfig hudLineConfig) {
        this.match = hudLineConfig.getMatch();
        this.action = ConditionAction.of(hudLineConfig.getAction());
        this.variable = hudLineConfig.getVariable();
        this.action.validateVariable(this.variable);
    }

    @Override
    public ConditionAction getAction() {
        return this.action;
    }

    @Override
    public String getVariable() {
        return this.variable;
    }

    protected abstract boolean conditionResult(Player var1);

    @Override
    public boolean result(Player player) {
        return this.conditionResult(player) == this.match;
    }
}

