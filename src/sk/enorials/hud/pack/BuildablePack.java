/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package sk.enorials.hud.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.avatar.AvatarFont;
import sk.enorials.hud.avatar.HeadMapFont;
import sk.enorials.hud.avatar.MinimapLayoutFonts;
import sk.enorials.hud.compat.ClientRenderProfile;
import sk.enorials.hud.compat.HudShaderFixes;
import sk.enorials.hud.compat.PackProfile;
import sk.enorials.hud.manager.DisplayManager;
import sk.enorials.hud.menu.MainMenuArtwork;
import sk.enorials.hud.model.FunctionData;
import sk.enorials.hud.model.HudElement;
import sk.enorials.hud.model.HudLayer;
import sk.enorials.hud.model.HudLayout;
import sk.enorials.hud.model.PackFont;
import sk.enorials.hud.model.TextLayer;
import sk.enorials.hud.model.TextureLayer;
import sk.enorials.hud.model.position.OffsetPosition;
import sk.enorials.hud.model.position.PositionContainer;
import sk.enorials.hud.model.position.ScreenPosition;
import sk.enorials.hud.model.position.ShaderInfo;
import sk.enorials.hud.pack.BossBarUtil;
import sk.enorials.hud.pack.HudPackComposition;
import sk.enorials.hud.pack.PackRegistry;
import sk.enorials.hud.social.SocialArtwork;
import sk.enorials.hud.theme.HotbarOverlay;
import sk.enorials.hud.theme.PartyFrameOpening;
import sk.enorials.hud.theme.ThemePack;
import sk.enorials.hud.util.CopyUtil;

public class BuildablePack {
    private final EnorialsHUD plugin;
    private final File packDirectory;
    private final File fontDirectory;
    private final File textureDirectory;
    private final PackRegistry registry;
    private final HudPackComposition.Snapshot composition;
    private final PositionContainer positions = new PositionContainer();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private char nextTextureChar = (char)57344;
    private final Map<Path, BufferedImage> sourceImages = new HashMap<Path, BufferedImage>();
    private final Map<Path, Object> writtenTextures = new HashMap<Path, Object>();
    private final Map<TextureRegion, String> textureRegions = new HashMap<TextureRegion, String>();
    private final Map<TileKey, PackRegistry.GlyphInfo> tileGlyphs = new HashMap<TileKey, PackRegistry.GlyphInfo>();

    private BufferedImage readImage(File file) throws IOException {
        BufferedImage bufferedImage = this.sourceImages.get(file.toPath());
        if (bufferedImage == null) {
            bufferedImage = ImageIO.read(file);
            if (bufferedImage == null) {
                throw new IOException("Invalid texture: " + String.valueOf(file));
            }
            this.sourceImages.put(file.toPath(), bufferedImage);
        }
        return bufferedImage;
    }

    public BuildablePack(EnorialsHUD enorialsHUD, PackRegistry packRegistry) {
        this(enorialsHUD, packRegistry, enorialsHUD.packComposition().snapshot());
    }

    public BuildablePack(EnorialsHUD enorialsHUD, PackRegistry packRegistry, HudPackComposition.Snapshot snapshot) {
        this(enorialsHUD, packRegistry, snapshot, new File(enorialsHUD.getDataFolder(), "built-pack"));
    }

    public BuildablePack(EnorialsHUD enorialsHUD, PackRegistry packRegistry, HudPackComposition.Snapshot snapshot, File file) {
        this.plugin = enorialsHUD;
        this.composition = snapshot;
        this.registry = packRegistry;
        this.packDirectory = file;
        this.fontDirectory = new File(this.packDirectory, "assets/enorialshud/font");
        this.textureDirectory = new File(this.packDirectory, "assets/enorialshud/textures/font");
    }

    public File getPackDirectory() {
        return this.packDirectory;
    }

    public void build() {
        this.build(true);
    }

    public void buildRaw() {
        this.build(false);
    }

