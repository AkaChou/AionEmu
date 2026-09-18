#!/usr/bin/env python3
"""Quest XML 批量改写守卫：块级属性（如 transitions reported-reward-mode）静默丢失检测。

背景 / Why: 3b4e7fc4c 批量重写多杀任务的 <transitions> 开标签时，把 13841/13845/13849 的
reported-reward-mode="FIXED" 写成裸标签，直接关闭了客户端"无目标自动领奖"派生路径，
直到测试才发现。批量脚本/人工重写容器行前必须跑一次本守卫。

用法 / Usage:
  python3 .agents/summary/quest-counter-audit/check_quest_xml_block_attributes.py \
      [--base <git-rev>] [--head <git-rev>|WORKTREE] [--allow <path>:<tag>:<attr> ...]

退出码：0 = 没有属性丢失；1 = 发现丢失（列出 path:tag:attr）；2 = 用法/环境错误。
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

QUEST_XML_GLOB = "src/main/resources/aion/data/static_data/quest_definition/**/*.xml"
CONTAINER_TAGS = {
    "metadata", "progress", "nodes", "transitions", "kills", "items", "drops",
    "reward-groups", "class-rewards", "start-conditions", "prerequisites", "learn-skills",
    "bonuses", "rewards", "movie-continuation", "recovery", "use-item",
}
REPO_ROOT = Path(__file__).resolve().parents[3]


def git(*args: str) -> str:
    return subprocess.run(["git", *args], cwd=REPO_ROOT, check=True,
                          capture_output=True, text=True).stdout


def changed_quest_xml(base: str, head: str) -> list[str]:
    if head == "WORKTREE":
        args = ["diff", "--name-only", base, "--", QUEST_XML_GLOB]
    else:
        args = ["diff", "--name-only", base, head, "--", QUEST_XML_GLOB]
    return [line for line in git(*args).splitlines() if line.strip()]


def read_blob(rev: str, path: str) -> str | None:
    if rev == "WORKTREE":
        candidate = REPO_ROOT / path
        return candidate.read_text(encoding="utf-8") if candidate.exists() else None
    try:
        return git("show", f"{rev}:{path}")
    except subprocess.CalledProcessError:
        return None


def block_attributes(text: str) -> dict[str, dict[str, str]]:
    """返回 {tag: {attr: value}}，只统计容器级元素。 / Container-level attributes per tag."""
    root = ET.fromstring(text)
    found: dict[str, dict[str, str]] = {}
    for element in root.iter():
        if element.tag not in CONTAINER_TAGS:
            continue
        found.setdefault(element.tag, {}).update(element.attrib)
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="HEAD", help="对照基线（默认 HEAD）")
    parser.add_argument("--head", default="WORKTREE", help="被检查版本（默认工作区）")
    parser.add_argument("--allow", action="append", default=[],
                        help="显式放行 path:tag:attr（有意删除属性时必须留证）")
    args = parser.parse_args()

    allowed = set(args.allow)
    lost: list[str] = []
    checked = 0
    for path in changed_quest_xml(args.base, args.head):
        before = read_blob(args.base, path)
        after = read_blob(args.head, path)
        if before is None or after is None:
            continue
        checked += 1
        before_attrs = block_attributes(before)
        after_attrs = block_attributes(after)
        for tag, attrs in before_attrs.items():
            if tag not in after_attrs:
                continue
            for attr in attrs:
                if attr in after_attrs[tag]:
                    continue
                key = f"{path}:{tag}:{attr}"
                if key not in allowed:
                    lost.append(key)

    if checked == 0:
        print("QUEST_XML_REWRITE_GUARD_OK (no changed quest xml)")
        return 0
    if lost:
        print("QUEST_XML_BLOCK_ATTRIBUTE_LOSS_DETECTED")
        for key in lost:
            print(f"  lost: {key}")
        print("若确为有意删除，请在命令中加入 --allow <path>:<tag>:<attr> 并在 summary 留证。")
        return 1
    print(f"QUEST_XML_REWRITE_GUARD_OK (files={checked})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
