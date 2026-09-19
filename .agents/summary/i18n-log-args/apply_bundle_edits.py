#!/usr/bin/env python3
"""Apply the message-template edits required by the throwable-relocation codemod.

用法 / Usage: python3 .agents/summary/i18n-log-args/apply_bundle_edits.py [--write]
"""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
EN = ROOT / "src/main/resources/messages.properties"
ZH = ROOT / "src/main/resources/messages_zh_CN.properties"

# 模板不再接收异常：占位符随参数一并移除 / The template no longer receives the throwable.
EDITS = {
    "log.3d086e850a89": ("Script compression failed", "脚本压缩失败"),
    "log.4c634fc1f594": ("Script decompression failed", "脚本解压失败"),
    "log.6cadba7ab22f": ("Could not instantiate HouseScriptData", "无法实例化 HouseScriptData"),
    "log.cc03391ccf0f": ("Exception when running visitor on all players", "对所有玩家运行访问器时异常"),
    "log.dd62a8c2d8c4": ("Could not instantiate XmlFormatter", "无法实例化 XmlFormatter"),
    "log.cf34446ab3a6": ("Critical Error - Thread: {0} terminated abnormally", "严重错误 - 线程 {0} 异常终止"),
    "log.418d26f689fd": ("Failed to persist abyss rank for player {0}", "持久化玩家 {0} 的欧比斯军阶失败"),
    "log.96bb344ecaba": ("Failed to persist broker item {0}", "持久化交易所物品 {0} 失败"),
    "log.2d8239fcf778": ("Failed to persist letter for player {0}", "持久化玩家 {0} 的邮件失败"),
    "log.30a6c4139e12": ("Failed to persist broker transaction for player {0}", "持久化玩家 {0} 的交易所事务失败"),
    "log.39a7be863899": ("Failed to persist exchange transaction for players {0} and {1}", "持久化玩家 {0} 与 {1} 的交易事务失败"),
    "log.d40fe011fd92": ("Failed to persist mail transaction from player {0} to player {1}", "持久化玩家 {0} 到玩家 {1} 的邮件事务失败"),
    "log.9b61ee4df55e": ("Failed to persist mail attachment claim for player {0}, letter {1}", "持久化玩家 {0} 的邮件 {1} 附件领取事务失败"),
    "log.bf6c47a4810b": ("Failed to persist system mail transaction for player {0}", "持久化玩家 {0} 的系统邮件事务失败"),
    "log.07f358910e11": ("Error - Thread: {0} terminated abnormally", "错误 - 线程 {0} 异常终止"),
    "log.e0628bfd2f2b": ("Failed to persist player transfer transaction {0}", "持久化角色转服事务 {0} 失败"),
    "log.da39a3ee5e6b": ("Unexpected error", "发生未预期的错误"),
    "log.49697309af30": ("DeadLockDetector", "检测到死锁"),
}


def apply(path: Path, index: int, write: bool) -> int:
    lines = path.read_text(encoding="utf-8").split("\n")
    seen = set()
    for i, line in enumerate(lines):
        for key, values in EDITS.items():
            if line.startswith(key + "="):
                lines[i] = key + "=" + values[index]
                seen.add(key)
    missing = sorted(set(EDITS) - seen)
    if missing:
        raise SystemExit(f"{path.name}: missing keys {missing}")
    if write:
        path.write_text("\n".join(lines), encoding="utf-8")
    return len(seen)


def main() -> int:
    write = "--write" in sys.argv
    print("en updated:", apply(EN, 0, write))
    print("zh updated:", apply(ZH, 1, write))
    print("write =", write)
    return 0


if __name__ == "__main__":
    sys.exit(main())
