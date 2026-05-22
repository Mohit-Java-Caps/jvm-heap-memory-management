package com.mohit.jvm.tuning;

import org.springframework.web.bind.annotation.*;
import java.lang.management.*;
import java.util.*;

/**
 * GCTuningGuide — exposes JVM tuning flags with real-world context.
 *
 * KEY CONCEPTS DEMONSTRATED:
 * ─────────────────────────────────────────────────────────────
 *  TUNING PRINCIPLE: Measure first, tune second.
 *  Never guess. Use GC logs + heap profiler to diagnose before tuning.
 *
 *  THREE TUNING GOALS (pick 2 — you can't have all 3):
 *  ┌─────────────────┬──────────────┬───────────────────┐
 *  │  Low Latency    │  Throughput  │  Small Footprint  │
 *  ├─────────────────┼──────────────┼───────────────────┤
 *  │  ZGC/Shenandoah │  Parallel GC │  Serial GC        │
 *  │  G1 (tuned)     │  G1 (default)│  -Xmx small       │
 *  └─────────────────┴──────────────┴───────────────────┘
 *
 * Try it:
 *   GET /tuning/flags           → all JVM flags with explanations
 *   GET /tuning/recommended     → recommended flags for Spring Boot apps
 *   GET /tuning/current-runtime → JVM flags active in THIS JVM
 */
@RestController
@RequestMapping("/tuning")
public class GCTuningGuide {

    /**
     * Complete JVM tuning flag reference.
     * Bookmark this for interviews.
     */
    @GetMapping("/flags")
    public Map<String, Object> tuningFlags() {
        Map<String, Object> guide = new LinkedHashMap<>();

        guide.put("heap_sizing", Map.of(
            "-Xms",  "Initial heap size. Set equal to -Xmx to avoid heap resizing overhead. " +
                     "Example: -Xms512m",
            "-Xmx",  "Maximum heap size. Rule of thumb: 75% of available RAM for Java process. " +
                     "Example: -Xmx2g",
            "-Xmn",  "Young Generation size. Larger = more room in Eden = fewer Minor GCs. " +
                     "G1GC manages this automatically — don't set with G1.",
            "tip",   "Always set -Xms == -Xmx in production. Prevents heap growth pauses."
        ));

        guide.put("gc_algorithm_selection", Map.of(
            "-XX:+UseG1GC",           "G1 GC (default Java 9+). Balanced. Recommended for most apps.",
            "-XX:+UseZGC",            "ZGC. Sub-millisecond pauses. Java 15+ production ready.",
            "-XX:+UseShenandoahGC",   "Shenandoah. Like ZGC. OpenJDK builds.",
            "-XX:+UseParallelGC",     "Parallel GC. Max throughput. High pause times. Batch jobs only.",
            "-XX:+UseSerialGC",       "Serial GC. Single-threaded. Dev/embedded only. Never production server."
        ));

        guide.put("g1gc_tuning", Map.of(
            "-XX:MaxGCPauseMillis=200",  "Target max pause time (default 200ms). Lower = more frequent GC.",
            "-XX:G1HeapRegionSize=16m",  "Region size 1-32MB. Larger heap = larger regions. Usually auto.",
            "-XX:InitiatingHeapOccupancyPercent=45",
                                         "Old Gen % that triggers concurrent marking (default 45%). " +
                                         "Lower = more frequent concurrent cycles but avoids Full GC.",
            "-XX:G1NewSizePercent=5",    "Min Young Gen size as % of heap.",
            "-XX:G1MaxNewSizePercent=60","Max Young Gen size as % of heap.",
            "humongous_objects",         "Objects > 50% of region size go directly to Old Gen. " +
                                         "Avoid large object allocation in hot paths."
        ));

        guide.put("gc_logging", Map.of(
            "Java 9+",  "-Xlog:gc*:file=logs/gc.log:time,uptime,level,tags:filecount=10,filesize=20m",
            "Java 8",   "-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log",
            "tip",      "Always enable GC logging in production. It costs <1% CPU and saves hours of debugging."
        ));

        guide.put("heap_dump_on_oom", Map.of(
            "-XX:+HeapDumpOnOutOfMemoryError",  "Auto-dump heap when OOM occurs. Essential in production.",
            "-XX:HeapDumpPath=./heapdump.hprof","Dump file location.",
            "analysis_tool",                    "Eclipse MAT (Memory Analyzer Tool) — free, powerful.",
            "key_views",                        "Dominator Tree (what's holding most memory), " +
                                                "OQL (Object Query Language), Leak Suspects Report"
        ));

        guide.put("metaspace_tuning", Map.of(
            "-XX:MetaspaceSize=256m",      "Initial metaspace size. Avoids frequent metaspace GC at startup.",
            "-XX:MaxMetaspaceSize=512m",   "Cap metaspace. Without this, it grows unbounded. " +
                                           "OOM in Metaspace = too many class definitions (common with " +
                                           "reflection, bytecode generation, CGLIB proxies in Spring).",
            "java_8_note",                 "PermGen was replaced by Metaspace in Java 8. " +
                                           "PermGen was fixed-size and caused PermGen OOM errors."
        ));

        guide.put("thread_stack", Map.of(
            "-Xss512k",   "Stack size per thread (default 512k-1m). Reduce for apps with many threads. " +
                          "StackOverflowError = infinite recursion, not a heap issue.",
            "note",       "Thread stacks are NOT part of heap. They are in native memory."
        ));

        return guide;
    }

