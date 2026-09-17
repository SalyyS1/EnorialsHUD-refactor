/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.condition;

import java.util.List;

/*
 * Uses 'sealed' constructs - enablewith --sealed true
 */
public enum ConditionOperation {
    GREATER_THAN(new String[]{">"}){

        @Override
        public boolean evaluate(double d, double d2) {
            return d > d2;
        }
    }
    ,
    GREATER_THAN_OR_EQUAL(new String[]{">=", "=>"}){

        @Override
        public boolean evaluate(double d, double d2) {
            return d >= d2;
        }
    }
    ,
    LESS_THAN(new String[]{"<"}){

        @Override
        public boolean evaluate(double d, double d2) {
            return d < d2;
        }
    }
    ,
    LESS_THAN_OR_EQUAL(new String[]{"<=", "=<"}){

        @Override
        public boolean evaluate(double d, double d2) {
            return d <= d2;
        }
    }
    ,
    EQUAL(new String[]{"=", "=="}){

        @Override
        public boolean evaluate(double d, double d2) {
            return d == d2;
        }
    };

    private final List<String> aliases;

    private ConditionOperation(String[] stringArray) {
        this.aliases = List.of(stringArray);
    }

    public abstract boolean evaluate(double var1, double var3);

    public static ConditionOperation of(String string) {
        if (string != null) {
            string = string.replace("\"", "");
            for (ConditionOperation conditionOperation : ConditionOperation.values()) {
                if (!conditionOperation.aliases.contains(string)) continue;
                return conditionOperation;
            }
        }
        return EQUAL;
    }

    public static enum EvaluationType {
        PERCENT,
        NUMBER;

    }
}

