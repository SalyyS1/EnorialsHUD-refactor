/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.protocol;

import sk.enorials.common.pack.ResourcePackGuard;
import sk.enorials.hud.EnorialsHUD;
import sk.enorials.hud.protocol.VanillaBars;

public final class PackLoadGuard
extends ResourcePackGuard {
    public PackLoadGuard(EnorialsHUD enorialsHUD) {
        super(enorialsHUD, VanillaBars.MAIN_PACK_ID);
    }
}

