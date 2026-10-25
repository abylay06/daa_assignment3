"""Run own JSONL tests and validate witnesses, not just their text output.
Run from repository root: python tests/check_my_cases.py
"""
import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
from public_validate import check

cases = json.loads((ROOT / 'tests/cases.json').read_text(encoding='utf-8'))
run = subprocess.run([sys.executable, 'tools/run.py', '--input', 'tests/my_cases.jsonl'],
                     cwd=ROOT, capture_output=True, text=True, encoding='utf-8', check=True)
actual = [json.loads(line) for line in run.stdout.splitlines()]
assert len(actual) == len(cases), f'{len(actual)} outputs for {len(cases)} requests'

for case, result in zip(cases, actual):
    assert check(case['input'], result, case['output']), f'FAIL {case["name"]}: {result}'
print(f'PASS {len(cases)} custom JSON Lines requests (semantic witness checks)')

# Public validator must ACCEPT alternative correct witnesses...
tie = dict(task='kruskal', n=3,
           edges=[[0, 1, 1], [0, 1, 1], [1, 2, 1], [0, 2, 1]])
expected = dict(cost=2, components=1, edge_ids=[0, 2])
assert check(tie, dict(expected, edge_ids=[1, 3]), expected)
assert not check(tie, dict(expected, edge_ids=[0, 1]), expected)  # cycle + isolated 2

scc = dict(task='scc', n=3, edges=[[0, 1], [1, 0], [1, 2], [1, 2]])
scc_expected = dict(component=[0, 0, 1], condensation=[[0, 1]])
assert check(scc, dict(component=[7, 7, 9], condensation=[[7, 9]]), scc_expected)
assert not check(scc, dict(component=[7, 7, 9], condensation=[[7, 9], [7, 9]]), scc_expected)

topo = dict(task='topo', n=3, edges=[[0, 2], [1, 2]])
assert check(topo, dict(acyclic=True, order=[1, 0, 2]),
             dict(acyclic=True, order=[0, 1, 2]))
assert not check(topo, dict(acyclic=True, order=[2, 0, 1]),
                 dict(acyclic=True, order=[0, 1, 2]))

dag = dict(task='dag', n=4, source=0,
           edges=[[0, 1, 1], [0, 1, 1], [1, 2, 1], [0, 2, 2]])
dag_expected = dict(acyclic=True, distance=[0, 1, 2, None],
                    parent_edge=[-1, 0, 2, -1])
assert check(dag, dict(dag_expected, parent_edge=[-1, 1, 3, -1]), dag_expected)
assert not check(dag, dict(dag_expected, parent_edge=[-1, 0, 0, -1]), dag_expected)
print('PASS semantic checker accepts alternatives and rejects invalid witnesses')

(ROOT / 'tests/actual.jsonl').write_text(
    ''.join(json.dumps(result, separators=(',', ':')) + '\n' for result in actual),
    encoding='utf-8')
print('Updated tests/actual.jsonl')
