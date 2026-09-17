/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.TextReplacementConfig$Builder
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.party;

import java.util.Map;
import java.util.WeakHashMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import sk.enorials.gps.nav.NavigationManager;
import sk.enorials.gps.nav.NavigationSession;
import sk.enorials.gps.nav.target.PlayerTarget;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.compat.ClientProfiles;
import sk.enorials.hud.compat.LegacyGpsPose;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.model.HudElement;
import sk.enorials.hud.model.HudLayer;
import sk.enorials.hud.model.HudLayout;
import sk.enorials.hud.model.TextureLayer;
import sk.enorials.hud.pack.PackRegistry;
import sk.enorials.hud.party.GpsArrowPose;
import sk.enorials.hud.util.SpaceUtil;

public final class GpsPanel {
    private final EnorialsHUD plugin;
    private final Map<Player, GpsArrowPose.Sample> arrowPoses = new WeakHashMap<Player, GpsArrowPose.Sample>();

    public GpsPanel(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
    }

    public Component render(Player player, PackRegistry packRegistry) {
        int n;
        Object object;
        long l;
        Object object2;
        Location location;
        if (this.plugin.gps() == null || player.hasMetadata("enorials.conversation") || player.hasMetadata("enorials.dialogue-active")) {
            return Component.empty();
        }
        NavigationManager navigationManager = this.plugin.gps().navigation();
        NavigationSession navigationSession = navigationManager.session(player);
        if (navigationSession != null && !HudAccess.navigation(player, navigationSession.locator(), navigationSession.descriptor(), navigationSession.source())) {
            return Component.empty();
        }
        if (navigationSession == null) {
            this.arrowPoses.remove(player);
            return Component.empty();
        }
        if (navigationSession.source().equals("hud-party") && !this.plugin.gps().gui().samePartyTarget(player, navigationSession.locator())) {
            return Component.empty();
        }
        Location location2 = location = navigationSession.locator() instanceof PlayerTarget ? navigationSession.locator().resolve(player) : navigationSession.lastResolvedTarget();
        if (location == null) {
            return Component.empty();
        }
        boolean bl = navigationManager.isCompactHudEnabled(player);
        String string = bl ? "gps-compact" : "gps";
        Location location3 = player.getLocation();
        boolean bl2 = location3.getWorld().equals((Object)location.getWorld());
        boolean bl3 = navigationManager.isDistanceDisplayEnabled(player);
        boolean bl4 = navigationManager.isVerticalDisplayEnabled(player);
        boolean bl5 = bl2 && bl3 && bl4;
        PackRegistry.GlyphInfo glyphInfo = this.frame(packRegistry, string, bl5 ? "frame-split" : "frame");
        if (glyphInfo == null && bl5) {
            glyphInfo = this.frame(packRegistry, string, "frame");
        }
        if (glyphInfo == null) {
            return Component.empty();
        }
        Component component = packRegistry.metrics().neutral(glyphInfo.component(packRegistry));
        String string2 = packRegistry.getTextFont("layout", "gps-panel", string, "arrow");
        if (string2 != null) {
            object2 = this.arrowPoses.get(player);
            l = location3.getWorld().getGameTime();
            object = bl2 ? GpsArrowPose.direction(location.getX() - location3.getX(), location.getY() - location3.getY(), location.getZ() - location3.getZ(), location3.getYaw(), location3.getPitch(), object2 == null ? null : ((GpsArrowPose.Sample)object2).angles()) : new GpsArrowPose.Angles(0, 128, false);
            long l2 = GpsArrowPose.encode((GpsArrowPose.Sample)object2, (GpsArrowPose.Angles)object, l);
            Component component2 = ClientProfiles.legacy(player) ? LegacyGpsPose.arrow(string2, l2) : (Component)TextCompat.shadow((TextComponent)((TextComponent)LegacyText.font((Component)Component.text((String)String.valueOf(GpsArrowPose.glyph(l2))), Key.key((String)string2))).color(TextColor.color((int)0)), GpsArrowPose.shadow(l2));
            component = component.append(packRegistry.metrics().neutral(component2));
            this.arrowPoses.put(player, new GpsArrowPose.Sample((GpsArrowPose.Angles)object, l));
        }
        object2 = navigationManager.isTargetNameDisplayEnabled(player) ? navigationSession.locator().displayName() : "Navigation";
        object2 = GpsPanel.plainName((String)object2);
        component = component.append(this.text(packRegistry, string, "name", (String)object2, 15912070, bl ? 76 : 112));
        l = Math.round(location.getY() - location3.getY());
        Object object3 = l == 0L ? "STAY" : (object = l > 0L ? "UP" : "DOWN");
        int n2 = l == 0L ? 0xA6A6A6 : (n = l > 0L ? 0x55DD77 : 0xFF5555);
        if (!bl2) {
            component = component.append(this.text(packRegistry, string, "details", "World: " + location.getWorld().getName(), 14807026, bl ? 76 : 112));
        } else {
            if (bl3) {
                component = component.append(this.text(packRegistry, string, bl5 ? "distance" : "details", Math.round(location3.distance(location)) + "m", 14807026, bl5 ? (bl ? 32 : 50) : (bl ? 76 : 112)));
            }
            if (bl4) {
                Component component3 = this.text(packRegistry, string, bl5 ? "height" : "details", (String)object, 14807026, bl5 ? (bl ? 32 : 50) : (bl ? 76 : 112));
                component = component.append(component3.replaceText(arg_0 -> GpsPanel.lambda$render$0((String)object, n, arg_0)));
            }
        }
        return TextCompat.shadow(packRegistry.metrics().neutral(component), 0);
    }

