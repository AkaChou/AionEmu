#!/usr/bin/env python3
"""记录客户端实测通过（2026-10-05 用户确认）：DIAGNOSIS R/S/T 段 + QE-138 validation。"""
D_PATH = '.agents/summary/quest-accept-silent-refusal-20261003/DIAGNOSIS.zh-CN.md'
lines = open(D_PATH, encoding='utf-8').read().split('\n')

# 行号（1-based）→ 插入行；从后往前插入避免偏移。
insertions = {
    468: '- 客户端实测（2026-10-05，用户确认）：**1126 检查页通过**——未集齐点任务行得「拿出蘑菇」检查页（不再回列表）。',
    451: '- 客户端实测（2026-10-05，用户确认）：**1118 交付链通过**——203079 发 select5 报告页、点「拿出药膏」进奖励窗、领奖完成。',
    424: '- 客户端实测（2026-10-05，用户确认）：**1115 复测通过**——中继推进回选择页后点「询问有关钓鱼的事情」正常回显（不再关窗）。',
}
for ln in sorted(insertions, reverse=True):
    lines.insert(ln, insertions[ln])  # 在 1-based 行 ln 之后插入
open(D_PATH, 'w', encoding='utf-8').write('\n'.join(lines))

# QE-138 validation 尾补实测
Q_PATH = '.agents/memory-bank/patterns/quest-engine.md'
text = open(Q_PATH, encoding='utf-8').read()
start = text.index('## [QE-138]'); end = text.index('\n## [QE-', start + 10)
card = text[start:end]
old = 'ItemPlay 15/15 全绿（三族门各加「未持门点 31 → 报告页 2375 + 零推进零扣物」断言）'
assert old in card
card = card.replace(old, old + '; 真机客户端实测（2026-10-05 用户确认）：1118 交付链（31→select5→「拿出药膏」→奖励窗）、1126 检查页（未集齐→检查页）、1115 翻页回显均通过', 1)
open(Q_PATH, 'w', encoding='utf-8').write(text[:start] + card + text[end:])
print('client verification recorded OK')
