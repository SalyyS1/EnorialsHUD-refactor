/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.theme;

import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PartyFrameOpening {
    private static final Pattern FRAME = Pattern.compile("party_(small|medium|large)_frame(?:_compact)?\\.png");

    private PartyFrameOpening() {
    }

    public static BufferedImage apply(String string, BufferedImage bufferedImage) {
        int n;
        Matcher matcher = FRAME.matcher(string);
        if (!matcher.matches()) {
            return bufferedImage;
        }
        switch (matcher.group(1)) {
            case "small": {
                int n2 = 64;
                break;
            }
            case "medium": {
                int n2 = 80;
                break;
            }
            default: {
                int n2 = n = 96;
            }
        }
        if (bufferedImage.getWidth() < n + 16 || bufferedImage.getHeight() < n + 44) {
            return bufferedImage;
        }
        Object object = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), 2);
        ((BufferedImage)object).setRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null, 0, bufferedImage.getWidth()), 0, bufferedImage.getWidth());
        RoundRectangle2D.Double double_ = new RoundRectangle2D.Double(8.0, 44.0, n, n, 16.0, 16.0);
        for (int i = 44; i < 44 + n; ++i) {
            for (int j = 8; j < 8 + n; ++j) {
                if (!double_.contains((double)j + 0.5, (double)i + 0.5)) continue;
                ((BufferedImage)object).setRGB(j, i, 0);
            }
        }
        return object;
    }
}

