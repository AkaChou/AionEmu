#!/usr/bin/env python3
"""让客户端「血条显示数字」的 64 位 Game.dll 补丁。

Force numeric readouts on HP gauges by flipping bytes in Game.dll.

## 机理（证据文档 §14）

gauge 控件（UI 里 `type="gauge"` 的控件）是否画数字，由**控件字段**决定，
字段由 UI 数据属性写入：

| 字段 | UI 属性 | 取值 |
|---|---|---|
| `+0x8d4` | `num_type` | `default`(0) / `small`(1) / `micro`(2) |
| `+0x364` | `value_type` | `number`(0) / `float`(1) / `percent`(2) / `percent_float`(3) / `curNumber`(4) |

`num_type` 非 0 时，控件的更新函数 `0x108e125d`（在 `0x108e141c` 读 `num_type`、
`0x108e1465` 调数字文本函数 `0x1097d290`）会画数字。组队血条有数字，就是因为
数据里写了 `num_type="small"`（`UI/ui_game_override.xml` 的 party_dialog）。

本补丁把**读取 `num_type` 的那条指令强制成 1（small）**，于是所有走该更新路径的
gauge 控件都会画数字（目标窗口、组队、基础状态栏，以及——如果它用同一控件类——
怪物头顶的世界血条）。

## 补丁点

默认开启（v3，gauge 控件数字）：
| 文件偏移 | 原字节 | 改后 | 含义 |
|---|---|---|---|
| `0x8e141c` | `8b 83 d4 08 00 00` | `b8 01 00 00 00 90` | `mov eax,[rbx+0x8d4]` → `mov eax,1`（强制 small） |
| `0x8e13eb` | `74 7d` | `90 90` | 去掉「`[rbx+0x35c]==0` 就不画数字」 |
| `0x8e13f9` | `76 6f` | `90 90` | 去掉「`[rbx+0x368] > 当前值` 才画数字」 |

可选（默认关闭 = v1/v2 的旧假设，已被证伪为列表单元格渲染器）：
| 文件偏移 | 原字节 | 改后 | 含义 |
|---|---|---|---|
| `0x93ded4` | `bb 02 00 00 00` | `bb 01 00 00 00` | 单元格默认显示模式 2(text)→1(number) |
| `0x93e00a` | `8d 58 03` | `8d 58 01` | `naturally` 分支 3→1 |
| `0x93e021` | `b8 04 00 00 00` | `b8 01 00 00 00` | `param` 分支 4→1 |
| `0x93dd2b` | `02` | `01` | 兄弟函数 A' 常量行 1：2→1 |
| `0x93dd5c` | `02` | `01` | 兄弟函数 A' 常量行 2：2→1 |

用法 / Usage:
  python3 patch_game_dll_hpnum.py --source <原版 Game.dll> --out <输出 Game.dll>
  可选：--no-gauge_num / --no-gauge_cond1 / --no-gauge_cond2 关闭默认点；
        --with-cell_default 等打开旧点；--verify 只校验不写。

回滚 / Rollback：把客户端 bin64/Game.dll.bak 覆盖回 Game.dll（打补丁前逐字节相同）。
"""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path

IMG = 0x10000000
BASELINE_MD5 = "f77e0b4729842929d6ae8bb8ec128b6d"  # 客户端原版 bin64/Game.dll（== Game.dll.bak）

# (名称, 文件偏移, 原字节, 新字节, 默认开启)
EDITS = {
    # v3（默认开）：gauge 控件数字显示（控件更新函数 0x108e125d 内）
    "gauge_num": ("强制 num_type=small", 0x8E141C,
                  bytes.fromhex("8b83d4080000"), bytes.fromhex("b80100000090"), True),
    "gauge_cond1": ("去掉 [rbx+0x35c]==0 判定", 0x8E13EB,
                    bytes.fromhex("747d"), bytes.fromhex("9090"), True),
    "gauge_cond2": ("去掉 max>当前值 判定", 0x8E13F9,
                    bytes.fromhex("766f"), bytes.fromhex("9090"), True),
    # v1/v2（默认关）：列表单元格渲染器的显示模式
    "cell_default": ("单元格默认模式 2→1", 0x93DED4,
                     bytes.fromhex("bb02000000"), bytes.fromhex("bb01000000"), False),
    "cell_naturally": ("naturally 3→1", 0x93E00A,
                       bytes.fromhex("8d5803"), bytes.fromhex("8d5801"), False),
    "cell_param": ("param 4→1", 0x93E021,
                   bytes.fromhex("b804000000"), bytes.fromhex("b801000000"), False),
    "cell_static1": ("A' 常量行 1：2→1", 0x93DD2B,
                     bytes.fromhex("02"), bytes.fromhex("01"), False),
    "cell_static2": ("A' 常量行 2：2→1", 0x93DD5C,
                     bytes.fromhex("02"), bytes.fromhex("01"), False),
}


def md5(data: bytes) -> str:
    return hashlib.md5(data).hexdigest()


def main() -> None:
    ap = argparse.ArgumentParser(description="血条数字 Game.dll 补丁")
    ap.add_argument("--source", required=True, help="原版 bin64/Game.dll")
    ap.add_argument("--out", help="输出路径（--verify 时不需要）")
    ap.add_argument("--verify", action="store_true", help="只校验字节，不写文件")
    for key, (_label, _off, _old, _new, default_on) in EDITS.items():
        if default_on:
            ap.add_argument(f"--no-{key}", action="store_true", help=f"关闭默认补丁点 {key}")
        else:
            ap.add_argument(f"--with-{key}", action="store_true", help=f"启用（默认关）补丁点 {key}")
    args = ap.parse_args()

    src = Path(args.source).read_bytes()
    print(f"源文件 {args.source}: {len(src):,} 字节  MD5 {md5(src)}")
    if md5(src) == BASELINE_MD5:
        print("  → 与客户端原版一致（未打过其他补丁）")
    else:
        print("  → 注意：与记录的基线不同（可能已打过别的补丁），下面逐点校验原字节")

    selected = []
    for key, (_label, _off, _old, _new, default_on) in EDITS.items():
        want = (not getattr(args, f"no_{key}")) if default_on else getattr(args, f"with_{key}")
        if want:
            selected.append(key)
    if not selected:
        raise SystemExit("至少要保留一个补丁点")

    out = bytearray(src)
    for key in selected:
        label, off, old, new = EDITS[key][:4]
        cur = bytes(src[off:off + len(old)])
        if cur == new:
            print(f"  [跳过] {label}: 已是补丁后字节（VA 0x{IMG+off:012x}）")
            continue
        if cur != old:
            raise SystemExit(
                f"[失败] {label}: 偏移 0x{off:x} 期望 {old.hex(' ')}，实际 {cur.hex(' ')} —— 版本不符，已中止")
        out[off:off + len(new)] = new
        print(f"  [改写] {label}: VA 0x{IMG+off:012x}  {old.hex(' ')} → {new.hex(' ')}")

    diff = [i for i in range(len(src)) if src[i] != out[i]]
    print(f"差异字节共 {len(diff)} 处: {[hex(i) for i in diff]}")
    if args.verify:
        print("--verify：未写文件")
        return
    if not args.out:
        raise SystemExit("缺少 --out")
    Path(args.out).write_bytes(bytes(out))
    print(f"输出 {args.out}: {len(out):,} 字节  MD5 {md5(bytes(out))}")
    print("部署：覆盖客户端 bin64/Game.dll，然后**完全重启客户端**；回滚用 bin64/Game.dll.bak 覆盖回去。")


if __name__ == "__main__":
    main()
