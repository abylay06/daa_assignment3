# Assignment 3 — Graph Algorithms (Java 17)

## Setup and commands

This submission uses Java 17-compatible code, `submission.json` contains `{"language":"Java"}`, and the repository's existing `Main.java`, `Input.java`, `Json.java` and `tools/` are unchanged. Run these from the **repository root**:

```bash
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input tests/my_cases.jsonl
python tests/check_my_cases.py

# Java direct route-helper tests (after --setup has compiled src/Java/*.java)
javac --release 17 -cp .build/Java -d .build/Java tests/RouteHelperTest.java
java -Xmx384m -cp .build/Java RouteHelperTest

# Full constraints and 100,000-deep traversal stress tests
python tests/stress_test.py

# Fair, repeated MST benchmark (compilation outside the timed region)
javac --release 17 -cp .build/Java -d .build/Java tests/Benchmark.java
java -Xmx384m -cp .build/Java Benchmark
```

Java 17 or later JDK and Python 3.12+ are required for the supplied launcher. All six methods preserve one-result-per-request JSON Lines with no diagnostic stdout. The `ping` task continues to work. **All algorithm state is local to each request**: arrays, adjacency lists, heaps, counters and hash sets are allocated afresh. Only stateless helper methods and the DSU class definition are shared. Two batches of public requests and repeated mixed custom requests check state independence.

## 1. DSU — path compression and union by size

- **Steps:** Initialize every vertex as its own parent with size 1. For each `[u,v]`, find their roots using path compression, attach the smaller component to the larger when roots differ, update the component count, and record `[components,largest]` after that operation.
- **Invariant:** `find(v)` identifies the component containing `v`, `size[root]` is its actual vertex count, and `components` is the number of distinct roots.
- **Correctness:** The two endpoints are connected after an operation; if their roots already match, connectivity cannot change. Otherwise the two and only two existing components merge, so the number of components drops by 1 and the new maximum size is the maximum of its old value and the merged component size.
- **Boundary cases:** Empty graph: no states. Self-unions and repeated unions do not alter the partition. Isolates begin with size 1.
- **Complexity:** `O(n + q α(n))` amortized time for `q` operations, `O(n+q)` memory including output.

## 2. Kruskal — minimum spanning forest

- **Steps:** Keep the original edge IDs, sort those IDs by increasing edge weight, and accept an edge exactly when DSU reports its endpoints are in different components. Add weights into a `long`.
- **Invariant:** Chosen edges form a forest; each chosen edge connects two distinct current trees. The DSU count includes untouched isolated vertices.
- **Correctness:** By the MST cut property, the next lowest eligible crossing edge is safe to add. Sorting and checking all edges therefore generates a minimum spanning tree in every input connected component; no self-loop or cyclic edge is chosen.
- **Boundary cases:** Zero vertices, isolates, negative weights, parallel edges, ties, loops, and sums larger than `Integer.MAX_VALUE`. The IDs refer to original positions, not sorted positions.
- **Complexity:** `O(n + m log(m+1))` time, `O(n+m)` memory.

## 3. Prim — separate minimum spanning forest implementation

- **Steps:** Build undirected adjacency lists storing original edge IDs. For each unvisited starting vertex, count a new component and add its incident edges to a min-priority queue. Pop the lightest edge and select it only if it reaches an unvisited vertex; mark this vertex and add its incident edges. Move to the next unvisited start only after the heap is empty.
- **Invariant:** The visited vertices in the current component are connected by exactly the selected edges, and queued edges expose candidates from visited vertices to other vertices. Entries ending at an already visited vertex are ignored.
- **Correctness:** The lowest-weight edge crossing the visited/unvisited cut is safe by the MST cut property. A finished heap means this connected component is completely spanned. Restarting reaches other components, including isolates.
- **Boundary cases:** Negative and parallel edges are supported; self-loops are never selected because their destination is already visited. Zero vertices and empty adjacency lists work.
- **Complexity:** `O(n + m log(m+1))` time with a lazy min-heap, `O(n+m)` memory. This is a separate adjacency/heap algorithm, not a call to Kruskal.

## 4. Kosaraju — strongly connected components

