/*
 * Decompiled with CFR 0.152.
 */
package sk.enorials.hud.betonquest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import sk.enorials.hud.compat.Numbers;

public final class DialoguePages {
    public static final int BODY_LINES = 7;
    public static final int ANSWER_LINES = 6;
    private final List<String> body;
    private final List<List<String>> answers;
    private int page;
    private int selected;
    private int answerPage;

    public DialoguePages(String string2, List<String> list, ToIntFunction<String> toIntFunction) {
        this.body = DialoguePages.wrap(string2, 314, toIntFunction);
        int n = Math.max(80, 197 - toIntFunction.applyAsInt(list.size() + ". "));
        this.answers = list.stream().map(string -> DialoguePages.wrap(string, n, toIntFunction)).toList();
    }

    public static List<String> wrap(String string, int n, ToIntFunction<String> toIntFunction) {
        ArrayList<String> arrayList = new ArrayList<String>();
        for (String string2 : string.replace("\r", "").split("\n", -1)) {
            if (string2.isEmpty()) {
                arrayList.add("");
                continue;
            }
            StringBuilder stringBuilder = new StringBuilder();
            for (String string3 : string2.split("(?<=\\s)|(?=\\s)")) {
                if (stringBuilder.length() > 0 && toIntFunction.applyAsInt(String.valueOf(stringBuilder) + string3) > n) {
                    arrayList.add(stringBuilder.toString().stripTrailing());
                    stringBuilder.setLength(0);
                }
                if (stringBuilder.isEmpty() && string3.isBlank()) continue;
                for (int n2 : string3.codePoints().toArray()) {
                    String string4 = new String(Character.toChars(n2));
                    if (!stringBuilder.isEmpty() && toIntFunction.applyAsInt(String.valueOf(stringBuilder) + string4) > n) {
                        arrayList.add(stringBuilder.toString());
                        stringBuilder.setLength(0);
                    }
                    stringBuilder.append(string4);
                }
            }
            if (stringBuilder.isEmpty()) continue;
            arrayList.add(stringBuilder.toString().stripTrailing());
        }
        return arrayList.isEmpty() ? List.of("") : List.copyOf(arrayList);
    }

    public int pages() {
        return (this.body.size() + 7 - 1) / 7;
    }

    public int page() {
        return this.page;
    }

    public int selected() {
        return this.selected;
    }

    public int answerCount() {
        return this.answers.size();
    }

    public boolean reading() {
        return this.page + 1 < this.pages();
    }

    public List<String> body() {
        return this.body.subList(this.page * 7, Math.min(this.body.size(), (this.page + 1) * 7));
    }

    public void select(int n) {
        if (this.reading()) {
            this.page = Numbers.clamp(this.page + n, 0, this.pages() - 1);
            return;
        }
        if (!this.answers.isEmpty()) {
            this.selected = Math.floorMod(this.selected + n, this.answers.size());
        }
        this.answerPage = 0;
    }

    public int confirm() {
        if (this.reading()) {
            ++this.page;
            return -1;
        }
        if (this.answers.isEmpty()) {
            return 0;
        }
        if ((this.answerPage + 1) * 6 < this.answers.get(this.selected).size()) {
            ++this.answerPage;
            return -1;
        }
        return this.selected + 1;
    }

    public int answerPage() {
        return this.answerPage;
    }

    public int answerPages() {
        return this.answers.isEmpty() ? 1 : (this.answers.get(this.selected).size() + 6 - 1) / 6;
    }

    public List<Card> cards() {
        if (this.reading() || this.answers.isEmpty()) {
            return List.of();
        }
        List<String> list = this.answers.get(this.selected);
        int n = this.answerPage * 6;
        int n2 = Math.min(6, list.size() - n);
        ArrayList<Card> arrayList = new ArrayList<Card>();
        arrayList.add(new Card(this.selected, true, list.subList(n, n + n2)));
        int n3 = this.selected - 1;
        int n4 = this.selected + 1;
        while (arrayList.size() < 3) {
            if (n3 >= 0 && n2 + this.answers.get(n3).size() <= 6) {
                n2 += this.answers.get(n3).size();
                arrayList.add(0, new Card(n3, false, this.answers.get(n3--)));
                continue;
            }
            if (n4 >= this.answers.size() || n2 + this.answers.get(n4).size() > 6) break;
            n2 += this.answers.get(n4).size();
            arrayList.add(new Card(n4, false, this.answers.get(n4++)));
        }
        return List.copyOf(arrayList);
    }

    public List<Reply> replies() {
        ArrayList<Reply> arrayList = new ArrayList<Reply>();
        for (Card card : this.cards()) {
            for (int i = 0; i < card.lines().size(); ++i) {
                arrayList.add(new Reply(card.index(), card.selected(), (String)(i == 0 ? card.index() + 1 + ". " : "   ") + card.lines().get(i)));
            }
        }
        return List.copyOf(arrayList);
    }

    public record Card(int index, boolean selected, List<String> lines) {
    }

    public record Reply(int index, boolean selected, String text) {
    }
}

