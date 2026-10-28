package com.digitaltimeline.domain;

/** Why two activities are connected in the knowledge graph. */
public enum RelationKind {
    SIMILAR_TO,      // semantically similar content (embeddings)
    SHARES_TAGS,     // overlapping tags
    SAME_PROJECT,    // same project, close in time
    FOLLOWS_UP       // the previous step in the same project
}
