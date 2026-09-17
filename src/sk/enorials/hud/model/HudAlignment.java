/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent$Builder
 */
package sk.enorials.hud.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import sk.enorials.hud.model.position.OffsetPosition;
import sk.enorials.hud.util.RelativeWidth;
import sk.enorials.hud.util.SizedComponent;
import sk.enorials.hud.util.SpaceUtil;

/*
 * Uses 'sealed' constructs - enablewith --sealed true
 */
public enum HudAlignment {
    LEFT{

        @Override
        public Component align(SizedComponent sizedComponent) {
            return HudAlignment.notValid(sizedComponent) ? Component.empty() : ((TextComponent.Builder)((TextComponent.Builder)Component.text().append(SpaceUtil.space((sizedComponent.getWidth() + 1) * -1))).append(sizedComponent.getComponent())).asComponent();
        }

        @Override
        public RelativeWidth calculateRelativeWidth(int n, OffsetPosition offsetPosition) {
            return new RelativeWidth(n - offsetPosition.getX(), offsetPosition.getX());
        }

        @Override
        public Component relativeAlign(Component component, RelativeWidth relativeWidth) {
            if (relativeWidth != null) {
                int n = relativeWidth.getRight() * -1;
                return ((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append(SpaceUtil.space(n))).append(component)).append(SpaceUtil.space(n * -1))).asComponent();
            }
            return component;
        }
    }
    ,
    RIGHT{

        @Override
        public Component align(SizedComponent sizedComponent) {
            return HudAlignment.notValid(sizedComponent) ? Component.empty() : ((TextComponent.Builder)((TextComponent.Builder)Component.text().append(sizedComponent.getComponent())).append(SpaceUtil.space((sizedComponent.getWidth() + 1) * -1))).asComponent();
        }

        @Override
        public RelativeWidth calculateRelativeWidth(int n, OffsetPosition offsetPosition) {
            return new RelativeWidth(offsetPosition.getX() * -1, n + offsetPosition.getX());
        }

        @Override
        public Component relativeAlign(Component component, RelativeWidth relativeWidth) {
            if (relativeWidth != null) {
                int n = relativeWidth.getLeft() - 1;
                return ((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append(SpaceUtil.space(n))).append(component)).append(SpaceUtil.space(n * -1))).asComponent();
            }
            return component;
        }
    }
    ,
    CENTER{

        @Override
        public Component align(SizedComponent sizedComponent) {
            if (HudAlignment.notValid(sizedComponent)) {
                return Component.empty();
            }
            int n = sizedComponent.getWidth() / 2 * -1;
            return ((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append(SpaceUtil.space(n))).append(sizedComponent.getComponent())).append(SpaceUtil.space(n - 1 - sizedComponent.getWidth() % 2))).asComponent();
        }

        @Override
        public RelativeWidth calculateRelativeWidth(int n, OffsetPosition offsetPosition) {
            int n2 = n / 2 * -1;
            return new RelativeWidth(n2 + n % 2, n2);
        }

        @Override
        public Component relativeAlign(Component component, RelativeWidth relativeWidth) {
            if (relativeWidth != null) {
                int n = relativeWidth.getRight();
                int n2 = relativeWidth.getLeft();
                int n3 = n2 > n ? Math.max(0, n2 - n + 1) / 2 : Math.max(0, n - n2 + 1) / 2 * -1;
                return ((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append(SpaceUtil.space(n3))).append(component)).append(SpaceUtil.space(n3 * -1))).asComponent();
            }
            return component;
        }
    };


    private static boolean notValid(SizedComponent sizedComponent) {
        return sizedComponent == null || sizedComponent.getWidth() == 0;
    }

    public abstract Component align(SizedComponent var1);

    public abstract RelativeWidth calculateRelativeWidth(int var1, OffsetPosition var2);

    public abstract Component relativeAlign(Component var1, RelativeWidth var2);
}

