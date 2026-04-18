
# Old Generation – Where Long‑Lived Objects Reside

The **Old Generation** (also called **Tenured Generation**) is the most critical
area of heap memory when it comes to:
- Performance problems
- Long GC pauses
- Memory leaks
- OutOfMemoryErrors

If Young Generation explains **object creation and short life**,  
Old Generation explains **object survival and longevity**.

---

## What Is Old Generation?

> **Old Generation is the part of heap memory where long‑lived objects are stored.**

Objects are moved here after they:
- Survive multiple Minor GCs
- Reach a certain age (tenuring threshold)
- Or cannot fit in Survivor spaces

---

## Why Old Generation Exists

A key JVM design assumption:

> **Some objects live far longer than others.**

Examples of long‑lived objects:
- Cache entries
- Static collections
- Application state
- Singleton objects
- Session data
- Configuration objects

Keeping these in Young Generation would:
❌ Trigger frequent GC  
❌ Hurt performance  

So the JVM promotes them to Old Generation.

---

## Where Old Generation Fits in Heap

High‑level heap structure recap:

```

Heap Memory
├── Young Generation
│    ├── Eden
│    ├── Survivor S0
│    └── Survivor S1
└── Old Generation

```

Old Generation usually occupies **most of the heap**.

---

## Object Promotion: Young → Old

### When Objects Are Promoted

Objects move to Old Generation when:
✅ They survive enough Minor GCs  
✅ Survivor space becomes full  
✅ Object size exceeds Survivor capacity  

This process is called:
> **Promotion**

---

### Example Promotion Flow

```

Eden → S0 → S1 → Old Generation

```

Most objects:
❌ Die in Eden  
❌ Never reach Old Gen  

Only a small percentage get promoted.

---

## Old Generation Garbage Collection

### What Type of GC Cleans Old Gen?

Old Generation is cleaned by:
> **Major GC / Full GC**

Full GC usually:
- Scans Old Generation
- May scan entire heap
- Is more expensive than Minor GC

---

## Why Full GC Is Expensive

Unlike Minor GC:
- Old Gen is large
- Object graph is complex
- References span across heap

Full GC:
❌ Takes more time  
❌ Causes longer pause  
❌ Stops all application threads  

This is why:
> **Frequent Full GC is a serious problem.**

---

## Stop‑the‑World in Old Generation

Important interview question:

> **Does Full GC stop the world?**

✅ Yes

All application threads pause during Full GC
(unless special collectors are used).

---

## Old Generation and Memory Leaks

### What Is a Memory Leak in Java?

> **A memory leak happens when objects that are no longer needed are still reachable and cannot be garbage‑collected.**

Most memory leaks occur in **Old Generation**.

---

### Common Causes of Old Gen Memory Leaks

❌ Static collections growing endlessly  
❌ Caches without eviction policies  
❌ Listeners not removed  
❌ Holding references unintentionally  

Result:
✅ Old Gen keeps filling  
✅ GC frequency increases  
✅ Eventually → OutOfMemoryError  

---

## OutOfMemoryError: Old Generation

Classic error:
```

java.lang.OutOfMemoryError: Java heap space

```

This usually means:
- Old Generation is full
- GC cannot reclaim enough memory
- Application cannot allocate new objects

---

## Interview‑Ready Insight

> **Young Gen issues cause performance degradation.  
Old Gen issues cause application failure.**

This sentence alone shows maturity.

---

## Old Generation vs Young Generation (Interview Table)

| Aspect | Young Generation | Old Generation |
|----|----|----|
| Object lifetime | Short‑lived | Long‑lived |
| GC type | Minor GC | Full / Major GC |
| Frequency | Frequent | Rare |
| Performance impact | Low | High |
| Risk | Low | Critical |

---

## How to Keep Old Generation Healthy

✅ Avoid unnecessary object promotion  
✅ Limit long‑lived object growth  
✅ Use proper cache eviction  
✅ Avoid static references  

Good Young Gen behavior protects Old Gen.

---

## Common Interview Questions

### ❓ Are objects always promoted to Old Gen?
❌ No  
Most objects die before promotion.

---

### ❓ Can objects go directly to Old Gen?
✅ Yes  
Large objects may skip Survivor spaces.

---

### ❓ Is Old Gen cleaned during Minor GC?
❌ No  
Minor GC focuses mainly on Young Generation.

---

## Key Takeaways

✅ Old Gen stores long‑lived objects  
✅ Object promotion happens automatically  
✅ Full GC cleans Old Gen  
✅ Memory leaks usually live here  
✅ Old Gen health determines stability  

> **Young Generation affects speed.  
Old Generation affects survival.**

---

## What’s Next (Phase 4)

Now that you understand where objects live long‑term,
the next step is understanding the **complete object lifecycle**.

➡️ Object creation → aging → promotion → collection
