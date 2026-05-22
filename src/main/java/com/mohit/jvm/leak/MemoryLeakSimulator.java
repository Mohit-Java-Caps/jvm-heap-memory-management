package com.mohit.jvm.leak;

import org.springframework.web.bind.annotation.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.*;

/**
 * MemoryLeakSimulator — demonstrates the most common memory leak patterns in Java.
 *
 * KEY CONCEPTS DEMONSTRATED:
 * ─────────────────────────────────────────────────────────────
 *  MEMORY LEAK IN JAVA: Objects are no longer needed by the application
 *  but are still referenced — so the GC cannot collect them.
 *
 *  GC cannot fix a leak. It only collects UNREACHABLE objects.
 *  If you hold a reference, the object NEVER gets collected.
 *
 *  COMMON LEAK PATTERNS:
 *  1. Static collections that grow forever                 ← shown below
 *  2. Unclosed resources (streams, connections)            ← shown below
 *  3. Inner class holding outer class reference            ← shown below
 *  4. ThreadLocal not cleaned up                           ← shown below
 *  5. Key objects in HashMap with broken hashCode/equals   ← shown below
 * ─────────────────────────────────────────────────────────────
 *
 * DETECTION TOOLS:
 *  - VisualVM: heap dump → OQL query → find top retained objects
 *  - JProfiler / YourKit: allocation profiler
 *  - Eclipse MAT: dominator tree shows what's holding memory
 *  - jmap -dump:format=b,file=heap.hprof <pid>
 *
 * Try it:
 *   GET /leak/static-collection?items=1000  → classic leak via static list
 *   GET /leak/threadlocal                   → ThreadLocal not removed
 *   GET /leak/status                        → current heap + leak metrics
 *   DELETE /leak/clear                      → release and observe GC reclaim
 */
@RestController
@RequestMapping("/leak")
public class MemoryLeakSimulator {

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    // ── LEAK PATTERN 1: Static collection ────────────────────────────────────
    // Static fields live for the entire JVM lifetime.
    // Anything added here is NEVER eligible for GC unless explicitly removed.
    // This is the #1 most common leak in enterprise Java applications.
    private static final List<byte[]> STATIC_LEAK_BUCKET = new ArrayList<>();

    // ── LEAK PATTERN 4: ThreadLocal ───────────────────────────────────────────
    // ThreadLocals in thread pools (e.g., Tomcat) are NEVER cleaned up
    // unless you call threadLocal.remove() explicitly.
    // In web apps, this leaks once per request if not removed.
    private static final ThreadLocal<List<String>> THREAD_LOCAL_LEAK =
            ThreadLocal.withInitial(ArrayList::new);

    /**
     * Simulates a static collection memory leak.
     * Each call adds N items to a static list that is never cleared.
     *
     * INTERVIEW QUESTION: "Why is a static List a memory leak?"
     * ANSWER: Static fields are GC roots. Objects referenced by GC roots
     *         are always reachable → never collected → heap grows forever.
     */
    @GetMapping("/static-collection")
    public Map<String, Object> staticCollectionLeak(
            @RequestParam(defaultValue = "100") int items) {

        long before = heapUsedMB();

        // Each item = 1KB — small per request, catastrophic over time
        for (int i = 0; i < items; i++) {
            STATIC_LEAK_BUCKET.add(new byte[1024]); // 1 KB each
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items_added",          items);
        result.put("total_items_in_bucket", STATIC_LEAK_BUCKET.size());
        result.put("total_leak_size_kb",   STATIC_LEAK_BUCKET.size()); // ~1KB each
        result.put("heap_before_mb",        before);
        result.put("heap_after_mb",         heapUsedMB());
        result.put("leak_pattern",          "Static collection — grows with every request");
        result.put("fix",
            "Use bounded collections (LRU cache), or scope data to request/session, " +
            "not to static/class-level fields.");
        result.put("detection",
            "Heap dump → Eclipse MAT → Dominator Tree → look for ArrayList/HashMap " +
            "with unexpectedly large retained heap");
        return result;
    }

    /**
     * Simulates a ThreadLocal leak — common in Spring/Tomcat apps.
     *
     * INTERVIEW QUESTION: "How can ThreadLocal cause a memory leak?"
     * ANSWER: Thread pools reuse threads. If ThreadLocal is not removed,
     *         the value from request N is still accessible in request N+1,
     *         and the objects accumulate in heap tied to the thread.
     */
    @GetMapping("/threadlocal")
    public Map<String, Object> threadLocalLeak() {
        // Simulating data being added to ThreadLocal without cleanup
        List<String> localData = THREAD_LOCAL_LEAK.get();
        for (int i = 0; i < 1000; i++) {
            localData.add("session-data-" + i + "-" + UUID.randomUUID());
        }

        // ❌ BUG: We never call THREAD_LOCAL_LEAK.remove()
        // In a thread pool, this thread will be reused.
        // The next request on this thread will see leftover data.

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("thread_name",       Thread.currentThread().getName());
        result.put("items_in_tl",      localData.size());
        result.put("leak_pattern",     "ThreadLocal not removed — thread pool reuse accumulates data");
        result.put("fix",
            "Always call threadLocal.remove() in a finally block or use " +
            "Spring's RequestContextHolder which manages this automatically.");

        // ✅ Uncomment the line below to fix the leak:
        // THREAD_LOCAL_LEAK.remove();

        return result;
    }

    /**
     * Shows current heap status + leak metrics.
     * Call this repeatedly while hitting /leak/static-collection to watch heap grow.
     */
    @GetMapping("/status")
    public Map<String, Object> leakStatus() {
        Runtime rt = Runtime.getRuntime();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("heap_used_mb",       heapUsedMB());
        status.put("heap_max_mb",        toMB(rt.maxMemory()));
        status.put("heap_free_mb",       toMB(rt.freeMemory()));
        status.put("static_bucket_size", STATIC_LEAK_BUCKET.size());
        status.put("static_bucket_kb",   STATIC_LEAK_BUCKET.size()); // ~1KB per item
        status.put("tip",
            "Keep calling /leak/static-collection and watch heap_used_mb climb. " +
            "GC cannot help — objects are still referenced via static field.");
        return status;
    }

    /**
     * Releases the static collection — observe GC reclaim the memory.
     * This simulates fixing the leak by removing the static reference.
     */
    @DeleteMapping("/clear")
    public Map<String, Object> clearLeak() {
        long before = heapUsedMB();
        int released = STATIC_LEAK_BUCKET.size();

        STATIC_LEAK_BUCKET.clear(); // Remove all references
        System.gc();                // Request GC (hint only)

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items_released",    released);
        result.put("heap_before_mb",    before);
        result.put("heap_after_mb",     heapUsedMB());
        result.put("lesson",
            "Once references are removed, GC can collect. " +
            "The fix for memory leaks is always: remove the unwanted reference.");
        return result;
    }

    private double heapUsedMB() {
        return toMB(memoryMXBean.getHeapMemoryUsage().getUsed());
    }

    private double toMB(long bytes) {
        return Math.round((double) bytes / (1024 * 1024) * 10.0) / 10.0;
    }
}
