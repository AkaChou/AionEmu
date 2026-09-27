#!/usr/bin/env python3
"""T3 失败类对账工具 / T3 failure-class reconciler.

从 surefire 全量日志里抽取失败条目（`[ERROR] com.aionemu...Class.method:line`），
按「类集」与「类.方法集」两个粒度输出，用于切片前后零新增红点的机械对拍。
Extracts failing entries from a full surefire log and prints class-level and
class.method-level sets so a slice can prove "no new failures" mechanically.

用法 / Usage:
  python3 -B t3_failure_diff.py A.log B.log      # 两侧差集（B 相对 A 的新增/消失）
  python3 -B t3_failure_diff.py A.log            # 单侧清单
"""
import re
import sys
from pathlib import Path

ENTRY = re.compile(r'^\[ERROR\] (com\.aionemu\.[A-Za-z0-9_.$]+)')


def entries(path):
    methods = set()
    for line in Path(path).read_text(encoding='utf-8', errors='ignore').splitlines():
        match = ENTRY.match(line)
        if match:
            methods.add(match.group(1))
    # 摘要行会被 tee 截断（无 `:行号` 后缀、方法名可能截尾），类名在截断点之前，
    # 故类 = 末个「首字母大写」的包段；方法级字符串两侧同为确定性截断，可比。
    # The tee wrapper truncates summary lines (no `:line` suffix, possibly cut
    # method name). Class names sit before the cut, so the class is the last
    # capitalised package segment; truncated method strings compare consistently.
    classes = set()
    for name in methods:
        parts = name.split('.')
        last = max(i for i, part in enumerate(parts) if part[:1].isupper())
        classes.add('.'.join(parts[:last + 1]))
    return classes, methods


def owner(name, classes):
    for candidate in sorted(classes, key=len, reverse=True):
        if name == candidate or name.startswith(candidate + '.'):
            return candidate
    return name


def show(label, classes, methods):
    print(f'--- {label}: classes={len(classes)} methods={len(methods)} ---')
    for name in sorted(classes):
        count = sum(1 for m in methods if owner(m, classes) == name)
        print(f'  {count:3d}  {name}')


def main(argv):
    if len(argv) == 2:
        classes, methods = entries(argv[1])
        show(argv[1], classes, methods)
        return 0
    left_classes, left_methods = entries(argv[1])
    right_classes, right_methods = entries(argv[2])
    show(argv[1], left_classes, left_methods)
    show(argv[2], right_classes, right_methods)
    print('=== only-in-right (新增 / new) ===')
    for name in sorted(right_classes - left_classes):
        print(f'  CLASS  {name}')
    for name in sorted(right_methods - left_methods):
        print(f'  METHOD {name}')
    print('=== only-in-left (消失 / gone) ===')
    for name in sorted(left_classes - right_classes):
        print(f'  CLASS  {name}')
    for name in sorted(left_methods - right_methods):
        print(f'  METHOD {name}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main(sys.argv))
