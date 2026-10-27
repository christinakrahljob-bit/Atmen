package de.christina.atmen;

/** Eine Atemtechnik als Folge von Phasen, die sich endlos wiederholt. */
public final class Technique {

    public enum Kind { IN, HOLD_FULL, OUT, HOLD_EMPTY }

    public static final class Phase {
        public final Kind kind;
        public final int seconds;
        Phase(Kind kind, int seconds) { this.kind = kind; this.seconds = seconds; }
    }

    /** Zustand zu einem bestimmten Zeitpunkt der Übung. */
    public static final class State {
        public int phaseIndex;
        public Kind kind;
        /** Fortschritt innerhalb der Phase, 0..1 */
        public float progress;
        /** Ganze Sekunden, die in dieser Phase noch übrig sind (aufgerundet). */
        public int secondsLeft;
        /** Größe des Kreises, 0 = leer, 1 = voll. */
        public float level;
    }

    public final String name;
    public final String summary;
    public final Phase[] phases;
    public final long cycleMillis;

    Technique(String name, String summary, Phase... phases) {
        this.name = name;
        this.summary = summary;
        this.phases = phases;
        long total = 0;
        for (Phase p : phases) total += p.seconds * 1000L;
        this.cycleMillis = total;
    }

    public static final Technique[] ALL = {
        new Technique("4-7-8", "4 s ein · 7 s halten · 8 s aus",
            new Phase(Kind.IN, 4), new Phase(Kind.HOLD_FULL, 7), new Phase(Kind.OUT, 8)),
        new Technique("Box", "4 s ein · 4 s halten · 4 s aus · 4 s halten",
            new Phase(Kind.IN, 4), new Phase(Kind.HOLD_FULL, 4),
            new Phase(Kind.OUT, 4), new Phase(Kind.HOLD_EMPTY, 4)),
        new Technique("Ruhig", "4 s ein · 6 s aus",
            new Phase(Kind.IN, 4), new Phase(Kind.OUT, 6)),
    };

    public static String label(Kind kind) {
        switch (kind) {
            case IN: return "Einatmen";
            case OUT: return "Ausatmen";
            default: return "Halten";
        }
    }

    /** Berechnet den Zustand nach {@code elapsedMillis} seit Beginn der Übung. */
    public State stateAt(long elapsedMillis, State out) {
        if (out == null) out = new State();
        long t = elapsedMillis % cycleMillis;
        if (t < 0) t += cycleMillis;
        for (int i = 0; i < phases.length; i++) {
            long len = phases[i].seconds * 1000L;
            if (t < len || i == phases.length - 1) {
                Phase p = phases[i];
                out.phaseIndex = i;
                out.kind = p.kind;
                out.progress = Math.min(1f, t / (float) len);
                out.secondsLeft = (int) Math.ceil((len - t) / 1000.0);
                if (out.secondsLeft < 1) out.secondsLeft = 1;
                out.level = levelFor(p.kind, out.progress);
                return out;
            }
            t -= len;
        }
        return out; // nicht erreichbar
    }

    private static float levelFor(Kind kind, float p) {
        float eased = (float) ((1 - Math.cos(Math.PI * p)) / 2); // sanftes An- und Abschwellen
        switch (kind) {
            case IN: return eased;
            case OUT: return 1f - eased;
            case HOLD_FULL: return 1f;
            default: return 0f;
        }
    }
}
