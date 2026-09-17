/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.NonNullList
 *  net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
 *  net.minecraft.world.item.ItemStack
 *  org.bukkit.entity.Player
 */
package sk.enorials.hud.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.world.item.ItemStack;
import org.bukkit.entity.Player;

public final class MenuPackets {
    private static final Method ID = MenuPackets.accessor("containerId", "getContainerId");
    private static final Method STATE = MenuPackets.accessor("stateId", "getStateId");
    private static final Method ITEMS = MenuPackets.accessor("items", "getItems");
    private static final Constructor<?> CONTENT = MenuPackets.constructor();
    private static final Method CLOSE_DIALOG = MenuPackets.closeDialogMethod();

    private MenuPackets() {
    }

    private static Method accessor(String string, String string2) {
        try {
            try {
                return ClientboundContainerSetContentPacket.class.getMethod(string, new Class[0]);
            }
            catch (NoSuchMethodException noSuchMethodException) {
                return ClientboundContainerSetContentPacket.class.getMethod(string2, new Class[0]);
            }
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new ExceptionInInitializerError(reflectiveOperationException);
        }
    }

    private static Constructor<?> constructor() {
        try {
            try {
                return ClientboundContainerSetContentPacket.class.getConstructor(Integer.TYPE, Integer.TYPE, List.class, ItemStack.class);
            }
            catch (NoSuchMethodException noSuchMethodException) {
                return ClientboundContainerSetContentPacket.class.getConstructor(Integer.TYPE, Integer.TYPE, NonNullList.class, ItemStack.class);
            }
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new ExceptionInInitializerError(reflectiveOperationException);
        }
    }

    private static Object read(Method method, Object object) {
        try {
            return method.invoke(object, new Object[0]);
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException("Cannot read container packet", reflectiveOperationException);
        }
    }

    public static int id(ClientboundContainerSetContentPacket clientboundContainerSetContentPacket) {
        return (Integer)MenuPackets.read(ID, clientboundContainerSetContentPacket);
    }

    public static int state(ClientboundContainerSetContentPacket clientboundContainerSetContentPacket) {
        return (Integer)MenuPackets.read(STATE, clientboundContainerSetContentPacket);
    }

    public static List<ItemStack> items(ClientboundContainerSetContentPacket clientboundContainerSetContentPacket) {
        return (List)MenuPackets.read(ITEMS, clientboundContainerSetContentPacket);
    }

    public static ClientboundContainerSetContentPacket content(int n, int n2, List<ItemStack> nonNullList) {
        NonNullList nonNullList2 = nonNullList;
        if (CONTENT.getParameterTypes()[2] == NonNullList.class) {
            NonNullList nonNullList3 = NonNullList.create();
            nonNullList3.addAll(nonNullList);
            nonNullList2 = nonNullList3;
        }
        try {
            return (ClientboundContainerSetContentPacket)CONTENT.newInstance(n, n2, nonNullList2, ItemStack.EMPTY);
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException("Cannot construct container packet", reflectiveOperationException);
        }
    }

    private static Method closeDialogMethod() {
        try {
            return Player.class.getMethod("closeDialog", new Class[0]);
        }
        catch (NoSuchMethodException noSuchMethodException) {
            return null;
        }
    }

    public static void closeDialog(Player player) {
        if (CLOSE_DIALOG == null) {
            return;
        }
        try {
            CLOSE_DIALOG.invoke((Object)player, new Object[0]);
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalStateException("Cannot close player dialog", reflectiveOperationException);
        }
    }
}

