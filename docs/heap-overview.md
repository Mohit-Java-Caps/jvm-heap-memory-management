# JVM Heap Memory – Complete Overview

Heap memory is one of the **most important concepts** for any Java developer.
Understanding it deeply separates **framework users** from **real backend engineers**.

Almost every performance issue, memory leak, or GC problem is directly related to **heap management**.

---

## What Is Heap Memory?

> **Heap memory is the runtime memory area where Java objects are allocated and managed by the JVM.**

Whenever you do:
```java
new Object();
````

That object is created **inside the heap**.

***

## Why Heap Memory Matters

Heap memory directly impacts:

*   Application performance
*   Garbage Collection behavior
*   Latency and throughput
*   OutOfMemoryErrors
*   System stability

In interviews:

> *If you understand heap memory clearly, you can reason about GC, performance, and memory leaks.*

***

## JVM Memory Model (Big Picture)

The JVM memory is broadly divided into:

    JVM Memory
    ├── Heap Memory
    │   ├── Young Generation
    │   └── Old Generation
    └── Non-Heap Memory

This repository focuses **primarily on Heap Memory**.

***

## Why the Heap Is Divided into Generations

A very important principle in JVM design:

> **Most objects die young.**

Example:

*   Temporary Strings
*   Loop variables
*   Request objects
*   DTOs

To optimize this behavior, the JVM splits the heap into **generations**.

***

## High-Level Heap Structure

    Heap Memory
    ├── Young Generation
    │   ├── Eden Space
    │   ├── Survivor Space S0
    │   └── Survivor Space S1
    └── Old Generation

Each part exists **for performance reasons**, not randomly.

***

## Young Generation (High Level)

The Young Generation is where:

*   New objects are created
*   Short‑lived objects live
*   Frequent Garbage Collection happens

It is optimized for:
✅ Fast allocation  
✅ Fast cleanup

***

## Old Generation (High Level)

The Old (Tenured) Generation stores:

*   Long‑lived objects
*   Objects that survived multiple GC cycles

It is optimized for:
✅ Stability  
✅ Fewer GCs  
✅ Larger object retention

***

## How Objects Move in the Heap (Very High Level)

1.  Object created in **Eden**
2.  Minor GC occurs
3.  Surviving objects move to **S0 or S1**
4.  After surviving multiple GCs → promoted to **Old Gen**

This movement is **fully automatic**.

***

## Why Developers Should Care

Even though JVM handles this automatically:
❌ Poor object creation patterns cause performance issues  
❌ Memory leaks hurt Old Generation  
❌ Wrong GC tuning causes latency spikes

Understanding heap memory helps you:
✅ Write better code  
✅ Diagnose memory issues  
✅ Answer JVM interview questions confidently

***

## Common Interview Question

> **“Is heap memory shared between threads?”**

✅ Yes  
Heap memory is shared across all threads of the same JVM.

Stack memory is **thread‑specific**, heap is **shared**.

***

## Key Takeaways

✅ Heap stores all Java objects  
✅ Heap is generational for performance  
✅ Young Gen handles short‑lived objects  
✅ Old Gen handles long‑lived objects  
✅ Garbage Collection manages heap automatically

> **Understanding heap memory is the foundation for understanding Garbage Collection.**

***

## What’s Next

Now that the foundation is clear, we will go deeper into:

➡️ **Young Generation**

*   Eden Space
*   Survivor Space S0 & S1
*   Minor GC
*   Object aging
