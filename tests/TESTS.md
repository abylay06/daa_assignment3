# Custom test cases

Run from the repository root:

```bash
python tools/run.py --input tests/my_cases.jsonl
python tests/check_my_cases.py
```

Alternative valid edge orders, SCC labels, topological orders, and shortest-path parents are accepted when they satisfy the graph requirements.

## 1. ping retained

**Reason:** The supplied entry point must remain working.

**Input:**
```json
{"task":"ping"}
```

**Expected:**
```json
{"status":"ready"}
```

**Actual:**
```json
{"status":"ready"}
```

## 2. empty DSU

**Reason:** No operations means no snapshots, even with zero vertices.

**Input:**
```json
{"task":"dsu","n":0,"operations":[]}
```

**Expected:**
```json
{"states":[]}
```

**Actual:**
```json
{"states":[]}
```

## 3. DSU repeated and self unions

**Reason:** The union 2-4 merges groups of sizes 3 and 2; repeats and self unions do not change size.

**Input:**
```json
{"task":"dsu","n":6,"operations":[[0,0],[0,1],[1,2],[3,4],[2,4],[0,4],[5,5],[4,5]]}
```

**Expected:**
```json
{"states":[[6,1],[5,2],[4,3],[3,3],[2,5],[2,5],[2,5],[1,6]]}
```

**Actual:**
```json
{"states":[[6,1],[5,2],[4,3],[3,3],[2,5],[2,5],[2,5],[1,6]]}
```

## 4. DSU repeat

**Reason:** The second operation repeats the existing connection.

**Input:**
```json
{"task":"dsu","n":3,"operations":[[0,1],[1,0],[2,2]]}
```

**Expected:**
```json
{"states":[[2,2],[2,2],[2,2]]}
```

**Actual:**
```json
{"states":[[2,2],[2,2],[2,2]]}
```

## 5. empty Kruskal

**Reason:** No vertices or edges.

**Input:**
```json
{"task":"kruskal","n":0,"edges":[]}
```

**Expected:**
```json
{"cost":0,"edge_ids":[],"components":0}
```

**Actual:**
```json
{"components":0,"edge_ids":[],"cost":0}
```

## 6. Kruskal loops and isolates

**Reason:** Self loops never join two different vertices, regardless of negative weight.

**Input:**
```json
{"task":"kruskal","n":5,"edges":[[0,0,-200],[2,2,1]]}
```

**Expected:**
```json
{"cost":0,"edge_ids":[],"components":5}
```

**Actual:**
```json
{"components":5,"edge_ids":[],"cost":0}
```

## 7. kruskal disconnected forest

**Reason:** Choose weights -4 and 2 for vertices 0-2, weight 1 for 3-4; vertex 5 is isolated. Do not use loop of weight -10.

**Input:**
```json
{"task":"kruskal","n":6,"edges":[[0,1,5],[1,2,-4],[0,2,2],[3,4,1],[4,3,1],[4,4,-10]]}
```

**Expected:**
```json
{"cost":-1,"edge_ids":[1,2,3],"components":3}
```

**Actual:**
```json
{"components":3,"edge_ids":[1,3,2],"cost":-1}
```

## 8. prim disconnected forest

**Reason:** Choose weights -4 and 2 for vertices 0-2, weight 1 for 3-4; vertex 5 is isolated. Do not use loop of weight -10.

**Input:**
```json
{"task":"prim","n":6,"edges":[[0,1,5],[1,2,-4],[0,2,2],[3,4,1],[4,3,1],[4,4,-10]]}
```

**Expected:**
```json
{"cost":-1,"edge_ids":[1,2,3],"components":3}
```

**Actual:**
```json
{"components":3,"edge_ids":[2,1,3],"cost":-1}
```

## 9. kruskal 64-bit sum

**Reason:** Three required edges sum to 3e9, outside signed int range.

**Input:**
```json
{"task":"kruskal","n":4,"edges":[[0,1,1000000000],[1,2,1000000000],[2,3,1000000000]]}
```

**Expected:**
```json
{"cost":3000000000,"edge_ids":[0,1,2],"components":1}
```

**Actual:**
```json
{"components":1,"edge_ids":[0,1,2],"cost":3000000000}
```

## 10. prim 64-bit sum

**Reason:** Three required edges sum to 3e9, outside signed int range.

**Input:**
```json
{"task":"prim","n":4,"edges":[[0,1,1000000000],[1,2,1000000000],[2,3,1000000000]]}
```

**Expected:**
```json
{"cost":3000000000,"edge_ids":[0,1,2],"components":1}
```

**Actual:**
```json
{"components":1,"edge_ids":[0,1,2],"cost":3000000000}
```

## 11. Kruskal parallel ties

**Reason:** Any 2-edge spanning tree of total weight 2 is optimal.

**Input:**
```json
{"task":"kruskal","n":3,"edges":[[0,1,1],[0,1,1],[1,2,1],[0,2,1]]}
```

**Expected:**
```json
{"cost":2,"edge_ids":[0,2],"components":1}
```

**Actual:**
```json
{"components":1,"edge_ids":[0,2],"cost":2}
```

