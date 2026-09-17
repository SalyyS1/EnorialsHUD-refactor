/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.compat;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import sk.enorials.hud.EnorialsHUD;

public final class LegacyBarAtlas {
    public static final String PATH = "assets/minecraft/textures/gui/icons.png";
    private static final String SPRITES = "assets/minecraft/textures/gui/sprites/hud/";
    private static final List<Slot> SLOTS = LegacyBarAtlas.slots();

    private LegacyBarAtlas() {
    }

    private static List<Slot> slots() {
        ArrayList<Slot> arrayList = new ArrayList<Slot>();
        for (boolean bl : new boolean[]{false, true}) {
            int n = bl ? 45 : 0;
            String string = bl ? "hardcore_" : "";
            arrayList.add(new Slot("heart/container" + (bl ? "_hardcore" : ""), 16, n));
            arrayList.add(new Slot("heart/container" + (bl ? "_hardcore" : "") + "_blinking", 25, n));
            String[] stringArray = new String[]{"", "poisoned_", "withered_", "absorbing_", "frozen_"};
            int[] nArray = new int[]{52, 88, 124, 160, 178};
            for (int i = 0; i < stringArray.length; ++i) {
                for (int j = 0; j < (i < 3 ? 4 : 2); ++j) {
                    arrayList.add(new Slot("heart/" + stringArray[i] + string + (j % 2 == 0 ? "full" : "half") + (j >= 2 ? "_blinking" : ""), nArray[i] + j * 9, n));
                }
            }
        }
        arrayList.add(new Slot("armor_empty", 16, 9));
        arrayList.add(new Slot("armor_half", 25, 9));
        arrayList.add(new Slot("armor_full", 34, 9));
        arrayList.add(new Slot("air", 16, 18));
        arrayList.add(new Slot("air_bursting", 25, 18));
        arrayList.add(new Slot("food_empty", 16, 27));
        arrayList.add(new Slot("food_empty_hunger", 133, 27));
        arrayList.add(new Slot("food_full", 52, 27));
        arrayList.add(new Slot("food_half", 61, 27));
        arrayList.add(new Slot("food_full_hunger", 88, 27));
        arrayList.add(new Slot("food_half_hunger", 97, 27));
        arrayList.add(new Slot("heart/vehicle_container", 52, 9));
        arrayList.add(new Slot("heart/vehicle_full", 88, 9));
        arrayList.add(new Slot("heart/vehicle_half", 97, 9));
        return List.copyOf(arrayList);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static byte[] visible(EnorialsHUD enorialsHUD, Map<String, byte[]> map) throws IOException {
        BufferedImage bufferedImage;
        try (Object object = enorialsHUD.getResource("default/pack/assets/minecraft/textures/gui/icons.png");){
            if (object == null) {
                throw new IOException("Missing legacy GUI atlas");
            }
            bufferedImage = ImageIO.read((InputStream)object);
        }
        if (bufferedImage == null) {
            throw new IOException("Invalid legacy GUI atlas");
        }
        object = bufferedImage.createGraphics();
        ((Graphics2D)object).setComposite(AlphaComposite.Src);
        try {
            for (Slot slot : SLOTS) {
                byte[] byArray = map.get(SPRITES + slot.sprite + ".png");
                if (byArray == null) {
                    throw new IOException("Missing bar sprite: " + slot.sprite);
                }
                BufferedImage bufferedImage2 = ImageIO.read(new ByteArrayInputStream(byArray));
                ((Graphics)object).drawImage(bufferedImage2, slot.x, slot.y, 9, 9, null);
            }
        }
        finally {
            ((Graphics)object).dispose();
        }
        return LegacyBarAtlas.encode(bufferedImage);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static byte[] hidden(byte[] byArray) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(byArray));
        if (bufferedImage == null || bufferedImage.getWidth() != bufferedImage.getHeight() || bufferedImage.getWidth() % 256 != 0) {
            throw new IOException("Legacy GUI atlas must be a square multiple of 256 pixels");
        }
        int n = bufferedImage.getWidth() / 256;
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setComposite(AlphaComposite.Clear);
        try {
            for (Slot slot : SLOTS) {
                graphics2D.fillRect(slot.x * n, slot.y * n, 9 * n, 9 * n);
            }
        }
        finally {
            graphics2D.dispose();
        }
        return LegacyBarAtlas.encode(bufferedImage);
    }

    private static byte[] encode(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write((RenderedImage)bufferedImage, "PNG", byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private record Slot(String sprite, int x, int y) {
    }
}

