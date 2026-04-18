# Garbage Collection Algorithms – How JVM Chooses to Clean Memory

Garbage Collection is not a single algorithm.
The JVM provides **multiple GC algorithms**, each designed for different goals:

- Throughput
- Low latency
- Large heap sizes
- Predictable pause times

Understanding GC algorithms is about knowing **trade‑offs**, not memorizing flags.

---

## Why Multiple GC Algorithms Exist

Different applications have different needs:

- Batch processing → High throughput
- Web services → Low latency
- Real‑time systems → Predictable pauses
- Large heaps → Scalable collectors

No single GC algorithm is best for all cases.

---

## High‑Level Categories of GC Algorithms

JVM GC algorithms can be grouped into:

✅ **Simple / Stop‑the‑World Collectors**  
✅ **Parallel Collectors**  
✅ **Concurrent / Low‑latency Collectors**  

We’ll go through the most important ones **conceptually**.

---

## 1️⃣ Serial Garbage Collector

### What Is Serial GC?

> **Serial GC uses a single thread to perform garbage collection.**

Key characteristics:
- Single‑threaded
- Stop‑the‑World
- Very simple

---

### How It Works

- Minor GC → single thread
- Full GC → single thread
- Application threads paused completely

---

### When Serial GC Is Used

✅ Small heap sizes  
✅ Simple applications  
✅ Client‑side or test environments  

---

### Interview Perspective

❌ Not used in modern server applications  
✅ Good for understanding GC basics  

---

## 2️⃣ Parallel Garbage Collector (Throughput GC)

### What Is Parallel GC?

> **Parallel GC uses multiple threads to perform garbage collection.**

Goal:
✅ Maximize throughput (application work vs GC work)

---

### How It Works

- Minor GC → parallel threads
- Full GC → parallel threads
- Still Stop‑the‑World

---

### Characteristics

✅ Faster than Serial GC  
✅ Efficient for CPU‑heavy workloads  
❌ Long pause times possible  

---

### When to Use

✅ Batch jobs  
✅ Background processing  
✅ Throughput‑focused systems  

---

### Interview Line ✅

> “Parallel GC improves throughput but does not focus on low latency.”

---

## 3️⃣ CMS (Concurrent Mark Sweep) – Deprecated but Important

### What Is CMS?

> **CMS is a low‑latency GC that aims to reduce long pause times by doing most work concurrently with application threads.**

---

### Key Idea

Instead of stopping the world for long periods:
✅ Do marking concurrently  
✅ Reduce pause times  

---

### CMS Characteristics

✅ Shorter pauses  
✅ Concurrent marking  
❌ Fragmentation issues  
❌ Complex tuning  

---

### Why CMS Was Popular

- Web applications
- Latency‑sensitive systems
- Interactive services

---

### Why CMS Is Deprecated

❌ High complexity  
❌ Fragmentation problems  
❌ Replaced by better alternatives  

Still important for interview understanding.

---

## 4️⃣ G1 (Garbage First) – Modern Default GC

### What Is G1 GC?

> **G1 is a server‑style GC designed for large heaps with predictable pause times.**

G1 is the **default GC** in modern Java versions.

---

### Key Design Idea

Instead of fixed Young/Old areas:
✅ Heap divided into **regions**
✅ G1 collects regions with most garbage first

---

### Why It’s Called “Garbage First”

G1 prioritizes:
✅ Regions with high garbage  
✅ Regions that give best cleanup value  

---

### How G1 Improves Pauses

- Region‑based collection
- Predictable pause goals
- Concurrent marking

---

### When G1 Is Used

✅ Large heap applications  
✅ Web services  
✅ Microservices  
✅ Balanced throughput & latency  

---

### Interview‑Ready Explanation ✅

> “G1 GC uses region‑based collection to provide predictable pause times while scaling well for large heaps.”

---

## 5️⃣ ZGC / Shenandoah (Ultra‑Low Latency – High Level)

### What Are They?

> **ZGC and Shenandoah are ultra‑low latency collectors that aim for extremely short GC pauses.**

---

### Key Characteristics

✅ Pause times in milliseconds  
✅ Mostly concurrent  
✅ Scales to very large heaps  

---

### Trade‑Off

❌ Slightly lower throughput  
❌ Complex internals  

---

### When Used

✅ Real‑time systems  
✅ Trading platforms  
✅ Latency‑critical systems  

You don’t need deep details for most interviews.

---

## Stop‑the‑World Across GC Algorithms

Important interview concept:

| Collector | Stop‑the‑World |
|----|----|
| Serial | Yes (long) |
| Parallel | Yes (shorter) |
| CMS | Short pauses |
| G1 | Predictable short pauses |
| ZGC | Very short pauses |

No GC completely eliminates Stop‑the‑World.

---

## Choosing the Right GC (Conceptual)

Interviewers expect **reasoning**, not flags.

| Requirement | GC Choice |
|----|----|
| Simple app | Serial |
| High throughput | Parallel |
| Low latency | G1 |
| Ultra‑low latency | ZGC / Shenandoah |

---

## Common Interview Mistakes

❌ “One GC is best for everything”  
❌ Over‑tuning without understanding  
❌ Ignoring pause time vs throughput trade‑off  

---

## Interview‑Ready Summary Answer ✅

> “JVM provides multiple GC algorithms optimized for different goals. Serial and Parallel focus on simplicity and throughput, while CMS and G1 reduce pause times. G1 is the modern default, balancing performance and latency for large heaps.”

This answer signals **strong JVM understanding**.

---

## Key Takeaways

✅ GC algorithm choice is trade‑off‑based  
✅ Throughput vs latency must be balanced  
✅ G1 is modern and practical  
✅ Older collectors still matter conceptually  

> **GC tuning starts with understanding behavior, not flags.**

---

## What’s Next (Final Phase)

Now that you understand:
✅ Heap structure  
✅ Object lifecycle  
✅ Minor & Full GC  
✅ GC algorithms  

The final phase is:

➡️ **Tuning & Interview Cheat Sheet**
- Common pitfalls
- Memory leak patterns
- How to explain GC in interviews
- JVM warm‑up & allocation tips
