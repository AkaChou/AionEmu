#!/usr/bin/env python3
"""Aggregate a JFR text dump for the play-N allocation sampling rounds.

三部分：
  1) ThreadAllocationStatistics：按 (线程名, javaThreadId) 取 max-min，再按线程组汇总。
  2) ObjectAllocationSample：按栈顶帧（分配点）与栈内第一个 com.aionemu.* 帧（业务归因）聚合权重。
  3) 可选：把两组结果打印成便于与上一轮对照的表。

Usage:
  python3 jfr_play15_analysis.py --threads /tmp/play-15-threads.txt \
      --alloc /tmp/play-15-alloc.txt [--tlab /tmp/play-15-tlab.txt] [--top 25]
"""
from __future__ import annotations

import argparse
import re
from collections import defaultdict

UNITS = {"bytes": 1.0 / 1048576.0, "kB": 1.0 / 1024.0, "MB": 1.0, "GB": 1024.0}

TH_START = re.compile(r"startTime = (\S+) ")
TH_ALLOC = re.compile(r"allocated = ([0-9.]+) (bytes|kB|MB|GB)")
TH_THREAD = re.compile(r'thread = "([^"]+)" \(javaThreadId = (\d+)\)')

ALLOC_HEADER = "jdk.ObjectAllocationSample {"
WEIGHT_RE = re.compile(r"^\s*weight = ([0-9.]+) (bytes|kB|MB|GB)")
CLASS_RE = re.compile(r"^\s*objectClass = (\S+)")
FRAME_RE = re.compile(r"^\s{4}(\S.*)$")

# 线程组：把同类线程合并。 / thread groups collapse worker families.
GROUP_RULES = [
    (re.compile(r"^ForkJoinPool-\d+-worker-\d+$"), lambda n: "ForkJoinPool worker"),
    (re.compile(r"^pool-\d+-thread-\d+$"), lambda n: "pool-N-thread-M"),
    (re.compile(r"^pathfinder"), lambda n: "pathfinder"),
    (re.compile(r"^multiThreadIoEventLoopGroup"), lambda n: "netty eventloop"),
    (re.compile(r"^RMI TCP Connection"), lambda n: "RMI TCP"),
    (re.compile(r"^(Reference Handler|Finalizer|Service Thread|Common-Cleaner|Signal Dispatcher|Notification Thread|Attach Listener)"),
     lambda n: "JVM housekeeping"),
]


def group_of(name: str) -> str:
    for rx, fn in GROUP_RULES:
        if rx.search(name):
            return fn(name)
    return name


def parse_threads(path: str):
    """按 (name, tid) 聚合，取区间内的最大分配量（min 恒为 0）。"""
    per_thread: dict[tuple[str, int], float] = defaultdict(float)
    with open(path, encoding="utf-8", errors="replace") as fh:
        cur_name = cur_tid = None
        for line in fh:
            m = TH_THREAD.search(line)
            if m:
                cur_name, cur_tid = m.group(1), int(m.group(2))
                continue
            m = TH_ALLOC.search(line)
            if m and cur_name is not None:
                mb = float(m.group(1)) * UNITS[m.group(2)]
                per_thread[(cur_name, cur_tid)] = max(per_thread[(cur_name, cur_tid)], mb)
                cur_name = cur_tid = None
    return per_thread


def parse_alloc(path: str):
    samples = []
    with open(path, encoding="utf-8", errors="replace") as fh:
        cur = None
        in_stack = False
        for line in fh:
            if line.startswith(ALLOC_HEADER):
                cur = {"weight": 0.0, "cls": None, "thread": None, "stack": []}
                in_stack = False
                continue
            if cur is None:
                continue
            if "stackTrace = [" in line:
                in_stack = True
                continue
            if in_stack:
                if line.strip() == "]":
                    in_stack = False
                    samples.append(cur)
                    cur = None
                    continue
                m = FRAME_RE.match(line)
                if m:
                    cur["stack"].append(m.group(1).strip())
                continue
            if line.lstrip().startswith("eventThread = "):
                m = TH_THREAD.search(line)
                if m:
                    cur["thread"] = m.group(1)
                continue
            m = WEIGHT_RE.match(line)
            if m:
                cur["weight"] = float(m.group(1)) * UNITS[m.group(2)]
                continue
            m = CLASS_RE.match(line)
            if m:
                cur["cls"] = m.group(1)
    return samples


def frame_head(frame: str) -> str:
    head = frame.split("(")[0]
    if " line:" in head:
        head = head.split(" line:")[0]
    return head.strip()


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--threads")
    ap.add_argument("--alloc")
    ap.add_argument("--tlab")
    ap.add_argument("--top", type=int, default=25)
    args = ap.parse_args()

    if args.threads:
        per_thread = parse_threads(args.threads)
        total = sum(per_thread.values())
        per_group: dict[str, float] = defaultdict(float)
        for (name, _tid), mb in per_thread.items():
            per_group[group_of(name)] += mb
        print(f"=== ThreadAllocationStatistics: distinct(thread,tid)={len(per_thread)} total={total:.1f} MB ===")
        print("-- by group --")
        for g, mb in sorted(per_group.items(), key=lambda kv: -kv[1]):
            if mb >= 0.05:
                print(f"{mb:9.2f} MB  {g}")
        print("-- top threads --")
        for (name, tid), mb in sorted(per_thread.items(), key=lambda kv: -kv[1])[:20]:
            if mb >= 0.05:
                print(f"{mb:9.2f} MB  {name} (tid={tid})")
        print()

    for label, path in (("ObjectAllocationSample", args.alloc), ("ObjectAllocationInNewTLAB", args.tlab)):
        if not path:
            continue
        samples = parse_alloc(path)
        total = sum(s["weight"] for s in samples)
        print(f"=== {label}: events={len(samples)} weight={total:.1f} MB ===")
        by_top: dict[str, float] = defaultdict(float)
        by_biz: dict[str, float] = defaultdict(float)
        by_biz_cls: dict[str, dict[str, float]] = defaultdict(lambda: defaultdict(float))
        by_thread: dict[str, float] = defaultdict(float)
        for s in samples:
            if s["stack"]:
                by_top[frame_head(s["stack"][0])] += s["weight"]
            if s["thread"]:
                by_thread[s["thread"]] += s["weight"]
            biz = "<library/jdk>"
            for frame in s["stack"]:
                head = frame_head(frame)
                if head.startswith("com.aionemu."):
                    biz = head
                    break
            by_biz[biz] += s["weight"]
            by_biz_cls[biz][s["cls"] or "?"] += s["weight"]
        print("-- top allocating frames --")
        for top, mb in sorted(by_top.items(), key=lambda kv: -kv[1])[: args.top]:
            if mb < 0.05:
                break
            print(f"{mb:9.2f} MB  {top}")
        print("-- attributed to first com.aionemu frame --")
        for biz, mb in sorted(by_biz.items(), key=lambda kv: -kv[1])[: args.top]:
            if mb < 0.05:
                break
            print(f"{mb:9.2f} MB  {biz}")
            for cls, cmb in sorted(by_biz_cls[biz].items(), key=lambda kv: -kv[1])[:3]:
                print(f"              - {cls}: {cmb:.2f} MB")
        print("-- top event threads --")
        for th, mb in sorted(by_thread.items(), key=lambda kv: -kv[1])[:10]:
            print(f"{mb:9.2f} MB  {th}")
        print()


if __name__ == "__main__":
    main()
