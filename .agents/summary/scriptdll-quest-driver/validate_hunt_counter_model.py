#!/usr/bin/env python3
"""交叉验证 SimpleHunt 计数模型：注册表参数 vs 客户端 quest_monster.csv。

模型（来自反编译 FUN_180cb13b0）：
  param_3 = 计数器序号（1 起），占 packed 字段的第 (param_3-1)*6 位起 6 bit
  param_4 = 该计数器的上限（守卫 SECTION_(param_3-1) < param_4）
  param_5 = 整条任务该字段的“完成值” = Σ limits[n] << (6*n)

数据源：
  .agents/summary/scriptdll-quest-driver/quest_registry.tsv
  ${AION_UNPACK_ROOT:-$HOME/PycharmProjects/unpak}/Quest_unpacked/quest_monster.csv
"""
import csv
import os
import re
import sys

BASE = os.path.dirname(os.path.abspath(__file__))
REG = os.path.join(BASE, "quest_registry.tsv")
MON = f"{os.environ.get('AION_UNPACK_ROOT', os.path.expanduser('~/PycharmProjects/unpak'))}/Quest_unpacked/quest_monster.csv"

GUARD = re.compile(r"SECTION_(\d+)<(\d+)")


def load_monster():
    rows = {}
    with open(MON, newline="", encoding="utf-8", errors="replace") as fh:
        for row in csv.DictReader(fh, skipinitialspace=True):
            qid = row["questId"].strip()
            if not qid.isdigit():
                continue
            rows.setdefault(int(qid), []).append(row)
    return rows


def load_registry():
    rows = {}
    with open(REG, encoding="utf-8") as fh:
        for line in fh:
            if line.startswith("#") or line.startswith("quest_id"):
                continue
            parts = line.rstrip("\n").split("\t")
            if len(parts) < 6 or not parts[0].isdigit():
                continue
            qid, step, helper, params, owner, off = parts[:6]
            rows.setdefault(int(qid), []).append(
                {"step": int(step), "helper": helper, "params": params.split(), "offset": int(off)}
            )
    return {k: sorted(v, key=lambda r: r["offset"]) for k, v in rows.items()}


def expected_goal(limits_by_section):
    goal = 0
    for sec, limit in limits_by_section.items():
        goal |= (limit & 0x3F) << (6 * sec)
    return goal


def main():
    mon = load_monster()
    reg = load_registry()
    checked = mismatch_limits = mismatch_goal = no_csv = 0
    bad_goal, bad_limit = [], []
    for qid, steps in sorted(reg.items()):
        hunts = [s for s in steps if s["helper"] == "FUN_180cb13b0"]
        if not hunts or qid not in mon:
            continue
        limits = {}
        for row in mon[qid]:
            for m in GUARD.finditer(row["questProgress"]):
                sec, lim = int(m.group(1)), int(m.group(2))
                if sec == 5:  # SECTION_5 是守卫/整体进度位，不是击杀计数
                    continue
                limits[sec] = max(limits.get(sec, 0), lim)
        if not limits:
            continue
        checked += 1
        goal = expected_goal(limits)
        rg = {int(s["params"][1], 0): int(s["params"][2], 0) for s in hunts if len(s["params"]) >= 5}
        reg_goal = {int(s["params"][3], 0) for s in hunts if len(s["params"]) >= 5}
        csv_limits = {sec + 1: lim for sec, lim in limits.items()}
        if rg != csv_limits:
            mismatch_limits += 1
            if len(bad_limit) < 8:
                bad_limit.append((qid, rg, csv_limits))
        if reg_goal != {goal}:
            mismatch_goal += 1
            if len(bad_goal) < 8:
                bad_goal.append((qid, sorted(reg_goal), goal))
    print(f"参与校验的任务: {checked}")
    print(f"计数器序号/上限 != CSV 守卫: {mismatch_limits}")
    print(f"param_5 != Σ limits<<6n    : {mismatch_goal}")
    for qid, rg, cs in bad_limit:
        print(f"  [limit] {qid} registry={rg} csv={cs}")
    for qid, rg, cs in bad_goal:
        print(f"  [goal ] {qid} registry={rg} expected={cs}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
