/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.channel.Channel
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.ChannelOutboundHandlerAdapter
 *  io.netty.channel.ChannelPromise
 *  io.papermc.paper.adventure.PaperAdventure
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextColor
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientboundContainerClosePacket
 *  net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
 *  net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
 *  net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.item.ItemStack
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.command.CommandSender
 *  org.bukkit.craftbukkit.entity.CraftPlayer
 *  org.bukkit.craftbukkit.inventory.CraftItemStack
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.profile.PlayerProfile
 *  org.bukkit.scheduler.BukkitTask
 */
package sk.enorials.hud.menu;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.adventure.PaperAdventure;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitTask;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.access.HudAccess;
import sk.enorials.hud.compat.ClientProfiles;
import sk.enorials.hud.compat.LegacyText;
import sk.enorials.hud.compat.MenuHead;
import sk.enorials.hud.compat.MenuPackets;
import sk.enorials.hud.compat.TextCompat;
import sk.enorials.hud.menu.HudMenu;
import sk.enorials.hud.menu.MenuGeometry;
import sk.enorials.hud.menu.MenuIcons;
import sk.enorials.hud.theme.ThemeStyle;
import sk.enorials.hud.util.HudFontMetrics;
import sk.enorials.hud.util.PackHostingSetup;
import sk.enorials.hud.util.SpaceUtil;

