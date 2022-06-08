# Unreleased

## Added

- Add `world-seed`

# 0.35.314 (2022-06-08 / 4f9afaf)

## Fixed

- fix `empty-inventory` and `shapes/line`

# 0.34.310 (2022-06-07 / a7af9c7)

## Fixed

- Fix chicken-shape example
- Optimize wc/loc for maps and vectors
- Augment launcher code to deal with spaces in paths

# 0.33.307 (2022-06-05 / 4ae4713)

## Added

- Add `set-game-mode`

# 0.32.303 (2022-06-01 / 0cdeed9)

## Added

- Add matrix/translate

## Changed

- Fine tune gradient-gen, tube


# 0.31.300 (2022-06-01 / dbda30c)

## Added

- Add `lambdaisland.witchcraft.shapes/arch`
- Add `lambdaisland.witchcraft.matrix/extrude`

## Fixed

- Fix `lambdaisland.witchcraft.matrix/rotation-matrix`

## Changed

# 0.30.297 (2022-05-24 / a3d3a74)

## Added

- Add `lambdaisland.witchcraft.matrix/cross-product`
- Allow passing `:material` to `set-blocks` as a default
- make the step size for `lambdaisland.witchcraft.shapes/line` configurable

## Fixed

- return `nil` when asking for the material of a vector that doesn't have one, instead of erroring out

## Changed

- handle `item-meta` more generally (read-only currently)
- return an ordered sequence from `lambdaisland.witchcraft.shapes/line` in the order of the line, instead of a set
- Improvements to `lambdaisland.witchcraft.shapes/line`

# 0.29.292 (2022-05-17 / ad428d6)

## Fixed

- Locally cache results to `satisfies?` calls in `lambdaisland.witchcraft`,
  leading to a big speedup for a lot of operations

# 0.28.286 (2022-05-03 / 1cabce3)

## Added

- Added different arities for
  set-game-rule/set-game-rules/difficulty/set-difficulty, making the world
  argument optional

## Fixed

- Fix `map->Location` for vectors that contain material/direction info

## Changed

- Optimize `loc` for blocks
- Make `set-block` accept vectors for consistency

# 0.27.283 (2022-05-03 / 48a23fa)

## Added

- Added `lambdaisland.witchcraft.adventure.text`, which contains a conversion
  from Hiccup-like markup (a la `lambdaisland.witchcraft.markup`) to a Adventure
  Component
- When setting a display-name, if supported by the server, render to a Component
  instead of to a string. This fixes item-stack equality/isSimilar checks on Paper.
- Make inventory functions (add-inventory, contents, etc) take more types of
  objects, so you can call them directly on a block or itemstack that has an
  inventory.
- Added `into-inventory` as a convenience function
- Added `despawn`
- Added `create-explosion`

# 0.26.277 (2022-04-17 / eebe741)

## Fixed

- Make sure we use the right Glowstone artifact, in an attempt to appease cljdoc
- Add CitizensNPCs are optional dep, in an attempt to appease cljdoc
- Remove reference to non-API class from Citizens, in an attempt to appease cljdoc
- Remove second reference to non-API class from Citizens, in an attempt to appease cljdoc
- Put gallery code inside a function, in an attempt to appease cljdoc
- Put more gallery code inside a function, in an attempt to appease cljdoc
- Include launcher-api/progrock as optional deps in the pom, in an attempt to appease cljdoc
- Include paper-api as optional dep in the pom, in an attempt to appease cljdoc
- Remove paper-api as optional dep again, it requires JDK 17
- Remove reference to org.bukkit.block.data.BlockData, in an attempt to appease cljdoc
- Check for org.bukkit.block.data.Directional with `util/when-class-exists`, in an attempt to appease cljdoc

# 0.16.246 (2022-04-15 / 73f9b24)

## Changed

- Reduce usage of lambdaisland.classpath (and thus tools.deps), to make AOT easier

# 0.15.242 (2022-04-15 / 2a76029)

## Added

- More interop wrappers! `-block`, `-block-state`, `-set/get-custom-name`, `-player`
- Handle more input types in `player`, `display-name`, `set-display-name`, `get-block`
- Add `locv` as an alias to `xyz`
- Add `listen-raw!` for when you want to skip the `bean` call and get the raw bukkit event
- Add `e/cancel!` for convenient cancelling of events
- Add `private-chest` to the gallery, a mini-mod for when there's too much stealing on the server

