# Sirens Visuals 2.0 (Fabric, Minecraft 1.21.11)

Open the menu with **Right Shift**. Everything is customizable from there (search box, per-module reset, HSV color pickers).
Also: HUD Editor (drag/drop, scroll to scale, right-click to toggle), Hand Position editor, Skin & Cape Studio, Emote wheel (V),
Zoom (C, scroll to adjust), Freelook (Left Alt), Cinematic camera (O), Fullbright (B).

Settings: `config/sirensvisuals.json`. Skins go in `config/sirensvisuals/skins`, capes in `config/sirensvisuals/capes`.

## YouTube Music lyrics / ad mute
Install Tampermonkey, open the menu > Music > "Copy Userscript Install URL", paste it into your browser, install, then open music.youtube.com.
(Also in `userscripts/`.)

## Hook diagnostics
Menu > System > Hook Diagnostics shows which render hooks are running. Hooks that fail to apply are skipped, never crash the game.
If something does nothing, send `logs/latest.log` and this screen's state.
