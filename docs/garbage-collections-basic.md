
# Garbage Collection Basics – How JVM Reclaims Memory

Garbage Collection (GC) is the mechanism by which the JVM **automatically frees heap memory**
by removing objects that are no longer needed.

Understanding GC basics is essential because:
- Every Java application depends on GC
- Performance issues are often GC‑related
- Interviews almost always test GC understanding

---

## What Is Garbage Collection?

> **Garbage Collection is the process by which the JVM automatically identifies and removes objects that are no longer reachable.**

Key point:
✅ GC deals only with **heap memory**  
❌ GC does not manage stack memory  

---

## Why Garbage Collection Exists

In lower‑level languages:
- Developers allocate memory manually
- Developers must free memory manually

This leads to:
❌ Memory leaks  
❌ Dangling pointers  
❌ Crashes  

Java introduced GC to:
✅ Reduce developer error  
✅ Improve safety  
✅ Simplify memory management  

---

## Important GC Principle (Interview Critical)

> **An object becomes eligible for GC when it is unreachable, NOT when it goes out of scope.**

Example:
```java
obj = null;
````

This does NOT mean:

*   Object is immediately removed

It only means:
✅ Object is now *eligible* for GC

***

## What Makes an Object Reachable?

An object is considered **reachable** if:

*   It is referenced by a GC Root
*   It can be reached through a chain of references

### Common GC Roots

✅ Active thread stacks  
✅ Static variables  
✅ JNI references  
✅ Local method references

If an object is not reachable from any root:
➡️ It becomes garbage

***

## Stop‑the‑World (Very Important Concept)

Most GC events involve **Stop‑the‑World pauses**.

This means:
✅ All application threads are paused  
✅ JVM performs GC work  
✅ Threads resume afterward

Interview insight:

> **Even short Stop‑the‑World pauses can impact latency‑sensitive systems.**

***

## Types of Garbage Collection

At a high level, JVM performs **two major types of GC**:

***

## 1️⃣ Minor Garbage Collection

> **Minor GC cleans the Young Generation only.**

### Trigger

✅ Eden space becomes full

### Scope

✅ Eden  
✅ Survivor spaces (S0 & S1)

### Characteristics

✅ Frequent  
✅ Fast  
✅ Short pause

Most Java applications experience **many Minor GCs**.

***

## What Happens During Minor GC

1.  Application threads pause
2.  Live objects copied from Eden to Survivor
3.  Dead objects discarded
4.  Eden cleared

Important:
✅ Uses copying algorithm  
✅ No memory fragmentation

***

## 2️⃣ Full (Major) Garbage Collection

> **Full GC cleans the Old Generation, and often the entire heap.**

### Trigger

✅ Old Generation becomes full  
✅ Promotion failure  
✅ Explicit GC call (discouraged)

### Characteristics

❌ Infrequent  
❌ Slower  
❌ Long pause

***

## What Happens During Full GC

1.  Application threads pause
2.  Old Generation is scanned
3.  Live objects retained
4.  Dead objects reclaimed
5.  Memory compacted (depending on collector)

Full GC is **expensive and dangerous** if frequent.

***

## Minor GC vs Full GC (Interview Table)

| Aspect             | Minor GC   | Full GC                     |
| ------------------ | ---------- | --------------------------- |
| Cleans             | Young Gen  | Old Gen (often entire heap) |
| Frequency          | High       | Low                         |
| Pause time         | Short      | Long                        |
| Performance impact | Low        | High                        |
| Risk               | Acceptable | Critical                    |

Golden interview line:

> **Minor GC is normal. Frequent Full GC is a red flag.**

***

## Garbage Collection Is Not Deterministic

Important to understand:

✅ JVM decides **when** to run GC  
✅ JVM decides **how much** to clean  
❌ Developers cannot force precise GC timing

Calling:

```java
System.gc();
```

Is only a **request**, not a command.

***

## Common GC Myths (Interview Traps)

❌ “GC runs when memory is full”  
✅ GC may run before memory is full

❌ “Objects are deleted immediately”  
✅ Objects are removed when GC runs

❌ “GC solves all memory problems”  
✅ Poor object design can still cause memory leaks

***

## How GC Impacts Performance

GC affects:

*   Latency (pause times)
*   Throughput (application work vs GC work)
*   Memory footprint

Good GC behavior:
✅ Many fast Minor GCs  
✅ Few Full GCs

Bad GC behavior:
❌ Frequent Full GCs  
❌ Long Stop‑the‑World pauses

***

## Interview‑Ready Explanation (Use This)

> “Garbage Collection automatically reclaims heap memory by removing unreachable objects. Minor GC cleans the Young Generation and is fast and frequent, while Full GC cleans the Old Generation and is expensive. GC involves stop‑the‑world pauses, so tuning aims to minimize Full GC.”

✅ Clear  
✅ Accurate  
✅ Senior‑level

***

## Key Takeaways

✅ GC manages heap memory automatically  
✅ Reachability defines garbage  
✅ Minor GC cleans Young Gen  
✅ Full GC cleans Old Gen  
✅ Stop‑the‑World pauses impact latency

> **Understanding GC is about understanding object lifetime and heap structure.**

***

## What’s Next (Phase 6)

Now that you understand **what GC is**,
the next step is understanding **how different GC algorithms work**.

➡️ **GC Algorithms Overview**

*   Serial
*   Parallel
*   CMS
*   G1
*   (High‑level comparison)