    /*
     * WARNING - void declaration
     */
    private void build(boolean bl) {
        try {
            FileConfiguration fileConfiguration;
            Object object;
            if (this.packDirectory.exists()) {
                CopyUtil.deleteDirectory(this.packDirectory.toPath());
            }
            this.packDirectory.mkdirs();
            Path path = Path.of(this.plugin.getDataFolder().getPath(), "pack");
            if (Files.exists(path, new LinkOption[0])) {
                CopyUtil.copyDirectory(path, this.packDirectory.toPath());
            }
            this.plugin.packComposition().apply(this.packDirectory.toPath(), this.composition);
            File file = new File(this.packDirectory, "minecraft");
            if (file.exists()) {
                object = new File(this.packDirectory, "assets/minecraft");
                CopyUtil.copyDirectory(file.toPath(), ((File)object).toPath());
                CopyUtil.deleteDirectory(file.toPath());
            }
            this.resetGeneratedHudResources();
            object = new OffsetPosition(this.plugin.cfg().getInt("layout.global-offset.x"), this.plugin.cfg().getInt("layout.global-offset.y"), 0.0);
            this.buildHolderFonts(this.plugin.layouts().getLayouts(), "layout", (OffsetPosition)object);
            if (this.plugin.displays().hasDisplays()) {
                fileConfiguration = new ArrayList();
                for (DisplayManager.DisplayEntry object2 : this.plugin.displays().getDisplays()) {
                    fileConfiguration.add(object2.layout());
                }
                this.buildHolderFonts((Iterable<? extends HolderLike>)fileConfiguration, "layout", (OffsetPosition)object);
            }
            this.buildHolderFonts(this.plugin.popups().getPopups(), "popup", (OffsetPosition)object);
            this.buildThemeForegrounds((OffsetPosition)object);
            ThemePack.build(this.plugin, this.packDirectory.toPath());
            MainMenuArtwork.build(this.packDirectory.toPath());
            SocialArtwork.build(this.packDirectory.toPath());
            HotbarOverlay.build(this.packDirectory.toPath(), this.positions);
            AvatarFont.build(this.packDirectory.toPath(), this.registry);
            HeadMapFont.build(this.packDirectory.toPath(), ClientRenderProfile.protocol(776));
            MinimapLayoutFonts.build(this.packDirectory.toPath(), false);
            this.registry.loadMetrics(this.packDirectory.toPath());
            fileConfiguration = this.plugin.cfg();
            File file2 = new File(this.packDirectory, "assets/minecraft/shaders/core/text.vsh");
            if (file2.exists()) {
                String string2 = this.readAll(file2);
                string2 = HudShaderFixes.legacyHudShadows(string2);
                if (this.positions.usesExtendedIds()) {
                    String string3 = "id > 99.0 && id < 500.0";
                    if (!string2.contains(string3)) {
                        throw new IOException("HUD shader is missing its layout range; cannot safely add foreground layers");
                    }
                    String string5 = string2.replace(string3, "((id > 99.0 && id < 500.0) || (id >= 1000.0 && id < 4000.0))");
                    string5 = string5.replace("happyId >= 500.0 &&", "happyId >= 500.0 && happyId < 1000.0 &&");
                }
                String string8 = string2.replace("%BOSSBAR_OFFSET%", String.valueOf(BossBarUtil.getOffset(fileConfiguration.getInt("boss-bar.hud-position", 4))));
                string8 = string8.replace("%POSITIONS%", String.join((CharSequence)"\n", this.positions.build()));
                string8 = string8.replace("HH_", "EH_");
                this.writeAll(file2, string8);
            }
            if (bl) {
                PackProfile.apply(this.packDirectory.toPath());
            }
        }
        catch (IOException iOException) {
            throw new UncheckedIOException("Failed to build resource pack", iOException);
        }
    }

