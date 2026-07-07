# Lovic's Better Command Block

[![English](https://img.shields.io/badge/lang-English-blue)](./README.md)
[![简体中文](https://img.shields.io/badge/lang-简体中文-red)](./README_zh.md)

---


**Better Command Block UI** —— A command block GUI enhancement mod built for map makers and command creators.

| Detail | Description |
|------|------|
| **Mod ID** | `lovicsbettercommandblock` |
| **Current Version** | 1.1.1 |
| **Game Version** | Minecraft Java Edition **1.20.1** |
| **Loader** | Fabric (Fabric API Required) |
| **Author** | ZhuHongMing (Bilibili: 洛维Lovic) |

---

## Introduction

Lovic's Better Command Block (LBCB) fully optimizes the command block editing experience **without changing vanilla command block mechanics**:

- Large **multi-line text field** with auto-wrap and smooth vertical scrolling
- Command **syntax highlighting** and vanilla-style **auto-completion**
- **Custom names**, hand-held editing, and rich tooltips
- **Command preset** system: Export / import `.pvic` templates, sharing the config folder with VicColor
- **Alt+C** to quickly copy integer coordinates of the block you are looking at

Perfect for map creators, server admins, and advanced technical players who constantly write, debug, and reuse command blocks.

---

## Requirements

1. **Minecraft Java Edition 1.20.1**
2. **Fabric Loader** (≥ 0.15.0 recommended)
3. **Fabric API**
4. Place the `.jar` into your game's `mods` folder

---

## Features

### UI Enhancements

Right-clicking a command block (or command block minecart) opens the newly designed GUI:

| Feature | Description |
|------|------|
| Multi-line Field | Replaces the single-line input; wraps long commands with mouse scroll support |
| Syntax Highlighting | Unparsed segments, literals, and parameters are distinguished by colors |
| Auto-completion | Retains vanilla Brigadier suggestions, displayed in an independent panel |
| Undo / Redo | Text undo/redo actions are fully supported within the input field |
| Focus Mode | Focus the input field during deep edits; press **Esc** or click **×** to exit focus |
| Custom Names | Add a custom name at the top; persists even after reloading the world |
| Name Plates | Looking at a named command block displays its custom name hovering above |

The top of the interface provides three tabs:
- **Edit Command** — Daily command block editing
- **Import Preset** — Instantly import a configuration from the `.pvic` preset list
- **Export Template** — Export the current command block settings as a `.pvic` file

### Hand-held Editing

While holding a **Command Block** or **Command Block Minecart** item:
- **Middle-Click** (Without holding Ctrl): Directly opens the editing UI for the held item
- Hold **Ctrl** + Middle-Click: Performs the vanilla "Pick Block" behavior

Edited data is saved directly to the item's NBT and applies once placed in the world.

### Tooltip Details

Hover over a command block item that contains data:
- Default hint: Hold **[Ctrl]** to view detailed information
- While holding **Ctrl**: Displays command content, block type (Impulse/Chain/Repeat), conditional status, redstone control, and output tracking

### Command Presets

LBCB offers a complete template workflow. The preset files use the **`.pvic`** format and share a directory with the **VicColor** mod:
`config/viccolor/custom/`

#### Preset Menu
- Press **`\\`** in-game (Backslash key, default, rebindable in Controls)
- Browse built-in and custom presets, filterable by categories and tags
- Click an entry to obtain a **preset item** that generates the configured command block upon placement
- Features buttons to "Open Template Folder" and "Export Command Template" inside the menu

#### GUI Importing
1. Open the command block GUI
2. Switch to the **"Import Preset"** tab
3. Click your target preset → Auto-imports and returns to the **"Edit Command"** page

#### Template Exporting
1. Switch to the **"Export Template"** tab in the GUI
2. Fill in the name, select an icon, block type, tags, etc.
3. Exports as a `.pvic` file to the config directory (supports sub-folders)

You can assign the LBCB-exclusive **"command"** tag (displayed in cyan) to make pure command templates easy to identify.

#### Preset Tags

| Tag | Meaning |
|------|------|
| pattern | Pattern-based presets |
| animation | Animation-based presets |
| text | Text-based presets |
| command | LBCB-exclusive "Command" templates |

### Copy Coordinates

Aim at any block and press **Alt + C**:
- Copies the **integer coordinates** of the targeted block to your clipboard
- Format: `x y z` (three integers, separated by a single space)
- Rounding logic is identical to the vanilla **F3 + I** data copy
- Upon success, a chat message will confirm the copy

> Only works when looking directly at a block; does not copy when pointing at air or entities.

---

## Keybinds

| Key | Function |
|------|------|
| `\` | Open the command preset menu (rebindable) |
| **Alt + C** | Copy integer coordinates of targeted block |
| **Middle-Click** (Holding block, no Ctrl) | Open editing GUI for the held item |
| **Ctrl + Hover** | View detailed tooltips on command block items |
| **Esc** (When focused) | Exit input field focus |

Keybinds can be adjusted in **Options → Controls → Lovic's Better Command Block** (Alt+C is a global key combination and currently has no separate rebind option).

---

## Compatibility

- Preset folder is unified at `config/viccolor/custom/`
- `.pvic` file structures are completely compatible with VicColor; LBCB's new `command` tag is ignored by VicColor and won't affect its normal operation
- **Can be used standalone** for preset features without installing VicColor

---

## FAQ

**Q: The interface didn't change after installation?**  
A: Please ensure your game version is 1.20.1, and both Fabric Loader and Fabric API are correctly installed. The mod jar must be in the `mods` folder. You must be in **Creative Mode** or have OP permissions to edit command blocks.

**Q: The preset list is completely empty?**  
A: Move `.pvic` files into `config/viccolor/custom/` (or its sub-folders), or click "Export Command Template" in the preset menu to create a new one.

**Q: Command content is lost after exporting a template?**  
A: This issue where command text went missing when returning from the "Export Template" page has been fixed since version 1.1.0. Please update to the latest version.

**Q: Command block custom names disappear after reloading the world?**  
A: Fixed since 1.1.0; custom display names are now saved persistently.

**Q: Game crashes when middle-clicking a held block?**  
A: Fixed since 1.1.0; please update to the latest release.

**Q: What should I know when upgrading from the old `bettercommandblock`?**  
A: Please delete the old `bettercommandblock-*.jar` and use `lovicsbettercommandblock-1.1.1.jar`. The Mod ID has been changed to `lovicsbettercommandblock`. Saved custom names and preset items from older worlds will automatically migrate from old NBT keys (`BCBDisplayName`, etc.) and will overwrite with new keys (`LBCBDisplayName`, etc.) upon saving.

**Q: Mod icon doesn't display in launchers like PCL?**  
A: Please use the official release build jar that embeds `assets/lovicsbettercommandblock/icon.png`.

---

## Changelog

### 1.1.1
- **Copy Coordinates**: Press Alt+C while aiming at a block to copy its integer coordinates `x y z` (matching F3+I rounding logic).

### 1.1.0
- **New Features**: Command preset system, import/export pagination, custom cyan "command" tag, hand-held middle-click editing, and persistent custom names.
- **UI Tweaks**: Text color in the custom name box changed to white; reworked multi-line fields and tab layout interfaces.
- **Bug Fixes**: Resolved major bugs including text loss when exiting the export page, name resetting on reload, and crashes triggered by hand-held middle-clicks.

---

## License & Contact

- **QQ Group / Author QQ**: 656147529
- **License**: This mod is released under the **MIT License**. Feedbacks and suggestions are always welcome!
