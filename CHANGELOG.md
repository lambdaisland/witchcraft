# 0.4.94 (2021-10-13 / ebf25c2)

## Fixed

- Fix support for Glowstone 1.12, which doesn't yet have the GameRule class

# 0.3.90 (2021-10-13 / aec52ea)

## Added

- Support for PaperMC, see `start-paper!` and `lambdaisland.witchcraft.paper`.
  This means you have now the option to use Witchcraft with a fully up-to-date
  1.17.1 version of Minecraft
- A cross-server/cross-version plugin for easier getting started, see [https://github.com/lambdaisland/witchcraft-plugin](lambdaisland/witchcraft-plugin)
- Support for flattened blocks/materials, using XSeries
- `loc`, like `location`, but returns a Clojure map instead of a bukkit `Location`
- `xmaterial` to coerce to an XSeries `XMaterial` instance
- added a printer implementation for `bukkit/Block`
- Palette/gradient helpers (see `lambdaisland.witchcraft.palette`
- nREPL middleware to dispatch evaluations on the main server thread

## Fixed

- Much improved cross-server and cross-version support
- Detect event types dynamically
- Post-flattening material names, even on 1.12 (e.g. `:red-wool` instead of
  `[:wool 14]`, `:crafting-table` instead of `:workbench`)

## Changed

- The Glowstone dependency is now BYO (bring your own), since you can alternatively use Paper
- `start!` is gone, use `start-glowstone!` or `start-paper!`
- `players` is now called `online-players`
- `set-block-direction` is now called `set-direction`
- `set-block` no longer takes a vector of `[material data]`, since this is going
  away post-flattening. Variants can be specified with their flattened name
  directly, direction can be provided in the map as a `:direction` keyword.
- `direction` now returns a direction keyword rather than a Vector. The former
  is now called `direction-vec`

# 0.0.28 (2021-07-07 / a28e463)

# 0.0.17 (2021-07-05 / b32fdef)