    private void buildHolderFonts(Iterable<? extends HolderLike> iterable, String string, OffsetPosition offsetPosition) throws IOException {
        ArrayList<HolderLike> arrayList = new ArrayList<HolderLike>();
        for (HolderLike holderLike2 : iterable) {
            arrayList.add(holderLike2);
        }
        arrayList.sort(Comparator.comparing(holderLike -> holderLike.getKey().toLowerCase()));
        for (HolderLike holderLike2 : arrayList) {
            File file = new File(this.fontDirectory, string + "/" + holderLike2.getKey() + "/textures.json");
            JsonObject jsonObject = new JsonObject();
            JsonArray jsonArray = new JsonArray();
            ArrayList<HudElement> arrayList2 = new ArrayList<HudElement>();
            for (HudElement hudElement2 : holderLike2.getElements()) {
                arrayList2.add(hudElement2);
            }
            arrayList2.sort(Comparator.comparing(hudElement -> hudElement.getKey().toLowerCase()));
            for (HudElement hudElement2 : arrayList2) {
                ArrayList<HudLayer> arrayList3 = new ArrayList<HudLayer>(hudElement2.getAsset().getLayers());
                arrayList3.sort(Comparator.comparing(hudLayer -> hudLayer.getKey().toLowerCase()));
                for (HudLayer hudLayer2 : arrayList3) {
                    if (hudLayer2 instanceof TextureLayer) {
                        TextureLayer textureLayer = (TextureLayer)hudLayer2;
                        this.addTextureLayer(string, holderLike2.getKey(), hudElement2, textureLayer, jsonArray, offsetPosition);
                        continue;
                    }
                    if (!(hudLayer2 instanceof TextLayer)) continue;
                    TextLayer textLayer = (TextLayer)hudLayer2;
                    this.addTextLayer(string, holderLike2.getKey(), hudElement2, textLayer, offsetPosition);
                }
            }
            jsonObject.add("providers", (JsonElement)jsonArray);
            if (jsonArray.isEmpty()) continue;
            this.writeJson(file, jsonObject);
        }
    }

    private void resetGeneratedHudResources() throws IOException {
        Path path;
        Path path2;
        Path path3 = new File(this.fontDirectory, "layout").toPath();
        if (Files.exists(path3, new LinkOption[0])) {
            CopyUtil.deleteDirectory(path3);
        }
        if (Files.exists(path2 = new File(this.fontDirectory, "popup").toPath(), new LinkOption[0])) {
            CopyUtil.deleteDirectory(path2);
        }
        if (Files.exists(path = this.textureDirectory.toPath(), new LinkOption[0])) {
            CopyUtil.deleteDirectory(path);
        }
    }

