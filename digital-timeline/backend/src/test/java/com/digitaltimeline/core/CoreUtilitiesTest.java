package com.digitaltimeline.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CoreUtilitiesTest {

    @Test
    void topKKeepsBestScoresInOrder() {
        TopK<String> top = new TopK<>(2);
        top.offer("a", 1);
        top.offer("b", 5);
        top.offer("c", 3);
        assertThat(top.toSortedList()).extracting(TopK.Entry::item).containsExactly("b", "c");
    }

    @Test
    void cosineOfIdenticalVectorsIsOne() {
        assertThat(VectorMath.cosine(new float[]{1, 2, 3}, new float[]{1, 2, 3})).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void tokenBucketDrainsAndRefills() {
        long[] now = {0};
        TokenBucket bucket = new TokenBucket(2, 1, () -> now[0]);
        assertThat(bucket.tryAcquire()).isTrue();
        assertThat(bucket.tryAcquire()).isTrue();
        assertThat(bucket.tryAcquire()).isFalse();
        now[0] += 1_000_000_000L;
        assertThat(bucket.tryAcquire()).isTrue();
    }

    @Test
    void timeRangeParserUnderstandsPhrases() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-25T10:00:00Z"), ZoneOffset.UTC);
        TimeRangeParser parser = new TimeRangeParser(clock, ZoneOffset.UTC);
        assertThat(parser.parse("what did I do yesterday").orElseThrow().from()).isEqualTo(Instant.parse("2026-10-24T00:00:00Z"));
        assertThat(parser.parse("summarise last week").orElseThrow().to()).isEqualTo(Instant.parse("2026-10-19T00:00:00Z"));
        assertThat(parser.parse("last 10 days").orElseThrow().from()).isEqualTo(Instant.parse("2026-10-15T00:00:00Z"));
        assertThat(parser.parse("hello there")).isEmpty();
    }

    @Test
    void redactorRemovesSecretsAndContacts() {
        PiiRedactor.Redacted r = PiiRedactor.withDefaults()
                .redact("mail a@b.com key sk-abcdefghijklmnop1234 aadhaar 1234 5678 9012 phone 9876543210");
        assertThat(r.text()).doesNotContain("@", "sk-abc", "1234 5678", "9876543210");
        assertThat(r.total()).isEqualTo(4);
    }

    @Test
    void unionFindGroupsConnectedItems() {
        UnionFind<Integer> uf = new UnionFind<>();
        uf.union(1, 2);
        uf.union(3, 4);
        uf.union(2, 3);
        uf.add(9);
        assertThat(uf.groups()).hasSize(2);
    }

    @Test
    void jaccardAndHashtags() {
        assertThat(TextTools.jaccard(Set.of(1, 2, 3), Set.of(2, 3, 4))).isEqualTo(0.5);
        assertThat(TextTools.hashtags("done #thesis and #ligo-data")).containsExactly("thesis", "ligo-data");
    }

    @Test
    void icsParserReadsEvents() {
        String ics = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nUID:x1\r\nSUMMARY:Viva prep\r\n"
                + "DTSTART:20261026T093000Z\r\nEND:VEVENT\r\nEND:VCALENDAR";
        var events = IcsParser.parse(ics, ZoneOffset.UTC);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).start()).isEqualTo(Instant.parse("2026-10-26T09:30:00Z"));
    }
}
