#!/usr/bin/env python3
"""Aggregate jdk.ObjectAllocationSample weights by allocating frame and by quest-engine site.

按采样权重聚合 jdk.ObjectAllocationSample：分别按分配点（栈顶帧）和 questEngine 站点（栈内最上层匹配帧）分组。
Usage:
  jfr print --events jdk.ObjectAllocationSample --stack-depth 60 f.jfr | python3 jfr_alloc_sites.py
"""
import re
import sys
from collections import defaultdict

WEIGHT_RE = re.compile(r"^\s*weight = ([0-9.]+) (bytes|kB|MB)")
CLASS_RE = re.compile(r"^\s*objectClass = (\S+)")
FRAME_RE = re.compile(r"^\s{4}(\S.*)$")
HEADER = "jdk.ObjectAllocationSample {"
UNITS = {"bytes": 1.0, "kB": 1024.0, "MB": 1024.0 * 1024.0}

# questEngine 站点：按栈内最上层匹配的帧归因。 / quest-engine sites: attribute to the topmost matching frame.
SITE_RE = re.compile(
    r"com\.aionemu\.gameserver\.questEngine\.(?:runtime\.)?(QuestSnapshot|PlayerQuestEventPort|QuestEventIndex|QuestExecutionCoordinator)")


def parse(stream):
    samples = []
    cur = None
    in_stack = False
    for line in stream:
        if line.startswith(HEADER):
            cur = {"weight": 0.0, "cls": None, "stack": []}
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
        m = WEIGHT_RE.match(line)
        if m:
            cur["weight"] = float(m.group(1)) * UNITS[m.group(2)]
            continue
        m = CLASS_RE.match(line)
        if m:
            cur["cls"] = m.group(1)
    return samples


def frame_cls(frame):
    head = frame.split("(")[0]
    if " line:" in head:
        head = head.split(" line:")[0]
    return head.strip()


def main():
    samples = parse(sys.stdin)
    total = sum(s["weight"] for s in samples)
    print(f"samples={len(samples)} total_sampled_bytes={total/1024/1024:.1f} MB")

    by_top = defaultdict(float)
    by_top_cls = defaultdict(lambda: defaultdict(float))
    by_site = defaultdict(float)
    by_site_cls = defaultdict(lambda: defaultdict(float))
    for s in samples:
        top = frame_cls(s["stack"][0]) if s["stack"] else "<unknown>"
        by_top[top] += s["weight"]
        by_top_cls[top][s["cls"]] += s["weight"]
        site = None
        for frame in s["stack"]:
            head = frame_cls(frame)
            if SITE_RE.search(head):
                site = f"{head} line:{frame.split(' line:')[1].split(')')[0].strip()}" if " line:" in frame else head
                break
        if site:
            by_site[site] += s["weight"]
            by_site_cls[site][s["cls"]] += s["weight"]

    print("\n=== top allocating frames / 分配点 ===")
    for top, w in sorted(by_top.items(), key=lambda kv: -kv[1])[:30]:
        print(f"{w/1024/1024:8.2f} MB  {top}")
        for cls, cw in sorted(by_top_cls[top].items(), key=lambda kv: -kv[1])[:4]:
            print(f"               - {cls}: {cw/1024/1024:.2f} MB")

    print("\n=== questEngine sites / 任务引擎站点 ===")
    for site, w in sorted(by_site.items(), key=lambda kv: -kv[1]):
        print(f"{w/1024/1024:8.2f} MB  {site}")
        for cls, cw in sorted(by_site_cls[site].items(), key=lambda kv: -kv[1])[:4]:
            print(f"               - {cls}: {cw/1024/1024:.2f} MB")

    quest_total = sum(w for site, w in by_site.items() if ".questEngine." in site)
    print(f"\nquestEngine attributed total: {quest_total/1024/1024:.2f} MB of {total/1024/1024:.1f} MB")


if __name__ == "__main__":
    main()
