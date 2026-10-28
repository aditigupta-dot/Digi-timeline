package com.digitaltimeline.ai;

import com.digitaltimeline.domain.Activity;

/** One retrieved activity that is handed to the LLM as evidence. ref is its citation label, e.g. "A3". */
public record ContextItem(String ref, Activity activity, double score, String why) {}
