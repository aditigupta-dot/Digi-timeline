package com.digitaltimeline.seed;

import com.digitaltimeline.domain.ActivityType;
import com.digitaltimeline.domain.User;
import com.digitaltimeline.dto.ActivityRequest;
import com.digitaltimeline.repository.UserRepository;
import com.digitaltimeline.service.ActivityService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates demo@digitaltimeline.dev / demo1234 with ~5 weeks of realistic activity so the demo is instantly rich. */
@Component
public class DemoDataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    public static final String DEMO_EMAIL = "demo@digitaltimeline.dev";

    private final UserRepository users;
    private final ActivityService activities;
    private final PasswordEncoder encoder;

    public DemoDataSeeder(UserRepository users, ActivityService activities, PasswordEncoder encoder) {
        this.users = users;
        this.activities = activities;
        this.encoder = encoder;
    }

    private record Seed(int daysAgo, int hour, ActivityType type, String project, String title, String content,
                        String tags, Map<String, String> attrs) {}

    private static Seed task(int d, int h, String project, String title, String content, String status, int priority, String tags) {
        return new Seed(d, h, ActivityType.TASK, project, title, content, tags, Map.of("status", status, "priority", String.valueOf(priority)));
    }

    /** Task with an explicit due date, given as days from today (negative = already overdue). */
    private static Seed taskDue(int d, int h, String project, String title, String content, String status, int priority,
                                String tags, int dueInDays) {
        return new Seed(d, h, ActivityType.TASK, project, title, content, tags,
                Map.of("status", status, "priority", String.valueOf(priority), "dueInDays", String.valueOf(dueInDays)));
    }

    private static Seed note(int d, int h, String project, String title, String content, String tags) {
        return new Seed(d, h, ActivityType.NOTE, project, title, content, tags, Map.of());
    }

    private static Seed git(int d, int h, String project, String title, String content, String tags) {
        return new Seed(d, h, ActivityType.GITHUB, project, title, content, tags,
                Map.of("repo", "demo/" + project.toLowerCase().replace(' ', '-'), "eventType", "PushEvent",
                        "url", "https://github.com/demo/" + project.toLowerCase().replace(' ', '-')));
    }

    private static Seed doc(int d, int h, String project, String title, String content, String tags) {
        return new Seed(d, h, ActivityType.DOCUMENT, project, title, content, tags,
                Map.of("filePath", "/docs/" + title, "mimeType", "text/markdown", "sizeBytes", "24576"));
    }

    private static Seed event(int d, int h, String project, String title, String content, String tags, String where) {
        return new Seed(d, h, ActivityType.EVENT, project, title, content, tags, Map.of("location", where, "attendees", "Prof. Rao, Meera, Kiran"));
    }

    private static final List<Seed> SEEDS = List.of(
            // --- Thesis Research ---
            doc(34, 11, "Thesis Research", "literature-review.md", "Survey of anomaly detection in time series sensor data. Covers wavelet transforms, autoencoders and spectrogram classifiers for noisy signals.", "literature,survey"),
            task(33, 10, "Thesis Research", "Read the spectrogram classification paper", "Compare CNN spectrogram classifiers against wavelet features for noisy signals. Take notes on dataset splits.", "DONE", 2, "reading,paper"),
            note(30, 21, "Thesis Research", "Idea: topological features for glitch-like noise", "Persistent homology could separate transient noise from real signals. Try Betti numbers on sliding windows. #thesis #topology", "idea"),
            task(28, 14, "Thesis Research", "Build baseline classifier with scikit-learn", "Random forest on spectrogram statistics as the baseline to beat. Track accuracy per noise class.", "DONE", 1, "baseline,ml"),
            task(26, 16, "Thesis Research", "Write related-work section draft", "Two pages on spectrogram and topological methods. Needs citations for persistent homology.", "TODO", 2, "writing"),
            git(21, 22, "Thesis Research", "Pushed 3 commits to demo/thesis-research (main)", "- add sliding window persistence features\n- fix dataset loader for noisy samples\n- baseline results notebook", "github,code"),
            note(19, 20, "Thesis Research", "Meeting notes with supervisor", "Supervisor wants a cleaner evaluation: stratified splits, confusion matrix per noise class, and an ablation of topological features. Deadline for draft: end of next month. #thesis #meeting", "meeting"),
            task(18, 11, "Thesis Research", "Run ablation: with and without topological features", "Use the same splits. Report accuracy, F1 per class and training time.", "TODO", 1, "experiment,ablation"),
            git(12, 23, "Thesis Research", "Pushed 5 commits to demo/thesis-research (main)", "- topological feature extractor\n- parallel window processing\n- unit tests for Betti numbers\n- plotting helpers\n- README update", "github,code"),
            note(9, 22, "Thesis Research", "Results look promising", "Topological features improved F1 on the rare noise classes by a clear margin. Still need the ablation to be sure it is not just more features. #thesis #results", "results"),
            doc(6, 15, "Thesis Research", "results-draft.md", "Evaluation tables: baseline versus topological features, confusion matrices per noise class, training time comparison.", "results,draft"),
            git(3, 23, "Thesis Research", "Pushed 2 commits to demo/thesis-research (main)", "- stratified splits\n- confusion matrix plots", "github,code"),
            note(1, 22, "Thesis Research", "Why do rare noise classes improve most?", "Hypothesis: persistent homology captures short-lived structure that spectrogram statistics average away. Need an experiment to confirm. #thesis #hypothesis", "idea"),
            event(2, 10, "Thesis Research", "Supervisor review meeting", "Show evaluation tables and discuss the ablation plan.", "meeting", "Room 4, Academic Block"),
            // --- Hackathon ---
            note(31, 19, "Hackathon", "Problem statement shortlisted", "Team picked the shelter thermal modelling problem for high-altitude regions. Passive design, no heating. #hackathon", "planning"),
            task(29, 17, "Hackathon", "Research insulation materials for cold climates", "Compare aerogel, sheep wool, and rammed earth thermal conductivity. Cost per square metre matters.", "DONE", 2, "research,materials"),
            doc(25, 13, "Hackathon", "thermal-model-notes.md", "Lumped-capacitance model of a shelter: wall conductance, solar gain through south glazing, night-time heat loss.", "model,thermal"),
            git(22, 21, "Hackathon", "Pushed 4 commits to demo/hackathon (main)", "- thermal simulation core\n- solar gain function\n- unit conversion fixes\n- first plots", "github,code"),
            task(20, 10, "Hackathon", "Prepare the presentation slide", "One slide with the architecture, the thermal model result and the cost estimate.", "IN_PROGRESS", 1, "slides,presentation"),
            event(16, 18, "Hackathon", "Team sync", "Divide work: simulation, slide, cost model.", "meeting", "Online"),
            note(14, 21, "Hackathon", "Simulation says night temperature stays 9 degrees above outside", "With 30 cm rammed earth and south glazing the shelter holds heat overnight. Need to validate solar gain assumptions. #hackathon #results", "results"),
            task(10, 12, "Hackathon", "Validate solar gain assumptions with real irradiance data", "Download irradiance data for the target region and compare with the model constants.", "TODO", 1, "validation,data"),
            git(5, 22, "Hackathon", "Pushed 2 commits to demo/hackathon (main)", "- parametrise wall thickness\n- cost estimate script", "github,code"),
            // --- Java Lab ---
            note(32, 20, "Java Lab", "Project idea: DigitalTimeline", "A timeline of all digital activity with AI search and a knowledge graph. Use Spring Boot, Neo4j and embeddings. #java #project", "idea"),
            task(27, 15, "Java Lab", "Design the class hierarchy for activities", "Abstract Activity with Task, Note, Document, GitHub and Event subclasses. Think about polymorphism and generics.", "DONE", 1, "oop,design"),
            git(24, 22, "Java Lab", "Pushed 6 commits to demo/digital-timeline (main)", "- JPA entities with inheritance\n- repository layer\n- REST controllers\n- JWT security\n- exception handler\n- first tests", "github,code"),
            doc(17, 14, "Java Lab", "architecture.md", "Layers: web, service, ingestion pipeline, collectors, graph store, AI providers. Events decouple saving from linking.", "architecture,design"),
            task(15, 11, "Java Lab", "Implement multithreaded collectors", "Run GitHub, folder and calendar collectors in parallel with CompletableFuture and virtual threads.", "DONE", 2, "concurrency,java"),
            task(13, 16, "Java Lab", "Add Neo4j knowledge graph for related activities", "Store links with weight and reason. Query neighbours for graph-based retrieval in the AI assistant.", "IN_PROGRESS", 1, "neo4j,graph"),
            note(11, 21, "Java Lab", "RAG design decision", "Hybrid search: embeddings plus keyword coverage, then expand with graph neighbours, then ask the LLM with citations. Redact personal data before sending. #java #rag", "design"),
            git(8, 23, "Java Lab", "Pushed 4 commits to demo/digital-timeline (main)", "- hybrid search index with top-k heap\n- PII redaction chain\n- token bucket rate limiter\n- SSE live stream", "github,code"),
            task(7, 10, "Java Lab", "Write JUnit tests for the core utilities", "Cover TopK, TokenBucket, TimeRangeParser and the redactor.", "TODO", 2, "testing,junit"),
            event(4, 11, "Java Lab", "Project review with sir", "Demo the timeline, the AI assistant and the knowledge graph. Explain design patterns used.", "viva", "Computer Lab 2"),
            doc(2, 17, "Java Lab", "report-outline.md", "Sections: problem, architecture, Java concepts used, AI pipeline, security, results, future work.", "report,outline"),
            // --- Personal ---
            task(30, 9, "Personal", "Renew library membership", "Bring ID card. Library closes at 5 pm.", "DONE", 3, "errand"),
            note(23, 8, "Personal", "Morning routine experiment", "Tried studying 6 to 8 am for a week. Focus was good but energy dropped in the afternoon. #personal", "habits"),
            task(21, 18, "Personal", "Book train tickets for the holidays", "Check the festival rush, book at least three weeks ahead.", "TODO", 2, "travel"),
            event(10, 19, "Personal", "Dinner with friends", "Celebrate end of semester exams.", "social", "Campus cafe"),
            // --- upcoming (negative daysAgo = in the future) ---
            event(-1, 10, "Thesis Research", "Project presentation: AI/ML project review", "Present the evaluation tables and the ablation plan.", "presentation", "Seminar Hall"),
            event(-3, 9, "Hackathon", "Internal hackathon round", "Pitch the shelter thermal digital twin to the panel.", "hackathon", "Innovation Lab"),
            event(-5, 17, "Java Lab", "Club meeting", "Community meeting, share the DigitalTimeline demo.", "club", "Room 12"),
            event(-8, 11, "Java Lab", "Final project submission demo", "Full demo of timeline, AI assistant and knowledge map.", "viva", "Computer Lab 2"),
            taskDue(1, 20, "Java Lab", "Submit lab record", "Print and submit the Java lab record file.", "TODO", 1, "submission", 1),
            taskDue(2, 18, "Hackathon", "Rehearse the pitch", "Practise the 3-minute pitch twice with the team.", "TODO", 1, "presentation", 2)
    );

    public void seedIfEmpty() {
        if (users.existsByEmail(DEMO_EMAIL)) return;
        User demo = users.save(new User(DEMO_EMAIL, "Demo User", encoder.encode("demo1234")));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int count = 0;
        for (Seed s : SEEDS) {
            Instant at = today.minusDays(s.daysAgo()).atTime(s.hour(), (s.title().length() * 7) % 60).toInstant(ZoneOffset.UTC);
            Set<String> tags = new LinkedHashSet<>(List.of(s.tags().split(",")));
            Map<String, String> attrs = new HashMap<>(s.attrs());
            if (s.type() == ActivityType.TASK) {
                String inDays = attrs.remove("dueInDays");
                if (inDays != null) {
                    attrs.put("dueAt", today.plusDays(Long.parseLong(inDays)).atTime(17, 0).toInstant(ZoneOffset.UTC).toString());
                } else if (!"DONE".equals(attrs.get("status"))) {
                    attrs.put("dueAt", at.plus(java.time.Duration.ofDays(14)).toString());   // old open tasks become overdue
                }
            }
            ActivityRequest request = new ActivityRequest(s.type(), s.title(), s.content(), at, s.project(), tags, attrs);
            activities.ingest(demo.getId(), request, "demo", "seed:" + count);
            count++;
        }
        log.info("Seeded demo account {} with {} activities (password: demo1234)", DEMO_EMAIL, count);
    }
}
