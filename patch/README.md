# Aion 5.8 客户端补丁 | Client Patches

[中文](#目录结构说明) | [English](#english)

本项目提供的配套 Aion 5.8 客户端补丁文件与资源。

## 目录结构说明

- **`bin64/`** — 核心运行库补丁（`Game.dll`），提供客户端运行与兼容支持。
- **`bin64.7z`** — 客户端运行库压缩包，需先解压再覆盖（见下方安装步骤）。
- **`L10N/CHS/`** — 简体中文任务文本与界面汉化包（`data.pak`）。
- **`Levels/`** — 地图与地形数据修正（`Level.pak`）。
- **`Textures/ui/`** — 界面 UI 图标与纹理更新（`ui.pak`）。

---

## 安装与使用方法

> **重要提示**：在覆盖任何补丁前，请务必先备份客户端中的原始同名文件与目录！

1. 找到您的 **Aion 5.8 客户端** 安装根目录。
2. 备份客户端中原有的 `bin64/`、`L10N/`、`Levels/`、`Textures/` 等目录。
3. 使用 7-Zip 解压 `bin64.7z`，将其内容覆盖到客户端安装根目录的 `bin64/` 目录。
4. 按照本 `patch/` 下的目录结构（对应子目录名：`bin64`、`L10N`、`Levels`、`Textures`），直接复制并覆盖到对应的客户端安装根目录中；其中 `bin64/Game.dll` 必须最后覆盖（晚于 `bin64.7z` 的解压）。

---

## English

Companion Aion 5.8 client patch files and resources provided by this project.

### Directory Structure

- **`bin64/`** — Core runtime patch (`Game.dll`), providing client runtime and compatibility support.
- **`bin64.7z`** — Client runtime archive; extract it before overwriting (see installation steps below).
- **`L10N/CHS/`** — Simplified Chinese quest dialogue and UI localization package (`data.pak`).
- **`Levels/`** — Map and terrain data corrections (`Level.pak`).
- **`Textures/ui/`** — Interface UI icons and texture updates (`ui.pak`).

---

### Installation & Usage Guide

> **Important Notice**: Before overwriting any patch files, always make sure to back up the original files and directories in your game client!

1. Locate your **Aion 5.8 Client** root installation directory.
2. Back up existing directories in your client, such as `bin64/`, `L10N/`, `Levels/`, `Textures/`, etc.
3. Extract `bin64.7z` with 7-Zip and overwrite the client's `bin64/` directory with its contents.
4. Following the directory structure under this `patch/` folder (matching subdirectories: `bin64`, `L10N`, `Levels`, `Textures`), copy and overwrite them directly into your client root installation directory; `bin64/Game.dll` must be overwritten last (after extracting `bin64.7z`).
