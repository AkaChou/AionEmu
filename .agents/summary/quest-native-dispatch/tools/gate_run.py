#!/usr/bin/env python3
"""看门狗执行器：跑长命令、落日志、写**完成哨兵**，超时杀进程组。

用途（quest-native-dispatch 车道）：消除"命令/测试已经跑完但执行体仍在等待"的空等——
① 完成状态**写进日志尾部**（`[DONE] exit=…`），执行体只需一次 `tail` 即可判定，不依赖进程退出事件；
② 超时硬上限（macOS 无 GNU `timeout`，本机实测只有 `perl`/`python3`）；
③ 超时按**进程组**杀（`start_new_session` + `killpg`），连带 surefire fork 一起收掉，
   避免"测试已出结果、fork 悬停、调用方永等"。

用法 / Usage:
    直接前台跑：   python3 gate_run.py <超时秒> <日志路径> <命令> [参数...]
    后台不阻塞：   tools/gate_bg.sh <日志名> <超时秒> -- <命令...>

退出码：命令退出码；超时 = 124。

中文/English bilingual notes kept minimal per .agents/rules/i18n.md (tooling script, not product code).
"""
from __future__ import annotations

import os
import signal
import subprocess
import sys
import time


def main() -> int:
    if len(sys.argv) < 4:
        print(__doc__, file=sys.stderr)
        return 2
    limit = float(sys.argv[1])
    log_path = sys.argv[2]
    cmd = sys.argv[3:]

    os.makedirs(os.path.dirname(os.path.abspath(log_path)), exist_ok=True)
    started = time.strftime("%F %T")
    t0 = time.time()
    with open(log_path, "w", encoding="utf-8") as fh:
        fh.write(f"# cmd: {' '.join(cmd)}\n# start: {started} limit={limit:.0f}s\n")
        fh.flush()
        # 独立会话 ⇒ 可用 killpg 连带杀掉 mvn 派生的 surefire fork。
        # New session => killpg reaches the forked surefire JVM too.
        proc = subprocess.Popen(
            cmd,
            stdout=fh,
            stderr=subprocess.STDOUT,
            stdin=subprocess.DEVNULL,
            start_new_session=True,
        )
        try:
            rc = proc.wait(timeout=limit)
        except subprocess.TimeoutExpired:
            os.killpg(os.getpgid(proc.pid), signal.SIGKILL)
            proc.wait()
            rc = 124
        elapsed = time.time() - t0
        fh.write(f"[DONE] exit={rc} elapsed={elapsed:.0f}s {time.strftime('%F %T')}\n")
    print(f"[DONE] exit={rc} elapsed={elapsed:.0f}s log={log_path}")
    return rc


if __name__ == "__main__":
    sys.exit(main())
