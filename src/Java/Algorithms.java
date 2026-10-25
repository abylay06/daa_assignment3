import java.util.*;

/** Assignment 3 graph algorithms. Every request uses fresh local state. */
public final class Algorithms {
    private Algorithms() {}

    // Union-find with path compression and union by size.
    private static class DSU {
        int[] parent, size;
        int components;

        DSU(int n) {
            parent = new int[n];
            size = new int[n];
            components = n;
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
            }
        }

        int find(int x) {
            while (x != parent[x]) {
                parent[x] = parent[parent[x]];
                x = parent[x];
            }
            return x;
        }

        boolean union(int a, int b) {
            a = find(a);
            b = find(b);
            if (a == b) return false;
            if (size[a] < size[b]) {
                int temp = a;
                a = b;
                b = temp;
            }
            parent[b] = a;
            size[a] += size[b];
            components--;
            return true;
        }
    }

    static Map<String, Object> dsu(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] operations = Input.operations(input);
        DSU dsu = new DSU(n);
        int largest = n == 0 ? 0 : 1;
        List<int[]> states = new ArrayList<>();

        for (int[] op : operations) {
            if (dsu.union(op[0], op[1])) {
                largest = Math.max(largest, dsu.size[dsu.find(op[0])]);
            }
            states.add(new int[]{dsu.components, largest});
        }
        return Map.of("states", states);
    }

    static Map<String, Object> kruskal(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        Integer[] ids = new Integer[edges.length];
        for (int i = 0; i < ids.length; i++) ids[i] = i;
        Arrays.sort(ids, Comparator.comparingInt(i -> edges[i][2]));

        DSU dsu = new DSU(n);
        List<Integer> selected = new ArrayList<>();
        long cost = 0;
        for (int id : ids) {
            int[] e = edges[id];
            if (dsu.union(e[0], e[1])) {
                selected.add(id);
                cost += e[2];
            }
        }
        return Map.of("cost", cost, "edge_ids", selected, "components", dsu.components);
    }

    static Map<String, Object> prim(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        List<Integer>[] graph = adjacency(n);
        for (int i = 0; i < edges.length; i++) {
            graph[edges[i][0]].add(i);
            graph[edges[i][1]].add(i);
        }

        boolean[] seen = new boolean[n];
        PriorityQueue<int[]> pq = new PriorityQueue<>(
                Comparator.<int[]>comparingInt(a -> a[0]).thenComparingInt(a -> a[1]));
        List<Integer> selected = new ArrayList<>();
        long cost = 0;
        int components = 0;

        for (int start = 0; start < n; start++) {
            if (seen[start]) continue;
            components++;
            seen[start] = true;
            addEdges(start, graph, edges, pq);

            while (!pq.isEmpty()) {
                int[] item = pq.poll();
                int to = item[2];
                if (seen[to]) continue;
                seen[to] = true;
                selected.add(item[1]);
                cost += item[0];
                addEdges(to, graph, edges, pq);
            }
        }
        return Map.of("cost", cost, "edge_ids", selected, "components", components);
    }

    private static void addEdges(int u, List<Integer>[] graph, int[][] edges,
                                 PriorityQueue<int[]> pq) {
        for (int id : graph[u]) {
            int[] e = edges[id];
            int to = e[0] == u ? e[1] : e[0];
            pq.add(new int[]{e[2], id, to});
        }
    }

    static Map<String, Object> scc(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        List<Integer>[] graph = adjacency(n);
        List<Integer>[] reverse = adjacency(n);
        for (int[] e : edges) {
            graph[e[0]].add(e[1]);
            reverse[e[1]].add(e[0]);
        }

        boolean[] seen = new boolean[n];
        int[] next = new int[n], stack = new int[n], finish = new int[n];
        int finished = 0;
        for (int start = 0; start < n; start++) {
            if (seen[start]) continue;
            int top = 0;
            stack[top++] = start;
            seen[start] = true;
            while (top > 0) {
                int u = stack[top - 1];
                if (next[u] < graph[u].size()) {
                    int v = graph[u].get(next[u]++);
                    if (!seen[v]) {
                        seen[v] = true;
                        stack[top++] = v;
                    }
                } else {
                    finish[finished++] = u;
                    top--;
                }
            }
        }

        int[] component = new int[n];
        Arrays.fill(component, -1);
        int count = 0;
        for (int i = finished - 1; i >= 0; i--) {
            int start = finish[i];
            if (component[start] != -1) continue;
            int top = 0;
            stack[top++] = start;
            component[start] = count;
            while (top > 0) {
                int u = stack[--top];
                for (int v : reverse[u]) {
                    if (component[v] == -1) {
                        component[v] = count;
                        stack[top++] = v;
                    }
                }
            }
            count++;
        }

        List<int[]> condensation = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        for (int[] e : edges) {
            int a = component[e[0]], b = component[e[1]];
            long key = ((long) a << 32) | (b & 0xffffffffL);
            if (a != b && used.add(key)) condensation.add(new int[]{a, b});
        }
        return Map.of("component", component, "condensation", condensation);
    }

    static Map<String, Object> topo(Map<String, Object> input) {
        List<Integer> order = topologicalOrder(Input.vertexCount(input), Input.edges(input));
        if (order == null) return Map.of("acyclic", false, "order", List.of());
        return Map.of("acyclic", true, "order", order);
    }

    private static List<Integer> topologicalOrder(int n, int[][] edges) {
        List<Integer>[] graph = adjacency(n);
        for (int[] e : edges) graph[e[0]].add(e[1]);
        int[] state = new int[n], next = new int[n], stack = new int[n];
        List<Integer> order = new ArrayList<>(n);

        for (int start = 0; start < n; start++) {
            if (state[start] != 0) continue;
            int top = 0;
            stack[top++] = start;
            state[start] = 1;
            while (top > 0) {
                int u = stack[top - 1];
                if (next[u] < graph[u].size()) {
                    int v = graph[u].get(next[u]++);
                    if (state[v] == 1) return null;
                    if (state[v] == 0) {
                        state[v] = 1;
                        stack[top++] = v;
                    }
                } else {
                    state[u] = 2;
                    order.add(u);
                    top--;
                }
            }
        }
        Collections.reverse(order);
        return order;
    }

    static Map<String, Object> dag(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int source = Input.source(input);
        int[][] edges = Input.edges(input);
        List<Integer> order = topologicalOrder(n, edges);
        if (order == null) {
            return Map.of("acyclic", false, "distance", List.of(), "parent_edge", List.of());
        }

        List<Integer>[] graph = adjacency(n);
        for (int i = 0; i < edges.length; i++) graph[edges[i][0]].add(i);
        Long[] distance = new Long[n];  // null means unreachable.
        int[] parentEdge = new int[n];
        Arrays.fill(parentEdge, -1);
        distance[source] = 0L;

        for (int u : order) {
            if (distance[u] == null) continue;
            for (int id : graph[u]) {
                int v = edges[id][1];
                long candidate = distance[u] + (long) edges[id][2];
                if (distance[v] == null || candidate < distance[v]) {
                    distance[v] = candidate;
                    parentEdge[v] = id;
                }
            }
        }
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("acyclic", true);
        answer.put("distance", distance);
        answer.put("parent_edge", parentEdge);
        return answer;
    }

    static List<Integer> reconstructRoute(int source, int target,
                                          int[][] edges, int[] parentEdge) {
        List<Integer> route = new ArrayList<>();
        int current = target;
        while (current != source) {
            route.add(current);
            int id = parentEdge[current];
            if (id == -1) return List.of();
            current = edges[id][0];
        }
        route.add(source);
        Collections.reverse(route);
        return route;
    }

    @SuppressWarnings("unchecked")
    private static List<Integer>[] adjacency(int n) {
        List<Integer>[] graph = (List<Integer>[]) new List<?>[n];
        for (int i = 0; i < n; i++) graph[i] = new ArrayList<>();
        return graph;
    }
}
