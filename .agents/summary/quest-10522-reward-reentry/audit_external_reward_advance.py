#!/usr/bin/env python3
"""审计“引擎外推进 REWARD”的任务合同（writer packed step / reward 投影 / 领奖态入口页）。

背景：typed 引擎按 (status, packed step) 匹配路由，而部分客户端包、服务与 AI 直接
`qs.setStatus(QuestStatus.REWARD)` 把任务推进到领奖态。只要这些写入方留下的 packed step
与任务定义 `reward` 节点投影不一致，该状态就匹配不到任何领奖路由（客户端点任务行后只剩
通用“结束对话”页）；即使一致，若定义没有注册 `reward + QUEST_SELECT(31) -> DEFAULT_SUCCESS`
入口页，玩家也无法从领奖态打开 select_success(10002) → 1009 → 奖励窗口 5。

本脚本只做只读扫描 + 输出基线 TSV，不修改任何文件。用法：

  python3 .agents/summary/quest-10522-reward-reentry/audit_external_reward_advance.py --out <path>
"""
from __future__ import annotations

import argparse
import re
import subprocess
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
JAVA_ROOT = REPO / "src/main/java"
QUEST_DIR = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"

# 引擎内写入方（questEngine 包）与管理员/GM 指令不属于“引擎外推进”，不参与本审计。
EXCLUDED_PATH_PARTS = (
    "src/main/java/com/aionemu/gameserver/questEngine/",
    "src/main/java/com/aionemu/gameserver/commands/",
    "src/main/java/com/aionemu/gameserver/network/aion/gmhandler/",
)
REWARD_WRITE_RE = re.compile(r"setStatus\s*\(\s*QuestStatus\.REWARD\s*\)")
QUEST_ID_RE = re.compile(r"\b(\d{3,5})\b")
SET_VAR_RE = re.compile(r"setQuestVar(?:ById\s*\(\s*0\s*,\s*(\d+)\s*\)\s*|\(\s*(\d+)\s*\))")


METHOD_DECL_RE = re.compile(
    r"(?P<sig>(?:public|private|protected|static|final|synchronized|native|abstract|default|\s)*"
    r"[\w<>\[\],.\s]+?\s+"
    r"(?P<name>(?!if\b|for\b|while\b|switch\b|catch\b|do\b|else\b|try\b|return\b)\w+)"
    r"\s*\([^;{}]*\)\s*(?:throws[\w,\s.]+)?)\{")


def method_spans(text: str) -> list[tuple[str, int, int]]:
    """返回文件中全部方法声明的 (方法名, 起止下标)。 / All method spans as (name, start, end)."""
    spans = []
    for match in METHOD_DECL_RE.finditer(text):
        start = match.start()
        depth = 1
        cursor = match.end()
        while cursor < len(text) and depth:
            if text[cursor] == "{":
                depth += 1
            elif text[cursor] == "}":
                depth -= 1
            cursor += 1
        spans.append((match.group("name"), start, cursor))
    return spans


def enclosing_method(text: str, index: int) -> tuple[str, str]:
    """返回包含 index 的最近方法 (方法名, 方法体)。 / Nearest enclosing method as (name, body)."""
    candidates = [span for span in method_spans(text) if span[1] <= index < span[2]]
    if not candidates:
        return "?", ""
    name, start, end = min(candidates, key=lambda span: span[2] - span[1])
    return name, text[start:end]


def writer_step(block: str) -> int | None:
    match = SET_VAR_RE.search(block)
    if match is None:
        return None
    return int(match.group(1) if match.group(1) is not None else match.group(2))


def git_cat(revision: str, rel: str) -> str:
    """按 revision 读取仓库文件；HEAD/worktree 之外的历史 ref 用于“修复前”对照。 / Reads a file at a revision."""
    if revision in ("", "worktree"):
        return (REPO / rel).read_text(encoding="utf-8")
    return subprocess.run(["git", "-C", str(REPO), "show", f"{revision}:{rel}"],
                          check=True, capture_output=True, text=True).stdout


def java_files(revision: str) -> list[str]:
    if revision in ("", "worktree"):
        return sorted(path.relative_to(REPO).as_posix() for path in JAVA_ROOT.rglob("*.java"))
    listing = subprocess.run(["git", "-C", str(REPO), "ls-tree", "-r", "--name-only", revision],
                             check=True, capture_output=True, text=True).stdout.splitlines()
    return sorted(name for name in listing if name.startswith("src/main/java/") and name.endswith(".java"))