## 12. empty SCC

**Reason:** No vertices means no components or condensation edges.

**Input:**
```json
{"task":"scc","n":0,"edges":[]}
```

**Expected:**
```json
{"component":[],"condensation":[]}
```

**Actual:**
```json
{"component":[],"condensation":[]}
```

## 13. SCC 4 groups with duplicate

**Reason:** SCC groups are {0,1},{2,3},{4,5},{6}; repeated 4->6 appears once in condensation.

**Input:**
```json
{"task":"scc","n":7,"edges":[[0,1],[1,0],[1,2],[2,3],[3,2],[3,4],[4,5],[5,4],[4,6],[4,6],[6,6]]}
```

**Expected:**
```json
{"component":[0,0,1,1,2,2,3],"condensation":[[0,1],[1,2],[2,3]]}
```

**Actual:**
```json
{"component":[0,0,1,1,2,2,3],"condensation":[[0,1],[1,2],[2,3]]}
```

## 14. SCC self loops and isolate

**Reason:** A self loop does not combine distinct vertices into an SCC.

**Input:**
```json
{"task":"scc","n":3,"edges":[[0,0],[1,1]]}
```

**Expected:**
```json
{"component":[0,1,2],"condensation":[]}
```

**Actual:**
```json
{"component":[2,1,0],"condensation":[]}
```

## 15. empty topo

**Reason:** The empty graph is acyclic.

**Input:**
```json
{"task":"topo","n":0,"edges":[]}
```

**Expected:**
```json
{"acyclic":true,"order":[]}
```

**Actual:**
```json
{"order":[],"acyclic":true}
```

## 16. topo with parallel edges

**Reason:** All directed edges must go left-to-right in the returned ordering; isolated vertices are still included.

**Input:**
```json
{"task":"topo","n":6,"edges":[[0,2],[1,2],[0,2],[2,4],[3,5]]}
```

**Expected:**
```json
{"acyclic":true,"order":[0,1,2,3,4,5]}
```

**Actual:**
```json
{"order":[3,5,1,0,2,4],"acyclic":true}
```

## 17. topo disconnected cycle

**Reason:** Cycle 2->3->2 invalidates the entire graph, including its other component.

**Input:**
```json
{"task":"topo","n":4,"edges":[[0,1],[2,3],[3,2]]}
```

**Expected:**
```json
{"acyclic":false,"order":[]}
```

**Actual:**
```json
{"order":[],"acyclic":false}
```

## 18. topo self loop

**Reason:** A self-loop forms a directed cycle.

**Input:**
```json
{"task":"topo","n":2,"edges":[[0,0]]}
```

**Expected:**
```json
{"acyclic":false,"order":[]}
```

**Actual:**
```json
{"order":[],"acyclic":false}
```

## 19. DAG parallel negative and unreachable

**Reason:** Shortest 0->1->2->3 has cost 2-5+3=0; vertex 4 is unreachable.

**Input:**
```json
{"task":"dag","n":5,"source":0,"edges":[[0,1,4],[0,1,2],[1,2,-5],[0,2,10],[2,3,3]]}
```

**Expected:**
```json
{"acyclic":true,"distance":[0,2,-3,0,null],"parent_edge":[-1,1,2,4,-1]}
```

**Actual:**
```json
{"acyclic":true,"distance":[0,2,-3,0,null],"parent_edge":[-1,1,2,4,-1]}
```

## 20. DAG source not zero

**Reason:** Ignore unreachable 0->1 even though it is negative; best path to 4 costs -1.

**Input:**
```json
{"task":"dag","n":5,"source":2,"edges":[[0,1,-100],[2,3,4],[3,4,-5],[2,4,0]]}
```

**Expected:**
```json
{"acyclic":true,"distance":[null,null,0,4,-1],"parent_edge":[-1,-1,-1,1,2]}
```

**Actual:**
```json
{"acyclic":true,"distance":[null,null,0,4,-1],"parent_edge":[-1,-1,-1,1,2]}
```

## 21. DAG cycle unreachable from source

**Reason:** 2->3->2 is a cycle, so reject the graph despite its being unreachable from 0.

**Input:**
```json
{"task":"dag","n":4,"source":0,"edges":[[0,1,5],[2,3,-1],[3,2,0]]}
```

**Expected:**
```json
{"acyclic":false,"distance":[],"parent_edge":[]}
```

**Actual:**
```json
{"parent_edge":[],"acyclic":false,"distance":[]}
```

## 22. DAG signed 64-bit

**Reason:** Relaxation must use signed 64-bit sums.

**Input:**
```json
{"task":"dag","n":4,"source":0,"edges":[[0,1,1000000000],[1,2,1000000000],[2,3,-1000000000]]}
```

**Expected:**
```json
{"acyclic":true,"distance":[0,1000000000,2000000000,1000000000],"parent_edge":[-1,0,1,2]}
```

**Actual:**
```json
{"acyclic":true,"distance":[0,1000000000,2000000000,1000000000],"parent_edge":[-1,0,1,2]}
```

## 23. DAG tied parallel routes

**Reason:** Tied 0->1 parallel edges and two routes to 3: both parent choices are acceptable if consistent.

