(ns lambdaisland.witchcraft.events
  (:refer-clojure :exclude [bean])
  (:require [lambdaisland.witchcraft :as wc :refer :all]
            [lambdaisland.witchcraft.bukkit :as bukkit :refer [entities materials]]
            [lambdaisland.witchcraft.safe-bean :refer [bean bean->]]
            [lambdaisland.witchcraft.util :as util]
            [clojure.string :as str]))

(def event-classes [org.spigotmc.event.player.PlayerSpawnLocationEvent
                    org.spigotmc.event.entity.EntityMountEvent
                    org.spigotmc.event.entity.EntityDismountEvent
                    org.bukkit.event.hanging.HangingEvent
                    org.bukkit.event.hanging.HangingPlaceEvent
                    org.bukkit.event.hanging.HangingBreakByEntityEvent
                    org.bukkit.event.hanging.HangingBreakEvent
                    org.bukkit.event.server.PluginDisableEvent
                    org.bukkit.event.server.ServiceUnregisterEvent
                    org.bukkit.event.server.TabCompleteEvent
                    org.bukkit.event.server.ServiceRegisterEvent
                    org.bukkit.event.server.BroadcastMessageEvent
                    org.bukkit.event.server.ServerCommandEvent
                    org.bukkit.event.server.PluginEnableEvent
                    org.bukkit.event.server.ServerEvent
                    org.bukkit.event.server.MapInitializeEvent
                    org.bukkit.event.server.PluginEvent
                    org.bukkit.event.server.ServiceEvent
                    org.bukkit.event.server.ServerListPingEvent
                    org.bukkit.event.server.RemoteServerCommandEvent
                    org.bukkit.event.player.PlayerRespawnEvent
                    org.bukkit.event.player.PlayerMoveEvent
                    org.bukkit.event.player.PlayerItemDamageEvent
                    org.bukkit.event.player.PlayerUnregisterChannelEvent
                    org.bukkit.event.player.PlayerTeleportEvent
                    org.bukkit.event.player.PlayerPickupItemEvent
                    org.bukkit.event.player.PlayerChatTabCompleteEvent
                    org.bukkit.event.player.PlayerInteractEvent
                    org.bukkit.event.player.PlayerItemHeldEvent
                    org.bukkit.event.player.PlayerDropItemEvent
                    org.bukkit.event.player.PlayerBucketFillEvent
                    org.bukkit.event.player.PlayerChangedMainHandEvent
                    org.bukkit.event.player.PlayerToggleFlightEvent
                    org.bukkit.event.player.PlayerChannelEvent
                    org.bukkit.event.player.PlayerJoinEvent
                    org.bukkit.event.player.PlayerUnleashEntityEvent
                    org.bukkit.event.player.PlayerAdvancementDoneEvent
                    org.bukkit.event.player.PlayerKickEvent
                    org.bukkit.event.player.PlayerPortalEvent
                    org.bukkit.event.player.PlayerAttemptPickupItemEvent
                    org.bukkit.event.player.PlayerArmorStandManipulateEvent
                    org.bukkit.event.player.PlayerItemBreakEvent
                    org.bukkit.event.player.PlayerExpChangeEvent
                    org.bukkit.event.player.PlayerEditBookEvent
                    org.bukkit.event.player.PlayerInteractAtEntityEvent
                    org.bukkit.event.player.PlayerChatEvent
                    org.bukkit.event.player.PlayerLevelChangeEvent
                    org.bukkit.event.player.PlayerGameModeChangeEvent
                    org.bukkit.event.player.PlayerAchievementAwardedEvent
                    org.bukkit.event.player.PlayerItemConsumeEvent
                    org.bukkit.event.player.AsyncPlayerChatEvent
                    org.bukkit.event.player.PlayerPickupArrowEvent
                    org.bukkit.event.player.PlayerQuitEvent
                    org.bukkit.event.player.PlayerRegisterChannelEvent
                    org.bukkit.event.player.PlayerAnimationEvent
                    org.bukkit.event.player.PlayerShearEntityEvent
                    org.bukkit.event.player.PlayerBedLeaveEvent
                    org.bukkit.event.player.PlayerFishEvent
                    org.bukkit.event.player.PlayerChangedWorldEvent
                    org.bukkit.event.player.PlayerLocaleChangeEvent
                    org.bukkit.event.player.PlayerEvent
                    org.bukkit.event.player.AsyncPlayerPreLoginEvent
                    org.bukkit.event.player.PlayerItemMendEvent
                    org.bukkit.event.player.PlayerToggleSneakEvent
                    org.bukkit.event.player.PlayerCommandPreprocessEvent
                    org.bukkit.event.player.PlayerVelocityEvent
                    org.bukkit.event.player.PlayerStatisticIncrementEvent
                    org.bukkit.event.player.PlayerEggThrowEvent
                    org.bukkit.event.player.PlayerSwapHandItemsEvent
                    org.bukkit.event.player.PlayerBucketEvent
                    org.bukkit.event.player.PlayerPreLoginEvent
                    org.bukkit.event.player.PlayerResourcePackStatusEvent
                    org.bukkit.event.player.PlayerBedEnterEvent
                    org.bukkit.event.player.PlayerInteractEntityEvent
                    org.bukkit.event.player.PlayerBucketEmptyEvent
                    org.bukkit.event.player.PlayerToggleSprintEvent
                    org.bukkit.event.player.PlayerLoginEvent
                    org.bukkit.event.command.UnknownCommandEvent
                    org.bukkit.event.inventory.InventoryInteractEvent
                    org.bukkit.event.inventory.InventoryClickEvent
                    org.bukkit.event.inventory.InventoryDragEvent
                    org.bukkit.event.inventory.InventoryMoveItemEvent
                    org.bukkit.event.inventory.BrewingStandFuelEvent
                    org.bukkit.event.inventory.CraftItemEvent
                    org.bukkit.event.inventory.PrepareItemCraftEvent
                    org.bukkit.event.inventory.FurnaceSmeltEvent
                    org.bukkit.event.inventory.FurnaceExtractEvent
                    org.bukkit.event.inventory.InventoryCreativeEvent
                    org.bukkit.event.inventory.PrepareAnvilEvent
                    org.bukkit.event.inventory.InventoryCloseEvent
                    org.bukkit.event.inventory.BrewEvent
                    org.bukkit.event.inventory.FurnaceBurnEvent
                    org.bukkit.event.inventory.InventoryOpenEvent
                    org.bukkit.event.inventory.InventoryEvent
                    org.bukkit.event.inventory.InventoryPickupItemEvent
                    org.bukkit.event.enchantment.PrepareItemEnchantEvent
                    org.bukkit.event.enchantment.EnchantItemEvent
                    org.bukkit.event.weather.ThunderChangeEvent
                    org.bukkit.event.weather.WeatherChangeEvent
                    org.bukkit.event.weather.WeatherEvent
                    org.bukkit.event.weather.LightningStrikeEvent
                    org.bukkit.event.block.BlockGrowEvent
                    org.bukkit.event.block.BlockFadeEvent
                    org.bukkit.event.block.BlockCanBuildEvent
                    org.bukkit.event.block.NotePlayEvent
                    org.bukkit.event.block.SignChangeEvent
                    org.bukkit.event.block.LeavesDecayEvent
                    org.bukkit.event.block.BlockBurnEvent
                    org.bukkit.event.block.BlockRedstoneEvent
                    org.bukkit.event.block.BlockPlaceEvent
                    org.bukkit.event.block.BlockPistonExtendEvent
                    org.bukkit.event.block.EntityBlockFormEvent
                    org.bukkit.event.block.BlockDamageEvent
                    org.bukkit.event.block.BlockDispenseEvent
                    org.bukkit.event.block.BlockExpEvent
                    org.bukkit.event.block.BlockPistonRetractEvent
                    org.bukkit.event.block.BlockFormEvent
                    org.bukkit.event.block.BlockPistonEvent
                    org.bukkit.event.block.BlockIgniteEvent
                    org.bukkit.event.block.BlockSpreadEvent
                    org.bukkit.event.block.BlockEvent
                    org.bukkit.event.block.BlockFromToEvent
                    org.bukkit.event.block.BlockBreakEvent
                    org.bukkit.event.block.BlockPhysicsEvent
                    org.bukkit.event.block.CauldronLevelChangeEvent
                    org.bukkit.event.block.BlockExplodeEvent
                    org.bukkit.event.block.BlockMultiPlaceEvent
                    org.bukkit.event.entity.VillagerAcquireTradeEvent
                    org.bukkit.event.entity.EntityPortalExitEvent
                    org.bukkit.event.entity.EntityDamageByBlockEvent
                    org.bukkit.event.entity.EntityUnleashEvent
                    org.bukkit.event.entity.SpawnerSpawnEvent
                    org.bukkit.event.entity.PigZapEvent
                    org.bukkit.event.entity.EntityTargetLivingEntityEvent
                    org.bukkit.event.entity.VillagerReplenishTradeEvent
                    org.bukkit.event.entity.EntitySpawnEvent
                    org.bukkit.event.entity.EntityPortalEvent
                    org.bukkit.event.entity.EntityDamageEvent
                    org.bukkit.event.entity.EntityCreatePortalEvent
                    org.bukkit.event.entity.EntityTameEvent
                    org.bukkit.event.entity.EntityAirChangeEvent
                    org.bukkit.event.entity.EntityChangeBlockEvent
                    org.bukkit.event.entity.HorseJumpEvent
                    org.bukkit.event.entity.EntityDamageByEntityEvent
                    org.bukkit.event.entity.FireworkExplodeEvent
                    org.bukkit.event.entity.ProjectileLaunchEvent
                    org.bukkit.event.entity.EntityCombustByBlockEvent
                    org.bukkit.event.entity.EntityResurrectEvent
                    org.bukkit.event.entity.EntityShootBowEvent
                    org.bukkit.event.entity.ItemDespawnEvent
                    org.bukkit.event.entity.EnderDragonChangePhaseEvent
                    org.bukkit.event.entity.EntityPortalEnterEvent
                    org.bukkit.event.entity.SlimeSplitEvent
                    org.bukkit.event.entity.EntityCombustEvent
                    org.bukkit.event.entity.ExplosionPrimeEvent
                    org.bukkit.event.entity.EntityRegainHealthEvent
                    org.bukkit.event.entity.EntityBreakDoorEvent
                    org.bukkit.event.entity.EntityTargetEvent
                    org.bukkit.event.entity.EntityBreedEvent
                    org.bukkit.event.entity.ProjectileHitEvent
                    org.bukkit.event.entity.ItemSpawnEvent
                    org.bukkit.event.entity.SheepRegrowWoolEvent
                    org.bukkit.event.entity.PotionSplashEvent
                    org.bukkit.event.entity.EntityExplodeEvent
                    org.bukkit.event.entity.EntityInteractEvent
                    org.bukkit.event.entity.LingeringPotionSplashEvent
                    org.bukkit.event.entity.CreeperPowerEvent
                    org.bukkit.event.entity.EntityCombustByEntityEvent
                    org.bukkit.event.entity.PlayerLeashEntityEvent
                    org.bukkit.event.entity.AreaEffectCloudApplyEvent
                    org.bukkit.event.entity.FoodLevelChangeEvent
                    org.bukkit.event.entity.EntityEvent
                    org.bukkit.event.entity.CreatureSpawnEvent
                    org.bukkit.event.entity.EntityToggleGlideEvent
                    org.bukkit.event.entity.EntityDeathEvent
                    org.bukkit.event.entity.EntityTeleportEvent
                    org.bukkit.event.entity.SheepDyeWoolEvent
                    org.bukkit.event.entity.ExpBottleEvent
                    org.bukkit.event.entity.EntityPickupItemEvent
                    org.bukkit.event.entity.PlayerDeathEvent
                    org.bukkit.event.entity.ItemMergeEvent
                    org.bukkit.event.vehicle.VehicleDamageEvent
                    org.bukkit.event.vehicle.VehicleCollisionEvent
                    org.bukkit.event.vehicle.VehicleDestroyEvent
                    org.bukkit.event.vehicle.VehicleEnterEvent
                    org.bukkit.event.vehicle.VehicleBlockCollisionEvent
                    org.bukkit.event.vehicle.VehicleEntityCollisionEvent
                    org.bukkit.event.vehicle.VehicleMoveEvent
                    org.bukkit.event.vehicle.VehicleCreateEvent
                    org.bukkit.event.vehicle.VehicleEvent
                    org.bukkit.event.vehicle.VehicleExitEvent
                    org.bukkit.event.vehicle.VehicleUpdateEvent
                    org.bukkit.event.world.PortalCreateEvent
                    org.bukkit.event.world.WorldUnloadEvent
                    org.bukkit.event.world.SpawnChangeEvent
                    org.bukkit.event.world.ChunkUnloadEvent
                    org.bukkit.event.world.WorldLoadEvent
                    org.bukkit.event.world.WorldSaveEvent
                    org.bukkit.event.world.StructureGrowEvent
                    org.bukkit.event.world.ChunkPopulateEvent
                    org.bukkit.event.world.ChunkLoadEvent
                    org.bukkit.event.world.WorldInitEvent
                    org.bukkit.event.world.ChunkEvent
                    org.bukkit.event.world.WorldEvent
                    org.bukkit.conversations.ConversationAbandonedEvent
                    com.destroystokyo.paper.event.server.PaperServerListPingEvent
                    com.destroystokyo.paper.event.server.ServerExceptionEvent
                    com.destroystokyo.paper.event.server.AsyncTabCompleteEvent
                    com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent
                    com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent
                    com.destroystokyo.paper.event.player.PlayerReadyArrowEvent
                    com.destroystokyo.paper.event.player.IllegalPacketEvent
                    com.destroystokyo.paper.event.player.PlayerInitialSpawnEvent
                    com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
                    com.destroystokyo.paper.event.player.PlayerJumpEvent
                    com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent
                    com.destroystokyo.paper.event.player.PlayerLocaleChangeEvent
                    com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent
                    com.destroystokyo.paper.event.player.PlayerHandshakeEvent
                    com.destroystokyo.paper.event.block.BeaconEffectEvent
                    com.destroystokyo.paper.event.entity.ExperienceOrbMergeEvent
                    com.destroystokyo.paper.event.entity.WitchThrowPotionEvent
                    com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent
                    com.destroystokyo.paper.event.entity.ProjectileCollideEvent
                    com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent
                    com.destroystokyo.paper.event.entity.EntityPathfindEvent
                    com.destroystokyo.paper.event.entity.WitchConsumePotionEvent
                    com.destroystokyo.paper.event.entity.EntityTeleportEndGatewayEvent
                    com.destroystokyo.paper.event.entity.EndermanAttackPlayerEvent
                    com.destroystokyo.paper.event.entity.WitchReadyPotionEvent
                    com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent
                    com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
                    com.destroystokyo.paper.event.entity.EndermanEscapeEvent
                    com.destroystokyo.paper.event.entity.EntityZapEvent
                    com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
                    com.destroystokyo.paper.event.profile.PreLookupProfileEvent
                    com.destroystokyo.paper.event.profile.LookupProfileEvent
                    com.destroystokyo.paper.event.profile.ProfileWhitelistVerifyEvent
                    com.destroystokyo.paper.event.profile.FillProfileEvent
                    com.destroystokyo.paper.event.profile.PreFillProfileEvent
                    com.destroystokyo.paper.loottable.LootableInventoryReplenishEvent])

