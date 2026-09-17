/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.ArgumentBuilder
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandExceptionType
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.arguments.EntityArgument
 *  net.minecraft.commands.arguments.coordinates.Vec3Argument
 *  net.minecraft.commands.arguments.selector.EntitySelector
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.phys.Vec3
 *  org.bukkit.Location
 *  org.bukkit.craftbukkit.entity.CraftPlayer
 *  org.bukkit.entity.Player
 */
package sk.enorials.minimap.command;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.minimap.EnorialsMinimap;
import sk.enorials.minimap.command.BrigadierCommand;
import sk.enorials.minimap.config.Config;
import sk.enorials.minimap.listener.MinimapListener;
import sk.enorials.minimap.map.Minimap;
import sk.enorials.minimap.map.MinimapLayer;
import sk.enorials.minimap.map.MinimapPreset;
import sk.enorials.minimap.map.MinimapScreenPosition;
import sk.enorials.minimap.map.MinimapShape;
import sk.enorials.minimap.map.MinimapSize;
import sk.enorials.minimap.map.SecondaryMinimapLayer;
import sk.enorials.minimap.map.fullscreen.FullscreenMinimap;
import sk.enorials.minimap.map.icon.MinimapIcon;
import sk.enorials.minimap.map.icon.provider.MinimapIconProvider;
import sk.enorials.minimap.map.marker.MarkerCategory;
import sk.enorials.minimap.map.marker.MarkerLimits;
import sk.enorials.minimap.map.marker.MarkerMinimapLayer;
import sk.enorials.minimap.map.renderer.MinimapIconRenderer;
import sk.enorials.minimap.menu.EnomapMenuListener;
import sk.enorials.minimap.storage.MinimapPlayerDatabase;

