/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.condition.type;

import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.condition.AbstractHudCondition;
import sk.enorials.hud.condition.ConditionOperation;
import sk.enorials.hud.condition.HudLineConfig;
import sk.enorials.hud.model.HudAsset;
import sk.enorials.hud.model.HudLayer;
import sk.enorials.hud.model.TextureLayer;

public class ProgressCondition
extends AbstractHudCondition {
    private final double value;
    private final ConditionOperation operation;
    private final ConditionOperation.EvaluationType evaluationType;
    private final String layerKey;
    private final HudAsset asset;
    private String listener;

    public ProgressCondition(HudLineConfig hudLineConfig, HudAsset hudAsset) {
        super(hudLineConfig);
        this.asset = hudAsset;
        this.layerKey = hudLineConfig.getString("l", "lay", "layer");
        String string = hudLineConfig.getString("v", "val", "value");
        if (string == null) {
            throw new IllegalArgumentException("Value does not exist.");
        }
        if (string.endsWith("%")) {
            this.evaluationType = ConditionOperation.EvaluationType.PERCENT;
            try {
                this.value = Double.parseDouble(string.replace("%", ""));
            }
            catch (NumberFormatException numberFormatException) {
                throw new IllegalArgumentException("Argument is not a number.");
            }
        } else {
            this.evaluationType = ConditionOperation.EvaluationType.NUMBER;
            Double d = hudLineConfig.getDouble("v", "val", "value");
            if (d == null) {
                throw new IllegalArgumentException("Argument is not a number.");
            }
            this.value = d;
        }
        this.operation = hudLineConfig.getOperation();
    }

    @Override
    protected boolean conditionResult(Player player) {
        if (this.listener == null) {
            TextureLayer textureLayer;
            HudLayer hudLayer = this.asset.getLayer(this.layerKey);
            if (!(hudLayer instanceof TextureLayer) || (textureLayer = (TextureLayer)hudLayer).getFunction() == null) {
                throw new IllegalArgumentException("Cannot apply this condition on a non functional layer.");
            }
            this.listener = textureLayer.getFunction().listener();
        }
        double d = EnorialsHUD.getInstance().listeners().getCurrent(player, this.listener);
        double d2 = EnorialsHUD.getInstance().listeners().getMax(player, this.listener);
        double d3 = d;
        if (this.evaluationType == ConditionOperation.EvaluationType.PERCENT) {
            d3 = d2 != 0.0 ? d / d2 * 100.0 : 0.0;
        }
        return this.operation.evaluate(d3, this.value);
    }
}