# 0.14.239 (2022-04-14 / 855da06)

## Fixed

- Fix a regression that caused a load error on servers that support BlockData (1.13+)

## Changed

- Bump Clojure2D, XSeries

# 0.13.235 (2022-03-30 / 922aeda)

## Changed

- Rename `launch!` to the more accurate `launch-cmd`

# 0.12.232 (2022-03-30 / 7350cc8)

## Added

- Added functions to access entities, get/set difficulty, work with enchantments
- Overhaul and expand the support for working with inventories
- Add wrapper API for launcher-api

## Fixed

- Fixed Glowstone 1.12 compat

## Changed

- General version bumps

# 0.11.216 (2022-01-03 / 13ea02d)

## Added

- Add `HasXYZ`, `HasPitchYaw`, `HasEntity`, and `CanSpawn` protocol

## Changed

- Make `x`/`y`/`z`/`pitch`/`yaw` use the newer protocol + reflected
  implementation generation approach
- Make `spawn` work with anything that `CanSpawn`, including Citizens NPCs

# 0.10.213 (2022-01-02 / 1a44b44)

## Added

- Added reflection and event support for the Citizens API

## Changed

- Improvements to `cursor/block-facing`

# 0.8.205 (2021-12-31 / 637ef2a)

## Fixed

- Fix compatiblity issue on Paper 1.17

# 0.8.201 (2021-12-31 / 05608cb)

## Added

- Added a `:palette` option to `set-blocks`, similar to how the palette works
  with cursors
- Introduce a new `:block-facing` option in the cursor, to force the direction
  blocks face regardless of cursor direction.
- Added convenience functions in `lambdaisland.witchcraft.cursor`: `blocks` and
  `facing-direction?`.
  
## Fixed

- Improve `material-name`/`mat` and `xmaterial` to handle keywords and vectors
  consistently
- Set block direction after setting block-data, because block-data will
  otherwise overrule the direction.

## Changed

- Improve material-handling in cursor, material can also be a two element vector
  (material+block-data).

# 0.8.189 (2021-12-09 / 14cbbd6)

## Fixed

- Be more conservative when recursing in `display-name`, return `nil` if the
  type of object passed in does not provide a display-name

# 0.8.186 (2021-12-09 / 69954ed)

## Fixed

- Bugfix: Pre-declare `item-stack`

# 0.8.183 (2021-12-08 / 3a5471c)

## Added

- `break-naturally`
- `blockv`


# 0.8.178 (2021-12-07 / f93ce7b)

## Fixed

- Fix compile erros
- Make `wc/location` check for `HasLocation`
- Bring back coercion from Vector to Location
- In extend-signatures only extend the minimal set, so exclude descendant types
- Wrap the name normalization in megachop9000 in a delay so it can't cause load
  errors

# 0.7.175 (2021-12-06 / 8b3190d)

## Added

- The `fill` API can now take a set of `:materials` instead of a `:pred`
- Pas direction of a block in `set-blocks` when using vectors
- Accept both `:start` and `:anchor` in `set-blocks`
- Improved interop calls

## Changed

- `inventory` now returns a sequence of maps, use `get-inventory` if you want
  the bukkit object.

# 0.6.172 (2021-12-04 / 6d3ded1)

## Added

- API additions:
- `target-block` : find the block you are looking at
- `block-data` : get info about block properties as a map (post-flattening only)
- `set-block-data` : complimentary setter, set block data as map
- `cursor/rotation` : helper for a previously hidden flag to add an extra
  rotatio n to every block
- Rework `material` and `material-name` to be more polymorphic
- `set-blocks!` and `cursor/build!` now both optionally take an `:start`, to
  make the pattern easier of having location-independent structure generators
- Significant docstring improvements
- support for block data in `set-block`, `set-blocks`, `cursor/material`
- Add `shapes/rectube`, handy for houses and lot of other stuff.

## Fixed

- Reflection-based protocol implementations: skip private classes

## Changed

# 0.6.167 (2021-12-03 / 8b5d891)

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