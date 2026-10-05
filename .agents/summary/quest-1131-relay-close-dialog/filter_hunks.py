#!/usr/bin/env python3
"""按标记筛选 unified diff 的 hunk：新增/删除行含标记才保留。

提交拆分用：工作区叠加了并行任务改动时，只把本任务的 hunk 送入 `git apply --cached`，
其余 hunk 留在工作区未暂存。用法：`git diff -- <file> | python3 filter_hunks.py "<marker>"`。
"""
import sys

marker = sys.argv[1]
lines = sys.stdin.read().splitlines(keepends=True)
out: list[str] = []
cur: list[str] | None = None
keep = False

for line in lines:
    if line.startswith("@@"):
        if cur is not None and keep:
            out.extend(cur)
        cur = [line]
        keep = False
        continue
    if cur is None:
        out.append(line)  # 文件头（diff --git / index / --- / +++）
        continue
    cur.append(line)
    if line.startswith(("+", "-")) and marker in line:
        keep = True

if cur is not None and keep:
    out.extend(cur)

sys.stdout.write("".join(out))
