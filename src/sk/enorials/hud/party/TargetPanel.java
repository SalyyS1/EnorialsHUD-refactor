/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.bukkit.attribute.AttributeInstance
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Horse
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Llama
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.party;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.compat.HudAttributes;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.pack.PackRegistry;
import sk.enorials.hud.party.TargetPortraits;
import sk.enorials.hud.theme.ThemeForeground;
import sk.enorials.hud.util.SpaceUtil;

public final class TargetPanel {
    private final EnorialsHUD plugin;
    private final Properties heads = new Properties();

    public TargetPanel(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
        try (InputStream inputStream = enorialsHUD.getResource("target-heads.properties");){
            if (inputStream != null) {
                this.heads.load(inputStream);
            }
        }
        catch (IOException iOException) {
            enorialsHUD.getLogger().warning("Could not load target portraits: " + iOException.getMessage());
        }
    }

    public Component render(Player player, PackRegistry packRegistry) {
        int n;
        if (!HudAccess.has((CommandSender)player, "enorials.hud.target")) {
            return Component.empty();
        }
        LivingEntity livingEntity = this.plugin.renderer().getCombatTarget(player);
        if (livingEntity == null) {
            return Component.empty();
        }
        PackRegistry.GlyphInfo glyphInfo = packRegistry.getTexture("layout", "target-panel", "combat-target", "frame", "assets/enorialshud/combat_target_frame.png");
        if (glyphInfo == null) {
            return Component.empty();
        }
        Component component = packRegistry.metrics().neutral(glyphInfo.component(packRegistry));
        String string = this.portraitGlyph(livingEntity);
        component = component.append(this.text(packRegistry, "head", String.valueOf((char)Integer.parseInt(string, 16)), false, 24));
        String string2 = this.plugin.listeners().getTargetName(player, livingEntity);
        component = component.append(this.text(packRegistry, "name", string2, false, 82));
        AttributeInstance attributeInstance = livingEntity.getAttribute(HudAttributes.MAX_HEALTH);
        double d = Math.max(1.0, attributeInstance == null ? livingEntity.getMaxHealth() : attributeInstance.getValue());
        PackRegistry.TextureSteps textureSteps = packRegistry.getTextureSteps("layout", "target-panel", "combat-target", "health", "assets/enorialshud/combat_target_health.png");
        if (textureSteps != null && (n = (int)Math.ceil(Math.max(0.0, Math.min(1.0, livingEntity.getHealth() / d)) * (double)textureSteps.steps().size()) - 1) >= 0) {
            component = component.append(packRegistry.metrics().neutral(textureSteps.steps().get(n).component(packRegistry)));
        }
        component = component.append(this.text(packRegistry, "health-number", Math.round(livingEntity.getHealth()) + "/" + Math.round(d), true, 78));
        component = component.append(ThemeForeground.render(this.plugin, player, packRegistry, "target-panel", "combat-target", "frame", "assets/enorialshud/combat_target_frame.png"));
        return TextCompat.shadow(packRegistry.metrics().neutral(component), 0);
    }

    public String portraitGlyph(LivingEntity livingEntity) {
        Object object;
        String string;
        String string2 = TargetPortraits.select(this.plugin, livingEntity, this.heads);
        if (string2 != null) {
            return string2;
        }
        String string3 = livingEntity.getType().name();
        if (livingEntity instanceof Horse) {
            Horse horse = (Horse)livingEntity;
            string = horse.getColor().name();
        } else if (livingEntity instanceof Llama) {
            object = (Llama)livingEntity;
            string = object.getColor().name();
        } else {
            string = null;
        }
        String string4 = string;
        object = this.heads.getProperty(string3, this.heads.getProperty("UNKNOWN", "f16c"));
        return string4 == null ? object : this.heads.getProperty(string3 + "." + string4, (String)object);
    }

    private Component text(PackRegistry packRegistry, String string, String string2, boolean bl, int n) {
        String string3 = packRegistry.getTextFont("layout", "target-panel", "combat-target", string);
        if (string3 == null || string2 == null || string2.isBlank()) {
            return Component.empty();
        }
        if (string.equals("name")) {
            string2 = TargetPanel.plainName(string2);
        }
        Component component = ((TextComponent)LegacyText.font((Component)Component.text((String)string2), Key.key((String)string3))).color(TextColor.color((int)0xFFFFFF));
        if (string.equals("name")) {
            if (packRegistry.metrics().measure(component).advance() > (float)n) {
                while (!string2.isEmpty() && packRegistry.metrics().measure(LegacyText.font((Component)Component.text((String)(string2 + "..")), Key.key((String)string3))).advance() > (float)n) {
                    string2 = string2.substring(0, string2.offsetByCodePoints(string2.length(), -1));
                }
                component = ((TextComponent)LegacyText.font((Component)Component.text((String)(string2 + "..")), Key.key((String)string3))).color(TextColor.color((int)16047808));
            }
            component = TextCompat.shadow(component, -586675184);
        }
        if (bl) {
            component = SpaceUtil.space(-Math.round(packRegistry.metrics().measure(component).center())).append(component);
        }
        return packRegistry.metrics().neutral(component);
    }

    static String plainName(String string) {
        return PlainTextComponentSerializer.plainText().serialize((Component)LegacyComponentSerializer.legacySection().deserialize(string));
    }
}

