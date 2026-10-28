package com.digitaltimeline.link;

import com.digitaltimeline.core.TextTools;
import com.digitaltimeline.domain.Activity;
import com.digitaltimeline.domain.RelationKind;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Explainable link scoring: every edge carries a human-readable reason, not just a number. */
public final class RelationScorer {
    private RelationScorer() {}

    public record Score(double weight, RelationKind kind, String reason) {}

    public static Score score(Activity a, Activity b, double semantic) {
        Set<String> shared = new TreeSet<>(a.getTags());
        shared.retainAll(b.getTags());
        double tagSim = TextTools.jaccard(a.getTags(), b.getTags());
        boolean sameProject = a.getProject() != null && !a.getProject().isBlank()
                && a.getProject().equalsIgnoreCase(b.getProject());
        long hours = Math.abs(Duration.between(a.getOccurredAt(), b.getOccurredAt()).toHours());
        double temporal = Math.exp(-hours / 72.0);

        double weight = 0.55 * semantic + 0.20 * tagSim + 0.15 * (sameProject ? 1 : 0) + 0.10 * temporal;

        RelationKind kind;
        if (semantic >= 0.5) kind = RelationKind.SIMILAR_TO;
        else if (!shared.isEmpty() && tagSim >= 0.25) kind = RelationKind.SHARES_TAGS;
        else if (sameProject) kind = RelationKind.SAME_PROJECT;
        else kind = RelationKind.SIMILAR_TO;

        List<String> why = new ArrayList<>();
        if (semantic >= 0.25) why.add(String.format("similar content (%d%%)", Math.round(semantic * 100)));
        if (!shared.isEmpty()) why.add("shared tags: " + String.join(", ", shared.stream().limit(4).toList()));
        if (sameProject) why.add("same project “" + a.getProject() + "”");
        if (hours <= 48) why.add("within " + Math.max(1, hours) + "h of each other");
        if (why.isEmpty()) why.add("weakly related");
        return new Score(Math.min(1.0, weight), kind, String.join("; ", why));
    }
}
