"""Full published bounds: n=100000, q/m=250000, deep chains.
Run after `python tools/run.py --setup`: python tests/stress_test.py
Does not create or commit enormous generated input/output files.
"""
import json
import pathlib
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[1]
N, M = 100_000, 250_000
JAVA = ['java', '-Xmx384m', '-cp', str(ROOT / '.build/Java'), 'Main']

def run(request):
    process = subprocess.run(JAVA, input=json.dumps(request, separators=(',', ':')) + '\n',
                             text=True, capture_output=True, encoding='utf-8',
                             cwd=ROOT, timeout=20, check=True)
    answer = json.loads(process.stdout)
    assert 'error' not in answer, answer
    return answer

# DSU: join a chain, then do repeated self-unions.
operations = [[i, i + 1] for i in range(N - 1)]
operations.extend([[0, 0]] * (M - len(operations)))
a = run(dict(task='dsu', n=N, operations=operations))
assert len(a['states']) == M and a['states'][0] == [N - 1, 2]
assert a['states'][N - 2] == [1, N] and a['states'][-1] == [1, N]
print('PASS DSU: 100000 vertices and 250000 operations')

# MST: the only useful edges form a zero-weight chain; the rest are negative loops.
forest = [[i, i + 1, 0] for i in range(N - 1)]
forest.extend([[0, 0, -1_000_000_000]] * (M - len(forest)))
for task in ('kruskal', 'prim'):
    a = run(dict(task=task, n=N, edges=forest))
    assert a['cost'] == 0 and a['components'] == 1
    assert len(a['edge_ids']) == N - 1
    assert set(a['edge_ids']) == set(range(N - 1))
    print(f'PASS {task}: 100000 vertices and 250000 edges')

# SCC: a 100000-deep directed chain; duplicates do not change condensation.
arcs = [[i, i + 1] for i in range(N - 1)]
arcs.extend([[0, 1]] * (M - len(arcs)))
a = run(dict(task='scc', n=N, edges=arcs))
labels = a['component']
assert len(labels) == N and len(set(labels)) == N
assert len(a['condensation']) == N - 1
assert set(map(tuple, a['condensation'])) == {
    (labels[i], labels[i + 1]) for i in range(N - 1)}
print('PASS SCC: 100000-deep chain and 250000 edges')

a = run(dict(task='topo', n=N, edges=arcs))
assert a['acyclic'] and a['order'] == list(range(N))
print('PASS topo: 100000-deep chain and 250000 edges')

# DAG: the chain has edge weight 1; all extra parallel edges have weight 2.
weighted = [[i, i + 1, 1] for i in range(N - 1)]
weighted.extend([[0, 1, 2]] * (M - len(weighted)))
a = run(dict(task='dag', n=N, source=0, edges=weighted))
assert a['acyclic'] and a['distance'] == list(range(N))
assert a['parent_edge'] == [-1] + list(range(N - 1))
print('PASS DAG: 100000-deep chain and 250000 edges')