public class MinimapCommand
extends BrigadierCommand {
    public MinimapCommand(EnorialsMinimap enorialsMinimap) {
        super(enorialsMinimap);
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        LiteralArgumentBuilder literalArgumentBuilder = (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"map").requires(commandSourceStack -> MinimapCommand.access(commandSourceStack, "enorials.minimap.use"))).executes(this::openMenu);
        literalArgumentBuilder.then((ArgumentBuilder)MinimapCommand.runFor(Commands.literal((String)"enable"), this::enable));
        literalArgumentBuilder.then((ArgumentBuilder)MinimapCommand.runFor(Commands.literal((String)"disable"), this::disable));
        LiteralArgumentBuilder literalArgumentBuilder2 = Commands.literal((String)"position");
        for (MinimapScreenPosition minimapScreenPosition : MinimapScreenPosition.values()) {
            literalArgumentBuilder2.then(Commands.literal((String)minimapScreenPosition.name().toLowerCase(Locale.ROOT)).executes(commandContext -> this.position((CommandContext<CommandSourceStack>)commandContext, minimapScreenPosition)));
        }
        literalArgumentBuilder.then((ArgumentBuilder)literalArgumentBuilder2);
        LiteralArgumentBuilder literalArgumentBuilder3 = Commands.literal((String)"size");
        for (MinimapSize minimapSize : MinimapSize.values()) {
            literalArgumentBuilder3.then(Commands.literal((String)minimapSize.name().toLowerCase(Locale.ROOT)).executes(commandContext -> this.size((CommandContext<CommandSourceStack>)commandContext, minimapSize)));
        }
        literalArgumentBuilder.then((ArgumentBuilder)literalArgumentBuilder3);
        LiteralArgumentBuilder literalArgumentBuilder4 = Commands.literal((String)"shape");
        for (MinimapShape minimapShape : MinimapShape.values()) {
            literalArgumentBuilder4.then(Commands.literal((String)minimapShape.name().toLowerCase(Locale.ROOT)).executes(commandContext -> this.shape((CommandContext<CommandSourceStack>)commandContext, minimapShape)));
        }
        literalArgumentBuilder.then((ArgumentBuilder)literalArgumentBuilder4);
        literalArgumentBuilder.then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"danger").then(Commands.literal((String)"on").executes(commandContext -> this.setDangerRing((CommandContext<CommandSourceStack>)commandContext, true)))).then(Commands.literal((String)"off").executes(commandContext -> this.setDangerRing((CommandContext<CommandSourceStack>)commandContext, false)))).then(Commands.literal((String)"toggle").executes(this::toggleDangerRing)));
        literalArgumentBuilder.then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"fog").then(Commands.literal((String)"on").executes(commandContext -> this.setFogStyling((CommandContext<CommandSourceStack>)commandContext, true)))).then(Commands.literal((String)"off").executes(commandContext -> this.setFogStyling((CommandContext<CommandSourceStack>)commandContext, false)))).then(Commands.literal((String)"toggle").executes(this::toggleFogStyling)));
        literalArgumentBuilder.then(Commands.literal((String)"preset").then(((RequiredArgumentBuilder)Commands.argument((String)"slot", (ArgumentType)IntegerArgumentType.integer((int)1, (int)3)).then(Commands.literal((String)"save").executes(this::savePreset))).then(Commands.literal((String)"load").executes(this::loadPreset))));
        LiteralArgumentBuilder literalArgumentBuilder5 = (LiteralArgumentBuilder)Commands.literal((String)"marker").requires(commandSourceStack -> MinimapCommand.access(commandSourceStack, "enorials.minimap.markers.use"));
        literalArgumentBuilder5.then(Commands.literal((String)"add").then(Commands.argument((String)"name", (ArgumentType)StringArgumentType.string()).then(MinimapCommand.runFor(this.iconArgument(), this::addMarker))));
        RequiredArgumentBuilder requiredArgumentBuilder = Commands.argument((String)"name", (ArgumentType)StringArgumentType.string()).suggests(this::suggestMarkers);
        requiredArgumentBuilder.then(MinimapCommand.runFor(this.iconArgument(), this::changeMarkerIcon));
        requiredArgumentBuilder.then(Commands.literal((String)"icon").then(MinimapCommand.runFor(this.iconArgument(), this::changeMarkerIcon)));
        requiredArgumentBuilder.then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"range").then((ArgumentBuilder)MinimapCommand.runFor(Commands.argument((String)"distance", (ArgumentType)IntegerArgumentType.integer((int)1, (int)50000)), this::setMarkerRange))).then((ArgumentBuilder)MinimapCommand.runFor(Commands.literal((String)"off"), this::clearMarkerRange))).then((ArgumentBuilder)MinimapCommand.runFor(Commands.literal((String)"reset"), this::clearMarkerRange)));
        requiredArgumentBuilder.then(Commands.literal((String)"category").then((ArgumentBuilder)MinimapCommand.runFor(Commands.argument((String)"category", (ArgumentType)StringArgumentType.word()).suggests(this::suggestMarkerCategories), this::setMarkerCategory)));
        requiredArgumentBuilder.then(Commands.literal((String)"name").then((ArgumentBuilder)MinimapCommand.runFor(Commands.argument((String)"new_name", (ArgumentType)StringArgumentType.string()), this::renameMarker)));
        literalArgumentBuilder5.then(Commands.literal((String)"set").then((ArgumentBuilder)requiredArgumentBuilder));
        literalArgumentBuilder5.then(Commands.literal((String)"rename").then(Commands.argument((String)"name", (ArgumentType)StringArgumentType.string()).suggests(this::suggestMarkers).then((ArgumentBuilder)MinimapCommand.runFor(Commands.argument((String)"new_name", (ArgumentType)StringArgumentType.string()), this::renameMarker))));
        literalArgumentBuilder5.then(Commands.literal((String)"remove").then((ArgumentBuilder)MinimapCommand.runFor(Commands.argument((String)"name", (ArgumentType)StringArgumentType.string()).suggests(this::suggestMarkers), this::removeMarker)));
        literalArgumentBuilder5.then(this.fullscreenArgument());
        literalArgumentBuilder5.then(Commands.literal((String)"at").then(Commands.argument((String)"name", (ArgumentType)StringArgumentType.string()).then(this.iconArgument().then(Commands.argument((String)"coordinates", (ArgumentType)Vec3Argument.vec3()).executes(this::addMarkerAt)))));
        literalArgumentBuilder.then((ArgumentBuilder)literalArgumentBuilder5);
        literalArgumentBuilder.then(((LiteralArgumentBuilder)Commands.literal((String)"poi").requires(commandSourceStack -> MinimapCommand.access(commandSourceStack, "enorials.minimap.markers.use"))).then(Commands.literal((String)"add").then(Commands.argument((String)"name", (ArgumentType)StringArgumentType.string()).then(this.iconArgument().then((ArgumentBuilder)MinimapCommand.runFor(Commands.argument((String)"minutes", (ArgumentType)IntegerArgumentType.integer((int)1, (int)10080)), this::addTimedMarker))))));
        literalArgumentBuilder.then(((LiteralArgumentBuilder)Commands.literal((String)"resourcepack").requires(commandSourceStack -> MinimapCommand.access(commandSourceStack, "enorials.hud.admin"))).then(Commands.literal((String)"build").executes(this::buildResourcePack)));
        literalArgumentBuilder.then(this.fullscreenArgument());
        commandDispatcher.register(literalArgumentBuilder);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, EntitySelector> targetsExecute(MultiTargetCommand multiTargetCommand) {
        return (RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument((String)"targets", (ArgumentType)EntityArgument.players()).requires(commandSourceStack -> HudAccess.has(commandSourceStack.getBukkitSender(), "enorials.hud.admin"))).executes(commandContext -> multiTargetCommand.run((CommandContext<CommandSourceStack>)commandContext, EntityArgument.getPlayers((CommandContext)commandContext, (String)"targets")));
    }

    private static Collection<ServerPlayer> sourceAsSinglePlayer(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        return ImmutableList.of((Object)((CommandSourceStack)commandContext.getSource()).getPlayerOrException());
    }

    private static void sendSuccess(CommandSourceStack commandSourceStack, String string) {
        commandSourceStack.sendSuccess(() -> Component.literal((String)string), false);
    }

    private boolean save(Minimap minimap) {
        try {
            this.getPlugin().playerDataStorage().save(minimap);
            this.getPlugin().menuListener().restoreOwnedPublicMarkers(minimap);
            return true;
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
        }
    }

    private static boolean isSystemMarkerName(String string) {
        return "player".equals(string) || "death_point".equals(string) || "__coords__".equals(string) || string.startsWith("__radar::") || string.startsWith("__betonquest::") || EnomapMenuListener.isPublicLayerKey(string);
    }

    private int openMenu(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        CraftPlayer craftPlayer = ((CommandSourceStack)commandContext.getSource()).getPlayerOrException().getBukkitEntity();
        this.getPlugin().menuListener().openMainMenu((Player)craftPlayer);
        return 1;
    }

    private int buildResourcePack(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        this.getPlugin().owner().packs().requestBuildImmediate();
        MinimapCommand.sendSuccess((CommandSourceStack)commandContext.getSource(), "HUD resource pack build queued (includes the minimap).");
        return 1;
    }

    private int enable(CommandContext<CommandSourceStack> commandContext, Collection<ServerPlayer> collection) throws CommandSyntaxException {
        return this.setMapEnabled(commandContext, collection, true);
    }

    private int disable(CommandContext<CommandSourceStack> commandContext, Collection<ServerPlayer> collection) throws CommandSyntaxException {
        return this.setMapEnabled(commandContext, collection, false);
    }

    private int position(CommandContext<CommandSourceStack> commandContext, MinimapScreenPosition minimapScreenPosition) throws CommandSyntaxException {
        Minimap minimap = this.getPlugin().getPlayerMinimap((Player)((CommandSourceStack)commandContext.getSource()).getPlayerOrException().getBukkitEntity());
        if (minimap == null) {
            throw new SimpleCommandExceptionType((Message)Component.literal((String)"Minimap is disabled")).create();
        }
        minimap.screenPosition(minimapScreenPosition);
        minimap.update(this.getPlugin());
        if (this.save(minimap)) {
            return 1;
        }
        throw new SimpleCommandExceptionType((Message)Component.literal((String)"Unable to save player data, see console for error")).create();
    }

    private int size(CommandContext<CommandSourceStack> commandContext, MinimapSize minimapSize) throws CommandSyntaxException {
        ServerPlayer serverPlayer = ((CommandSourceStack)commandContext.getSource()).getPlayerOrException();
        CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
        Minimap minimap = this.getPlugin().getPlayerMinimap((Player)craftPlayer);
        if (minimap == null) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Minimap is disabled");
        }
        minimap.size(minimapSize);
        minimap.respawn(this.getPlugin());
        minimap.update(this.getPlugin());
        if (!this.save(minimap)) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Unable to save player data, see console for error");
        }
        return 1;
    }

    private int shape(CommandContext<CommandSourceStack> commandContext, MinimapShape minimapShape) throws CommandSyntaxException {
        ServerPlayer serverPlayer = ((CommandSourceStack)commandContext.getSource()).getPlayerOrException();
        CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
        Minimap minimap = this.getPlugin().getPlayerMinimap((Player)craftPlayer);
        if (minimap == null) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Minimap is disabled");
        }
        minimap.shape(minimapShape);
        minimap.respawn(this.getPlugin());
        minimap.update(this.getPlugin());
        if (!this.save(minimap)) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Unable to save player data, see console for error");
        }
        return 1;
    }

    private int setDangerRing(CommandContext<CommandSourceStack> commandContext, boolean bl) throws CommandSyntaxException {
        ServerPlayer serverPlayer = ((CommandSourceStack)commandContext.getSource()).getPlayerOrException();
        CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
        Minimap minimap = this.getPlugin().getPlayerMinimap((Player)craftPlayer);
        if (minimap == null) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Minimap is disabled");
        }
        minimap.dangerRingEnabled(bl);
        minimap.respawn(this.getPlugin());
        minimap.update(this.getPlugin());
        if (!this.save(minimap)) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Unable to save player data, see console for error");
        }
        return 1;
    }

    private int toggleDangerRing(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer serverPlayer = ((CommandSourceStack)commandContext.getSource()).getPlayerOrException();
        CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
        Minimap minimap = this.getPlugin().getPlayerMinimap((Player)craftPlayer);
        if (minimap == null) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Minimap is disabled");
        }
        minimap.dangerRingEnabled(!minimap.dangerRingEnabled());
        minimap.respawn(this.getPlugin());
        minimap.update(this.getPlugin());
        if (!this.save(minimap)) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Unable to save player data, see console for error");
        }
        return 1;
    }

    private int setFogStyling(CommandContext<CommandSourceStack> commandContext, boolean bl) throws CommandSyntaxException {
        ServerPlayer serverPlayer = ((CommandSourceStack)commandContext.getSource()).getPlayerOrException();
        CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
        Minimap minimap = this.getPlugin().getPlayerMinimap((Player)craftPlayer);
        if (minimap == null) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Minimap is disabled");
        }
        minimap.fogStylingEnabled(bl);
        minimap.respawn(this.getPlugin());
        minimap.update(this.getPlugin());
        if (!this.save(minimap)) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Unable to save player data, see console for error");
        }
        return 1;
    }

    private int toggleFogStyling(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer serverPlayer = ((CommandSourceStack)commandContext.getSource()).getPlayerOrException();
        CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
        Minimap minimap = this.getPlugin().getPlayerMinimap((Player)craftPlayer);
        if (minimap == null) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Minimap is disabled");
        }
        minimap.fogStylingEnabled(!minimap.fogStylingEnabled());
        minimap.respawn(this.getPlugin());
        minimap.update(this.getPlugin());
        if (!this.save(minimap)) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Unable to save player data, see console for error");
        }
        return 1;
    }

    private int savePreset(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer serverPlayer = ((CommandSourceStack)commandContext.getSource()).getPlayerOrException();
        CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
        int n = IntegerArgumentType.getInteger(commandContext, (String)"slot") - 1;
        Minimap minimap = this.getPlugin().getPlayerMinimap((Player)craftPlayer);
        if (minimap == null) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Minimap is disabled");
        }
        minimap.preset(n, MinimapPreset.capture(minimap));
        if (!this.save(minimap)) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Unable to save player data, see console for error");
        }
        return 1;
    }

    private int loadPreset(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer serverPlayer = ((CommandSourceStack)commandContext.getSource()).getPlayerOrException();
        CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
        int n = IntegerArgumentType.getInteger(commandContext, (String)"slot") - 1;
        Minimap minimap = this.getPlugin().getPlayerMinimap((Player)craftPlayer);
        if (minimap == null) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Minimap is disabled");
        }
        MinimapPreset minimapPreset = minimap.preset(n);
        if (minimapPreset == null) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Preset is empty");
        }
        minimapPreset.apply(minimap);
        minimap.respawn(this.getPlugin());
        minimap.update(this.getPlugin());
        if (!this.save(minimap)) {
            throw new CommandSyntaxException(new CommandExceptionType(){}, () -> "Unable to save player data, see console for error");
        }
        return 1;
    }

    private CompletableFuture<Suggestions> suggestIcons(CommandContext<CommandSourceStack> commandContext, SuggestionsBuilder suggestionsBuilder) {
        String string = suggestionsBuilder.getRemaining();
        for (String string2 : this.getPlugin().iconProvider().genericIconKeys()) {
            if (!string2.startsWith(string)) continue;
            suggestionsBuilder.suggest(string2);
        }
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestMarkers(CommandContext<CommandSourceStack> commandContext, SuggestionsBuilder suggestionsBuilder) {
        Minimap minimap;
        ServerPlayer serverPlayer = ((CommandSourceStack)commandContext.getSource()).getPlayer();
        Minimap minimap2 = minimap = serverPlayer == null ? null : this.getPlugin().getPlayerMinimap((Player)serverPlayer.getBukkitEntity());
        if (minimap != null) {
            for (String string : minimap.secondaryLayers().keySet()) {
                if (MinimapCommand.isSystemMarkerName(string) || !string.startsWith(suggestionsBuilder.getRemaining())) continue;
                suggestionsBuilder.suggest((String)(string.indexOf(32) < 0 ? string : "\"" + string + "\""));
            }
        }
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestMarkerCategories(CommandContext<CommandSourceStack> commandContext, SuggestionsBuilder suggestionsBuilder) {
        for (MarkerCategory markerCategory : MarkerCategory.values()) {
            String string = markerCategory.name().toLowerCase(Locale.ENGLISH);
            if (!string.startsWith(suggestionsBuilder.getRemaining().toLowerCase(Locale.ENGLISH))) continue;
            suggestionsBuilder.suggest(string);
        }
        return suggestionsBuilder.buildFuture();
    }

    private MinimapIcon minimapIcon(String string) throws CommandSyntaxException {
        MinimapIcon minimapIcon;
        MinimapIconProvider minimapIconProvider = this.getPlugin().iconProvider();
        MinimapIcon minimapIcon2 = minimapIcon = minimapIconProvider.specialIconKeys().contains(string) ? null : minimapIconProvider.getIcon(string);
        if (minimapIcon != null) {
            return minimapIcon;
        }
        throw new SimpleCommandExceptionType((Message)Component.literal((String)"Invalid icon.")).create();
    }

    private boolean modifyMarker(CommandSourceStack commandSourceStack, Player player, String string, BiConsumer<Minimap, SecondaryMinimapLayer> biConsumer) throws CommandSyntaxException {
        String string2;
        Minimap minimap = this.getPlugin().getPlayerMinimap(player);
        String string3 = minimap == null ? "Minimap is disabled" : (MinimapCommand.isSystemMarkerName(string) ? "This marker cannot be modified" : (string2 = !minimap.secondaryLayers().containsKey(string) ? "There is no such marker" : null));
        if (string2 != null) {
            return this.markerFailure(commandSourceStack, player, string2);
        }
        biConsumer.accept(minimap, minimap.secondaryLayers().get(string));
        return this.persistMarker(commandSourceStack, minimap);
    }

    private int changeMarkerIcon(CommandContext<CommandSourceStack> commandContext, Collection<ServerPlayer> collection) throws CommandSyntaxException {
        MinimapIcon minimapIcon = this.minimapIcon(StringArgumentType.getString(commandContext, (String)"icon"));
        String string = StringArgumentType.getString(commandContext, (String)"name");
        int n = 0;
        for (ServerPlayer serverPlayer : collection) {
            if (!this.modifyMarker((CommandSourceStack)commandContext.getSource(), (Player)serverPlayer.getBukkitEntity(), string, (minimap, secondaryMinimapLayer) -> secondaryMinimapLayer.setRenderer(new MinimapIconRenderer(minimapIcon)))) continue;
            ++n;
            MinimapCommand.sendSuccess((CommandSourceStack)commandContext.getSource(), serverPlayer.getScoreboardName() + ": Updated icon for '" + string + "'");
        }
        return n;
    }

    private boolean modifyCustomMarkerLayer(CommandSourceStack commandSourceStack, Player player, String string, BiConsumer<Minimap, MarkerMinimapLayer> biConsumer) throws CommandSyntaxException {
        Minimap minimap = this.getPlugin().getPlayerMinimap(player);
        if (minimap == null) {
            commandSourceStack.sendFailure((Component)Component.literal((String)(player.getName() + ": Minimap is disabled")));
            return false;
        }
        if (MinimapCommand.isSystemMarkerName(string)) {
            commandSourceStack.sendFailure((Component)Component.literal((String)(player.getName() + ": This marker cannot be modified")));
            return false;
        }
        SecondaryMinimapLayer secondaryMinimapLayer = minimap.secondaryLayers().get(string);
        if (secondaryMinimapLayer instanceof MarkerMinimapLayer) {
            MarkerMinimapLayer markerMinimapLayer = (MarkerMinimapLayer)secondaryMinimapLayer;
            biConsumer.accept(minimap, markerMinimapLayer);
            minimap.update(this.getPlugin());
            if (!this.save(minimap)) {
                commandSourceStack.sendFailure((Component)Component.literal((String)(player.getName() + ": Unable to save player data, see console for error")));
                return false;
            }
            return true;
        }
        commandSourceStack.sendFailure((Component)Component.literal((String)(player.getName() + ": There is no such custom marker")));
        return false;
    }

    private int setMarkerRange(CommandContext<CommandSourceStack> commandContext, Collection<ServerPlayer> collection) throws CommandSyntaxException {
        String string = StringArgumentType.getString(commandContext, (String)"name");
        int n = IntegerArgumentType.getInteger(commandContext, (String)"distance");
        int n2 = 0;
        for (ServerPlayer serverPlayer : collection) {
            CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
            Minimap minimap2 = this.getPlugin().getPlayerMinimap((Player)craftPlayer);
            if (n <= 0 && minimap2 != null && !MarkerLimits.canSetUnlimited(minimap2, string)) {
                ((CommandSourceStack)commandContext.getSource()).sendFailure((Component)Component.literal((String)MarkerLimits.message()));
                continue;
            }
            if (!this.modifyCustomMarkerLayer((CommandSourceStack)commandContext.getSource(), (Player)craftPlayer, string, (minimap, markerMinimapLayer) -> markerMinimapLayer.setMaxVisibleDistance(n))) continue;
            MinimapCommand.sendSuccess((CommandSourceStack)commandContext.getSource(), craftPlayer.getName() + ": Marker '" + string + "' range set to " + n + " blocks");
            ++n2;
        }
        return n2;
    }

    private int clearMarkerRange(CommandContext<CommandSourceStack> commandContext, Collection<ServerPlayer> collection) throws CommandSyntaxException {
        String string = StringArgumentType.getString(commandContext, (String)"name");
        int n = 0;
        for (ServerPlayer serverPlayer : collection) {
            CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
            Minimap minimap2 = this.getPlugin().getPlayerMinimap((Player)craftPlayer);
            if (minimap2 != null && !MarkerLimits.canSetUnlimited(minimap2, string)) {
                ((CommandSourceStack)commandContext.getSource()).sendFailure((Component)Component.literal((String)MarkerLimits.message()));
                continue;
            }
            if (!this.modifyCustomMarkerLayer((CommandSourceStack)commandContext.getSource(), (Player)craftPlayer, string, (minimap, markerMinimapLayer) -> markerMinimapLayer.setMaxVisibleDistance(null))) continue;
            MinimapCommand.sendSuccess((CommandSourceStack)commandContext.getSource(), craftPlayer.getName() + ": Marker '" + string + "' range cleared");
            ++n;
        }
        return n;
    }

    private int setMarkerCategory(CommandContext<CommandSourceStack> commandContext, Collection<ServerPlayer> collection) throws CommandSyntaxException {
        String string = StringArgumentType.getString(commandContext, (String)"name");
        String string2 = StringArgumentType.getString(commandContext, (String)"category");
        MarkerCategory markerCategory = MarkerCategory.fromString(string2);
        int n = 0;
        for (ServerPlayer serverPlayer : collection) {
            CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
            if (!this.modifyCustomMarkerLayer((CommandSourceStack)commandContext.getSource(), (Player)craftPlayer, string, (minimap, markerMinimapLayer) -> markerMinimapLayer.setCategory(markerCategory))) continue;
            this.getPlugin().menuListener().onMarkerCategoryChanged(craftPlayer.getUniqueId(), string, markerCategory);
            MinimapCommand.sendSuccess((CommandSourceStack)commandContext.getSource(), craftPlayer.getName() + ": Marker '" + string + "' category set to " + markerCategory.name());
            ++n;
        }
        return n;
    }

    private int renameMarker(CommandContext<CommandSourceStack> commandContext, Collection<ServerPlayer> collection) throws CommandSyntaxException {
        String string = StringArgumentType.getString(commandContext, (String)"name");
        String string2 = StringArgumentType.getString(commandContext, (String)"new_name");
        int n = 0;
        if (MinimapCommand.isSystemMarkerName(string2)) {
            ((CommandSourceStack)commandContext.getSource()).sendFailure((Component)Component.literal((String)"This marker name is unavailable"));
            return 0;
        }
        for (ServerPlayer serverPlayer : collection) {
            CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
            Minimap minimap2 = this.getPlugin().getPlayerMinimap((Player)craftPlayer);
            if (minimap2 != null && minimap2.secondaryLayers().containsKey(string2)) {
                ((CommandSourceStack)commandContext.getSource()).sendFailure((Component)Component.literal((String)(craftPlayer.getName() + ": Marker with this name already exists")));
                continue;
            }
            if (!this.modifyMarker((CommandSourceStack)commandContext.getSource(), (Player)craftPlayer, string, (minimap, secondaryMinimapLayer) -> {
                if (minimap.secondaryLayers().remove(string, secondaryMinimapLayer)) {
                    minimap.secondaryLayers().put(string2, (SecondaryMinimapLayer)secondaryMinimapLayer);
                }
            })) continue;
            this.getPlugin().menuListener().onMarkerRenamed(craftPlayer.getUniqueId(), string, string2);
            MinimapCommand.sendSuccess((CommandSourceStack)commandContext.getSource(), craftPlayer.getName() + ": Marker '" + string + "' renamed to '" + string2 + "'");
            ++n;
        }
        return n;
    }

    private int removeMarker(CommandContext<CommandSourceStack> commandContext, Collection<ServerPlayer> collection) throws CommandSyntaxException {
        String string = StringArgumentType.getString(commandContext, (String)"name");
        int n = 0;
        for (ServerPlayer serverPlayer : collection) {
            String string2;
            CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
            Minimap minimap = this.getPlugin().getPlayerMinimap((Player)craftPlayer);
            if (string.equals("death_point")) {
                try {
                    this.getPlugin().playerDataStorage().clearDeathLocation(craftPlayer.getUniqueId());
                    if (minimap != null) {
                        minimap.resetDeathPoint(this.getPlugin());
                    }
                    ++n;
                }
                catch (SQLException sQLException) {
                    ((CommandSourceStack)commandContext.getSource()).sendFailure((Component)Component.literal((String)"Could not remove death marker."));
                }
                continue;
            }
            String string3 = minimap == null ? "Minimap is disabled" : (MinimapCommand.isSystemMarkerName(string) ? "This marker cannot be removed" : (string2 = !minimap.secondaryLayers().containsKey(string) ? "There is no such marker" : null));
            if (string2 != null) {
                this.markerFailure((CommandSourceStack)commandContext.getSource(), (Player)craftPlayer, string2);
                continue;
            }
            SecondaryMinimapLayer secondaryMinimapLayer = (SecondaryMinimapLayer)minimap.secondaryLayers().remove(string);
            this.getPlugin().packetSender().despawnLayer((Player)craftPlayer, secondaryMinimapLayer.getBaseLayer());
            this.getPlugin().menuListener().onMarkerRemoved(craftPlayer.getUniqueId(), string);
            if (!this.persistMarker((CommandSourceStack)commandContext.getSource(), minimap)) continue;
            MinimapCommand.sendSuccess((CommandSourceStack)commandContext.getSource(), craftPlayer.getName() + ": Marker '" + string + "' removed");
            ++n;
        }
        return n;
    }

    private int addMarker(CommandContext<CommandSourceStack> commandContext, Collection<ServerPlayer> collection) throws CommandSyntaxException {
        return this.createMarker(commandContext, collection, null);
    }

    private int addMarkerAt(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        CraftPlayer craftPlayer = ((CommandSourceStack)commandContext.getSource()).getPlayerOrException().getBukkitEntity();
        Vec3 vec3 = Vec3Argument.getVec3(commandContext, (String)"coordinates");
        try {
            if (Math.abs(vec3.x) >= 3.0E7 || Math.abs(vec3.z) >= 3.0E7 || vec3.y < (double)craftPlayer.getWorld().getMinHeight() || vec3.y >= (double)craftPlayer.getWorld().getMaxHeight()) {
                throw new IllegalArgumentException("Coordinates are outside this world's bounds");
            }
            this.getPlugin().menuListener().markerWizard().create((Player)craftPlayer, StringArgumentType.getString(commandContext, (String)"name"), StringArgumentType.getString(commandContext, (String)"icon"), MarkerCategory.GENERAL, false, null, null, new Location(craftPlayer.getWorld(), vec3.x, vec3.y, vec3.z));
            MinimapCommand.sendSuccess((CommandSourceStack)commandContext.getSource(), "Marker created at the specified coordinates");
            return 1;
        }
        catch (Exception exception) {
            ((CommandSourceStack)commandContext.getSource()).sendFailure((Component)Component.literal((String)("Could not create marker: " + exception.getMessage())));
            return 0;
        }
    }

    private int addTimedMarker(CommandContext<CommandSourceStack> commandContext, Collection<ServerPlayer> collection) throws CommandSyntaxException {
        return this.createMarker(commandContext, collection, IntegerArgumentType.getInteger(commandContext, (String)"minutes"));
    }

    private int fullscreen(CommandContext<CommandSourceStack> commandContext, Collection<ServerPlayer> collection) throws CommandSyntaxException {
        int n = 0;
        for (ServerPlayer serverPlayer : collection) {
            CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
            Minimap minimap = this.getPlugin().getPlayerMinimap((Player)craftPlayer);
            if (minimap == null) {
                this.markerFailure((CommandSourceStack)commandContext.getSource(), (Player)craftPlayer, "Minimap is disabled");
                continue;
            }
            MinimapListener minimapListener = this.getPlugin().minimapListener();
            if (this.getPlugin().getFullscreenMinimap((Player)craftPlayer) != null) {
                minimapListener.closeFullscreen((Player)craftPlayer);
                continue;
            }
            minimapListener.openFullscreen(FullscreenMinimap.create(this.getPlugin(), minimap)).onSneak(arg_0 -> MinimapCommand.lambda$fullscreen$0(minimapListener, (Player)craftPlayer, arg_0));
            ++n;
        }
        return n;
    }

    private boolean markerFailure(CommandSourceStack commandSourceStack, Player player, String string) {
        commandSourceStack.sendFailure((Component)Component.literal((String)(player.getName() + ": " + string)));
        return false;
    }

    private boolean persistMarker(CommandSourceStack commandSourceStack, Minimap minimap) {
        minimap.update(this.getPlugin());
        return this.save(minimap) || this.markerFailure(commandSourceStack, minimap.holder(), "Unable to save player data, see console for error");
    }

    private int createMarker(CommandContext<CommandSourceStack> commandContext, Collection<ServerPlayer> collection, Integer n) throws CommandSyntaxException {
        String string = StringArgumentType.getString(commandContext, (String)"name");
        String string2 = StringArgumentType.getString(commandContext, (String)"icon");
        long l = System.currentTimeMillis();
        Long l2 = n == null ? null : Long.valueOf(l + (long)n.intValue() * 60000L);
        int n2 = 0;
        Config.Markers.CustomMarkers customMarkers = Config.instance().markers.customMarkers;
        for (ServerPlayer serverPlayer : collection) {
            Location location;
            CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
            Minimap minimap = this.getPlugin().getPlayerMinimap((Player)craftPlayer);
            if (minimap == null) {
                this.markerFailure((CommandSourceStack)commandContext.getSource(), (Player)craftPlayer, "Minimap is disabled");
                continue;
            }
            MinimapIcon minimapIcon = this.minimapIcon(string2);
            int n3 = 0;
            Object object = minimap.secondaryLayers().entrySet().iterator();
            while (object.hasNext()) {
                location = object.next();
                if (MinimapCommand.isSystemMarkerName(location.getKey()) || !(location.getValue() instanceof MarkerMinimapLayer)) continue;
                ++n3;
            }
            Object object2 = MinimapCommand.isSystemMarkerName(string) ? "This marker name is unavailable" : (minimap.secondaryLayers().containsKey(string) ? "Marker with this name already exists" : (object = n3 >= customMarkers.limit ? "You cannot place more than " + customMarkers.limit + " markers." : null));
            if (object != null) {
                this.markerFailure((CommandSourceStack)commandContext.getSource(), (Player)craftPlayer, (String)object);
                continue;
            }
            if (!MarkerLimits.canSetUnlimited(minimap, string)) {
                ((CommandSourceStack)commandContext.getSource()).sendFailure((Component)Component.literal((String)MarkerLimits.message()));
                continue;
            }
            location = craftPlayer.getLocation();
            MinimapLayer minimapLayer = this.getPlugin().clientsideMinimapFactory().createMinimapLayer(craftPlayer.getWorld(), null);
            MarkerMinimapLayer markerMinimapLayer = new MarkerMinimapLayer(minimapLayer, new MinimapIconRenderer(minimapIcon), true, customMarkers.stickToBorder, location.getWorld(), location.getBlockX(), location.getBlockZ(), 0.05f + (float)minimap.secondaryLayers().size() * 0.01f, null, MarkerCategory.GENERAL, l, l2);
            minimap.secondaryLayers().put(string, markerMinimapLayer);
            this.getPlugin().packetSender().spawnLayer((Player)craftPlayer, minimapLayer);
            if (!this.persistMarker((CommandSourceStack)commandContext.getSource(), minimap)) continue;
            MinimapCommand.sendSuccess((CommandSourceStack)commandContext.getSource(), craftPlayer.getName() + ": " + (n == null ? "Marker '" + string + "' added with icon '" + string2 + "'" : "Timed POI '" + string + "' added for " + n + " minute(s)"));
            ++n2;
        }
        return n2;
    }

    private static boolean access(CommandSourceStack commandSourceStack, String string) {
        return HudAccess.has(commandSourceStack.getBukkitSender(), string);
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> iconArgument() {
        return Commands.argument((String)"icon", (ArgumentType)StringArgumentType.string()).suggests(this::suggestIcons);
    }

    private LiteralArgumentBuilder<CommandSourceStack> fullscreenArgument() {
        return MinimapCommand.runFor((LiteralArgumentBuilder)Commands.literal((String)"fullscreen").requires(commandSourceStack -> MinimapCommand.access(commandSourceStack, "enorials.minimap.fullscreen")), this::fullscreen);
    }

    private static <B extends ArgumentBuilder<CommandSourceStack, B>> B runFor(B b, MultiTargetCommand multiTargetCommand) {
        b.executes(commandContext -> multiTargetCommand.run((CommandContext<CommandSourceStack>)commandContext, MinimapCommand.sourceAsSinglePlayer((CommandContext<CommandSourceStack>)commandContext)));
        b.then(MinimapCommand.targetsExecute(multiTargetCommand));
        return b;
    }

    private int setMapEnabled(CommandContext<CommandSourceStack> commandContext, Collection<ServerPlayer> collection, boolean bl) {
        int n = 0;
        MinimapPlayerDatabase minimapPlayerDatabase = this.getPlugin().playerDataStorage();
        for (ServerPlayer serverPlayer : collection) {
            CraftPlayer craftPlayer = serverPlayer.getBukkitEntity();
            try {
                if (bl) {
                    minimapPlayerDatabase.enableMinimap((Player)craftPlayer);
                    minimapPlayerDatabase.restore(this.getPlugin(), (Player)craftPlayer);
                } else {
                    this.getPlugin().minimapListener().disableMinimap((Player)craftPlayer);
                    minimapPlayerDatabase.disableMinimap((Player)craftPlayer);
                }
                ++n;
            }
            catch (Exception exception) {
                this.getPlugin().getLogger().log(Level.WARNING, "Could not update minimap for " + String.valueOf(craftPlayer.getUniqueId()), exception);
                this.markerFailure((CommandSourceStack)commandContext.getSource(), (Player)craftPlayer, "Unable to update saved settings; see console");
            }
        }
        return n;
    }

    private static /* synthetic */ void lambda$fullscreen$0(MinimapListener minimapListener, Player player, Void void_) {
        minimapListener.closeFullscreen(player);
    }

    @FunctionalInterface
    private static interface MultiTargetCommand {
        public int run(CommandContext<CommandSourceStack> var1, Collection<ServerPlayer> var2) throws CommandSyntaxException;
    }
}

