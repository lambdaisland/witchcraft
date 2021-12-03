# Unreleased

## Fixed

- Fixed megachop 9000

# 0.6.163 (2021-12-03 / 493bdc7)

## Fixed

- Bugfix release

# 0.6.160 (2021-12-03 / 1857689)

## Added

- lambdaisland.witchcraft.fill API
- gallery item: MegaChop 9000
- more docstrings
- normalize-text

## Changed

- `set-lore` and `set-display-name` now implicitly render markup

# 0.6.154 (2021-12-02 / dfda8b7)

## Fixed

- Fixed reflection warnings

# 0.6.142 (2021-12-01 / 787e7c1)

## Added

- Support for Paper 1.18

## Fixed

- Issues with Paper's new classloader behavior
- Fixed cljdoc builds, finally browsable docs!

# 0.5.98 (2021-10-17 / 55eabf3)

## Fixed

- Fix  Glowstone's optimized set-blocks implementation,  it used a way  to get a
  default world which has been superceded by `(wc/default-world)`

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