- **Steps:** Build the directed graph and its transpose. In the first pass, run DFS from **all** vertices, recording each vertex when its DFS finishes. In decreasing finishing order, start DFS on the transpose from each not-yet-labeled vertex and assign a new component label to all reached vertices. Finally, scan input edges and output each distinct cross-component directed label pair once.
- **Invariant:** In pass 1, each vertex is finished after all reachable unvisited successors have been explored. In pass 2, every labeled group is exactly one SCC and groups already labeled are never reentered. A hash set records precisely the inter-SCC pairs already emitted.
- **Correctness:** Decreasing finish time in the original graph chooses, among the unassigned condensation DAG nodes, an SCC that cannot reach another unassigned SCC in the transpose. The next DFS therefore collects that whole SCC and no different unassigned SCC. Every SCC eventually receives one label. Scanning all edges yields all and only condensation arcs; deduplication removes repeats.
- **Boundary cases:** Isolates each get their own label, loops remain inside an SCC, duplicates cause no extra condensation edge, and `n=0` yields two empty arrays.
- **Complexity:** `O(n+m)` expected time using hash-set deduplication; `O(n+m)` memory.

## 5. DFS topological sort

- **Steps:** Build directed adjacency lists and run three-state DFS from every vertex: 0=unseen, 1=active/on DFS path, 2=completed. Encountering an edge to state 1 means a cycle and immediately returns `acyclic=false` with an empty order. Otherwise append vertices on DFS completion and reverse the finishing order.
- **Invariant:** State-1 vertices form the current DFS ancestry, and each fully scanned state-2 vertex appears exactly once in finishing order. The per-vertex `next` index keeps the exact neighbor iteration position after returning from a child.
- **Correctness:** A directed DFS back edge to an active ancestor is equivalent to a directed cycle. If no such edge exists, every directed edge places its source before its target in reversed finishing order.
- **Boundary cases:** Empty graph is acyclic. Self-loops and cycles in disconnected components are detected; duplicate edges do not break ordering.
- **Complexity:** `O(n+m)` time and `O(n+m)` memory.

## 6. DAG shortest path

- **Steps:** Invoke whole-graph three-state topological sort **before** relaxing any edges. If any cycle exists anywhere, return `false,[],[]`. Otherwise set `distance[source]=0`, every other distance to `null`, and all `parent_edge` entries to -1. In topological order, relax outgoing edge IDs only from reachable vertices, using `long` arithmetic. On improvement, store the actual original input edge ID.
- **Invariant:** After a vertex is processed, its distance is the optimum among all paths from the source; each reachable non-source vertex's parent edge enters that vertex and represents a shortest-path relaxation. Topological order ensures all its predecessors were considered first.
- **Correctness:** Each source-to-vertex path in a DAG ends with an edge from a predecessor earlier in the ordering. Inductively, relaxing every incoming candidate after its predecessor's optimum is known yields the shortest path even with negative weights. No unreachable component can create fake distances because it is skipped. Cycle checking occurs on **all** components.
- **Boundary cases:** Negative weights and valid distance -1, parallel edges, tied routes, unreachable vertices, self-loop cycles, unreachable cycles, a one-vertex graph, and 64-bit totals. `distance` is a nullable `Long[]`; `parent_edge` stores edge IDs and is -1 for the source/unreachable vertices.
- **Complexity:** `O(n+m)` time and memory, including building adjacency and topological ordering.

### Route reconstruction

`Algorithms.reconstructRoute(int source, int target, int[][] edges, int[] parentEdge)` follows original parent edge IDs backward from target to source, collects the vertices, and reverses the list. If a parent ID is -1 before reaching the source, it returns an **empty list `[]`** (no route). The source itself yields `[source]`. For valid parent arrays generated by a DAG, following parents must terminate because the graph has no cycles. Complexity: `O(L)` time and output space for a route containing `L` vertices.

Actual direct calls from `tests/RouteHelperTest.java`, on the first example below:

```java
Algorithms.reconstructRoute(0, 3, edges, parentEdge); // [0,1,2,3]
Algorithms.reconstructRoute(0, 0, edges, parentEdge); // [0]
Algorithms.reconstructRoute(0, 4, edges, parentEdge); // []
```

For edges `[ [0,1,3], [0,2,8], [1,2,-7], [2,3,4] ]`, the selected original IDs for vertices 1, 2, 3 are `[0,2,3]`. Their directed path is `0 -> 1 -> 2 -> 3`, with weight `3 + (-7) + 4 = 0`, equal to `distance[3]`. Vertex 4 is unreachable.

The tied/parallel case uses edges `[[0,1,1],[0,1,1],[0,2,1],[1,3,2],[2,3,2]]`. In the recorded run `parent_edge=[-1,0,2,4]`: the shortest route to 3 is `[0,2,3]`, via original edge IDs `[2,4]`, with weight `1+2=3`; the parallel choices for target 1 are edge ID 0 or 1, each weight 1. The direct helper test accepts either tied shortest route and verifies edge directions, source and target, and total cost. **This helper is not a new JSON task.**

