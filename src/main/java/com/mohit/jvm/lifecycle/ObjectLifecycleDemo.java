package com.mohit.jvm.lifecycle;

import org.springframework.web.bind.annotation.*;
import java.lang.management.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ObjectLifecycleDemo — tracks and demonstrates the full object lifecycle in JVM heap.
 *
 * KEY CONCEPTS DEMONSTRATED:
 * ─────────────────────────────────────────────────────────────
 *  OBJECT LIFECYCLE:
 *
 *  [1] BIRTH        → new MyObject() → allocated in Eden Space
 *  [2] FIRST MINOR GC → Eden fills → Minor GC fires
 *                       Live objects copied to Survivor S0, age = 1
 *  [3] SUBSEQUENT GCs → Survivors alternate between S0 ↔ S1
 *                       Age increments each cycle
 *  [4] TENURING      → Age >= MaxTenuringThreshold (default 15)
 *                       → Object promoted to Old Generation
 *  [5] FULL GC       → Old Gen fills → Full GC (expensive!)
 *                       Stop-the-world. All threads paused.
 *  [6] DEATH         → No more references → eligible for collection
 *                       GC reclaims memory
 *
 *  WEAK REFERENCES   → Do not prevent GC collection
 *  SOFT REFERENCES   → Collected only when JVM needs memory (good for caches)
 *  PHANTOM REFERENCES → Post-finalization hooks
 * ─────────────────────────────────────────────────────────────
 *
 *  PREMATURE PROMOTION (avoid this!):
 *  Happens when Survivor spaces are too small to hold all survivors.
 *  Objects get promoted to Old Gen too early → Old Gen fills faster
 *  → more frequent Full GC → higher latency.
 *  Fix: -XX:SurvivorRatio=N (default 8, meaning Eden:Survivor = 8:1:1)
 *
 * Try it:
 *   GET /lifecycle/simulate    → simulates object promotion across generations
 *   GET /lifecycle/references  → demonstrates weak vs soft reference behaviour
 *   GET /lifecycle/interview   → top interview Q&A on object lifecycle
 */
@RestController
@RequestMapping("/lifecycle")
public class ObjectLifecycleDemo {

    private static final AtomicLong objectsCreated = new AtomicLong(0);

    /**
     * Simulates the object lifecycle — creation, survival, promotion.
     * Shows live before/after stats for each generation.
     */
    @GetMapping("/simulate")
    public Map<String, Object> simulateLifecycle() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Phase 1: Birth in Eden
        result.put("phase_1_birth", Map.of(
            "action",    "new byte[1024] × 1000 → allocated in Eden Space",
            "heap_mb",   heapUsedMB(),
            "note",      "All new objects start here. Eden fills fast."
        ));