    private PackRegistry.GlyphInfo frame(PackRegistry packRegistry, String string, String string2) {
        HudLayout hudLayout = this.plugin.layouts().get("gps-panel");
        if (hudLayout == null) {
            return null;
        }
        for (HudElement hudElement : hudLayout.getElements()) {
            HudLayer hudLayer;
            if (!hudElement.getKey().equals(string) || !((hudLayer = hudElement.getAsset().getLayer(string2)) instanceof TextureLayer)) continue;
            TextureLayer textureLayer = (TextureLayer)hudLayer;
            return packRegistry.getTexture("layout", "gps-panel", string, string2, textureLayer.getTexturePath());
        }
        return null;
    }

    static String plainName(String string) {
        if (string == null) {
            return "";
        }
        LegacyComponentSerializer legacyComponentSerializer = LegacyComponentSerializer.builder().character('&').hexColors().build();
        return PlainTextComponentSerializer.plainText().serialize((Component)legacyComponentSerializer.deserialize(string.replace('\u00a7', '&')));
    }

    private Component text(PackRegistry packRegistry, String string, String string2, String string3, int n, int n2) {
        String string4 = packRegistry.getTextFont("layout", "gps-panel", string, string2);
        if (string4 == null) {
            return Component.empty();
        }
        Component component = ((TextComponent)LegacyText.font((Component)Component.text((String)string3), Key.key((String)string4))).color(TextColor.color((int)n));
        if (!string2.equals("arrow") && packRegistry.metrics().measure(component).advance() > (float)n2) {
            while (!string3.isEmpty() && packRegistry.metrics().measure(LegacyText.font((Component)Component.text((String)(string3 + "..")), Key.key((String)string4))).advance() > (float)n2) {
                string3 = string3.substring(0, string3.offsetByCodePoints(string3.length(), -1));
            }
            component = ((TextComponent)LegacyText.font((Component)Component.text((String)(string3 + "..")), Key.key((String)string4))).color(TextColor.color((int)n));
        }
        if (!string2.equals("arrow")) {
            component = SpaceUtil.space(-Math.round(packRegistry.metrics().measure(component).center())).append(component);
        }
        return packRegistry.metrics().neutral(component);
    }

    private static /* synthetic */ void lambda$render$0(String string, int n, TextReplacementConfig.Builder builder2) {
        builder2.matchLiteral(string).replacement(builder -> builder.color(TextColor.color((int)n)));
    }
}

