/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.condition;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sk.enorials.hud.condition.ConditionOperation;

public class HudLineConfig {
    private static final Pattern CONFIG_PATTERN = Pattern.compile("(?<key>\\w+) *\\{(?<args>\\S*)} *(?<match>true|false)? *(?<action>\\S+)? *(?<variable>\\S+)?");
    private final String key;
    private final Map<String, String> args;
    private final boolean match;
    private final String action;
    private final String variable;

    private HudLineConfig(String string, Map<String, String> map, boolean bl, String string2, String string3) {
        this.key = string.toLowerCase(Locale.ROOT);
        this.args = map;
        this.match = bl;
        this.action = string2;
        this.variable = string3;
    }

    public static HudLineConfig of(String string) {
        Matcher matcher = CONFIG_PATTERN.matcher(string);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid line config");
        }
        String string2 = matcher.group("key");
        HashMap<String, String> hashMap = new HashMap<String, String>();
        String string3 = matcher.group("args");
        if (string3 != null && !string3.isEmpty()) {
            for (String string4 : string3.split(" *; *")) {
                String[] stringArray = string4.split("=", 2);
                if (stringArray.length != 2) continue;
                hashMap.put(stringArray[0], stringArray[1]);
            }
        }
        boolean bl = matcher.group("match") != null && matcher.group("match").equalsIgnoreCase("true");
        String string5 = matcher.group("action");
        String string6 = matcher.group("variable");
        return new HudLineConfig(string2, hashMap, bl, string5, string6);
    }

    public String getKey() {
        return this.key;
    }

    public boolean isArg(String ... stringArray) {
        for (String string : stringArray) {
            if (!this.args.containsKey(string)) continue;
            return true;
        }
        return false;
    }

    public String getString(String ... stringArray) {
        for (String string : stringArray) {
            if (!this.args.containsKey(string)) continue;
            return this.args.get(string);
        }
        return null;
    }

    public Double getDouble(String ... stringArray) {
        for (String string : stringArray) {
            if (!this.args.containsKey(string)) continue;
            try {
                return Double.parseDouble(this.args.get(string).replace(",", "."));
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        return null;
    }

    public String[] getStringList(String ... stringArray) {
        for (String string : stringArray) {
            if (!this.args.containsKey(string)) continue;
            return this.args.get(string).split(" *, *");
        }
        return null;
    }

    public ConditionOperation getOperation() {
        return ConditionOperation.of(this.getString("o", "oper", "operation"));
    }

    public boolean getMatch() {
        return this.match;
    }

    public String getAction() {
        return this.action;
    }

    public String getVariable() {
        return this.variable;
    }
}

