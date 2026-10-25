# Java Assignment 3 — installation checklist

These are the **only repository files that need to be added or replaced**. Extract this archive into the **root of your existing course repository**, preserving folder structure:

- Replace `src/Java/Algorithms.java`.
- Set `submission.json` to `{"language":"Java"}` (included).
- Add `ANALYSIS.md` to the root.
- Add the supplied new files inside `tests/` (keep your existing `tests/README.md`).
- Keep **all** existing template files, especially `src/Java/Main.java`, `Input.java`, `Json.java`, `.github/`, and `tools/`. You do not need to edit them.

From the repository root on Windows in VS Code Terminal / PowerShell:

```powershell
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input tests/my_cases.jsonl
python tests/check_my_cases.py
javac --release 17 -cp .build/Java -d .build/Java tests/RouteHelperTest.java
java -Xmx384m -cp .build/Java RouteHelperTest
python tests/stress_test.py
javac --release 17 -cp .build/Java -d .build/Java tests/Benchmark.java
java -Xmx384m -cp .build/Java Benchmark
```

If Windows recognizes `py` instead of `python`, substitute `py -3` for `python`. If Java is not found, set up a JDK 17+ and ensure `java` and `javac` are on PATH. The supplied launcher compiles the project for you; no Maven or external libraries are required.

Expected headlines from the first checks:

- `--setup`: ping passed.
- `--check`: 36 public cases passed (and repeated batch passed).
- `check_my_cases.py`: 30 own test requests passed, plus semantic witness tests.
- Route helper: passed reachable, source, unreachable, parallel, tied.
- Stress: all six tasks passed 100,000-vertex / 250,000-edge-or-operation cases.

`ANALYSIS.md` includes recorded benchmark timings from the environment where this package was developed. Re-run `Benchmark` on your own computer and update timings/environment in `ANALYSIS.md` before submission if your instructor expects your own machine's measurements.

**Submission:** Commit the changed files, push to GitHub, ensure Actions `Public checks` passes, obtain your *exact commit URL* (not just the repository home page), paste that into Moodle Online text, click Submit assignment, and check the submission status. Grant instructor access if the repository is private. Follow your syllabus's AI disclosure guidance, and understand the code for the defense quiz.