public final class GraphicalHudMenu
implements Listener,
AutoCloseable {
    private static final String HANDLER = "enorials_menu_inventory";
    private final EnorialsHUD plugin;
    private final BukkitTask refreshTask;
    private final Map<UUID, View> views = new ConcurrentHashMap<UUID, View>();
    private final Map<UUID, Channel> channels = new ConcurrentHashMap<UUID, Channel>();
    private final Set<UUID> switching = ConcurrentHashMap.newKeySet();

    public GraphicalHudMenu(EnorialsHUD enorialsHUD) {
        this.plugin = enorialsHUD;
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)enorialsHUD);
        this.refreshTask = Bukkit.getScheduler().runTaskTimer((Plugin)enorialsHUD, () -> {
            for (Map.Entry<UUID, View> entry : List.copyOf(this.views.entrySet())) {
                View view = entry.getValue();
                Player player = Bukkit.getPlayer((UUID)entry.getKey());
                if (view.refresh == null || player == null || player.getOpenInventory().getTopInventory() != view.inventory) continue;
                view.refresh.run();
            }
        }, 20L, 20L);
    }

    public void open(Player player, String string, List<Button> list, Runnable runnable) {
        this.open(player, string, list, runnable, "Nothing here yet", null);
    }

    public void open(Player player, String string, List<Button> list, Runnable runnable, String string2, Runnable runnable2) {
        int n;
        View view = this.views.get(player.getUniqueId());
        int n2 = n = view != null && view.title.equals(string) ? Math.min(view.page, Math.max(0, (list.size() - 1) / 6)) : 0;
        if (runnable2 != null && view != null && view.refresh != null && view.title.equals(string) && view.empty.equals(string2) && view.page == n && GraphicalHudMenu.sameButtons(view.buttons, list)) {
            view.buttons = List.copyOf(list);
            view.back = runnable;
            view.refresh = runnable2;
            return;
        }
        this.open(player, string, list, runnable, n, string2, runnable2);
    }

    private static boolean sameButtons(List<Button> list, List<Button> list2) {
        if (list.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list.size(); ++i) {
            if (list.get(i).icon().equals(list2.get(i).icon()) && list.get(i).label().equals(list2.get(i).label()) && list.get(i).kind() == list2.get(i).kind()) continue;
            return false;
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void open(Player player, String string, List<Button> list, Runnable runnable, int n, String string2, Runnable runnable2) {
        if (!PackHostingSetup.ready(player)) {
            PackHostingSetup.explain((CommandSender)player);
            return;
        }
        if (!HudAccess.check((CommandSender)player, "enorials.menu.use")) {
            return;
        }
        this.install(player);
        View view = this.views.get(player.getUniqueId());
        if (view != null && player.getOpenInventory().getTopInventory() == view.inventory) {
            if (view.rows() == 6) {
                view.title = string;
                view.buttons = List.copyOf(list);
                view.back = runnable;
                view.page = n;
                view.empty = string2;
                view.refresh = runnable2;
                this.prepareHeads(player, view);
                ServerPlayer serverPlayer = ((CraftPlayer)player).getHandle();
                serverPlayer.connection.send((Packet)new ClientboundOpenScreenPacket(serverPlayer.containerMenu.containerId, serverPlayer.containerMenu.getType(), PaperAdventure.asVanilla((Component)this.themedTitle(player, view))));
                player.updateInventory();
                return;
            }
        } else {
            MenuPackets.closeDialog(player);
        }
        this.switching.add(player.getUniqueId());
        View view2 = new View(string, list, runnable, n);
        view2.empty = string2;
        view2.refresh = runnable2;
        view2.inventory = Bukkit.createInventory((InventoryHolder)view2, (int)(view2.rows() * 9), (Component)this.themedTitle(player, view2));
        this.prepareHeads(player, view2);
        this.views.put(player.getUniqueId(), view2);
        try {
            player.openInventory(view2.inventory);
        }
        finally {
            this.channels.get(player.getUniqueId()).eventLoop().execute(() -> this.switching.remove(player.getUniqueId()));
        }
        if (player.getOpenInventory().getTopInventory() != view2.inventory) {
            this.views.remove(player.getUniqueId(), view2);
            player.updateInventory();
        }
    }

    private void install(Player player) {
        Channel channel;
        final UUID uUID = player.getUniqueId();
        if (this.channels.putIfAbsent(uUID, channel = ((CraftPlayer)player).getHandle().connection.connection.channel) != null) {
            return;
        }
        channel.pipeline().addBefore("packet_handler", HANDLER, (ChannelHandler)new ChannelOutboundHandlerAdapter(this){
            final /* synthetic */ GraphicalHudMenu this$0;
            {
                this.this$0 = graphicalHudMenu;
            }

            public void write(ChannelHandlerContext channelHandlerContext, Object object, ChannelPromise channelPromise) throws Exception {
                if (object instanceof ClientboundContainerClosePacket && this.this$0.switching.contains(uUID)) {
                    channelPromise.setSuccess();
                    return;
                }
                View view = this.this$0.views.get(uUID);
                if (view != null) {
                    ClientboundContainerSetSlotPacket clientboundContainerSetSlotPacket;
                    ClientboundContainerSetContentPacket clientboundContainerSetContentPacket;
                    if (object instanceof ClientboundOpenScreenPacket) {
                        ClientboundOpenScreenPacket clientboundOpenScreenPacket = (ClientboundOpenScreenPacket)object;
                        view.container = clientboundOpenScreenPacket.getContainerId();
                    } else if (object instanceof ClientboundContainerSetContentPacket && (MenuPackets.id(clientboundContainerSetContentPacket = (ClientboundContainerSetContentPacket)object) == view.container || MenuPackets.id(clientboundContainerSetContentPacket) == 0)) {
                        int n2;
                        ArrayList<net.minecraft.world.item.ItemStack> arrayList = new ArrayList<net.minecraft.world.item.ItemStack>(MenuPackets.items(clientboundContainerSetContentPacket));
                        int n3 = n2 = MenuPackets.id(clientboundContainerSetContentPacket) == 0 ? 0 : view.inventory.getSize();
                        while (n2 < arrayList.size()) {
                            arrayList.set(n2, net.minecraft.world.item.ItemStack.EMPTY);
                            ++n2;
                        }
                        if (MenuPackets.id(clientboundContainerSetContentPacket) == view.container) {
                            view.heads.forEach((n, itemStack) -> arrayList.set((int)n, (net.minecraft.world.item.ItemStack)itemStack));
                        }
                        object = MenuPackets.content(MenuPackets.id(clientboundContainerSetContentPacket), MenuPackets.state(clientboundContainerSetContentPacket), arrayList);
                    } else if (object instanceof ClientboundContainerSetSlotPacket && ((clientboundContainerSetSlotPacket = (ClientboundContainerSetSlotPacket)object).getContainerId() == view.container && (clientboundContainerSetSlotPacket.getSlot() >= view.inventory.getSize() || view.heads.containsKey(clientboundContainerSetSlotPacket.getSlot())) || clientboundContainerSetSlotPacket.getContainerId() == 0)) {
                        object = new ClientboundContainerSetSlotPacket(clientboundContainerSetSlotPacket.getContainerId(), clientboundContainerSetSlotPacket.getStateId(), clientboundContainerSetSlotPacket.getSlot(), clientboundContainerSetSlotPacket.getContainerId() == view.container ? view.heads.getOrDefault(clientboundContainerSetSlotPacket.getSlot(), net.minecraft.world.item.ItemStack.EMPTY) : net.minecraft.world.item.ItemStack.EMPTY);
                    }
                }
                super.write(channelHandlerContext, object, channelPromise);
            }
        });
    }

    public boolean isOpen(Player player) {
        return this.views.containsKey(player.getUniqueId());
    }

    private void prepareHeads(Player player, View view) {
        HashMap<Integer, net.minecraft.world.item.ItemStack> hashMap = new HashMap<Integer, net.minecraft.world.item.ItemStack>();
        if (!view.root()) {
            for (MenuGeometry.Card card : view.cards()) {
                String string = view.buttons.get(view.page * 6 + card.index()).icon();
                if (!string.startsWith("head:")) continue;
                UUID uUID = UUID.fromString(string.substring(5));
                ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta)itemStack.getItemMeta();
                Player player2 = Bukkit.getPlayer((UUID)uUID);
                skullMeta.setOwnerProfile((PlayerProfile)(player2 == null ? Bukkit.getOfflinePlayer((UUID)uUID).getPlayerProfile() : player2.getPlayerProfile()));
                MenuHead.apply((ItemMeta)skullMeta, card.headModel(), ClientProfiles.of(player));
                itemStack.setItemMeta((ItemMeta)skullMeta);
                hashMap.put(card.headSlot(), CraftItemStack.asNMSCopy((ItemStack)itemStack));
            }
        }
        view.heads = Map.copyOf(hashMap);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void click(InventoryClickEvent inventoryClickEvent) {
        Object object;
        Player player;
        HumanEntity humanEntity;
        block25: {
            block24: {
                humanEntity = inventoryClickEvent.getWhoClicked();
                if (!(humanEntity instanceof Player)) break block24;
                player = (Player)humanEntity;
                humanEntity = inventoryClickEvent.getView().getTopInventory().getHolder();
                if (humanEntity instanceof View) break block25;
            }
            return;
        }
        View view = (View)humanEntity;
        inventoryClickEvent.setCancelled(true);
        int n = inventoryClickEvent.getRawSlot();
        if (!inventoryClickEvent.isLeftClick() && !inventoryClickEvent.isRightClick()) {
            return;
        }
        Runnable runnable = null;
        int n2 = view.inventory.getSize();
        int n3 = -1;
        if (view.root()) {
            n3 = MenuGeometry.rootButton(n);
            object = MenuGeometry.slotCenter(n, 6);
            if (object != null) {
                for (int i = 4; i < view.buttons.size(); ++i) {
                    if (!MenuGeometry.rootExtra(i, view.quests(), view.admin()).contains((int)object[0], (int)object[1])) continue;
                    n3 = i;
                    break;
                }
            }
        } else if (n >= 0 && n < n2 + 27 && (object = MenuGeometry.slotCenter(n, view.rows())) != null) {
            for (MenuGeometry.Card card : view.cards()) {
                if (!card.contains((int)object[0], (int)object[1])) continue;
                n3 = view.page * 6 + card.index();
                break;
            }
        }
        if (n3 >= 0 && n3 < view.buttons.size()) {
            object = view.buttons.get(n3);
            Runnable runnable2 = inventoryClickEvent.isShiftClick() && object.alternate() != null ? object.alternate() : object.action();
            runnable = runnable2 == null ? null : () -> GraphicalHudMenu.lambda$click$0(player, view, (Button)object, runnable2);
        } else if (!view.root() && MenuGeometry.navigation(n) >= 0) {
            int n4 = MenuGeometry.navigation(n);
            if (n4 == 0) {
                runnable = view.back;
            } else if (n4 == 3 && view.page > 0) {
                runnable = () -> this.open(player, view.title, view.buttons, view.back, view.page - 1, view.empty, view.refresh);
            } else if (n4 == 1 && view.buttons.size() > (view.page + 1) * 6) {
                runnable = () -> this.open(player, view.title, view.buttons, view.back, view.page + 1, view.empty, view.refresh);
            } else if (n4 == 2) {
                runnable = () -> ((Player)player).closeInventory();
            }
        }
        if (runnable != null) {
            Runnable runnable3 = runnable;
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (player.isOnline() && player.getOpenInventory().getTopInventory() == view.inventory) {
                    try {
                        runnable3.run();
                    }
                    catch (IllegalArgumentException | IllegalStateException runtimeException) {
                        player.sendMessage(Component.text((String)Objects.toString(runtimeException.getMessage(), "This action is currently unavailable.")).color(TextColor.color((int)0xFF7070)));
                    }
                }
            });
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void drag(InventoryDragEvent inventoryDragEvent) {
        if (inventoryDragEvent.getView().getTopInventory().getHolder() instanceof View) {
            inventoryDragEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent inventoryCloseEvent) {
        InventoryHolder inventoryHolder = inventoryCloseEvent.getInventory().getHolder();
        if (inventoryHolder instanceof View) {
            View view = (View)inventoryHolder;
            inventoryHolder = inventoryCloseEvent.getPlayer();
            if (inventoryHolder instanceof Player) {
                Player player = (Player)inventoryHolder;
                this.views.remove(player.getUniqueId(), view);
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                    if (player.isOnline()) {
                        player.updateInventory();
                    }
                });
            }
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent playerQuitEvent) {
        UUID uUID = playerQuitEvent.getPlayer().getUniqueId();
        this.views.remove(uUID);
        this.removeChannel(uUID);
    }

    private void removeChannel(UUID uUID) {
        Channel channel = this.channels.remove(uUID);
        if (channel != null) {
            channel.eventLoop().execute(() -> {
                if (channel.pipeline().get(HANDLER) != null) {
                    channel.pipeline().remove(HANDLER);
                }
            });
        }
    }

    @Override
    public void close() {
        this.refreshTask.cancel();
        for (UUID uUID : List.copyOf(this.views.keySet())) {
            Player player = Bukkit.getPlayer((UUID)uUID);
            if (player == null) continue;
            this.views.remove(uUID);
            player.closeInventory();
            player.updateInventory();
        }
        for (UUID uUID : List.copyOf(this.channels.keySet())) {
            this.removeChannel(uUID);
        }
        HandlerList.unregisterAll((Listener)this);
    }

    private Component themedTitle(Player player, View view) {
        return ThemeStyle.paint(this.title(view), this.plugin.storage().theme(player.getUniqueId()));
    }

    private Component title(View view) {
        if (view.root()) {
            Component component = this.tiles("root40_" + (view.quests() ? 1 : 0) + "_" + (view.admin() ? 1 : 0), -26, 4, 4);
            for (int i = 4; i < view.buttons.size(); ++i) {
                MenuGeometry.Card card = MenuGeometry.rootExtra(i, view.quests(), view.admin());
                Button button = view.buttons.get(i);
                int n = 18;
                int n2 = card.y() + (card.height() - n) / 2;
                Component component2 = GraphicalHudMenu.letters(button.label().toUpperCase(Locale.ROOT), Key.key((String)("enorials:menu/ref/native6_" + (card.y() + 12))));
                HudFontMetrics.Bounds bounds = this.plugin.packs().registry().metrics().measure(component2);
                float f = bounds.right() - bounds.left();
                int n3 = Math.round((float)card.centerX() - ((float)(n + 5) + f) / 2.0f);
                component = component.append(this.icon(button.icon(), n3 + n / 2, n2 + 2, n)).append(this.at(component2, Math.round((float)(n3 + n + 5) - bounds.left()) - 8));
            }
            return TextCompat.shadow(component.color(TextColor.color((int)0xFFFFFF)), 0);
        }
        String string = view.title.toUpperCase(Locale.ROOT) + (String)(view.buttons.size() > 6 ? " " + (view.page + 1) + "/" + (view.buttons.size() + 5) / 6 : "");
        Component component = this.tiles("shell", 0, 4, 4).append(this.words(string, 88, 5, 9, 118, 1));
        int n = 6;
        HudFontMetrics hudFontMetrics = this.plugin.packs().registry().metrics();
        for (MenuGeometry.Card card : view.cards()) {
            for (String string2 : view.buttons.get(view.page * 6 + card.index()).label().split("\n")) {
                if (!(hudFontMetrics.measure(GraphicalHudMenu.letters(string2, Key.key((String)"enorials:menu/ref/native6_82"))).advance() > (float)(card.width() - 10))) continue;
                n = 5;
            }
        }
        for (MenuGeometry.Card card : view.cards()) {
            Button button = view.buttons.get(view.page * 6 + card.index());
            int n4 = card.centerX();
            component = component.append(this.tiles("card_" + card.width() + "_" + card.height() + "_y" + card.y(), card.x(), (card.width() + 56) / 57, (card.height() + 56) / 57));
            if (!button.icon().startsWith("head:")) {
                component = component.append(this.icon(MenuIcons.resolve(view.title, button.icon(), button.label()), n4, card.iconY(), card.iconSize()));
            }
            component = component.append(this.cardLabel(button.label(), card, n));
        }
        if (view.buttons.isEmpty()) {
            component = component.append(this.words(view.empty, 88, 101, 6, 140, 2));
        }
        if (view.back != null) {
            component = component.append(this.navPlate(18)).append(this.navigation("back", "Back", 18, 40, 198));
        }
        if (view.buttons.size() > 6) {
            component = component.append(this.navPlate(68)).append(this.pageArrow("back", 78, view.page > 0)).append(this.words("|", 88, 202, 6, 4, 1).color(TextColor.color((int)6911095))).append(this.pageArrow("next", 98, view.buttons.size() > (view.page + 1) * 6));
        }
        component = component.append(this.navPlate(118)).append(this.navigation("close", "Close", 118, 40, 198));
        return TextCompat.shadow(component.color(TextColor.color((int)16645884)), 0);
    }

    private Component tiles(String string, int n, int n2, int n3) {
        TextComponent textComponent = Component.empty();
        for (int i = 0; i < n3; ++i) {
            for (int j = 0; j < n2; ++j) {
                textComponent = textComponent.append(this.at(LegacyText.font((Component)Component.text((char)((char)(60416 + i * 4 + j))), Key.key((String)("enorials:menu/ref/" + string + "_" + i))), n + j * 57 - 8));
            }
        }
        return textComponent;
    }

    private Component navPlate(int n) {
        return this.at(LegacyText.font((Component)Component.text((char)'\ued00'), Key.key((String)"enorials:menu/ref/nav")), n - 8);
    }

    private Component pageArrow(String string, int n, boolean bl) {
        return this.centeredIcon(LegacyText.font((Component)Component.text((char)'\ued00'), Key.key((String)("enorials:menu/ref/nav_" + string))), n).color(TextColor.color((int)(bl ? 16645884 : 5857638)));
    }

    private Component at(Component component, int n) {
        return SpaceUtil.space(n).append(this.plugin.packs().registry().metrics().neutral(component)).append(SpaceUtil.space(-n));
    }

    private Component icon(String string, int n, int n2, int n3) {
        if (string.startsWith("theme:")) {
            int n4 = List.of("original", "frostbound", "wildwood", "inferno", "astral", "corsair", "themes").indexOf(string.substring(6));
            return this.centeredIcon(LegacyText.font((Component)Component.text((char)((char)(60992 + Math.max(0, n4)))), Key.key((String)("enorials:menu/themes_" + n3 + "_" + n2))), n);
        }
        if (string.startsWith("atlas:")) {
            int n5 = List.of("customization", "quests", "admin", "player", "party", "minimap", "navigation", "accepted", "search", "permissions", "inventory", "teleport", "grant", "deny", "reset", "quest_settings").indexOf(string.substring(6));
            return this.centeredIcon(LegacyText.font((Component)Component.text((char)((char)(60928 + Math.max(0, n5)))), Key.key((String)("enorials:menu/ref/atlas41_" + n3 + "_" + n2))), n);
        }
        if (string.startsWith("ui:")) {
            return this.centeredIcon(LegacyText.font((Component)Component.text((char)'\ued00'), Key.key((String)("enorials:menu/ref/symbol/" + string.substring(3) + "_" + n3 + "_" + n2))), n);
        }
        if (Set.of("party", "map", "hud", "gps", "party_hud").contains(string)) {
            return this.at(LegacyText.font((Component)Component.text((char)'\ued00'), Key.key((String)("enorials:menu/ref/category_" + string + "_" + n3 + "_" + n2))), n - n3 / 2 - 8);
        }
        if (string.startsWith("marker:")) {
            String string2 = string.substring(7).replace("quest_!", "quest_exclamation");
            Key key = Key.key((String)("enorials:menu/ref/marker/" + string2 + "_" + n3 + "_" + n2));
            Component component = LegacyText.font((Component)Component.text((char)'\uea40'), key);
            HudFontMetrics.Bounds bounds = this.plugin.packs().registry().metrics().measure(component);
            return this.at(component, n - 8 - Math.round(bounds.center()));
        }
        return this.centeredIcon(LegacyText.font((Component)Component.text((char)((char)(59904 + HudMenu.iconIndex(string)))), Key.key((String)("enorials:menu/ref/icon" + n3 + "_" + n2))), n);
    }

    private Component centeredIcon(Component component, int n) {
        return this.at(component, n - 8 - Math.round(this.plugin.packs().registry().metrics().measure(component).center()));
    }

    private Component navigation(String string, String string2, int n, int n2, int n3) {
        HudFontMetrics hudFontMetrics = this.plugin.packs().registry().metrics();
        Component component = LegacyText.font((Component)Component.text((char)'\ued00'), Key.key((String)("enorials:menu/ref/nav_" + string)));
        Component component2 = GraphicalHudMenu.letters(string2, Key.key((String)("enorials:menu/ref/native6_" + (n3 + 4))));
        HudFontMetrics.Bounds bounds = hudFontMetrics.measure(component);
        HudFontMetrics.Bounds bounds2 = hudFontMetrics.measure(component2);
        float f = bounds.right() - bounds.left();
        float f2 = bounds2.right() - bounds2.left();
        float f3 = (float)n + ((float)n2 - f - 3.0f - f2) / 2.0f;
        return this.at(component, Math.round(f3 - bounds.left()) - 8).append(this.at(component2, Math.round(f3 + f + 3.0f - bounds2.left()) - 8));
    }

    private static Component letters(String string, Key key) {
        return LegacyText.font((Component)Component.text((String)string), key);
    }

    private Component cardLabel(String string, MenuGeometry.Card card, int n) {
        HudFontMetrics hudFontMetrics = this.plugin.packs().registry().metrics();
        int n2 = card.width() - 10;
        String[] stringArray = string.split("\n", -1);
        String string2 = stringArray.length > 1 ? stringArray[stringArray.length - 1].strip() : null;
        String string3 = string2 == null ? string : String.join((CharSequence)" ", Arrays.copyOf(stringArray, stringArray.length - 1));
        int n3 = n;
        if (string2 != null && hudFontMetrics.measure(GraphicalHudMenu.letters(string3, Key.key((String)("enorials:menu/ref/native" + n3 + "_82")))).advance() > (float)n2) {
            n3 = 5;
        }
        Key key = Key.key((String)("enorials:menu/ref/native" + n3 + "_82"));
        List<String> list = this.wrapLabel(string3, key, n2, string2 == null ? 2 : 1);
        if (string2 != null) {
            list.add(this.wrapLabel(string2, key, n2, 1).get(0));
        }
        int n4 = n3 + 2;
        int n5 = n3 + (list.size() - 1) * n4;
        int n6 = Math.min(card.labelY() - 3, card.y() + card.height() - 6 - n5);
        TextComponent textComponent = Component.empty();
        for (int i = 0; i < list.size(); ++i) {
            int n7;
            String string4 = list.get(i);
            switch (string4.toLowerCase(Locale.ROOT)) {
                case "on": {
                    int n8 = 5627769;
                    break;
                }
                case "off": {
                    int n8 = 0xFF6666;
                    break;
                }
                case "selected": {
                    int n8 = 0x40DDD4;
                    break;
                }
                default: {
                    int n8 = n7 = 16645884;
                }
            }
            if (n7 != 16645884) {
                string4 = string4.toUpperCase(Locale.ROOT);
            }
            String string5 = GraphicalHudMenu.letters(string4, Key.key((String)("enorials:menu/ref/native" + n3 + "_" + (n6 + i * n4)))).color(TextColor.color((int)n7));
            HudFontMetrics.Bounds bounds = hudFontMetrics.measure((Component)string5);
            textComponent = textComponent.append(this.at((Component)string5, card.centerX() - 8 - Math.round(bounds.center())));
        }
        return textComponent;
    }

    private List<String> wrapLabel(String string, Key key, int n, int n2) {
        boolean bl;
        HudFontMetrics hudFontMetrics = this.plugin.packs().registry().metrics();
        ArrayList<String> arrayList = new ArrayList<String>();
        String string2 = "";
        String[] stringArray = string.split(" +");
        int n3 = stringArray.length;
        for (int i = 0; i < n3; ++i) {
            String string3;
            String string4 = stringArray[i];
            String string5 = string3 = string2.isEmpty() ? string4 : string2 + " " + string4;
            if (!string2.isEmpty() && hudFontMetrics.measure(GraphicalHudMenu.letters(string3, key)).advance() > (float)n) {
                arrayList.add(string2);
                string2 = string4;
                continue;
            }
            string2 = string3;
        }
        arrayList.add(string2);
        boolean bl2 = bl = arrayList.size() > n2;
        if (bl) {
            arrayList = new ArrayList(arrayList.subList(0, n2));
        }
        for (n3 = 0; n3 < arrayList.size(); ++n3) {
            boolean bl3;
            Object object = (String)arrayList.get(n3);
            boolean bl4 = bl3 = hudFontMetrics.measure(GraphicalHudMenu.letters((String)object, key)).advance() > (float)n || bl && n3 == n2 - 1;
            if (bl3) {
                while (!((String)object).isEmpty() && hudFontMetrics.measure(GraphicalHudMenu.letters((String)object + "..", key)).advance() > (float)n) {
                    object = ((String)object).substring(0, ((String)object).length() - 1);
                }
                object = (String)object + "..";
            }
            arrayList.set(n3, (String)object);
        }
        return arrayList;
    }

    private Component words(String string, int n, int n2, int n3, int n4, int n5) {
        return this.words(string, n, n2, n3, n4, n5, false);
    }

    /*
     * WARNING - void declaration
     */
    private Component words(String string, int n, int n2, int n3, int n4, int n5, boolean bl) {
        Object object;
        HudFontMetrics hudFontMetrics = this.plugin.packs().registry().metrics();
        int n6 = n3 == 9 ? 7 : n3;
        Key key = Key.key((String)("enorials:menu/ref/native" + n6 + (n6 == 7 ? "_5" : "_82")));
        ArrayList<Object> arrayList = new ArrayList<Object>();
        if (n3 != 9) {
            for (String string2 : string.split("\n")) {
                if (!(hudFontMetrics.measure(GraphicalHudMenu.letters(string2, key)).advance() > (float)n4)) continue;
                n6 = 5;
                break;
            }
        }
        key = Key.key((String)("enorials:menu/ref/native" + n6 + (n6 == 7 ? "_5" : "_82")));
        for (String string2 : string.split("\n", -1)) {
            object = "";
            for (String string3 : string2.split(" ")) {
                void object2;
                String string4;
                String string5 = string4 = ((String)object).isEmpty() ? string3 : (String)object + " " + string3;
                if (hudFontMetrics.measure(GraphicalHudMenu.letters(string4, key)).advance() <= (float)n4) {
                    object = string4;
                    continue;
                }
                if (!((String)object).isEmpty()) {
                    arrayList.add(object);
                    object = "";
                }
                if (hudFontMetrics.measure(GraphicalHudMenu.letters(string3, key)).advance() > (float)n4) {
                    void var20_23;
                    while (!var20_23.isEmpty() && hudFontMetrics.measure(GraphicalHudMenu.letters((String)var20_23 + "..", key)).advance() > (float)n4) {
                        String string6 = var20_23.substring(0, var20_23.length() - 1);
                    }
                    String string7 = (String)var20_23 + "..";
                }
                object = object2;
            }
            arrayList.add(((String)object).strip());
        }
        if (bl) {
            n2 -= (Math.min(n5, arrayList.size()) * 8 - 2) / 2;
        }
        TextComponent textComponent = Component.empty();
        for (int i = 0; i < Math.min(n5, arrayList.size()); ++i) {
            String string2;
            Object object3 = ((String)arrayList.get(i)).strip();
            if (i == n5 - 1 && arrayList.size() > n5) {
                while (!((String)object3).isEmpty() && hudFontMetrics.measure(GraphicalHudMenu.letters((String)object3 + "..", key)).advance() > (float)n4) {
                    object3 = ((String)object3).substring(0, ((String)object3).length() - 1);
                }
                object3 = (String)object3 + "..";
            }
            string2 = GraphicalHudMenu.letters((String)object3, Key.key((String)("enorials:menu/ref/native" + n6 + "_" + (n2 + i * 8))));
            object = hudFontMetrics.measure((Component)string2);
            textComponent = textComponent.append(this.at((Component)string2, n - 8 - Math.round(((HudFontMetrics.Bounds)object).center())));
        }
        return textComponent;
    }

    private static /* synthetic */ void lambda$click$0(Player player, View view, Button button, Runnable runnable) {
        if (HudAccess.menu(player, view.title, button.label())) {
            runnable.run();
        }
    }

    private static final class View
    implements InventoryHolder {
        Inventory inventory;
        volatile int container = -1;
        List<Button> buttons;
        Runnable back;
        Runnable refresh;
        String title;
        String empty = "Nothing here yet";
        int page;
        volatile Map<Integer, net.minecraft.world.item.ItemStack> heads = Map.of();

        View(String string, List<Button> list, Runnable runnable, int n) {
            this.title = string;
            this.buttons = List.copyOf(list);
            this.back = runnable;
            this.page = n;
        }

        boolean root() {
            return this.title.equals("ENORIALS");
        }

        int rows() {
            return 6;
        }

        boolean quests() {
            return this.root() && this.buttons.stream().anyMatch(button -> button.label().equals("Quests"));
        }

        boolean admin() {
            return this.root() && this.buttons.stream().anyMatch(button -> button.label().equals("Admin"));
        }

        int visible() {
            return Math.min(6, this.buttons.size() - this.page * 6);
        }

        List<MenuGeometry.Card> cards() {
            return MenuGeometry.cards(this.buttons.subList(this.page * 6, Math.min(this.buttons.size(), this.page * 6 + 6)).stream().map(Button::kind).toList());
        }

        public Inventory getInventory() {
            return this.inventory;
        }
    }

    public record Button(String icon, String label, Runnable action, Runnable alternate, MenuGeometry.Kind kind) {
        public Button(String string, String string2, Runnable runnable) {
            this(string, string2, runnable, null, MenuGeometry.Kind.CATEGORY);
        }

        public Button(String string, String string2, Runnable runnable, Runnable runnable2) {
            this(string, string2, runnable, runnable2, MenuGeometry.Kind.CATEGORY);
        }

        public Button as(MenuGeometry.Kind kind) {
            return new Button(this.icon, this.label, this.action, this.alternate, kind);
        }
    }
}

