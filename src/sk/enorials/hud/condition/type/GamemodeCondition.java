/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.condition.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import sk.enorials.hud.condition.AbstractHudCondition;
import sk.enorials.hud.condition.HudLineConfig;

public class GamemodeCondition
extends AbstractHudCondition {
    private final List<GameMode> gamemodes = new ArrayList<GameMode>();

    public GamemodeCondition(HudLineConfig hudLineConfig) {
        super(hudLineConfig);
        String[] stringArray = hudLineConfig.getStringList("gm", "gamemode", "gamemodes");
        if (stringArray == null) {
            throw new IllegalArgumentException("Gamemodes do not exist.");
        }
        for (String string : stringArray) {
            this.gamemodes.add(GameMode.valueOf((String)string.toUpperCase(Locale.ROOT)));
        }
    }

    @Override
    protected boolean conditionResult(Player player) {
        return this.gamemodes.contains(player.getGameMode());
    }
}

