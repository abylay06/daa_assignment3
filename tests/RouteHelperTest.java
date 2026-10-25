import java.util.*;

/** Run directly; the route helper is not part of the JSON protocol. */
public class RouteHelperTest {
    static void equal(Object actual, Object expected, String what) {
        if (!actual.equals(expected)) throw new AssertionError(what + ": " + actual);
    }

    static long pathCost(List<Integer> route, int[] parents, int[][] edges) {
        long sum = 0;
        for (int i = 1; i < route.size(); i++) {
            int id = parents[route.get(i)];
            if (id < 0 || edges[id][0] != route.get(i - 1)
                    || edges[id][1] != route.get(i)) {
                throw new AssertionError("Wrong direction/edge ID in route: " + route);
            }
            sum += edges[id][2];
        }
        return sum;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        // Reachable, source, and unreachable destinations on the published graph.
        Map<String, Object> request = Json.parseRequest(
            "{\"task\":\"dag\",\"n\":5,\"source\":0,"
          + "\"edges\":[[0,1,3],[0,2,8],[1,2,-7],[2,3,4]]}");
        int[][] edges = Input.edges(request);
        Map<String, Object> answer = Algorithms.dag(request);
        int[] parents = (int[]) answer.get("parent_edge");
        Long[] distances = (Long[]) answer.get("distance");

        List<Integer> to3 = Algorithms.reconstructRoute(0, 3, edges, parents);
        equal(to3, List.of(0, 1, 2, 3), "reachable route");
        equal(pathCost(to3, parents, edges), 0L, "reachable route cost");
        equal(distances[3], 0L, "returned shortest distance");
        equal(Algorithms.reconstructRoute(0, 0, edges, parents), List.of(0), "source route");
        equal(Algorithms.reconstructRoute(0, 4, edges, parents), List.of(), "unreachable route");

        // Parallel edges and tied routes: either optimal route is acceptable.
        Map<String, Object> tied = Json.parseRequest(
            "{\"task\":\"dag\",\"n\":4,\"source\":0,"
          + "\"edges\":[[0,1,1],[0,1,1],[0,2,1],[1,3,2],[2,3,2]]}");
        int[][] tiedEdges = Input.edges(tied);
        Map<String, Object> tiedAnswer = Algorithms.dag(tied);
        int[] tiedParents = (int[]) tiedAnswer.get("parent_edge");
        List<Integer> route = Algorithms.reconstructRoute(0, 3, tiedEdges, tiedParents);
        if (!route.equals(List.of(0, 1, 3)) && !route.equals(List.of(0, 2, 3))) {
            throw new AssertionError("Wrong tied route: " + route);
        }
        equal(pathCost(route, tiedParents, tiedEdges), 3L, "tied route cost");
        int parallelId = tiedParents[1];
        if (parallelId != 0 && parallelId != 1) {
            throw new AssertionError("Must select one of the parallel edges");
        }
        equal(Algorithms.reconstructRoute(0, 1, tiedEdges, tiedParents),
              List.of(0, 1), "parallel-edge route");
        System.out.println("PASS route helper: reachable, source, unreachable, parallel, tied");
    }
}
