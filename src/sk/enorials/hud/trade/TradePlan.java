/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.trade;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class TradePlan<T> {
    private final Stacks<T> stacks;

    public TradePlan(Stacks<T> stacks) {
        this.stacks = stacks;
    }

    public boolean matches(T t, Reservation<T> reservation) {
        return !this.stacks.empty(t) && this.stacks.similar(t, reservation.snapshot()) && this.stacks.amount(t) == this.stacks.amount(reservation.snapshot()) && reservation.amount() > 0 && reservation.amount() <= this.stacks.amount(t);
    }

    public Result<T> build(List<T> list, List<Reservation<T>> list2, List<T> list3, List<Reservation<T>> list4) {
        List<T> list5 = this.copy(list);
        List<T> list6 = this.copy(list3);
        this.take(list5, list2);
        this.take(list6, list4);
        this.give(list5, list4);
        this.give(list6, list2);
        return new Result<T>(list5, list6);
    }

    private List<T> copy(List<T> list) {
        ArrayList<Object> arrayList = new ArrayList<Object>();
        for (T t : list) {
            arrayList.add(this.stacks.empty(t) ? null : (Object)this.stacks.amount(t, this.stacks.amount(t)));
        }
        return arrayList;
    }

    private void take(List<T> list, List<Reservation<T>> list2) {
        HashSet<Integer> hashSet = new HashSet<Integer>();
        for (Reservation<T> reservation : list2) {
            int n = reservation.slot();
            if (n < 0 || n >= list.size() || !hashSet.add(n) || !this.matches(list.get(n), reservation)) {
                throw new IllegalArgumentException("An offered item changed. Please review the offer again.");
            }
            T t = list.get(n);
            int n2 = this.stacks.amount(t) - reservation.amount();
            list.set(n, n2 == 0 ? null : (Object)this.stacks.amount(t, n2));
        }
    }

    private void give(List<T> list, List<Reservation<T>> list2) {
        for (Reservation<T> reservation : list2) {
            int n;
            T t = reservation.snapshot();
            int n2 = reservation.amount();
            for (n = 0; n < list.size() && n2 > 0; ++n) {
                T t2 = list.get(n);
                if (this.stacks.empty(t2) || !this.stacks.similar(t2, t)) continue;
                int n3 = Math.min(n2, Math.max(0, this.stacks.limit(t2) - this.stacks.amount(t2)));
                list.set(n, this.stacks.amount(t2, this.stacks.amount(t2) + n3));
                n2 -= n3;
            }
            for (n = 0; n < list.size() && n2 > 0; ++n) {
                if (!this.stacks.empty(list.get(n))) continue;
                int n4 = Math.min(n2, this.stacks.limit(t));
                list.set(n, this.stacks.amount(t, n4));
                n2 -= n4;
            }
            if (n2 <= 0) continue;
            throw new IllegalArgumentException("Not enough inventory space. Make room and try again.");
        }
    }

    public static interface Stacks<T> {
        public boolean empty(T var1);

        public int amount(T var1);

        public int limit(T var1);

        public boolean similar(T var1, T var2);

        public T amount(T var1, int var2);
    }

    public record Reservation<T>(int slot, T snapshot, int amount) {
    }

    public record Result<T>(List<T> left, List<T> right) {
    }
}

