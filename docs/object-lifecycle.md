
# JVM Object Lifecycle – From Creation to Collection

The **Object Lifecycle** explains how a Java object:
- Is created
- Moves through heap memory
- Ages over time
- And is eventually garbage collected

Understanding this lifecycle allows you to:
✅ Predict GC behavior  
✅ Diagnose memory leaks  
✅ Explain JVM internals confidently in interviews  

---

## What Is Object Lifecycle?

> **Object lifecycle is the journey of a Java object from allocation in heap memory to its eventual garbage collection.**

This lifecycle is **fully managed by the JVM**, but deeply influenced by:
- Object lifetime
- Reference patterns
- Heap configuration
- Garbage Collection behavior

---

## High‑Level Lifecycle Phases

Every Java object goes through these phases:

```

Creation → Young Generation → Aging → Promotion → Old Generation → Collection

````

Most objects **never complete this full journey**.

---

## Phase 1: Object Creation

An object is created when you use:

```java
new MyObject();
````

Internally:
✅ Memory is allocated in **Eden Space**
✅ Constructor is executed
✅ Reference points to Eden

Object creation is:

*   Extremely fast
*   Optimized by JVM
*   Done via pointer bumping in most cases

***

## Phase 2: Object Lives in Eden

After creation:

*   Object resides in **Eden Space**
*   It is considered **young**
*   No GC has happened yet

Important insight:

> **Eden is designed to fill up quickly.**

This is intentional.

***

## Phase 3: Minor GC Triggered

When Eden is full:
✅ **Minor GC** is triggered

During Minor GC:

*   Application threads pause (Stop‑the‑World)
*   Eden is scanned
*   Live objects are copied out
*   Dead objects are discarded

Most objects:
❌ Die here  
❌ Never leave Eden

***

## Phase 4: Survivor Spaces (S0 & S1)

Objects that **survive Minor GC** move to Survivor space.

Key facts:

*   There are two survivor spaces: **S0 and S1**
*   Only one survivor space is active at a time
*   Objects move back and forth between them

This movement:
✅ Tracks object age  
✅ Prevents premature promotion

***

## Phase 5: Object Aging

Each surviving object has an **age counter**.

*   Age starts at 1 after first Minor GC
*   Increments with each GC survived
*   JVM tracks this automatically

This age is used to decide:

> *Should this object stay young, or be promoted?*

***

## Phase 6: Promotion to Old Generation

Objects are promoted to Old Generation when:
✅ Age exceeds threshold  
✅ Survivor space is full  
✅ Object is too large

This movement is called:

> **Promotion (Tenuring)**

Once promoted:

*   Object is considered **long‑lived**
*   Minor GC no longer scans it

***

## Phase 7: Object Lives in Old Generation

Objects in Old Gen:
✅ Are expected to live long  
✅ Are scanned during Full GC  
✅ Are expensive to clean

Typical Old Gen objects:

*   Caches
*   Static collections
*   Session data
*   Framework internals

***

## Phase 8: Full GC (Major GC)

When Old Gen fills up:
✅ **Full GC is triggered**

Full GC:

*   Scans Old Generation
*   May scan entire heap
*   Is usually Stop‑the‑World
*   Has longer pause times

This is the **most expensive GC phase**.

***

## Phase 9: Object Becomes Unreachable

Important concept:

> **An object is eligible for GC only when it becomes unreachable, not when it goes out of scope.**

Unreachable means:

*   No live references exist
*   Object cannot be accessed by any thread

***

## Phase 10: Garbage Collection

When GC runs:
✅ JVM reclaims memory
✅ Object memory becomes free
✅ Memory reused for new objects

There is:
❌ No guarantee *when* GC runs  
❌ No guarantee *when* memory is reclaimed

***

## Common Interview Misconception

❌ “Objects are deleted immediately”

Correct:
✅ JVM decides when to collect  
✅ GC is non‑deterministic  
✅ Reference loss ≠ immediate deletion

***

## A Typical Object Lifecycle Example

    new Object()        → Eden
    Minor GC            → S0 (age 1)
    Minor GC            → S1 (age 2)
    Minor GC (age > N)  → Old Gen
    Full GC             → Collected

In reality:
✅ Most objects disappear after first Minor GC

***

## Why Understanding Object Lifecycle Matters

If you don’t understand lifecycle:
❌ GC tuning feels random  
❌ Memory leaks are confusing  
❌ Performance issues are guesswork

If you do:
✅ You reason logically  
✅ You debug confidently  
✅ You sound senior in interviews

***

## Interview‑Ready Summary (Use This)

> “Objects are created in Eden, survivors move to survivor spaces, age across Minor GCs, and are promoted to Old Generation once they exceed the tenuring threshold. Old Gen objects are cleaned via Full GC.”

This answer is **clear, compact, and professional**.

***

## Key Takeaways

✅ Object lifecycle is generational  
✅ Most objects die young  
✅ Survivor spaces track aging  
✅ Promotion leads to Old Gen  
✅ Full GC is expensive

> **Understanding object lifecycle is understanding JVM behavior itself.**

***

## What’s Next (Phase 5)

Now that you know **how objects move**, the next step is understanding **how garbage collection actually works**.

➡️ **Garbage Collection Basics**

*   Minor GC vs Full GC
*   Stop‑the‑World
*   When GC is triggered
