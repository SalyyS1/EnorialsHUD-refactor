/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.betonquest.betonquest.BetonQuest
 *  org.betonquest.betonquest.api.BetonQuestApi
 *  org.betonquest.betonquest.api.QuestException
 *  org.betonquest.betonquest.api.config.quest.QuestPackage
 *  org.betonquest.betonquest.api.identifier.ConditionIdentifier
 *  org.betonquest.betonquest.api.identifier.JournalEntryIdentifier
 *  org.betonquest.betonquest.api.profile.OnlineProfile
 *  org.betonquest.betonquest.api.profile.Profile
 *  org.betonquest.betonquest.api.reload.ReloadPhase
 *  org.betonquest.betonquest.api.text.Text
 *  org.bukkit.Bukkit
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Player
 */
package sk.enorials.gps.integration;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.api.identifier.ConditionIdentifier;
import org.betonquest.betonquest.api.identifier.JournalEntryIdentifier;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.reload.ReloadPhase;
import org.betonquest.betonquest.api.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class QuestRouteNames {
    private Bridge bridge;

    public String name(Player player, ConfigurationSection configurationSection, String string) {
        if (!configurationSection.getBoolean("auto-name", true) || !Bukkit.getPluginManager().isPluginEnabled("BetonQuest")) {
            return string;
        }
        try {
            String string2;
            if (this.bridge == null) {
                this.bridge = new Bridge();
            }
            return (string2 = this.bridge.name(player, configurationSection)) == null || string2.isBlank() ? string : string2;
        }
        catch (Exception | LinkageError throwable) {
            return string;
        }
    }

    private static final class Bridge {
        private final BetonQuestApi api = BetonQuest.getInstance().getBetonQuestApi();
        private final Map<String, Text> texts = new HashMap<String, Text>();
        private static final Pattern TITLE = Pattern.compile("(?im)(?:Daily\\s+)?Quest\\s+name\\s*:[ \\t]*(?:\\R[ \\t]*)?([^\\r\\n]+)");

        Bridge() {
            this.api.reloader().register(ReloadPhase.POST, this.texts::clear);
        }

        String name(Player player, ConfigurationSection configurationSection) throws QuestException {
            OnlineProfile onlineProfile = this.api.profiles().getProfile(player);
            String string = configurationSection.getString("name-ref", "").trim();
            if (!string.isEmpty()) {
                int n = string.lastIndexOf(62);
                if (n < 1) {
                    return null;
                }
                QuestPackage questPackage = (QuestPackage)this.api.packages().getPackages().get(string.substring(0, n));
                if (questPackage == null) {
                    return null;
                }
                return this.render(questPackage, string.substring(n + 1), (Profile)onlineProfile);
            }
            for (String string2 : configurationSection.getStringList("conditions")) {
                JournalEntryIdentifier journalEntryIdentifier;
                String string3;
                Matcher matcher;
                String string4;
                String[] stringArray;
                ConditionIdentifier conditionIdentifier;
                if (!string2.startsWith("betonquest:") || string2.startsWith("betonquest:!") || (conditionIdentifier = (ConditionIdentifier)this.api.identifiers().getFactory(ConditionIdentifier.class).parseIdentifier(null, string2.substring(11))).isInverted() || (stringArray = (string4 = conditionIdentifier.getPackage().getConfig().getString("conditions." + conditionIdentifier.get(), "").trim()).split("\\s+")).length < 2 || !stringArray[0].equalsIgnoreCase("journal") || !(matcher = TITLE.matcher(string3 = this.render((journalEntryIdentifier = (JournalEntryIdentifier)this.api.identifiers().getFactory(JournalEntryIdentifier.class).parseIdentifier(conditionIdentifier.getPackage(), stringArray[1])).getPackage(), "journal." + journalEntryIdentifier.get(), (Profile)onlineProfile))).find()) continue;
                return matcher.group(1).strip();
            }
            return null;
        }

        String render(QuestPackage questPackage, String string, Profile profile) throws QuestException {
            String string2 = questPackage.getQuestPath() + ">" + string;
            Text text = this.texts.get(string2);
            if (text == null) {
                text = (Text)this.api.instructions().createSection(questPackage, (ConfigurationSection)questPackage.getConfig()).read().value(string.split("\\.")).translationSection().get().getValue(profile);
                this.texts.put(string2, text);
            }
            return PlainTextComponentSerializer.plainText().serialize(text.asComponent(profile));
        }
    }
}

