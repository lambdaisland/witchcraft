<!-- THIS FILE IS GENERATED FROM witchcraft_manual.org. DO NOT EDIT DIRECTLY -->

# Witchcraft Manual

1.  [Introduction](#org6766be1)
2.  [Minecraft Concepts](#orgbd729c0)
    1.  [Survival vs Creative](#org8cf9660)
    2.  [Whirlwind Tour of a Survival Game](#orge93da74)
    3.  [Worlds and Biomes](#orge39d1cc)
    4.  [Coordinate system](#org297e769)
    5.  [The flattening](#org8a6a95e)
    6.  [Resources and Crafting](#org7f2c762)
    7.  [Villagers and Traders](#org625a584)
    8.  [Farming](#orgb648f95)
    9.  [Redstone](#orge457fa6)
    10. [Enchanting and XP](#org5c83bd3)
    11. [Modding, Bukkit, SpigotMC](#orgfe9a22c)
    12. [Java vs Bedrock](#org79b6473)
    13. [Alternative Games](#org932f60d)
        1.  [Parkour](#org8f68c53)
        2.  [Speedrunner vs Hunters](#org977c184)
        3.  [Skyblock](#orgef8ae74)
        4.  [Build Wars](#orgeae4a1c)
        5.  [Bed Wars](#org541ad41)
3.  [Getting started with Witchcraft](#org1c500c9)
    1.  [Running the plugin](#org6643bf3)
    2.  [Running from the REPL](#orgfc4d378)
4.  [Learning to code with Witchcraft](#orgdb35bc6)
5.  [Topics](#orgd652bcc)
    1.  [Inspecting the world](#org1e85dd4)
    2.  [Creating blocks](#org6f8c576)
    3.  [Drawing with Cursor](#org0b6bb1b)
    4.  [Adding Event Handlers](#org40ded6e)
    5.  [Interacting with Players](#org7ab71a5)



<a id="org6766be1"></a>

# Introduction

Witchcraft is a library for interacting with Minecraft using Clojure. It's main
intended use case is doing interactive, creative coding from a Clojure REPL,
using the Minecraft world as your canvas. You can manipulate virtually every
aspect of the world directly from your Clojure editor.

Minecraft is one of the most successful video games in history, having sold over
two-hundred millions copies. It's an open world sandbox game, where players mine
for lego-like blocks and items, which they can use to craft and build
fantastical creations. Minecraft also has a long history of modding (modifying)
the game, with people creating and sharing their customizations and extensions.

Minecraft uses a client-server architecture. With Witchcraft your Clojure REPL
provides the server, the Minecraft game which you need to Download from
Microsoft/Mojang is the client.

Witchcraft builds on top of a open source reimplementation of the Minecraft
server called Glowstone. Glowstone is an ambitious project to create a Minecraft
server from scratch, without using any of the propietary Mojang code. Having
Glowstone available to us is invaluable, its open nature makes it very
convenient to work with, and gives us full flexibility to change things to suit
our needs. When working with Mojang's server, which is what most modded servers
do, you are reverse engineering proprietary, compiled, and obfuscated code. Not
a great prospect.

The downside is that Glowstone is continously struggling to keep up with the
official implementation. They're a small team, and Minecraft continues to
evolve. If you are an experienced Minecraft player than Glowstone may seem quite
limited. It's not yet ready for a full Survival Multiplayer (SMP) game, but it
has been used succesfully for things like lobby servers, and it provides a great
basis for creative coding, which is mainly what we are interested in.


<a id="orgbd729c0"></a>

# Minecraft Concepts

This section is meant as a Minecraft primer. A Minecraft 101 guide to get you on
your feet. There's a lot to this game, both in the game itself, and in the
culture, community, and lore surrounding it. When coming in cold this can all be
rather overwhelming. Reading this section top to bottom should give you a good
overview of the things you are going to find, and you can dig into the
individual topics as needed later on.

If you're an old hand at playing Minecraft then a lot of this will be familiar,
and you can safely skip to the next section, or skim through it to find any
unexplored areas.


<a id="org8cf9660"></a>

## Survival vs Creative

Minecraft can be played either in survival mode, or in creative mode. The main
difference is that in survival you start with nothing but your bare hands, and
have to collect all your resources from scratch. In creative mode you have
access to all the blocks you like. Creative is really meant for lego-like
playing around and creating beautiful structures, whereas in survival you need
to stay alive, defeat enemies, collect resources, and eventually defeat the big
boss, the Ender Dragon. But&#x2026; we're getting ahead of ourselves.

Glowstone, and hence Witchcraft, is not yet ready to support the full survival
experience. It simply lacks some of the features. But as a sandbox for creative
endeavors (whether you're in creative mode or not) it works great. Still, to get
a sense of what Minecraft is all about, it's good to understand what a "full"
survival game looks like.


<a id="orge93da74"></a>

## Whirlwind Tour of a Survival Game

The first thing you do after being dropped into the world is to punch a tree so
you get some wood blocks. With the wood you first create a crafting table, and
using the crafting table and more wood you craft some basic tools, like an axe,
a pickaxe, and a sword.

You find some surface level caves, collect some stone and look around for iron
ore and coal. You smelt the iron with the coal, and use it to create better
tools. At this point it's probably already getting late in the day, and once
night falls "mobs" (hostile creatures) like zombies, spiders, and skeletons come
out to harm you, so you either find shelter by building a house or mining out a
hideout, or you craft a bed, which lets you skip the night.

You're going to need some food as well. You can chop down a stray cow, pig, or
sheep, but that only gets you so far. If you're going to survive for the long
haul you'll need to start setting up some farms. Growing things like wheat,
carrots, beets, and breeding farm animals will provide you with food, as well as
with items to trade later on.

With your basic needs provided for it's time to head back into the mines. This
time we're going much deeper to look for rare diamonds. You can use these to
create better tools, weapons, and armor, and they are need if you want to start
putting enchantments on your items.

Hopefully before long you feel adequately protected, with diamond armor covered
in strong enchantments, it's time to hit the Nether, a devilish world of fire
and monstrosities, but also full of unique and useful resources. So you grab
your diamond axe, mine a dozen or so blocks of obsidian, and build yourself a
portal into the Nether.

With the stuff you collect from the Nether you can further level up and start
brewing potions. It also gives you the necessary items to go back to the
Overworld (the world you started in) and locate the Stronghold, and underground
dungeon, which contains a portal to the third and last world, the End. This dark
and sinister place consists of lonely barren islands floating in the void. Here
you need to slay the final boss, the Ender Dragon. And, tada! You've beaten
Minecraft. (This all is easier said than done.)


<a id="orge39d1cc"></a>

## Worlds and Biomes

By default a Minecraft server has three worlds: the Overworld, the Nether, and
the End. These are all generated based on a random seed, so every playthrough
you get a completely new and unique world, unless you reuse a previous seed.

These worlds are divided in "chunks", 16 by 16 block areas, which are generated
and loaded on demand. This way Minecraft can support humongous worlds, tens of
millions of blocks across, since only the parts where players venture are
generated, loaded, and rendered. The height of the playable world is 256 blocks,
with level 63 and below being ocean, and clouds around level 128.

World generation starts with generating the world height at each location. This
is done through a technique called Perlin Noise, which allows generating random
heights that are still continuous, so you get sloping mountains and oceans,
instead of just jagged columns of blocks. 

After that the world is divided into regions called biomes, for example: plains,
forest, jungle, or mountains. These determine the kind of blocks that will occur
there, the things that can grow, and the creatures that spawn.

Through a separate process rivers and caves are carved out, and special
locations like villages are generated.

Note that this description of the world generation process is highly simplistic,
but at least it gives you a basic idea of how things world.


<a id="org297e769"></a>

## Coordinate system

Minecraft uses an X/Y/Z coordinate system, where the Y-axis determines the
height. X runs from west to east, Z runs from north to south. It can be a bit
counterintuitive to have Y be the height level, but that's how it is.

Pressing \`F3\` will bring up the debug view, where you can see the coordinates
the player is currently at. Blocks and placeable items are always placed at
whole-number coordinates. In other words they always follow the grid. Players,
NPCs, and other creatures use floating points values, so they can move smoothly
through the world.

When manipulating the world with Witchcraft you are mainly changing which blocks
appear at specific X/Y/Z coordinates. Each block in the world has a block type,
and possibly some block data, to account for variants. If there is no block
present at a given spot then the block type is "air".

"Block data" is a 4-bit flag. What it indicates depends on the block type, the
most common uses are to indicate block variants. For example: birch wood, spruce
wood and acacia wood are all blocks of type "wood", but with different block
data. It is also used for blocks that can be placed in a specific direction, for
instance stair cases.


<a id="org8a6a95e"></a>

## The flattening

At this point it's worth pointing out that in version 1.13 Minecraft went
through a big change known as "the flattening", which got rid of these block
data flags. Blocks that previously were variants of the same block type now have
gotten a unique block type id, and block types are assigned "tags" which
indicate how they behave. (Does it burn? Can you break it with an axe? etc).

This has made it easier for Mojang to add new block types, which they have been
doing steadily in every version since. To make it easier for modders to keep up
Minecraft can now spit out a bunch of JSON files describing all known blocks and
their properties. It's all very nice and data-driven, but&#x2026; it's a big change,
and the Glowstone team has had a hard time keeping up.

So at the time of writing Glowstone is only compatible with Minecraft version
1.12, the last version before the flattening. They have been working hard though
at incorporating the necessary changes, and there is an experimental build
available for Minecraft 1.16. It's not fully functional yet, but it's coming!

The Glowstone team seems confident that once they get past this they'll be able
to catch up with newer versions quickly, as well as spending time again on
improving other aspects of Glowstone, so good times ahead! But we'll have to
have some patience.

This also means that once we get there Witchcraft will have to catch up, and
there will necessarily be breaking changes in any API calls that deal with block
data.

You can support the Glowstone by [Donating on Bountysource](https://salt.bountysource.com/checkout/amount?team=glowstonemc).


<a id="org7f2c762"></a>

## Resources and Crafting

A large part of the game is going around collecting resources. You chop down
trees for wood, mine for stone, iron, coal, gold, diamonds. You can mine blue
lapis lazuli stones needed for enchanting, or redstone dust to build automated
"machines". Whacking down grass gives you wheat seeds, which you can plant,
harvest, and feed to animals or bake into bread. All of these things show up in
your inventory (access by pressing \`E\`).

Killing skeletons can give you bones, which you can craft into bone meal, which
makes plants grow. Cows provide leather, which turns into basic armor as well as
books. Sheep can be shaved for wool, which is crafted into beds. And all of this
is just the tip of the iceberg.

Once you have some resources you can place them in a crafting grid to turn them
into other items, based on recipes. You always have a 2x2 grid available in your
inventory, which is good enough for some things like torches, shears, or wood
planks, but for most things you need to first craft a crafting table, which
provides you with a 3x3 grid.

To craft specific items you need the right ingredients and you need to know the
recipe, the exact way the items need to be placed into the grid. Recipes become
unlocked as you progress in the game, or you can look them up online.

The bottom bar of your inventory contains items that you have quick access to in
the game, and that you can hold in your hand. You should put your tools here,
and blocks and items you want to place into the world, like building blocks and
torches. Use the number keys for quick access to specific slots, or flip through
them with your mouse's scroll wheel.


<a id="org625a584"></a>

## Villagers and Traders

As you explore the world you will come across villagers, non-player characters
living together in villages or just wandering around. These can be used to trade
with. Each villager has its own list of things it will trade, typically trading
items for emeralds or vice versa. 

Which items are available partly depends on the villager's job. You can place
specific items like a composter, a cauldron, or a lectern, and when a jobless
villager comes across it it will take on the associated job, becoming a farmer,
leather worker, librarian, etc.

Every now and then you'll also come across a wandering trader, roaming around
the world with their llamas. These tend to have rare and useful items on them,
which you can buy with the emeralds you got from your villagers.


<a id="orgb648f95"></a>

## Farming

To get a good supply of food and other resources like leather and wool, and to
get enough items to trade, you will sooner or later want to set up some farms.
To farm crops you need to get some spots close to the water, brining in water
with your bucket if necessary, and turn the land into farmable land with your
hoe. Plant seeds, carrots, potatoes or other crops and wait until they are fully
grown, and you'll get more seeds and harvested crops in return.

Once you have the right type of food you can also breed animals. Create an
enclosed area so they don't escape, lure them in by holding the food they like
(cow and sheep like wheat, chickens like wheat seeds, pigs like beetroots), feed
them, and they will reproduce.

Sugarcane and cows are useful early game, they provide leather and paper, which
get you books, and eventually allow you to start enchanting.

Manually farming is fine for a while, but if you really want to cash in then you
need to automate things, this is where redstone comes in.


<a id="orge457fa6"></a>

## Redstone

Redstone is a mineable resource found at great depths, like diamonds or lapis
lazuli. It looks like a red ore, which yields redstone dust when mined. Redstone
is basically Minecraft's electricity, it allows you to build machines and
circuits to automate various tasks (or just to achieve cool effects).

Redstone is a big topic in and of itself. The basic principle is that blocks can
become powered, for instance by connecting them to a redstone torch. This power
can be distributed by placing redstone wire on nearby blocks, which starts
acting as a conductor (but with some resistance, so the power only carries a few
blocks).

When the power reaches a piston, trapdoor, dispenser, rail, or other
redstone-powered block, then these become activated. A piston will push the
block in front of it, a dispenser will dispense an item, a powered rail will
speed up a minecart that passes on top of it. Together with redstone repeaters
and comporators you can create intricate machinery, including complete logic
circuits, as well as fully automated farms.


<a id="org5c83bd3"></a>

## Enchanting and XP

When killing hostile mobs, mining certain resources like coal, or reaching other
in-game achievements, you will collect XP (experience points). These look like
little green marbles that chime when picked up. As you collect more XP you will
start to gain levels. Quickly at first, then more slowly, as higher levels
require more XP to progress.

Once you have progressed some levels you can start thinking of enchanting items.
You craft an enchanting table, place a tool, armor item, or book into it, plus
some lapis, and you get an enchanted item back. The enchantments are somewhat
random, so you don't know beforehand what exactly will happen. To get higher
levels of enchantments you need to upgrade your enchanting table by surrounding
it with bookshelves, as well as sufficient XP. You need to reach level 30 before
the highest levels become available.


<a id="orgfe9a22c"></a>

## Modding, Bukkit, SpigotMC

(This section is my retelling of a story that I had to put together by reading
about bits and pieces of it in my search to better understand the Minecraft
modding world. It may contain some inaccuracies in the details, but should
largely convey the big picture. You can find blog posts and other sources
talking more at length about what happened, by people much better informed.)

Minecraft has a long tradition of people customizing the game, both client and
server. Being Java-based allows injecting custom code by manipulating the
classpath, and provides good introspection through JVM reflection, and this
modding has become a key part of the Minecraft culture, embraced by its
creators.

Many projects have come and gone, but a few names you will come across time and
time again, so it's worth explaining a little what's what and who's who.

The most influential server modding project has been Bukkit. Technically Bukkit
provided a suite of related tools, but the most important things to come out of
the project are the Bukkit API, and the CraftBukkit server.

The Bukkit API gave plugin authors an open and stable wrapper API, so they
didn't have to tie their code directly to Mojang's proprietary code. CraftBukkit
was the first Minecraft server to implement the Bukkit API. It extended Mojang's
Minecraft server with the ability to load plugins, and by providing an
implementation of the Bukkit API shielded devs from dealing with propietary
classes and interfaces, which also allowed plugins to run unchanged across
Minecraft versions, since CraftBukkit would provide interface-level
compatibility.

Unfortunately Bukkit/CraftBukkit is also the source of some of the biggest drama
in the Minecraft modding world. Mojang at some point hired the CraftBukkit
creators to work for them, but with that they also acquired the Bukkit project
itself. This was all handled behind closed doors, and only became clear to the
community once one of the original creators decided to pull the plug on the
project, at which point Mojang stepped in and asserted their ownership, thus
setting a lot of bad blood with people who had assumed they were contributing to
a community project.

The end result is that the CraftBukkit code is now considered tainted, it's
essentially propietary code, like Minecraft itself, and community projects like
Glowstone stay away from it, to prevent legal issues and maintain independence.

That said there are some CraftBukkit forks that still operate as open source
community projects, notably SpigotMC and PaperMC, both of them focused on
providing better performance for big servers. So far it seems Mojang has allowed
these to continue to operate.

Luckily the Bukkit API is just an API, a set of interfaces, which we can assume
since Oracle vs Google is not copyrightable, so Glowstone does implement the
Bukkit API, providing interface-level compatibility for plugins written for
other servers. In fact Glowstone uses its own fork of Bukkit called Glowkit,
which incorporates improvements made by other projects, notably SpigotMC and
Paper.


<a id="org79b6473"></a>

## Java vs Bedrock

Mojang (now Microsoft) has released multiple editions of Minecraft over the
year, the two main ones are known as the Java edition, and the Bedrock edition.
Java edition is what you run on a desktop/laptop computers, and it's the main
one we are concerned with. Bedrock is the version for mobile devices (phones and
tablets).

The two versions are not compatible, you can't connect a Bedrock client to a
Java server or vice versa. They have different wire protocols, and there are a
slew of small differences in gameplay and features, although the two versions
are typically released in step, and things like new block types are usually
added to both at the same time.

There is a project named GeyserMC that acts as a proxy, translating packets over
the wire, so Bedrock clients can connect to a Java server. There are some things
it will never be able to fully support, due to inherent differences between the
servers, but it's a cool project nonetheless.


<a id="org932f60d"></a>

## Alternative Games

Being an infinitely moddable sandbox, Minecraft has basically become a platform
for people to implement their own (mini-)games. These range from players
determining their own rules on a vanilla server, to highly customized worlds and
mechanisms. I'm just listing a few common ones to give you an idea, since these
are the kind of things you could do with Witchcraft as well.


<a id="org8f68c53"></a>

### Parkour

Complete a custom trail high in the sky with lots of jumps and other challenges.
A great way to practice your gameplay dexterity.


<a id="org977c184"></a>

### Speedrunner vs Hunters

One player tries to speedrun the game, going through all the motions of a
survival minecraft game, up to killing the dragon. Meanwhile they are chased by
a pack of hunters, other players whose sole objective is to stop the speedrunner
from reaching their goal. Hilarity ensues.


<a id="orgef8ae74"></a>

### Skyblock

You spawn on a tiny island in the sky with nothing but a single tree and a few
random resources, and need to survive, eventually building up shelter and food
production.


<a id="orgeae4a1c"></a>

### Build Wars

Groups of players are tasked with building a specific item or structure within a
set time limit. Once time is up players rate each other's creations.


<a id="org541ad41"></a>

### Bed Wars

Popular game where groups of players need to try to destroy the bed of another
group, while protecting their own


<a id="org1c500c9"></a>

# Getting started with Witchcraft


<a id="org6643bf3"></a>

## Running the plugin

Find the latest release [here](https://github.com/lambdaisland/witchcraft-plugin/releases/tag/v0.0.14), and download the plugin that matches the server
type and version you are using. If there is no matching plugin then open an
issue and we'll make one.

Drop the `.jar` file into your server's plugin directory. Now after starting you
can connect via nREPL to port 25555.

The first time this runs it will create a `deps.edn` file and a
`plugins/witchcraft.edn` file. You can use the first to configure dependencies,
including the Witchcraft library/API version, and the second to configure the
plugin, and the Clojure code it should run at startup.


<a id="orgfc4d378"></a>

## Running from the REPL

You can also start a Clojure REPL yourself, and start Minecraft from there. This
works best with Glowstone, and can be a convenient way to get started **if you
are already familiar with how to start a Clojure REPL**.

To use the latest release, add the following to your \`deps.edn\` ([Clojure CLI](<https://clojure.org/guides/deps_and_cli>)) 

    com.lambdaisland/witchcraft {:mvn/version "<find version in README>"} 

or add the following to your \`project.clj\` ([Leiningen](<https://leiningen.org/>)) 

    [com.lambdaisland/witchcraft "<find version in README>"] 

You should also add some extra maven repos for getting Glowstone: 

    ;; deps.edn 
    {:deps {...} 
     :mvn/repos 
     {"glowstone-repo" {:url "https://repo.glowstone.net/content/repositories/snapshots/"} 
      "aikar"          {:url "https://repo.aikar.co/nexus/content/repositories/aikar-release/"}} 
    
    ;; or project.clj 
    (defproject ... 
      :repositories {"glowstone-repo" "https://repo.glowstone.net/content/repositories/snapshots/" 
    		 "aikar" "https://repo.aikar.co/nexus/content/repositories/aikar-release/"}) 

With that you can now start Witchcraft. Note that you ****should not download and 
run Glowstone itself****, the above is all you need. We'll run the Glowstone 
server directly inside the Clojure REPL process. 

Start by starting the server:

    (require '[lambdaisland.witchcraft :as wc]) 
    (wc/start!) 

And connect to it from Minecraft Java Edition. Glowstone at the time of writing
requires Minecraft 1.12.

Glowstone currently only supports minecraft version 1.12.2 (this may change 
soon). To install the 1.12.2 MC Client version, open the Minecraft launcher -> 
Installations -> New -> 1.12.2. 

After launching Minecraft, select Multiplayer and enter \`localhost:25565\` to 
join the server we just started. 

Hold \`F3\` and press \`p\` so you can tab out without the game pausing. 


<a id="orgdb35bc6"></a>

# Learning to code with Witchcraft

The `lambdaisland.witchcraft` namespace provides the main API. It's a lot like
`clojure.core` in that it contains lots and lots of basic utility functions.

Other namespaces provide higher level APIs to do specific things.

-   `lambdaisland.witchcraft.cursor` Use cursor/turtle style movements to draw.
-   `lambdaisland.witchcraft.shapes` Build predefined shapes like rectangles, circles, or cylinders.
-   `lambdaisland.witchcraft.palette` Helpers for constructing your block/color palette.
-   `lambdaisland.witchcraft.matrix` Matrix manipulations, to rotate, scale, etc.
-   `lambdaisland.witchcraft.events` Register event handler to add custom behavior.
-   `lambdaisland.witchcraft.fill` Fill (also called "flood") algorithm, to find contiguous blocks of a certain type

There is also a growing collection of `lambdaisland.witchcraft.gallery.*`
namespaces, like `lambdaisland.witchcraft.gallery.big-chicken`. These are
recipes and showcases showing Witchcraft in action. You can try them out to get
a feel for what you can do, or to get inspiration and ideas. They usually have a
rich comment block at the bottom that you can poke at with a REPL.

Witchcraft tries its best to be convenient and forgiving, and to make the
interop between Clojure and the Java classes provided by Bukkit as seemless as
possible. This means that functions can generally accept multiple types of
input. For instance, if a function expects you to provide a location (x/y/z
coordinates), then you can generally pass in

-   a Clojure vector: `[x y z]`
-   a Clojure map: `{:x x :y y :z z}`
-   a `org.bukkit.Location` object
-   a `org.bukkit.util.Vector` object
-   a `org.bukkit.block.Block` object

The return value of a function on the other hand is always of a fixed type. If
you want to use the return value in Clojure then you call the function that
returns a Clojure value, if you want to use the return value to do interop then
you ask for the specific Bukkit object you need.

For example:

    (wc/block [0 0 0])
    ;;=> {:x 0, :y 0, :z 0, :material :bedrock}
    
    (wc/get-block [0 0 0])
    ;;=> #bukkit/Block {:x 0.0, :y 0.0, :z 0.0, :world "world", :material :bedrock}

As another example take the `material` function, which converts its argument to
a `org.bukkit.Material`. It can handle these cases:

-   `Block` object: get the block's material
-   keyword: get the material with that name
-   map: get the `:material` key and convert it to a `Material`
-   vector: take the fourth element (`[x y z material]`) and convert it to a `Material`
-   `Material`: return the argument

This may seem like a lot to remember, but luckily you don't have to. All you
need to remember is that if you a `Material` then you call the `material`
function to get it, and it will generally do the right thing.

<table border="2" cellspacing="0" cellpadding="6" rules="groups" frame="hsides">


<colgroup>
<col  class="org-left" />

<col  class="org-left" />

<col  class="org-left" />
</colgroup>
<thead>
<tr>
<th scope="col" class="org-left">Class</th>
<th scope="col" class="org-left">Returns Clojure Value</th>
<th scope="col" class="org-left">Returns Bukkit Object</th>
</tr>
</thead>

<tbody>
<tr>
<td class="org-left">org.bukkit.Location</td>
<td class="org-left">loc / xyz</td>
<td class="org-left">location</td>
</tr>


<tr>
<td class="org-left">org.bukkit.Material</td>
<td class="org-left">material-name / material-data</td>
<td class="org-left">material</td>
</tr>


<tr>
<td class="org-left">org.bukkit.block.Block</td>
<td class="org-left">block</td>
<td class="org-left">get-block</td>
</tr>


<tr>
<td class="org-left">org.bukkit.util.Vector</td>
<td class="org-left">loc / xyz</td>
<td class="org-left">as-vec</td>
</tr>


<tr>
<td class="org-left">org.bukkit.entity.Entity</td>
<td class="org-left">&#xa0;</td>
<td class="org-left">&#xa0;</td>
</tr>


<tr>
<td class="org-left">org.bukkit.entity.Player</td>
<td class="org-left">&#xa0;</td>
<td class="org-left">player</td>
</tr>
</tbody>
</table>


<a id="orgd652bcc"></a>

# Topics


<a id="org1e85dd4"></a>

## Inspecting the world

When you start manipulating the world with code, often the first thing you need
to do is collect information about the world. Where am I? What's this block I'm
standing on? What's the location of all blocks that make up this tree?

Start by finding the Player object for yourself. Now you can look up the X/Y/Z
of where you are, since you'll probably want to manipulate the world close to
where you are, so you can see it.

    (def me (wc/player "my-user-name"))

Now to find the location of something, be it a player, a block, an enity, you
have a few options.

    (wc/xyz me)
    ;;=> [-16960.525728007666 116.0 -18060.47555957846]
    
    (wc/loc me)
    ;;=> {:x -16960.525728007666, :y 116.0, :z -18060.47555957846, :pitch 31.200085, :yaw 36.902298, :world "world"}
    
    (wc/location me)
    ;;=> #bukkit/Location [-16960.525728007666 116.0 -18060.47555957846 36.902298 31.200085 "world"]

The `xyz` function returns a three-element vector. It's nice and concise, and works great with destructuring:

    (let [[x y z] (wc/xyz me)] ,,,)

`loc` is similar but returns a map, which allows you to get a few more things,
like the pitch, yaw, and name of the world you are in.

`location` returns the underlying `org.bukkit.Location` object, in case you want
the actual Java object to do interop.

Note that the longer named one is the one that provides Java interop, since it's
something you'll need less often it's made a little less convenient.

Now let's look at some blocks:

    (wc/block [-16960.525728007666 116.0 -18060.47555957846])
    ;;=> {:x -16960, :y 116, :z -18060, :material :snow}
    
    (wc/get-block [-16960.525728007666 116.0 -18060.47555957846])
    ;;=> #bukkit/Block {:x -16960.0, :y 116.0, :z -18060.0, :world "world", :material :snow}

`get-block` gives you the \`org.bukkit.Block\` object, again in case you need it
for interop, but most of the time you'll use `block`, which gives you a map
representation of the most important aspects: its location and material.


<a id="org6f8c576"></a>

## Creating blocks


<a id="org0b6bb1b"></a>

## Drawing with Cursor


<a id="org40ded6e"></a>

## Adding Event Handlers


<a id="org7ab71a5"></a>

## Interacting with Players

