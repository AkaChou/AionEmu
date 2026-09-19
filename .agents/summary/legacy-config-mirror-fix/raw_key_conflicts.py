#!/usr/bin/env python3
"""列出跨服务取值不一致的遗留原始键（镜像必须丢弃的键）。

Mimics AionLegacyPropertySourceEnvironmentPostProcessor load order for a given
config root and reports raw keys that two services define with different values.

用法 / Usage: python3 raw_key_conflicts.py <config-root> [...]
"""

import sys
from pathlib import Path


def load_file(path, raw, service_raw, service_name):
    if not path.is_file():
        return
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()
        if raw:
            service_raw[key] = value


def load_dir(directory, raw, service_raw):
    if not directory.is_dir():
        return
    for path in sorted(p for p in directory.iterdir() if p.is_file() and p.suffix == ".properties"):
        load_file(path, raw, service_raw, directory.name)


def raw_maps(root):
    services = {}
    for name, dirs in (("game", ["administration", "main", "network"]), ("login", ["login"]), ("chat", ["chat"])):
        raw = {}
        for d in dirs:
            load_dir(root / d, True, raw)
        services[name] = raw
    # 共享 network.properties 在 login/chat 只写入带前缀的键。 / Shared file is prefixed-only for login and chat.
    load_file(root / "network/network.properties", False, services["login"], "login")
    load_file(root / "network/network.properties", False, services["chat"], "chat")
    load_file(root / "mygs.properties", True, services["game"], "game")
    load_file(root / "login/myls.properties", True, services["login"], "login")
    load_file(root / "chat/mycs.properties", True, services["chat"], "chat")
    return services


def main():
    for root_arg in sys.argv[1:]:
        root = Path(root_arg)
        services = raw_maps(root)
        print(f"=== {root} ===")
        keys = sorted(set().union(*(m.keys() for m in services.values())))
        for key in keys:
            values = {name: m[key] for name, m in services.items() if key in m}
            if len(set(values.values())) > 1:
                print(f"DROPPED  {key}")
                for name, value in values.items():
                    print(f"         {name}: {value}")
        watched = [k for k in keys if k.startswith("gameserver.thread.") or k.startswith("svstats.")]
        print(f"watched bean keys kept: {sorted(watched)}")


if __name__ == "__main__":
    main()
