package com.mohit.jvm.gc;

import org.springframework.web.bind.annotation.*;
import java.lang.management.*;
import java.util.*;

/**
 * GCAlgorithmDemo — explains GC algorithms and exposes live GC metrics.
 *
 * KEY CONCEPTS DEMONSTRATED:
 * ─────────────────────────────────────────────────────────────
 *  SERIAL GC       (-XX:+UseSerialGC)
 *    Single-threaded. Stop-the-world for all collections.
 *    Use case: small heaps, single-core, embedded/batch apps.
 *    Never use in production servers.
 *
 *  PARALLEL GC     (-XX:+UseParallelGC)   ← default before Java 9
 *    Multi-threaded Minor GC. Still stop-the-world.
 *    Use case: throughput-focused batch jobs.
 *    Tunable: -XX:ParallelGCThreads=N
 *
 *  CMS             (-XX:+UseConcMarkSweepGC)  ← deprecated Java 14+
 *    Concurrent Mark Sweep. Low pause time but CPU-intensive.
 *    Suffered from fragmentation — led to concurrent mode failures.
 *
 *  G1 GC           (-XX:+UseG1GC)         ← default Java 9+
 *    Region-based heap. Predictable pause times.
 *    Splits heap into ~2048 regions. Collects most-garbage-first.
 *    Tunable: -XX:MaxGCPauseMillis=200 (target pause time)
 *    Best for: most server-side Java applications.
 *
 *  ZGC             (-XX:+UseZGC)           ← production-ready Java 15+
 *    Sub-millisecond pauses. Scales to TB heaps.
 *    Almost all work concurrent. Tiny stop-the-world.
 *    Best for: latency-sensitive services.
 *
 *  SHENANDOAH      (-XX:+UseShenandoahGC)  ← Red Hat / OpenJDK
 *    Similar to ZGC. Concurrent compaction.
 *    Best for: ultra-low latency applications.
 * ─────────────────────────────────────────────────────────────
 *
 * Try it:
 *   GET /gc/metrics          → live GC count, pause time, collections
 *   GET /gc/comparison       → algorithm comparison table
 *   GET /gc/stress?mb=100    → stress test — triggers real GC events
 */
@RestController
@RequestMapping("/gc")
public class GCAlgorithmDemo {

    /**
     * Returns live GC metrics from the JVM.
     * Run this before and after /gc/stress to see GC events accumulate.
     *
     * The "collection_count" tells you how many times each GC fired.
     * The "collection_time_ms" is cumulative pause time — your app was FROZEN for this long.
     */
    @GetMapping("/metrics")
    public Map<String, Object> gcMetrics() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> gcBeans = new ArrayList<>();

        long totalCollections = 0;
        long totalPauseMs     = 0;

        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, Object> gcInfo = new LinkedHashMap<>();
            gcInfo.put("name",             gc.getName());
            gcInfo.put("collection_count", gc.getCollectionCount());
            gcInfo.put("collection_time_ms", gc.getCollectionTime());
            gcInfo.put("manages_pools",    Arrays.asList(gc.getMemoryPoolNames()));

            // Identify GC type from name
            String name = gc.getName().toLowerCase();
            String type = name.contains("young") || name.contains("minor") || name.contains("copy")
                    ? "Minor GC (Young Gen)" : "Major/Full GC (Old Gen)";
            gcInfo.put("gc_type", type);

