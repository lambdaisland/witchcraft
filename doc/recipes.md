# Preliminaries

Recipies assume the following namespace aliases:

```clojure
[lambdaisland.witchcraft :as wc]
[lambdaisland.witchcraft.cursor :as c]
```

## Keep it always day

Day follows night and night follows day, the same is true in Minecraft. But when
you're working on a great build it can be a bit annoying to have to continue
under the cover of darkness.

When the sun starts setting it's easy enough to skip through the night by
resetting the clock:

``` clojure
(wc/set-time 0)
```

But even that gets tedious. Instead you can use this recipe to automatically
flip back to the morning as soon as it starts getting late.

``` clojure
(future
  (while true
    (Thread/sleep 5000)
    (when (< 13000 (wc/time))
      (wc/set-time 0))))
```

At the heart of this is this simply `when` condition

```clojure
(when (< 12000 (wc/time))
  (wc/set-time 0))
```

Time in Minecraft passes exactly 72 times faster than in real life, which means
24 "hours" in Minecraft equals 20 minutes in real life. The number you get from
`wc/time` is the current time of day measured in "ticks", with a Minecraft day
containing 24000 of these ticks. When that time is half up, at 12000 ticks, it's
starting to turn night, so it's time to set it back to 0, the start of the day.

We do this in an infinite `(while true ,,,)` loop, and add a little
`(Thread/sleep ,,,)` to wait a few seconds between each check.

But this infinite loop never ends. If we do that in a REPL then that's the end
of the conversation, there's nothing else we can do after that. To prevent
locking up the REPL like that you can run this code on a separate "thread",
that's what `future` is for.
