import java.util.*;

/** Algorithm-only benchmark; excludes input generation, JSON parsing/output, compilation. */
public class Benchmark {
    private static List<List<Integer>> makeGraph(int n, int m, long seed) {
        Random r = new Random(seed);
        List<List<Integer>> edges = new ArrayList<>(m);
        // Connected chain makes every forest contain n - 1 edges.
        for (int i = 0; i < n - 1; i++) {
            edges.add(List.of(i, i + 1, r.nextInt(2001) - 1000));
        }
        while (edges.size() < m) {
            edges.add(List.of(r.nextInt(n), r.nextInt(n), r.nextInt(2001) - 1000));
        }
        return edges;
    }

    private static void verify(Map<String, Object> result, int n, List<List<Integer>> edges) {
        @SuppressWarnings("unchecked")
        List<Integer> chosen = (List<Integer>) result.get("edge_ids");
        long reported = ((Number) result.get("cost")).longValue();
        int components = ((Number) result.get("components")).intValue();
        if (components != 1 || chosen.size() != n - 1) throw new AssertionError("Wrong forest size");
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        Set<Integer> ids = new HashSet<>();
        long total = 0;
        for (int id : chosen) {
            if (!ids.add(id)) throw new AssertionError("Repeated ID");
            List<Integer> edge = edges.get(id);
            int a = edge.get(0), b = edge.get(1);
            while (parent[a] != a) a = parent[a];
            while (parent[b] != b) b = parent[b];
            if (a == b) throw new AssertionError("Cycle in returned forest");
            parent[b] = a;
            total += edge.get(2);
        }
        if (total != reported) throw new AssertionError("Wrong cost");
    }

    private static double median(double[] values) {
        double[] ordered = values.clone();
        Arrays.sort(ordered);
        return ordered[ordered.length / 2];
    }

    private static void bench(int n, int m) {
        long seed = 20261025L + n;
        List<List<Integer>> rows = makeGraph(n, m, seed);
        Map<String, Object> input = Map.of("n", n, "edges", rows);
        Map<String, Object> kruskal = Algorithms.kruskal(input);
        Map<String, Object> prim = Algorithms.prim(input);
        verify(kruskal, n, rows);
        verify(prim, n, rows);
        if (!kruskal.get("cost").equals(prim.get("cost"))) {
            throw new AssertionError("MST costs disagree");
        }

        for (int i = 0; i < 3; i++) {  // JIT and cache warm-up.
            Algorithms.kruskal(input);
            Algorithms.prim(input);
        }
        double[] k = new double[5], p = new double[5];
        for (int i = 0; i < 5; i++) {
            // Alternate order across repetitions to reduce systematic order effects.
            if ((i & 1) == 0) {
                long start = System.nanoTime();
                Algorithms.kruskal(input);
                k[i] = (System.nanoTime() - start) / 1_000_000.0;
                start = System.nanoTime();
                Algorithms.prim(input);
                p[i] = (System.nanoTime() - start) / 1_000_000.0;
            } else {
                long start = System.nanoTime();
                Algorithms.prim(input);
                p[i] = (System.nanoTime() - start) / 1_000_000.0;
                start = System.nanoTime();
                Algorithms.kruskal(input);
                k[i] = (System.nanoTime() - start) / 1_000_000.0;
            }
        }
        System.out.printf(Locale.US,
            "n=%d m=%d seed=%d cost=%d%nKruskal ms=%s median=%.3f%nPrim    ms=%s median=%.3f%n",
            n, m, seed, kruskal.get("cost"), Arrays.toString(k), median(k),
            Arrays.toString(p), median(p));
    }

    public static void main(String[] args) {
        System.out.println("Runtime: " + System.getProperty("java.version")
            + "; OS: " + System.getProperty("os.name")
            + "; CPU processors visible: " + Runtime.getRuntime().availableProcessors());
        bench(1000, 3000);
        bench(5000, 15000);
        bench(20000, 60000);
    }
}