(defn class->kw [klz]
  (-> klz
      .getName
      (str/replace #".*\." "")
      (str/replace #"Event$" "")
      (str/replace #"([a-z])([A-Z])" (fn [[_ a A]]
                                       (str a "-" A)))
      (str/lower-case)
      keyword))

(def events
  (into {}
        (map (juxt class->kw identity))
        event-classes))

(def priority (util/enum->map org.bukkit.event.EventPriority))

(defn unregister-all-event-listeners [event]
  (let [event-class (if (class? event) event (get events event))
        getHandlerList (.getMethod event-class "getHandlerList" (into-array Class []))
        handler-list (.invoke getHandlerList nil nil)]
    (doseq [handler (.getRegisteredListeners handler-list)]
      (.unregister handler-list handler))))

(defn unregister-event-listener [event key]
  (let [event-class (if (class? event) event (get events event))
        getHandlerList (.getMethod event-class "getHandlerList" (into-array Class []))
        handler-list (.invoke getHandlerList nil nil)]
    (doseq [handler (.getRegisteredListeners handler-list)
            :let [listener (:listener (bean handler))]]
      (when (= key (::key (meta listener)))
        (.unregister handler-list listener)))))

(defn register-event-listener [event k fn]
  (let [event-class (if (class? event) event (get events event))]
    (unregister-event-listener event-class k)
    (.registerEvent (plugin-manager)
                    event-class
                    (with-meta
                      (reify org.bukkit.event.Listener)
                      {::key k})
                    (priority :normal)
                    (reify org.bukkit.plugin.EventExecutor
                      (execute [this listener event]
                        (fn event)))
                    (proxy [org.bukkit.plugin.PluginBase] []
                      (getDescription []
                        (org.bukkit.plugin.PluginDescriptionFile. "my-clojure-plugin" "1.0" "test.test"))
                      (isEnabled []
                        true)))))

(comment
  (register-event-listener :async-player-chat
                           ::print-chat
                           (fn [e]
                             (prn "heyyyya" (:message (bean e)))))

  (unregister-event-listener :async-player-chat ::print-chat)

  (register-event-listener :block-damage
                           ::show-block-dmg
                           (fn [e]
                             (prn "You broke it!" (bean e))))

  (unregister-event-listener :block-damage ::show-block-dmg)

  (register-event-listener :block-damage
                           ::self-heal
                           (fn [e]
                             (let [block (:block (bean e))
                                   type (:type (bean block))]
                               (future
                                 (Thread/sleep 500)
                                 (prn type)
                                 (fill (offset (->location block) [-1 -1 -1])
                                       [3 3 3]
                                       type)))))



  (unregister-event-listener :block-damage ::show-block-dmg)

  (unregister-all-event-listeners :async-player-chat)
  )