        // Allocate short-lived objects (simulates request DTOs, temp strings, etc.)
        List<byte[]> shortLived = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            shortLived.add(new byte[1024]);
            objectsCreated.incrementAndGet();
        }

        result.put("phase_2_eden_full", Map.of(
            "heap_mb",  heapUsedMB(),
            "objects",  shortLived.size(),
            "note",     "Eden is filling. Minor GC will fire soon."
        ));

        // Phase 3: Release short-lived (they die before GC)
        shortLived.clear(); // Dereference — eligible for collection
        System.gc();

        result.put("phase_3_minor_gc", Map.of(
            "heap_mb",   heapUsedMB(),
            "note",      "Short-lived objects collected. Long-lived survivors move to Survivor space.",
            "gc_tip",    "Most Eden objects never reach Survivor space — they die in Minor GC. " +
                         "This is by design: generational hypothesis."
        ));

        // Phase 4: Long-lived objects — will survive GC and eventually be promoted
        List<byte[]> longLived = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            longLived.add(new byte[4096]); // 4 KB each = 800 KB total
        }

        result.put("phase_4_promotion_candidate", Map.of(
            "heap_mb",             heapUsedMB(),
            "long_lived_objects",  longLived.size(),
            "approx_size_kb",      longLived.size() * 4,
            "note",                "These objects survive GCs. After ~15 cycles: promoted to Old Gen.",
            "tuning_flag",         "-XX:MaxTenuringThreshold=15 (default) — tune based on object lifetime"
        ));

        result.put("total_objects_created_session", objectsCreated.get());
        result.put("generational_hypothesis",
            "Most objects die young. JVM is optimised for this. " +
            "Your job as engineer: ensure short-lived objects ARE short-lived " +
            "(don't cache them in static fields).");

        longLived.clear(); // cleanup
        return result;
    }

    /**
     * Demonstrates weak vs soft references — crucial for cache implementation.
     *
     * INTERVIEW QUESTION: "What is the difference between WeakReference and SoftReference?"
     *
     * STRONG reference: MyObject obj = new MyObject();
     *   → GC will NEVER collect this. Causes leaks if held too long.
     *
     * WEAK reference: WeakReference<MyObject> ref = new WeakReference<>(obj);
     *   → GC collects at NEXT GC cycle (even if memory available).
     *   → Use case: canonical maps, WeakHashMap, listener registries.
     *
     * SOFT reference: SoftReference<MyObject> ref = new SoftReference<>(obj);
     *   → GC collects ONLY when JVM needs memory (before OutOfMemoryError).
     *   → Use case: in-memory caches (perfect for image caches, result caches).
     */
    @GetMapping("/references")
    public Map<String, Object> referenceTypes() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Strong reference — stays alive as long as we hold it
        byte[] strongRef = new byte[1024 * 100]; // 100 KB
        result.put("strong_reference", Map.of(
            "size_kb",    100,
            "behaviour",  "Never collected while variable is in scope",
            "use_case",   "Normal object usage — the default",
            "risk",       "Holding strong refs in static fields / caches = memory leak"
        ));

        // Weak reference — will be collected at next GC
        java.lang.ref.WeakReference<byte[]> weakRef =
                new java.lang.ref.WeakReference<>(new byte[1024 * 50]); // 50 KB
        result.put("weak_reference_before_gc", Map.of(
            "is_null",    weakRef.get() == null,
            "size_kb",    weakRef.get() != null ? 50 : 0,
            "behaviour",  "Collected at next GC regardless of memory availability",
            "use_case",   "WeakHashMap, listener/observer patterns, intern pools"
        ));

        System.gc(); // Request GC — weak ref should be cleared

        result.put("weak_reference_after_gc", Map.of(
            "is_null",   weakRef.get() == null,
            "lesson",    weakRef.get() == null
                ? "Weak reference was cleared by GC — this is expected behaviour"
                : "Weak reference survived this GC cycle — JVM may clear it next cycle"
        ));

        // Soft reference — collected only under memory pressure
        java.lang.ref.SoftReference<byte[]> softRef =
                new java.lang.ref.SoftReference<>(new byte[1024 * 50]); // 50 KB
        System.gc();

        result.put("soft_reference_after_gc", Map.of(
            "is_null",   softRef.get() == null,
            "lesson",    softRef.get() == null
                ? "Soft ref was cleared — JVM was under memory pressure"
                : "Soft ref SURVIVED GC — JVM had enough memory. This is correct behaviour for caches.",
            "use_case",  "In-memory caches: SoftReference lets JVM reclaim cache memory before OOM"
        ));

        result.put("interview_summary", Map.of(
            "Strong", "obj = new Obj()         → Never collected while referenced",
            "Soft",   "new SoftReference(obj)  → Collected before OutOfMemoryError",
            "Weak",   "new WeakReference(obj)  → Collected at next GC cycle",
            "Phantom","new PhantomReference()" +
                      " → Post-finalization cleanup hooks"
        ));

        strongRef = null; // cleanup
        return result;
    }

    /**
     * Interview Q&A — most common object lifecycle questions.
     * This endpoint alone is interview prep gold.
     */
    @GetMapping("/interview")
    public Map<String, Object> interviewQA() {
        Map<String, Object> qa = new LinkedHashMap<>();

        qa.put("Q1_where_do_objects_start", Map.of(
            "question", "Where is a new object allocated?",
            "answer",   "Eden Space (part of Young Generation). Exception: objects larger than " +
                        "-XX:PretenureSizeThreshold go directly to Old Gen (Humongous in G1GC)."
        ));
        qa.put("Q2_what_triggers_minor_gc", Map.of(
            "question", "What triggers a Minor GC?",
            "answer",   "Eden Space fills up. JVM copies live objects to Survivor space (S0 or S1), " +
                        "increments age, and clears Eden. This is fast — typically < 10ms."
        ));
        qa.put("Q3_when_promoted", Map.of(
            "question", "When is an object promoted to Old Generation?",
            "answer",   "When its age (number of Minor GCs survived) reaches MaxTenuringThreshold " +
                        "(default 15). Also when Survivor space is full → premature promotion."
        ));
        qa.put("Q4_what_is_full_gc", Map.of(
            "question", "What is a Full GC and why is it bad?",
            "answer",   "Full GC collects both Young Gen and Old Gen. It is stop-the-world: " +
                        "ALL application threads pause. Duration: 100ms to several seconds. " +
                        "Frequent Full GC = memory pressure. Fix: increase heap, tune GC, fix leaks."
        ));
        qa.put("Q5_what_is_stop_the_world", Map.of(
            "question", "What is stop-the-world (STW)?",
            "answer",   "All application threads are paused while GC runs. Users experience latency spike. " +
                        "Modern GCs (G1, ZGC) minimize STW by doing most work concurrently."
        ));
        qa.put("Q6_oom_error", Map.of(
            "question", "What causes OutOfMemoryError: Java heap space?",
            "answer",   "1. Heap too small for workload (-Xmx). " +
                        "2. Memory leak: objects referenced but never released. " +
                        "3. Massive object allocation spike. " +
                        "Debug: -XX:+HeapDumpOnOutOfMemoryError then analyze with Eclipse MAT."
        ));

        return qa;
    }

    private double heapUsedMB() {
        Runtime rt = Runtime.getRuntime();
        return Math.round((double)(rt.totalMemory() - rt.freeMemory()) / 1024 / 1024 * 10.0) / 10.0;
    }
}
