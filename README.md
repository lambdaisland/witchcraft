# {project}

<!-- badges -->
[![CircleCI](https://circleci.com/gh/lambdaisland/{project}.svg?style=svg)](https://circleci.com/gh/lambdaisland/{project}) [![cljdoc badge](https://cljdoc.org/badge/lambdaisland/{project})](https://cljdoc.org/d/lambdaisland/{project}) [![Clojars Project](https://img.shields.io/clojars/v/lambdaisland/{project}.svg)](https://clojars.org/lambdaisland/{project})
<!-- /badges -->

This project uses the [GlowstoneMC](https://github.com/GlowstoneMC/Glowstone) open source minecraft server and wraps the [Bukkit](https://github.com/Bukkit/Bukkit) modding API.

## Installation

```bash
git clone https://github.com/plexus/witchcraft/
cd witchcraft

# this will download some libraries and run the repl
clj
```

Inside the repl we will start the minecraft server

```clojure
(require '[lambdaisland.witchcraft :as wc])
(wc/start!)
```

Now we can jack-in to this repl from our editor and start evaluating code. To check some sample session, check out the `repl_sessions` directory.

## MC Client Setup

GlowstoneMC only supports minecraft version 1.12.2. To install the 1.12.2 MC Client version, open the Minecraft launcher -> Installations -> New -> 1.12.2.

After launching Minecraft, select Multiplayer and enter `localhost:25565` to join the server we just started.

Hold `F3` and press `p` so you can tab out without the game pausing.

## License

Copyright &copy; 2019 Arne Brasseur

Licensed under the term of the Mozilla Public License 2.0, see LICENSE.

Available under the terms of the Eclipse Public License 1.0, see LICENSE.txt
