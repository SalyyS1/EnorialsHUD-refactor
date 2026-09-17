/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.condition.ConditionAction;
import sk.enorials.hud.condition.HudCondition;
import sk.enorials.hud.model.HudAsset;

public class ConditionHolder {
    private final List<HudCondition> conditions;
    private final String ownerKey;
    private final HudAsset asset;

    public ConditionHolder(EnorialsHUD enorialsHUD, String string, HudAsset hudAsset, List<String> list) {
        this.ownerKey = string;
        this.asset = hudAsset;
        ArrayList<HudCondition> arrayList = new ArrayList<HudCondition>();
        for (String string2 : list) {
            try {
                arrayList.add(enorialsHUD.conditions().getCondition(string2, hudAsset));
            }
            catch (IllegalArgumentException illegalArgumentException) {
                String string3 = string != null ? string : "unknown";
                enorialsHUD.getLogger().log(Level.WARNING, "Could not load condition '" + string2 + "' from '" + string3 + "': " + illegalArgumentException.getMessage());
            }
        }
        Collections.reverse(arrayList);
        this.conditions = arrayList;
    }

    public Set<String> getPossibleTexturePaths() {
        HashSet<String> hashSet = new HashSet<String>();
        for (HudCondition hudCondition : this.conditions) {
            if (hudCondition.getAction() != ConditionAction.TEXTURE || hudCondition.getVariable() == null) continue;
            hashSet.add(hudCondition.getVariable());
        }
        return hashSet;
    }

    public ConditionResult result(Player player) {
        return new ConditionResult(player);
    }

    private TextColor parseColor(String string) {
        if (string == null) {
            return null;
        }
        TextColor textColor = TextColor.fromHexString((String)string);
        return textColor != null ? textColor : (TextColor)NamedTextColor.NAMES.value((Object)string.toLowerCase());
    }

    public class ConditionResult {
        private final boolean hidden;
        private final TextColor color;
        private final String texturePath;

        public ConditionResult(Player player) {
            boolean bl = false;
            TextColor textColor = null;
            String string = null;
            block5: for (HudCondition hudCondition : ConditionHolder.this.conditions) {
                switch (hudCondition.getAction()) {
                    case HIDE: {
                        if (bl || !hudCondition.result(player)) break;
                        bl = true;
                        break;
                    }
                    case COLOR: {
                        if (!hudCondition.result(player)) break;
                        TextColor textColor2 = ConditionHolder.this.parseColor(hudCondition.getVariable());
                        if (textColor2 == null) continue block5;
                        textColor = textColor2;
                        break;
                    }
                    case TEXTURE: {
                        if (!hudCondition.result(player)) break;
                        string = hudCondition.getVariable();
                    }
                }
            }
            this.hidden = bl;
            this.color = textColor;
            this.texturePath = string;
        }

        public boolean isHidden() {
            return this.hidden;
        }

        public boolean hasColor() {
            return this.color != null;
        }

        public TextColor getColor() {
            return this.color;
        }

        public boolean hasTexturePath() {
            return this.texturePath != null;
        }

        public String getTexturePath() {
            return this.texturePath;
        }
    }
}