    private void buildThemeForegrounds(OffsetPosition offsetPosition) throws IOException {
        for (HudLayout hudLayout : this.plugin.layouts().getLayouts()) {
            File file;
            if (!List.of("player-panel", "target-panel", "party-panel").contains(hudLayout.getKey()) || !(file = new File(this.fontDirectory, "layout/" + hudLayout.getKey() + "/textures.json")).isFile()) continue;
            JsonObject jsonObject = (JsonObject)this.gson.fromJson(this.readAll(file), JsonObject.class);
            JsonArray jsonArray = jsonObject.getAsJsonArray("providers");
            for (HudElement hudElement : hudLayout.getElements()) {
                for (HudLayer hudLayer : hudElement.getAsset().getLayers()) {
                    HudLayer hudLayer2;
                    String string4;
                    if (!(hudLayer instanceof TextureLayer)) continue;
                    TextureLayer textureLayer = (TextureLayer)hudLayer;
                    if (!List.of("frame", "frame-compact").contains(textureLayer.getKey())) continue;
                    String string2 = textureLayer.getTexturePath();
                    File file2 = new File(this.plugin.getDataFolder(), "textures/" + string2);
                    if (!file2.isFile()) continue;
                    String string3 = string2.substring(string2.lastIndexOf(47) + 1).replace(".png", "_foreground.png");
                    InputStream inputStream = this.plugin.getResource("theme-art/frostbound/frames/" + string3);
                    try {
                        if (inputStream == null) {
                            continue;
                        }
                    }
                    finally {
                        if (inputStream == null) continue;
                        inputStream.close();
                        continue;
                    }
                    double d = textureLayer.getOffset().getZ();
                    for (String string4 : List.of("health", "mana", "food")) {
                        hudLayer2 = hudElement.getAsset().getLayer(string4);
                        if (hudLayer2 == null) continue;
                        d = Math.max(d, hudLayer2.getOffset().getZ());
                    }
                    YamlConfiguration yamlConfiguration = new YamlConfiguration();
                    string4 = string2.replace(".png", "_foreground.png");
                    yamlConfiguration.set("texture.path", (Object)string4);
                    yamlConfiguration.set("texture.resolution", (Object)textureLayer.getResolution());
                    yamlConfiguration.set("offset.x", (Object)textureLayer.getOffset().getX());
                    yamlConfiguration.set("offset.y", (Object)textureLayer.getOffset().getY());
                    yamlConfiguration.set("offset.z", (Object)(d + 0.5));
                    yamlConfiguration.set("scale.x", (Object)textureLayer.getScale().getX());
                    yamlConfiguration.set("scale.y", (Object)textureLayer.getScale().getY());
                    yamlConfiguration.set("scale.scale-offset", (Object)textureLayer.getScale().isScaleOffset());
                    hudLayer2 = new TextureLayer(hudElement.getAsset(), textureLayer.getKey() + "-foreground", (ConfigurationSection)yamlConfiguration, this.plugin);
                    PackRegistry.GlyphInfo glyphInfo = this.bakeTexture("layout", hudLayout.getKey(), hudElement, (TextureLayer)hudLayer2, jsonArray, offsetPosition, string4, this.readImage(file2));
                    this.registry.registerTextureInfo("layout", hudLayout.getKey(), hudElement.getKey(), ((TextureLayer)hudLayer2).getKey(), string4, glyphInfo);
                }
            }
            this.writeJson(file, jsonObject);
        }
    }

    private void addTextureLayer(String string, String string2, HudElement hudElement, TextureLayer textureLayer, JsonArray jsonArray, OffsetPosition offsetPosition) throws IOException {
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>();
        if (textureLayer.getTexturePath() != null && !textureLayer.getTexturePath().isEmpty()) {
            linkedHashSet.add(textureLayer.getTexturePath());
        }
        linkedHashSet.addAll(textureLayer.getConditions().getPossibleTexturePaths());
        ArrayList arrayList = new ArrayList(linkedHashSet);
        arrayList.sort(String.CASE_INSENSITIVE_ORDER);
        if (!arrayList.isEmpty()) {
            for (String string3 : arrayList) {
                this.addTextureLayerForPath(string, string2, hudElement, textureLayer, jsonArray, offsetPosition, string3);
            }
        }
    }

