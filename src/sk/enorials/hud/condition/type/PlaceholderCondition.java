/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.PlaceholderAPI
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.condition.type;

import java.util.Optional;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.condition.AbstractHudCondition;
import sk.enorials.hud.condition.ConditionOperation;
import sk.enorials.hud.condition.HudLineConfig;
import sk.enorials.hud.model.HudAsset;

public class PlaceholderCondition
extends AbstractHudCondition {
    private final String placeholder;
    private final String value;
    private final ConditionOperation operation;

    public PlaceholderCondition(HudLineConfig hudLineConfig, HudAsset hudAsset) {
        super(hudLineConfig);
        this.placeholder = hudLineConfig.getString("1", "ph", "placeholder");
        if (this.placeholder == null) {
            throw new IllegalArgumentException("Placeholder does not exist.");
        }
        this.value = hudLineConfig.getString("2", "v", "val", "value");
        this.operation = hudLineConfig.getOperation();
    }

    @Override
    protected boolean conditionResult(Player player) {
        String string = this.parse(player, this.placeholder);
        if (this.value == null) {
            return string.equalsIgnoreCase("true");
        }
        String string2 = this.parse(player, this.value);
        Optional<Double> optional = this.toDouble(string);
        Optional<Double> optional2 = this.toDouble(string2);
        return optional.isPresent() && optional2.isPresent() ? this.operation.evaluate(optional.get(), optional2.get()) : string.equalsIgnoreCase(string2);
    }

    private String parse(Player player, String string) {
        String string2 = string;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                string2 = PlaceholderAPI.setPlaceholders((Player)player, (String)string2);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return string2.replace("%target_exists%", this.resolveTargetExists(player));
    }

    private String resolveTargetExists(Player player) {
        EnorialsHUD enorialsHUD = EnorialsHUD.getInstance();
        return enorialsHUD != null && enorialsHUD.renderer() != null && enorialsHUD.renderer().getCurrentTarget(player) != null ? "true" : "false";
    }

    private Optional<Double> toDouble(String string) {
        if (string == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(string.replace(",", ".")));
        }
        catch (NumberFormatException numberFormatException) {
            return Optional.empty();
        }
    }
}