def scan_writers(revision: str) -> list[dict]:
    rows = []
    for rel in java_files(revision):
        if any(rel.startswith(part) for part in EXCLUDED_PATH_PARTS):
            continue
        text = git_cat(revision, rel)
        for match in REWARD_WRITE_RE.finditer(text):
            name, block = enclosing_method(text, match.start())
            quest_ids = sorted({int(q) for q in QUEST_ID_RE.findall(block)})
            rows.append({
                "file": rel,
                "method": name,
                "questIds": quest_ids,
                "step": writer_step(block),
            })
    return rows


def reward_projection(text: str) -> dict[str, int]:
    match = re.search(r'<node label="reward"[^>]*?(?:/>|>(.*?)</node>)', text, re.S)
    body = match.group(1) if match and match.group(1) else ""
    return {m.group(1): int(m.group(2)) for m in re.finditer(r'<var name="(\w+)" value="(\d+)"', body)}


def completion_npc_ids(text: str) -> list[int]:
    ids = {int(m.group(1)) for m in re.finditer(r'<npc-complete npc-id="(\d+)"', text)}
    for block in re.finditer(r"<transition\b[^>]*>(.*?)</transition>", text, re.S):
        body = block.group(1)
        if 'target="complete"' not in block.group(0):
            continue
        ids.update(int(m.group(1)) for m in re.finditer(r'npc-id="(\d+)"', body))
    return sorted(ids)


def has_reentry(text: str) -> bool:
    for block in re.finditer(r"<transition\b[^>]*>(.*?)</transition>", text, re.S):
        head, body = block.group(0), block.group(1)
        if 'source="reward"' not in head or 'target="reward"' not in head:
            continue
        if 'action="QUEST_SELECT"' in body and 'page="DEFAULT_SUCCESS"' in body:
            return True
    return False


def recovery_values(text: str) -> list[int]:
    values = []
    for block in re.finditer(r"<transition\b(?P<head>[^>]*)>(?P<body>.*?)</transition>", text, re.S):
        head, body = block.group("head"), block.group("body")
        if "source=" in head or "target=" not in head or 'target="reward"' not in head:
            continue
        if "<enter-world/>" not in body or '<status-is status="REWARD"/>' not in body:
            continue
        match = re.search(r'<variable-is field="var0" value="(\d+)"/>', body)
        if match:
            values.append(int(match.group(1)))
    return values


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--revision", default="worktree",
                        help="git ref to audit (default: working tree) / 要审计的 git ref（默认：工作区）")
    args = parser.parse_args()

    rows = []
    for writer in scan_writers(args.revision):
        for quest_id in writer["questIds"]:
            rel = f"src/main/resources/aion/data/static_data/quest_definition/quests/{quest_id}.xml"
            if not (REPO / rel).exists():
                continue
            try:
                text = git_cat(args.revision, rel)
            except subprocess.CalledProcessError:
                continue
            projection = reward_projection(text).get("var0")
            if projection is None:
                continue
            step = writer["step"]
            rows.append({
                "questId": quest_id,
                "writer": f'{writer["file"]}#{writer["method"]}',
                "writerStep": step if step is not None else 0,
                "writerKeepsStep": step is None,
                "rewardProjection": projection,
                "completionNpcIds": completion_npc_ids(text),
                "hasReentry": has_reentry(text),
                "recoveryValues": recovery_values(text),
            })

    dedup = {}
    for row in rows:
        dedup[(row["questId"], row["writer"])] = row
    rows = sorted(dedup.values(), key=lambda item: item["questId"])

    print(f"engine-external REWARD writers: {len({r['writer'] for r in rows})}, quests: {len(rows)}")
    for row in rows:
        flag = "MISMATCH" if row["writerStep"] != row["rewardProjection"] else "aligned"
        entry = "entry-page-ok" if row["hasReentry"] else "ENTRY-PAGE-MISSING"
        recovery = f"recovery={row['recoveryValues']}" if row["recoveryValues"] else "recovery=none"
        print(f'  {row["questId"]}: writer step={row["writerStep"]} '
              f'({"keeps" if row["writerKeepsStep"] else "writes"}) projection={row["rewardProjection"]} '
              f'{flag} {entry} {recovery} npcs={row["completionNpcIds"]}')

    lines = ["# questId\twriterEvidence\twriterStep\trewardProjection\tcompletionNpcIds\tstaleRewardSteps"]
    for row in rows:
        lines.append("\t".join([
            str(row["questId"]),
            row["writer"],
            str(row["writerStep"]),
            str(row["rewardProjection"]),
            " ".join(str(npc) for npc in row["completionNpcIds"]),
            # 空列用 "-" 占位：TSV 行尾 tab 会被 formatting.md 的「不得有行尾空白」规则判违规。
            # Empty column uses "-" so rows never end with a tab (no trailing whitespace rule).
            " ".join(str(value) for value in row["recoveryValues"]) or "-",
        ]))
    args.out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"baseline written: {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