    private void addTextureLayerForPath(String string, String string2, HudElement hudElement, TextureLayer textureLayer, JsonArray jsonArray, OffsetPosition offsetPosition, String string3) throws IOException {
        if (string3 == null || string3.isEmpty()) {
            return;
        }
        FunctionData functionData = textureLayer.getFunction();
        File file = new File(this.plugin.getDataFolder(), "textures/" + string3);
        if (file.exists()) {
            BufferedImage bufferedImage = PartyFrameOpening.apply(file.getName(), this.readImage(file));
            if (functionData == null) {
                this.registry.registerTextureInfo(string, string2, hudElement.getKey(), textureLayer.getKey(), string3, this.bakeTexture(string, string2, hudElement, textureLayer, jsonArray, offsetPosition, string3, bufferedImage));
                return;
            }
            int n = Math.max(1, functionData.increment()) * textureLayer.getResolution();
            int n2 = functionData.orientation() == FunctionData.Orientation.HORIZONTAL ? bufferedImage.getWidth() : bufferedImage.getHeight();
            ArrayList<PackRegistry.GlyphInfo> arrayList = new ArrayList<PackRegistry.GlyphInfo>();
            for (int i = 1; i <= (n2 + n - 1) / n; ++i) {
                int n3 = Math.min(n2, i * n);
                BufferedImage bufferedImage2 = functionData.orientation() == FunctionData.Orientation.HORIZONTAL ? bufferedImage.getSubimage(functionData.reversed() ? n2 - n3 : 0, 0, n3, bufferedImage.getHeight()) : bufferedImage.getSubimage(0, functionData.reversed() ? 0 : n2 - n3, bufferedImage.getWidth(), n3);
                arrayList.add(this.bakeTexture(string, string2, hudElement, textureLayer, jsonArray, offsetPosition, string3.replace(".png", "_" + i + ".png"), bufferedImage2));
            }
            this.registry.registerTextureSteps(string, string2, hudElement.getKey(), textureLayer.getKey(), string3, arrayList, functionData);
        } else if (functionData != null) {
            ArrayList<PackRegistry.GlyphInfo> arrayList = new ArrayList<PackRegistry.GlyphInfo>();
            for (File file2 : this.findStepFiles(string3)) {
                BufferedImage bufferedImage = this.readImage(file2);
                String string4 = new File(this.plugin.getDataFolder(), "textures").toPath().relativize(file2.toPath()).toString().replace('\\', '/');
                arrayList.add(this.bakeTexture(string, string2, hudElement, textureLayer, jsonArray, offsetPosition, string4, bufferedImage));
            }
            if (!arrayList.isEmpty()) {
                this.registry.registerTextureSteps(string, string2, hudElement.getKey(), textureLayer.getKey(), string3, arrayList, functionData);
            }
        }
    }

    private PackRegistry.GlyphInfo bakeTexture(String string, String string2, HudElement hudElement, TextureLayer textureLayer, JsonArray jsonArray, OffsetPosition offsetPosition, String string3, BufferedImage bufferedImage) throws IOException {
        int n;
        int n2;
        int n3 = textureLayer.getResolution();
        int n4 = 240;
        String string4 = this.fontKeyForTextures(string, string2);
        ScreenPosition screenPosition = hudElement.getScreenPosition().mergeOffset(offsetPosition, textureLayer.getOffset());
        if (bufferedImage.getWidth() <= n4 && bufferedImage.getHeight() <= n4) {
            return this.bakeTile(string4, textureLayer, jsonArray, screenPosition, string3, bufferedImage);
        }
        ArrayList<PackRegistry.GlyphInfo> arrayList = new ArrayList<PackRegistry.GlyphInfo>();
        int n5 = -1;
        for (n2 = 0; n2 < bufferedImage.getHeight(); ++n2) {
            for (n = 0; n < bufferedImage.getWidth(); ++n) {
                if (bufferedImage.getRGB(n, n2) >>> 24 == 0) continue;
                n5 = Math.max(n5, n);
            }
        }
        for (n2 = 0; n2 < bufferedImage.getHeight(); n2 += n4) {
            for (n = 0; n < bufferedImage.getWidth(); n += n4) {
                BufferedImage bufferedImage2 = bufferedImage.getSubimage(n, n2, Math.min(n4, bufferedImage.getWidth() - n), Math.min(n4, bufferedImage.getHeight() - n2));
                boolean bl = false;
                block4: for (int i = 0; i < bufferedImage2.getHeight() && !bl; ++i) {
                    for (int j = 0; j < bufferedImage2.getWidth(); ++j) {
                        if (bufferedImage2.getRGB(j, i) >>> 24 == 0) continue;
                        bl = true;
                        continue block4;
                    }
                }
                if (!bl) continue;
                OffsetPosition offsetPosition2 = screenPosition.getOffset();
                ScreenPosition screenPosition2 = new ScreenPosition(screenPosition.getRelativeX(), screenPosition.getRelativeY(), new OffsetPosition(offsetPosition2.getX() + (int)Math.round((double)n / (double)n3 * textureLayer.getScale().getX()), offsetPosition2.getY() + (int)Math.round((double)n2 / (double)n3 * textureLayer.getScale().getY()), offsetPosition2.getZ()));
                arrayList.add(this.bakeTile(string4, textureLayer, jsonArray, screenPosition2, string3.replace(".png", "_tile_" + n + "_" + n2 + ".png"), bufferedImage2));
            }
        }
        return new PackRegistry.GlyphInfo(arrayList, bufferedImage.getWidth() / n3, (int)(0.5f + (float)(n5 + 1) / (float)n3) + 1);
    }