## Iterative DFS and request-state reset

DFS recursion can overflow Java's limited call stack on a 100,000-vertex chain. Both Kosaraju and topological sorting instead use `int[] stack`, and first-pass/topological DFS also uses a per-vertex `int[] next` pointer to resume where that vertex left off. Marking vertices when pushed prevents duplicate stack entries. This keeps `O(n+m)` complexity and supports the published deep limit.

No static mutable graph or DSU state survives between requests. The same `Main` JVM can process mixed JSON Lines one after another without cross-request contamination.

## Testing

The supplied tests were evaluated in the local reproduction of the assignment runner using `python tools/run.py --check`: **36/36 public cases passed**, also **72/72** when the full batch was sent twice to the same Java process. `python tests/check_my_cases.py` passed **30/30 custom requests**, with input/expected/actual/explanations stored in `tests/`.

`tests/check_my_cases.py` also verifies acceptance of alternative correct witnesses and rejection of incorrect ones for forests, SCC condensation, topo order and shortest-path parent edges; it compares **semantic validity** rather than assuming there is a single valid output array. `RouteHelperTest.java` directly calls the route helper and checks reachable, source, unreachable, tied and parallel cases. The generated `tests/stress_test.py` passed the full `n=100000` and `m/q=250000` limits on all six methods, including 100,000-deep SCC/topo/DAG chains. An additional independent randomized check against NetworkX 3.6.1 succeeded on 1,500 small requests; NetworkX is **not** a submission dependency.

The explicit results and rationale for individual test cases appear in [`tests/TESTS.md`](tests/TESTS.md). The large stress tests deliberately generate inputs on demand rather than committing huge static files.

## Kruskal versus Prim benchmarks

**Reproducible setup.** `tests/Benchmark.java` constructs the **same** connected undirected weighted graph for both algorithms at each size. Its generator inserts a chain to guarantee connectedness, then adds pseudorandom edges with `java.util.Random(seed)`; every weight is uniformly generated from integers -1000 through +1000. Seeds are `20261025 + n`, shown below. Both algorithms' returned forest size, absence of cycles and edge-ID reuse, component count, sum of selected weights, and agreement on the optimum cost are checked **outside** the timed region.

**Timing method.** Execute with `java -Xmx384m -cp .build/Java Benchmark`. Use `System.nanoTime()`, record five independent invocations per method/size, and report their median. Perform **three unrecorded warm-up calls for each method/size** before timing. Alternate which algorithm runs first across repetitions. Timing starts just before `Algorithms.kruskal(input)` or `Algorithms.prim(input)` and ends on method return: it **includes** `Input.edges` conversion, sorting, adjacency/heap construction and the algorithm, and **excludes** graph generation, JSON parse/serialization, process startup, testing the results and Java compilation. Both have exactly the same timed boundary.

**Recorded environment:** Linux x86-64, AMD EPYC 9V74 virtualized CPU, 4 processors visible to JVM, OpenJDK 21.0.12.1 compiling Java source with `--release 17`, max heap 384 MiB. These are measurements from the provided execution environment, not automatically from the student's computer. Times can vary across machines and JVM warm-up state.

| n | m | seed | Kruskal runs (ms) | median (ms) | Prim runs (ms) | median (ms) | verified optimum cost |
|---:|---:|---:|---|---:|---|---:|---:|
| 1,000 | 3,000 | 20262025 | 1.319, 1.467, 1.252, 2.634, 1.969 | 1.467 | 2.888, 3.052, 2.943, 2.170, 2.283 | 2.888 | -603639 |
| 5,000 | 15,000 | 20266025 | 10.355, 10.088, 7.658, 21.855, 12.208 | 10.355 | 12.909, 13.899, 12.909, 12.724, 19.702 | 12.909 | -3111703 |
| 20,000 | 60,000 | 20281025 | 18.375, 17.733, 17.752, 18.672, 18.638 | 18.375 | 68.044, 78.106, 65.602, 77.979, 64.014 | 68.044 | -12347325 |

**Observations:** In these runs, Kruskal was faster at all three sizes. The lazy Prim heap stores multiple candidate entries (including edges later discarded); its constant costs are larger on these relatively sparse random graphs. Median timings show some variation due to GC, JIT and shared/virtualized CPU load. These data do **not** prove that Kruskal is universally faster; density, graph shape, priority-queue implementation, hardware and benchmarking boundary influence measured performance. Asymptotically both algorithms take `O(n + m log(m+1))` time in these implementations and `O(n+m)` memory.

## Sources and assistance

AI was used to understand some terminal commands and use github actions.