    /**
     * Recommended JVM flags for a production Spring Boot application.
     * Copy-paste ready for your application startup scripts.
     */
    @GetMapping("/recommended")
    public Map<String, Object> recommendedFlags() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("spring_boot_production_flags", Map.of(
            "G1GC_balanced", List.of(
                "-Xms512m",
                "-Xmx1g",
                "-XX:+UseG1GC",
                "-XX:MaxGCPauseMillis=200",
                "-XX:+HeapDumpOnOutOfMemoryError",
                "-XX:HeapDumpPath=./heapdump.hprof",
                "-XX:MetaspaceSize=256m",
                "-XX:MaxMetaspaceSize=512m",
                "-Xlog:gc*:file=logs/gc.log:time,uptime,level,tags:filecount=10,filesize=20m"
            ),
            "ZGC_low_latency", List.of(
                "-Xms1g",
                "-Xmx2g",
                "-XX:+UseZGC",
                "-XX:+HeapDumpOnOutOfMemoryError",
                "-XX:HeapDumpPath=./heapdump.hprof",
                "-XX:MetaspaceSize=256m",
                "-XX:MaxMetaspaceSize=512m",
                "-Xlog:gc*:file=logs/gc.log:time,uptime,level,tags"
            )
        ));

        result.put("docker_container_note",
            "In containers: add -XX:+UseContainerSupport (default Java 10+). " +
            "JVM will respect container memory limits, not host memory. " +
            "Without this, JVM sees host RAM and over-allocates heap → OOM kill.");

        result.put("tuning_process", List.of(
            "1. Enable GC logging in production",
            "2. Run under realistic load (load test)",
            "3. Analyse GC log with GCViewer or GCEasy.io",
            "4. Identify: Minor GC frequency, Full GC frequency, pause duration",
            "5. Tune one flag at a time — measure after each change",
            "6. Target: no Full GC, Minor GC < 50ms, throughput > 95%"
        ));
        return result;
    }

    /**
     * Shows JVM arguments active in the current running JVM.
     */
    @GetMapping("/current-runtime")
    public Map<String, Object> currentRuntimeFlags() {
        RuntimeMXBean runtimeMX = ManagementFactory.getRuntimeMXBean();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jvm_name",    runtimeMX.getVmName());
        result.put("jvm_version", runtimeMX.getVmVersion());
        result.put("jvm_vendor",  runtimeMX.getVmVendor());
        result.put("active_jvm_args", runtimeMX.getInputArguments());
        result.put("uptime_seconds",
                   Math.round(runtimeMX.getUptime() / 1000.0));
        result.put("tip",
            "Add -XX:+UseG1GC -Xms256m -Xmx512m to your run config " +
            "and restart — then check here to confirm flags are active.");
        return result;
    }
}
