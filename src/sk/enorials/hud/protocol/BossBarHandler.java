/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.bossbar.BossBar
 *  net.kyori.adventure.bossbar.BossBar$Color
 *  net.kyori.adventure.bossbar.BossBar$Overlay
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.compat.Numbers;
import sk.enorials.hud.theme.HotbarOverlay;
import sk.enorials.hud.theme.HudTheme;
import sk.enorials.hud.theme.ThemeStyle;
import sk.enorials.hud.util.SpaceUtil;

public final class BossBarHandler {
    private final Player player;
    private final List<BossBar> bars = new ArrayList<BossBar>();
    private final BossBar hud;
    private final BossBar minimapRow;
    private final ToIntFunction<Component> width;
    private Component hudText = Component.empty();
    private Component minimapText = Component.empty();
    private Component conversationText = Component.empty();
    private Component questLogText = Component.empty();
    private HudTheme paintedTheme;
    private Component lastHud;
    private Component lastConversation;
    private Component lastQuest;
    private Component themedHud;
    private Component themedConversation;
    private Component themedQuest;

    public BossBarHandler(Player player) {
        this(player, (ConfigurationSection)EnorialsHUD.getInstance().cfg(), component -> (int)Math.ceil(EnorialsHUD.getInstance().packs().registry().metrics().measure((Component)component).advance()));
    }

    BossBarHandler(Player player, ConfigurationSection configurationSection, ToIntFunction<Component> toIntFunction) {
        this.player = player;
        this.width = toIntFunction;
        int n = Math.max(0, configurationSection.getInt("boss-bar.max", 7));
        int n2 = Numbers.clamp(configurationSection.getInt("boss-bar.hud-position", 4) - 1, 0, n);
        BossBar.Color color = BossBar.Color.valueOf((String)configurationSection.getString("boss-bar.color", "yellow").toUpperCase(Locale.ROOT));
        this.hud = BossBar.bossBar((Component)Component.empty(), (float)0.0f, (BossBar.Color)color, (BossBar.Overlay)BossBar.Overlay.PROGRESS);
        for (int i = 0; i < n; ++i) {
            this.bars.add(BossBar.bossBar((Component)Component.empty(), (float)0.0f, (BossBar.Color)color, (BossBar.Overlay)BossBar.Overlay.PROGRESS));
        }
        this.bars.add(n2, this.hud);
        this.minimapRow = n2 > 0 ? this.bars.get(0) : null;
    }

    public void init() {
        this.bars.forEach(arg_0 -> ((Player)this.player).showBossBar(arg_0));
    }

    public void disable() {
        this.bars.forEach(arg_0 -> ((Player)this.player).hideBossBar(arg_0));
    }

    public void setHud(Component component) {
        this.hudText = component;
        this.refreshHud();
    }

    public void setConversationOverlay(Component component) {
        this.conversationText = component;
        this.refreshHud();
    }

    public void setQuestLogOverlay(Component component) {
        this.questLogText = component;
        this.refreshHud();
    }

    private void refreshHud() {
        this.refreshHud(-1);
    }

    public void refreshHotbar(int n) {
        this.refreshHud(n);
    }

    private void refreshHud(int n) {
        Component component;
        HudTheme hudTheme;
        EnorialsHUD enorialsHUD = EnorialsHUD.getInstance();
        HudTheme hudTheme2 = hudTheme = enorialsHUD == null ? HudTheme.ORIGINAL : enorialsHUD.storage().theme(this.player.getUniqueId());
        if (hudTheme != this.paintedTheme) {
            this.lastHud = null;
            this.lastConversation = null;
            this.lastQuest = null;
            this.paintedTheme = hudTheme;
        }
        if (this.lastHud != this.hudText) {
            this.themedHud = ThemeStyle.paint(this.hudText, hudTheme);
            this.lastHud = this.hudText;
        }
        if (this.lastConversation != this.conversationText) {
            this.themedConversation = ThemeStyle.paint(this.conversationText, hudTheme);
            this.lastConversation = this.conversationText;
        }
        if (this.lastQuest != this.questLogText) {
            this.themedQuest = ThemeStyle.paint(this.questLogText, hudTheme);
            this.lastQuest = this.questLogText;
        }
        Component component2 = component = this.minimapRow == null ? this.minimapText.append(this.themedConversation).append(this.themedQuest) : this.themedConversation.append(this.themedQuest);
        if (enorialsHUD != null) {
            component = component.append(HotbarOverlay.render(enorialsHUD, this.player, n));
        }
        Component component3 = enorialsHUD == null ? this.themedHud : enorialsHUD.avatars().paint(this.player, this.themedHud);
        this.hud.name(BossBarHandler.combineCentered(component, component3, this.width.applyAsInt(this.hudText)));
    }

    public void refreshTheme() {
        this.refreshHud();
    }

    public void setMinimapOverlay(Component component) {
        this.minimapText = component;
        if (this.minimapRow != null) {
            this.minimapRow.name(component);
        } else {
            this.refreshHud();
        }
    }

    public static Component combineCentered(Component component, Component component2, int n) {
        if (component.equals((Object)Component.empty())) {
            return component2;
        }
        int n2 = n / 2;
        return ((TextComponent)((TextComponent)((TextComponent)Component.empty().append(component)).append(SpaceUtil.space(-n2))).append(component2)).append(SpaceUtil.space(n2 - n));
    }
}

