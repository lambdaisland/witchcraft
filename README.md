# witchcraft

<!-- badges -->
[![cljdoc badge](https://cljdoc.org/badge/com.lambdaisland/witchcraft)](https://cljdoc.org/d/com.lambdaisland/witchcraft) [![Clojars Project](https://img.shields.io/clojars/v/com.lambdaisland/witchcraft.svg)](https://clojars.org/com.lambdaisland/witchcraft)
<!-- /badges -->

Clojure API for Minecraft/Glowstone/Bukkit

This project uses the [GlowstoneMC](https://github.com/GlowstoneMC/Glowstone) open source minecraft server and wraps the [Bukkit](https://github.com/Bukkit/Bukkit) modding API.

Note that the API is not meant to be exhaustive, it will likely still grow, as
well as evolve. We are growing this library literally by playing with it, trying
to bend Glowstone to our will, following our fancy. That means we aren't
committed to quite the same level of backwards compatibility as we are with our
"serious" projects. That said once Witchcraft reaches a certain degree of
adoption we'll think twice about making breaking changes.

As we go on we experiment with different approaches and try to make things
better. When in doubt just pin yourself to a specific version and have fun with
that! With cljdoc you should be able to find the docs for the version you are
running.

## Features

- Start a Minecraft server from your Clojure REPL
- Create and control world, blocks, creatures, players, etc
- Handle events and create custom interactions
- Generate landscape and buildings with a cursor/turtle style API

<!-- installation -->
## Installation

To use the latest release, add the following to your `deps.edn` ([Clojure CLI](https://clojure.org/guides/deps_and_cli))

```
com.lambdaisland/witchcraft {:mvn/version "0.0.19"}
```

or add the following to your `project.clj` ([Leiningen](https://leiningen.org/))

```
[com.lambdaisland/witchcraft "0.0.19"]
```
<!-- /installation -->

## Rationale

Most Minecraft "servers" are really just modifications or extensions of the
proprietary server software from Mojang. This means no source of developer docs
are available, and writing extensions often involves using reflection and using
obfuscated, cryptic method names. It also means elaborate hacks are involved in
launching the server, and patching the software.

Glowstone on the other hand is a truly open source Minecraft server written from
scratch, making it much easier to deal with. We can simply add it to a project
as another dependency, and start and control the server from the REPL.

Note that you still need Minecraft itself (Minecraft Java Edition in particular,
aka "the client"), to connect to this server.

What you do with Witchcraft is up to you. You can simply use it as a voxel
engine, a place to render your 3D block based creations, or you can create a
completely novel space for you and your friends to hang out in, filled with your
own creations, and flavored with custom behaviors, systems and mechanisms.

## Usage

Start by starting the server:

```clojure
(require '[lambdaisland.witchcraft :as wc])
(wc/start!)
```

And connecting to it from Minecraft Java Edition.

### Minecraft Client Setup

Glowstone currently only supports minecraft version 1.12.2 (this may change
soon). To install the 1.12.2 MC Client version, open the Minecraft launcher ->
Installations -> New -> 1.12.2.

After launching Minecraft, select Multiplayer and enter `localhost:25565` to join the server we just started.

Hold `F3` and press `p` so you can tab out without the game pausing.

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