    private PackRegistry.GlyphInfo bakeTile(String string, TextureLayer textureLayer, JsonArray jsonArray, ScreenPosition screenPosition, String string2, BufferedImage bufferedImage) throws IOException {
        int n = this.positions.addThenGetAscent(ShaderInfo.position(screenPosition).layer(textureLayer).build());
        int n2 = Math.max(1, bufferedImage.getHeight() / textureLayer.getResolution());
        int n3 = bufferedImage.getWidth() / textureLayer.getResolution();
        ImageRegion imageRegion = ImageRegion.of(bufferedImage);
        String string3 = string2.replaceFirst("(?:_tile_\\d+_\\d+)?\\.png$", "").replaceFirst("_\\d+$", "");
        TextureRegion textureRegion = new TextureRegion(string3, imageRegion);
        TileKey tileKey = new TileKey(string, textureRegion, n, n2, n3);
        PackRegistry.GlyphInfo glyphInfo = this.tileGlyphs.get(tileKey);
        if (glyphInfo != null) {
            return glyphInfo;
        }
        String string4 = this.textureRegions.putIfAbsent(textureRegion, string2);
        if (string4 != null) {
            string2 = string4;
        }
        char c = this.nextTextureChar;
        this.nextTextureChar = (char)(c + '\u0001');
        char c2 = c;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "bitmap");
        jsonObject.addProperty("file", "enorialshud:font/" + string2);
        jsonObject.addProperty("height", (Number)n2);
        jsonObject.addProperty("ascent", (Number)n);
        JsonArray jsonArray2 = new JsonArray();
        jsonArray2.add(String.valueOf(c2));
        jsonObject.add("chars", (JsonElement)jsonArray2);
        jsonArray.add((JsonElement)jsonObject);
        File file = new File(this.textureDirectory, string2);
        if (!imageRegion.equals(this.writtenTextures.get(file.toPath()))) {
            Files.createDirectories(file.getParentFile().toPath(), new FileAttribute[0]);
            ImageIO.write((RenderedImage)bufferedImage, "png", file);
            this.writtenTextures.put(file.toPath(), imageRegion);
        }
        PackRegistry.GlyphInfo glyphInfo2 = new PackRegistry.GlyphInfo(c2, string, n3);
        this.tileGlyphs.put(tileKey, glyphInfo2);
        return glyphInfo2;
    }

    private void addTextLayer(String string, String string2, HudElement hudElement, TextLayer textLayer, OffsetPosition offsetPosition) throws IOException {
        String string3 = textLayer.getFont();
        PackFont packFont = this.plugin.fonts().get(string3);
        if (packFont == null && string3.contains(":")) {
            this.registry.registerText(string, string2, hudElement.getKey(), textLayer.getKey(), string3);
        } else if (packFont == null) {
            this.registry.registerText(string, string2, hudElement.getKey(), textLayer.getKey(), "minecraft:default");
        } else {
            ScreenPosition screenPosition = hudElement.getScreenPosition().mergeOffset(offsetPosition, textLayer.getOffset());
            int n = this.positions.addThenGetAscent(ShaderInfo.position(screenPosition).layer(textLayer).build());
            JsonObject jsonObject = new JsonObject();
            JsonArray jsonArray = new JsonArray();
            for (JsonObject object2 : packFont.buildProviders(n)) {
                jsonArray.add((JsonElement)object2);
            }
            jsonObject.add("providers", (JsonElement)jsonArray);
            String string4 = "popup".equals(string) ? hudElement.getKey() : hudElement.getAsset().getKey();
            String string5 = string + "/" + string2 + "/fonts/" + (String)string4 + "/" + textLayer.getKey() + ".json";
            File file = new File(this.fontDirectory, string5);
            Files.createDirectories(file.getParentFile().toPath(), new FileAttribute[0]);
            this.writeJson(file, jsonObject);
            for (File file2 : packFont.getReferencedTextures(this.plugin.getDataFolder())) {
                if (!file2.exists()) continue;
                String string6 = file2.getAbsolutePath().replace(this.plugin.getDataFolder().getAbsolutePath() + File.separator + "textures" + File.separator, "");
                File file3 = new File(this.textureDirectory, string6);
                if (file2.toPath().equals(this.writtenTextures.get(file3.toPath()))) continue;
                Files.createDirectories(file3.getParentFile().toPath(), new FileAttribute[0]);
                Files.copy(file2.toPath(), file3.toPath(), StandardCopyOption.REPLACE_EXISTING);
                this.writtenTextures.put(file3.toPath(), file2.toPath());
            }
            this.registry.registerText(string, string2, hudElement.getKey(), textLayer.getKey(), "enorialshud:" + string5.replace(".json", ""));
        }
    }

    private List<File> findStepFiles(String string) {
        ArrayList<File> arrayList = new ArrayList<File>();
        if (string != null && !string.isEmpty()) {
            File file2 = new File(this.plugin.getDataFolder(), "textures");
            File file3 = new File(string);
            File file4 = file3.getParentFile() == null ? file2 : new File(file2, file3.getParent());
            String string3 = file3.getName();
            if (!string3.endsWith(".png")) {
                return arrayList;
            }
            String string4 = string3.substring(0, string3.length() - 4);
            if (file4.exists() && file4.isDirectory()) {
                File[] fileArray = file4.listFiles((file, string2) -> string2.startsWith(string4 + "_") && string2.endsWith(".png"));
                if (fileArray != null && fileArray.length != 0) {
                    TreeMap<Integer, File> treeMap = new TreeMap<Integer, File>();
                    for (File file5 : fileArray) {
                        String string5 = file5.getName();
                        String string6 = string5.substring((string4 + "_").length(), string5.length() - 4);
                        try {
                            int n = Integer.parseInt(string6);
                            treeMap.put(n, file5);
                        }
                        catch (NumberFormatException numberFormatException) {
                            // empty catch block
                        }
                    }
                    arrayList.addAll(treeMap.values());
                    return arrayList;
                }
                return arrayList;
            }
            return arrayList;
        }
        return arrayList;
    }

    private void writeJson(File file, JsonObject jsonObject) throws IOException {
        Files.createDirectories(file.getParentFile().toPath(), new FileAttribute[0]);
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8, new OpenOption[0]);){
            this.gson.toJson((JsonElement)jsonObject, (Appendable)bufferedWriter);
        }
    }

    private String fontKeyForTextures(String string, String string2) {
        return "enorialshud:" + string + "/" + string2 + "/textures";
    }

    private String readAll(File file) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader bufferedReader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);){
            String string;
            while ((string = bufferedReader.readLine()) != null) {
                stringBuilder.append(string).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    private void writeAll(File file, String string) throws IOException {
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8, new OpenOption[0]);){
            bufferedWriter.write(string);
        }
    }

    public static interface HolderLike {
        public String getKey();

        public Iterable<HudElement> getElements();
    }

    private record ImageRegion(DataBuffer buffer, SampleModel samples, ColorModel colors, int x, int y, int width, int height) {
        static ImageRegion of(BufferedImage bufferedImage) {
            WritableRaster writableRaster = bufferedImage.getRaster();
            return new ImageRegion(writableRaster.getDataBuffer(), writableRaster.getSampleModel(), bufferedImage.getColorModel(), writableRaster.getSampleModelTranslateX(), writableRaster.getSampleModelTranslateY(), bufferedImage.getWidth(), bufferedImage.getHeight());
        }
    }

    private record TextureRegion(String family, ImageRegion image) {
    }

    private record TileKey(String font, TextureRegion texture, int ascent, int height, int width) {
    }
}

