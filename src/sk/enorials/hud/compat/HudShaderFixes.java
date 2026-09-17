/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.compat;

import java.io.IOException;

public final class HudShaderFixes {
    private static final String MARKER = "// ENO_HUD_SHADOW_FIX_V1";
    private static final String BRANCH = "if (id > 99.0 && id < 500.0 && Color.a != 0.0) {";

    private HudShaderFixes() {
    }

    public static String legacyHudShadows(String string) throws IOException {
        if (string.contains(MARKER)) {
            return string;
        }
        if (!string.contains(BRANCH)) {
            throw new IOException("HUD shader has no encoded layout branch; cannot safely update legacy HUD shadows");
        }
        return string.replace(BRANCH, "if (id > 99.0 && id < 500.0 && Color.a != 0.0) {\n#ifdef ENO_LEGACY_GPS\n    // ENO_HUD_SHADOW_FIX_V1\n    // Reject the native shadow pass only for our encoded HUD glyphs.\n    if (fract(Position.z + 0.001) < 0.02) {\n        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);\n        return;\n    }\n#endif\n");
    }
}

