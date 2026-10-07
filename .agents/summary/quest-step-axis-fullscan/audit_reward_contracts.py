#!/usr/bin/env python3
"""领奖行批次 CONTRACTS 名单 × 真端 SetProgress 集合交叉审计。

背景（2026-10-07，接 itemUseArea 审计 §4 的未竟线索）：
- JournalRewardRowRepairContractTest 在册 193 行（第三批领奖行修复 203 任务），
  批次语义 = 「客户端末行是领奖行 ⇒ reward 投影抬到末行索引 rewardRow + 补 enter-world 自愈边」。
- 已实证 4 例（1361/11006/24021/24052）：玩法步后进 REWARD 的任务真端轴保持玩法末值
  （0x100 状态推进不写轴），批次抬行是错的，staleRow（旧值）才是权威。
- 本脚本对每行在册 Contract 交叉真端 SetProgress 常量集合：
  * RISKY —— 集合含 staleRow（或玩法链值）且不含 rewardRow：与 11006 同形态，高嫌疑；
  * OK_RETAIL —— 集合含 rewardRow：批次值有真端支撑；
  * NO_RETAIL —— 真端无该任务常量推进：需 legacy/客户端行人工复核。

用法：
    python3 .agents/summary/quest-step-axis-fullscan/audit_reward_contracts.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
TEST = REPO / "src/test/java/com/aionemu/gameserver/questEngine/definition/JournalRewardRowRepairContractTest.java"
OUT_DIR = Path(__file__).resolve().parent


def resolve_external(candidates: list[Path], probe: str) -> Path:
    for base in candidates:
        if (base / probe).exists():
            return base
    raise SystemExit(f"cannot resolve external root for {probe}; tried: {candidates}")


RETAIL_ROOT = resolve_external(
    [REPO.parent / "58Server", Path.home() / "IdeaProjects" / "58Server"],
    "server58/MainServer_ScriptDLL64/ScriptDLL64.c")
RETAIL_C = RETAIL_ROOT / "server58" / "MainServer_ScriptDLL64" / "ScriptDLL64.c"

SETPROG = re.compile(
    r"0xf0\)\)\(\s*[A-Za-z_0-9]+,\s*0x([0-9a-fA-F]{1,7})\s*,\s*(0x[0-9a-fA-F]{1,4}|[0-9]{1,4})\s*"
    r"(?:,\s*(?:0x[0-9a-fA-F]{1,4}|[0-9]{1,4})\s*)?\)")
AXISPROG = re.compile(
    r"0x110\)\)\(\s*[A-Za-z_0-9]+,\s*0x([0-9a-fA-F]{1,7})\s*,\s*(0x[0-9a-fA-F]{1,4}|[0-9]{1,4})\s*,\s*(0x[0-9a-fA-F]{1,4}|[0-9]{1,4})")


def scan_retail() -> dict[int, set[int]]:
    text = RETAIL_C.read_text(encoding="utf-8", errors="ignore")
    setprog: dict[int, set[int]] = {}
    for hex_id, value in SETPROG.findall(text):
        setprog.setdefault(int(hex_id, 16), set()).add(int(value, 0))
    for hex_id, _f, to in AXISPROG.findall(text):
        setprog.setdefault(int(hex_id, 16), set()).add(int(to, 0))
    return setprog


def load_contracts() -> list[tuple[int, int, int]]:
    """解析在册 Contract（跳过块注释与行注释内的条目）。"""
    text = TEST.read_text(encoding="utf-8")
    # 去块注释
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"//[^\n]*", "", text)
    return [(int(a), int(b), int(c)) for a, b, c in
            re.findall(r"new Contract\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)", text)]


def main() -> int:
    setprog = scan_retail()
    contracts = load_contracts()
    rows = {"RISKY": [], "OK_RETAIL": [], "NO_RETAIL": []}
    for quest_id, reward_row, stale_row in contracts:
        vals = setprog.get(quest_id, set())
        if not vals:
            rows["NO_RETAIL"].append((quest_id, reward_row, stale_row, ""))
            continue
        reward_ok = reward_row in vals
        stale_ok = stale_row in vals if stale_row != 0 else False
        if not reward_ok and stale_ok:
            rows["RISKY"].append((quest_id, reward_row, stale_row, "|".join(map(str, sorted(vals)))))
        elif reward_ok:
            rows["OK_RETAIL"].append((quest_id, reward_row, stale_row, "|".join(map(str, sorted(vals)))))
        else:
            # 集合既不含 reward 也不含 stale：轴值另有来源（计数/变量推进），需人工
            rows["NO_RETAIL"].append((quest_id, reward_row, stale_row, "|".join(map(str, sorted(vals)))))

    for cat in ("RISKY", "OK_RETAIL", "NO_RETAIL"):
        print(f"== {cat}: {len(rows[cat])}")
        for r in rows[cat]:
            print(f"   {r[0]}: rewardRow={r[1]} staleRow={r[2]} retail={r[3]}")

    out = OUT_DIR / "reward-contracts-crosscheck.tsv"
    with out.open("w", encoding="utf-8") as handle:
        handle.write("quest_id\treward_row\tstale_row\tretail_setprogress\tcategory\n")
        for cat in ("RISKY", "OK_RETAIL", "NO_RETAIL"):
            for r in rows[cat]:
                handle.write(f"{r[0]}\t{r[1]}\t{r[2]}\t{r[3]}\t{cat}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
