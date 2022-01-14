# witchcraft

<!-- badges -->
[![cljdoc badge](https://cljdoc.org/badge/com.lambdaisland/witchcraft)](https://cljdoc.org/d/com.lambdaisland/witchcraft) [![Clojars Project](https://img.shields.io/clojars/v/com.lambdaisland/witchcraft.svg)](https://clojars.org/com.lambdaisland/witchcraft)
<!-- /badges -->

Clojure API for [Bukkit](https://github.com/Bukkit/Bukkit)-based Minecraft
servers (Spigot/Paper/Glowstone)

## Features

- Create and control world, blocks, creatures, players, etc
- Handle events and create custom interactions
- Cursor/turtle API
- Shapes API
- Vector/Matrix transformation API
- Palette/Color API

<!-- installation -->
## Installation

There are two ways to use Witchcraft, for most cases the best option is using
the [Witchcraft Plugin](https://github.com/lambdaisland/witchcraft-plugin). This
provides a REPL connection, and the ability to load Clojure libraries. Download
the plugin jar for your server and start playing.

You can also start a Clojure REPL yourself, and then start a Minecraft server
from there. If you are already familiar with a Clojure workflow then maybe this
is the more convenient way to get started. This works best with Glowstone, since
then you don't have to worry about patching proprietary code or agreeing to the
EULA.


## Rationale

<!-- Most Minecraft "servers" are really just modifications or extensions of the -->
<!-- proprietary server software from Mojang. This means no source of developer docs -->
<!-- are available, and writing extensions often involves using reflection and using -->
<!-- obfuscated, cryptic method names. It also means elaborate hacks are involved in -->
<!-- launching the server, and patching the software. -->

<!-- Glowstone on the other hand is a truly open source Minecraft server written from -->
<!-- scratch, making it much easier to deal with. We can simply add it to a project -->
<!-- as another dependency, and start and control the server from the REPL. -->

<!-- Note that you still need Minecraft itself (Minecraft Java Edition in particular, -->
<!-- aka "the client"), to connect to this server. -->

<!-- What you do with Witchcraft is up to you. You can simply use it as a voxel -->
<!-- engine, a place to render your 3D block based creations, or you can create a -->
<!-- completely novel space for you and your friends to hang out in, filled with your -->
<!-- own creations, and flavored with custom behaviors, systems and mechanisms. -->

Clojure's interactive programming make it perfect for creative coding. We try to
provide convenient APIs so you can go and develop your ideas.

## Usage

Start by watching some of the [videos](https://www.youtube.com/playlist?list=PLhYmIiHOMWoGyYsWmcQN0sG40BRjnNGM3) to get a sense of what you can and to get inspired.

There is a work-in-progress [manual](doc/witchcraft_manual.org).


## Related projects

This is not the first attempt to bridge the gap between Clojure and Minecraft

- [clj-minecraft](https://github.com/CmdrDats/clj-minecraft)
- [bukkure](https://github.com/SevereOverfl0w/bukkure)
- [Bukkit4Clojure](https://github.com/cpmcdaniel/Bukkit4Clojure)
- [BukkitClj](https://github.com/mikroskeem/BukkitClj)


<!-- opencollective -->
## Lambda Island Open Source

<img align="left" src="https://github.com/lambdaisland/open-source/raw/master/artwork/lighthouse_readme.png">

&nbsp;

witchcraft is part of a growing collection of quality Clojure libraries created and maintained
by the fine folks at [Gaiwan](https://gaiwan.co).

Pay it forward by [becoming a backer on our Open Collective](http://opencollective.com/lambda-island),
so that we may continue to enjoy a thriving Clojure ecosystem.

You can find an overview of our projects at [lambdaisland/open-source](https://github.com/lambdaisland/open-source).

&nbsp;

&nbsp;
<!-- /opencollective -->

<!-- contributing -->
## Contributing

Everyone has a right to submit patches to witchcraft, and thus become a contributor.

Contributors MUST

- adhere to the [LambdaIsland Clojure Style Guide](https://nextjournal.com/lambdaisland/clojure-style-guide)
- write patches that solve a problem. Start by stating the problem, then supply a minimal solution. `*`
- agree to license their contributions as MPL 2.0.
- not break the contract with downstream consumers. `**`
- not break the tests.

Contributors SHOULD

- update the CHANGELOG and README.
- add tests for new functionality.

If you submit a pull request that adheres to these rules, then it will almost
certainly be merged immediately. However some things may require more
consideration. If you add new dependencies, or significantly increase the API
surface, then we need to decide if these changes are in line with the project's
goals. In this case you can start by [writing a pitch](https://nextjournal.com/lambdaisland/pitch-template),
and collecting feedback on it.

`*` This goes for features too, a feature needs to solve a problem. State the problem it solves, then supply a minimal solution.

`**` As long as this project has not seen a public release (i.e. is not on Clojars)
we may still consider making breaking changes, if there is consensus that the
changes are justified.
<!-- /contributing -->

<!-- license -->
## License

Copyright &copy; 2021 Arne Brasseur and Contributors

Licensed under the term of the Mozilla Public License 2.0, see LICENSE.
<!-- /license -->