            totalCollections += Math.max(0, gc.getCollectionCount());
            totalPauseMs     += Math.max(0, gc.getCollectionTime());
            gcBeans.add(gcInfo);
        }

        result.put("gc_collectors",         gcBeans);
        result.put("total_collections",      totalCollections);
        result.put("total_pause_time_ms",    totalPauseMs);
        result.put("active_gc_algorithm",    detectGCAlgorithm());
        result.put("interview_insight",
            "In production, monitor: minor_gc_frequency (should be fast + frequent), " +
            "full_gc_frequency (should be rare). Frequent Full GC = memory pressure problem.");
        return result;
    }

    /**
     * GC algorithm comparison — useful for interviews.
     * This is the kind of table that gets you hired.
     */
    @GetMapping("/comparison")
    public Map<String, Object> gcComparison() {
        Map<String, Object> result = new LinkedHashMap<>();

        List<Map<String, Object>> algorithms = new ArrayList<>();

        algorithms.add(algo("Serial GC",    "-XX:+UseSerialGC",
                "Single-core", "High (stop-the-world)", "Max throughput",
                "Embedded / batch / dev only", "Java 1.3+", false));

        algorithms.add(algo("Parallel GC",  "-XX:+UseParallelGC",
                "Multi-core batch", "Medium (parallel STW)", "Max throughput",
                "Throughput-focused batch jobs", "Java 5+ (default pre-Java 9)", false));

        algorithms.add(algo("CMS",          "-XX:+UseConcMarkSweepGC",
                "Multi-core, low-latency", "Low (concurrent)", "Low latency",
                "Legacy low-latency apps", "Deprecated Java 14, removed Java 15", true));

        algorithms.add(algo("G1 GC",        "-XX:+UseG1GC",
                "General purpose servers", "Predictable (<200ms default)",
                "Balanced throughput + latency",
                "Most Spring Boot / enterprise apps", "Default Java 9+", false));

        algorithms.add(algo("ZGC",          "-XX:+UseZGC",
                "Latency-critical services", "Ultra-low (<1ms)",
                "Sub-millisecond pauses",
                "APIs, trading, real-time systems", "Production-ready Java 15+", false));

        algorithms.add(algo("Shenandoah",   "-XX:+UseShenandoahGC",
                "Latency-critical, large heaps", "Ultra-low (<10ms)",
                "Concurrent compaction",
                "Ultra-low-latency workloads", "OpenJDK / Red Hat Java", false));

        result.put("gc_algorithms",     algorithms);
        result.put("recommendation",
            "For most Spring Boot apps: G1GC (default) is correct. " +
            "Only switch to ZGC if p99 latency matters and you're on Java 17+.");
        result.put("interview_tip",
            "Know G1GC deeply: regions, mixed collections, evacuation pauses, " +
            "Humongous objects (>50% of region size go directly to Old Gen).");
        return result;
    }

    /**
     * Stress test — allocate and release memory rapidly to trigger real GC events.
     * Watch /gc/metrics collection_count increase after running this.
     *
     * WHAT HAPPENS INTERNALLY:
     * 1. Objects born in Eden (Minor GC fires when Eden fills)
     * 2. Short-lived objects collected immediately
     * 3. Some survive → Survivor space
     * 4. If Survivor full → premature promotion to Old Gen
     * 5. Old Gen fills → Major GC (expensive!)
     */
    @GetMapping("/stress")
    public Map<String, Object> stressTest(@RequestParam(defaultValue = "50") int mb) {
        long gcCountBefore  = totalGCCount();
        long gcTimeBefore   = totalGCTime();
        long heapBefore     = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Rapid allocation — fills Eden repeatedly, triggers many Minor GCs
        List<byte[]> holder = new ArrayList<>();
        int chunkSize = 256 * 1024; // 256 KB chunks
        int chunks = (mb * 1024 * 1024) / chunkSize;

        for (int i = 0; i < chunks; i++) {
            holder.add(new byte[chunkSize]);
            // Release every other chunk — simulate real mixed-lifetime workload
            if (i % 2 == 0) holder.remove(0);
        }

        holder.clear(); // Release remaining

        long heapAfter    = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long gcCountAfter = totalGCCount();
        long gcTimeAfter  = totalGCTime();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stress_mb",             mb);
        result.put("gc_events_triggered",   gcCountAfter - gcCountBefore);
        result.put("gc_pause_added_ms",     gcTimeAfter - gcTimeBefore);
        result.put("heap_delta_mb",
                Math.round((heapAfter - heapBefore) / 1024.0 / 1024.0 * 10.0) / 10.0);
        result.put("interpretation",
            gcCountAfter - gcCountBefore > 0
                ? "GC fired during stress. Check /gc/metrics for cumulative stats."
                : "No GC during stress — heap had enough room. Try a larger mb value.");
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String detectGCAlgorithm() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .map(GarbageCollectorMXBean::getName)
                .reduce((a, b) -> a + " + " + b)
                .orElse("Unknown");
    }

    private long totalGCCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gc -> Math.max(0, gc.getCollectionCount())).sum();
    }

    private long totalGCTime() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gc -> Math.max(0, gc.getCollectionTime())).sum();
    }

    private Map<String, Object> algo(String name, String flag, String useCase,
            String pauseTime, String strength, String bestFor,
            String version, boolean deprecated) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("algorithm",   name);
        m.put("jvm_flag",    flag);
        m.put("use_case",    useCase);
        m.put("pause_time",  pauseTime);
        m.put("strength",    strength);
        m.put("best_for",    bestFor);
        m.put("version",     version);
        m.put("deprecated",  deprecated);
        return m;
    }
}
