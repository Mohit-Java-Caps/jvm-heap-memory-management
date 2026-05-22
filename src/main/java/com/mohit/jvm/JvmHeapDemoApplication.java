package com.mohit.jvm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * JVM Heap Memory Management — Demo Application
 *
 * This Spring Boot application demonstrates real JVM heap behaviour:
 *   - Object allocation in Eden / Young Generation
 *   - GC promotion to Old Generation
 *   - Memory leak simulation and detection
 *   - Heap dump trigger via JMX / Actuator
 *   - GC logging and metrics
 *
 * Run with these JVM flags to observe GC in action:
 *   -Xms256m -Xmx512m
 *   -XX:+UseG1GC
 *   -XX:+PrintGCDetails
 *   -XX:+PrintGCDateStamps
 *   -Xlog:gc*:file=logs/gc.log:time,uptime,level,tags
 *   -XX:+HeapDumpOnOutOfMemoryError
 *   -XX:HeapDumpPath=./heapdump.hprof
 *
 * Author: Mohit Kumar — github.com/Mohit-Java-Caps
 */
@SpringBootApplication
public class JvmHeapDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(JvmHeapDemoApplication.class, args);
        System.out.println("✅ JVM Heap Demo running — visit http://localhost:8080/actuator/metrics");
    }
}