**Input:**
```json
{"task":"dag","n":4,"source":0,"edges":[[0,1,1],[0,1,1],[0,2,1],[1,3,2],[2,3,2]]}
```

**Expected:**
```json
{"acyclic":true,"distance":[0,1,1,3],"parent_edge":[-1,0,2,3]}
```

**Actual:**
```json
{"acyclic":true,"distance":[0,1,1,3],"parent_edge":[-1,0,2,4]}
```

## 24. repeat-state: DSU repeated and self unions

**Reason:** The union 2-4 merges groups of sizes 3 and 2; repeats and self unions do not change size.

**Input:**
```json
{"task":"dsu","n":6,"operations":[[0,0],[0,1],[1,2],[3,4],[2,4],[0,4],[5,5],[4,5]]}
```

**Expected:**
```json
{"states":[[6,1],[5,2],[4,3],[3,3],[2,5],[2,5],[2,5],[1,6]]}
```

**Actual:**
```json
{"states":[[6,1],[5,2],[4,3],[3,3],[2,5],[2,5],[2,5],[1,6]]}
```

## 25. repeat-state: SCC 4 groups with duplicate

**Reason:** SCC groups are {0,1},{2,3},{4,5},{6}; repeated 4->6 appears once in condensation.

**Input:**
```json
{"task":"scc","n":7,"edges":[[0,1],[1,0],[1,2],[2,3],[3,2],[3,4],[4,5],[5,4],[4,6],[4,6],[6,6]]}
```

**Expected:**
```json
{"component":[0,0,1,1,2,2,3],"condensation":[[0,1],[1,2],[2,3]]}
```

**Actual:**
```json
{"component":[0,0,1,1,2,2,3],"condensation":[[0,1],[1,2],[2,3]]}
```

## 26. repeat-state: DAG cycle unreachable from source

**Reason:** 2->3->2 is a cycle, so reject the graph despite its being unreachable from 0.

**Input:**
```json
{"task":"dag","n":4,"source":0,"edges":[[0,1,5],[2,3,-1],[3,2,0]]}
```

**Expected:**
```json
{"acyclic":false,"distance":[],"parent_edge":[]}
```

**Actual:**
```json
{"parent_edge":[],"acyclic":false,"distance":[]}
```

## 27. repeat-state: prim disconnected forest

**Reason:** Choose weights -4 and 2 for vertices 0-2, weight 1 for 3-4; vertex 5 is isolated. Do not use loop of weight -10.

**Input:**
```json
{"task":"prim","n":6,"edges":[[0,1,5],[1,2,-4],[0,2,2],[3,4,1],[4,3,1],[4,4,-10]]}
```

**Expected:**
```json
{"cost":-1,"edge_ids":[1,2,3],"components":3}
```

**Actual:**
```json
{"components":3,"edge_ids":[2,1,3],"cost":-1}
```

## 28. repeat-state: DAG parallel negative and unreachable

**Reason:** Shortest 0->1->2->3 has cost 2-5+3=0; vertex 4 is unreachable.

**Input:**
```json
{"task":"dag","n":5,"source":0,"edges":[[0,1,4],[0,1,2],[1,2,-5],[0,2,10],[2,3,3]]}
```

**Expected:**
```json
{"acyclic":true,"distance":[0,2,-3,0,null],"parent_edge":[-1,1,2,4,-1]}
```

**Actual:**
```json
{"acyclic":true,"distance":[0,2,-3,0,null],"parent_edge":[-1,1,2,4,-1]}
```

## 29. repeat-state: DSU repeated and self unions

**Reason:** The union 2-4 merges groups of sizes 3 and 2; repeats and self unions do not change size.

**Input:**
```json
{"task":"dsu","n":6,"operations":[[0,0],[0,1],[1,2],[3,4],[2,4],[0,4],[5,5],[4,5]]}
```

**Expected:**
```json
{"states":[[6,1],[5,2],[4,3],[3,3],[2,5],[2,5],[2,5],[1,6]]}
```

**Actual:**
```json
{"states":[[6,1],[5,2],[4,3],[3,3],[2,5],[2,5],[2,5],[1,6]]}
```

## 30. repeat-state: DAG source not zero

**Reason:** Ignore unreachable 0->1 even though it is negative; best path to 4 costs -1.

**Input:**
```json
{"task":"dag","n":5,"source":2,"edges":[[0,1,-100],[2,3,4],[3,4,-5],[2,4,0]]}
```

**Expected:**
```json
{"acyclic":true,"distance":[null,null,0,4,-1],"parent_edge":[-1,-1,-1,1,2]}
```

**Actual:**
```json
{"acyclic":true,"distance":[null,null,0,4,-1],"parent_edge":[-1,-1,-1,1,2]}
```

## Large and witness tests

- `python tests/stress_test.py` tests the full graph and operation limits.
- `RouteHelperTest.java` tests reachable, source, unreachable, tied, and parallel-edge routes.
- `check_my_cases.py` checks alternative valid and invalid witnesses.
- `Benchmark.java` compares Kruskal and Prim on identical graphs at three sizes.
