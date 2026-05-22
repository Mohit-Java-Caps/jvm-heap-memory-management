package com.mohit.jvm.heap;

import org.springframework.web.bind.annotation.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HeapRegionDemo — REST controller that exposes live JVM heap region stats
 * and demonstrates object allocation across Eden, Survivor, and Old Generation.
 *
 * KEY CONCEPTS DEMONSTRATED:
 * ─────────────────────────────────────────────────────────────
 *  EDEN SPACE     → Where ALL new objects are first allocated.
 *                   Filled fast, cleared frequently (Minor GC).
 *
 *  SURVIVOR (S0/S1) → Objects that survive Minor GC move here.
 *                     JVM alternates between S0 and S1 (copying collector).
 *
 *  OLD GENERATION → Objects that survive many GC cycles get promoted here.
 *                   Cleaned by Major/Full GC (expensive, stop-the-world).
 *
 *  TENURING THRESHOLD → Default: object promoted after 15 Minor GCs.
 *                       Tunable via: -XX:MaxTenuringThreshold=N
 * ─────────────────────────────────────────────────────────────
 *
 * Try it:
 *   GET /heap/stats          → live memory pool breakdown
 *   GET /heap/allocate?mb=50 → allocate N MB in heap and observe
 *   GET /heap/gc             → request GC and compare before/after
 */
@RestController
@RequestMapping("/heap")
public class HeapRegionDemo {

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    /**
     * Returns a live breakdown of every JVM memory pool.
     * Run this before and after /heap/allocate to see Eden fill up.
     */
    @GetMapping("/stats")
    public Map<String, Object> heapStats() {
        Map<String, Object> stats = new HashMap<>();

        // Overall heap summary
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        stats.put("heap_used_mb",      toMB(heapUsage.getUsed()));
        stats.put("heap_committed_mb", toMB(heapUsage.getCommitted()));
        stats.put("heap_max_mb",       toMB(heapUsage.getMax()));
        stats.put("heap_used_percent",
                String.format("%.1f%%", (double) heapUsage.getUsed() / heapUsage.getMax() * 100));

        // Per-region breakdown (Eden, Survivor, Old Gen, Metaspace)
        List<Map<String, Object>> pools = new ArrayList<>();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            MemoryUsage usage = pool.getUsage();
            if (usage == null) continue;

            Map<String, Object> poolInfo = new HashMap<>();
            poolInfo.put("name",         pool.getName());
            poolInfo.put("type",         pool.getType().toString());
            poolInfo.put("used_mb",      toMB(usage.getUsed()));
            poolInfo.put("max_mb",       usage.getMax() < 0 ? "unlimited" : toMB(usage.getMax()));
            poolInfo.put("committed_mb", toMB(usage.getCommitted()));
            pools.add(poolInfo);
        }
        stats.put("memory_pools", pools);
        stats.put("explanation", Map.of(
            "Eden",    "Where new objects are born — fills up fast",
            "S0/S1",   "Survivors of Minor GC bounce between these two spaces",
            "Old Gen", "Long-lived objects promoted here — Full GC cleans it",
            "Metaspace","Class metadata — not part of heap since Java 8"
        ));
        return stats;
    }

    /**
     * Allocates N megabytes of short-lived objects in Eden.
     * Watch Eden usage spike via /heap/stats before and after.
     *
     * What happens internally:
     * 1. Objects created → land in Eden
     * 2. Eden fills up → Minor GC fires
     * 3. Live objects → move to Survivor space
     * 4. Dead objects → collected (most of them)
     */
    @GetMapping("/allocate")
    public Map<String, Object> allocateInEden(@RequestParam(defaultValue = "10") int mb) {
        Map<String, Object> result = new HashMap<>();

        long before = memoryMXBean.getHeapMemoryUsage().getUsed();
        result.put("heap_before_mb", toMB(before));

        // Allocate short-lived byte arrays — these go straight to Eden
        int bytesPerArray = 1024 * 1024; // 1 MB per array
        List<byte[]> allocations = new ArrayList<>();
        for (int i = 0; i < mb; i++) {
            allocations.add(new byte[bytesPerArray]);
        }

        long after = memoryMXBean.getHeapMemoryUsage().getUsed();
        result.put("heap_after_mb",    toMB(after));
        result.put("delta_mb",         toMB(after - before));
        result.put("allocated_arrays", mb);
        result.put("note", "These objects are in Eden right now. " +
                           "Call /heap/gc to trigger Minor GC and watch them get collected.");

        // Keep reference alive long enough to observe, then let GC collect
        allocations.clear();
        return result;
    }

    /**
     * Requests GC and shows before/after heap usage.
     *
     * IMPORTANT: System.gc() is a *request*, not a guarantee.
     * JVM may ignore it based on GC policy.
     * Use -XX:+DisableExplicitGC to disable this in production.
     */
    @GetMapping("/gc")
    public Map<String, Object> requestGC() {
        Map<String, Object> result = new HashMap<>();
        long before = memoryMXBean.getHeapMemoryUsage().getUsed();
        result.put("before_gc_mb", toMB(before));

        System.gc(); // Minor GC request

        long after = memoryMXBean.getHeapMemoryUsage().getUsed();
        result.put("after_gc_mb",  toMB(after));
        result.put("reclaimed_mb", toMB(Math.max(0, before - after)));
        result.put("note", "System.gc() is a hint. Check GC logs for actual GC events.");
        return result;
    }

    private double toMB(long bytes) {
        return Math.round((double) bytes / (1024 * 1024) * 10.0) / 10.0;
    }
}
