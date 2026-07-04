/*
 * Decompiled with CFR 0.2.2 (FabricMC 7c48b8c4).
 */
package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.DebugQueryHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DemoIntroScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.achievement.StatsUpdateListener;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientRegistryLayer;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.debug.BeeDebugRenderer;
import net.minecraft.client.renderer.debug.BrainDebugRenderer;
import net.minecraft.client.renderer.debug.GoalSelectorDebugRenderer;
import net.minecraft.client.renderer.debug.NeighborsUpdateRenderer;
import net.minecraft.client.renderer.debug.WorldGenAttemptRenderer;
import net.minecraft.client.resources.sounds.BeeAggressiveSoundInstance;
import net.minecraft.client.resources.sounds.BeeFlyingSoundInstance;
import net.minecraft.client.resources.sounds.BeeSoundInstance;
import net.minecraft.client.resources.sounds.GuardianAttackSoundInstance;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import net.minecraft.client.resources.sounds.SnifferSoundInstance;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Position;
import net.minecraft.core.PositionImpl;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.LocalChatSession;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.chat.SignedMessageLink;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAddExperienceOrbPacket;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundDeleteChatPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.realms.DisconnectedRealmsScreen;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.Crypt;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.PositionSourceType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ClientPacketListener
implements TickablePacketListener,
ClientGamePacketListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component GENERIC_DISCONNECT_MESSAGE = Component.translatable((String)"disconnect.lost");
    private static final Component UNSECURE_SERVER_TOAST_TITLE = Component.translatable((String)"multiplayer.unsecureserver.toast.title");
    private static final Component UNSERURE_SERVER_TOAST = Component.translatable((String)"multiplayer.unsecureserver.toast");
    private static final Component INVALID_PACKET = Component.translatable((String)"multiplayer.disconnect.invalid_packet");
    private static final Component CHAT_VALIDATION_FAILED_ERROR = Component.translatable((String)"multiplayer.disconnect.chat_validation_failed");
    private static final int PENDING_OFFSET_THRESHOLD = 64;
    private final Connection connection;
    private final List<DeferredPacket> deferredPackets = new ArrayList<DeferredPacket>();
    @Nullable
    private final ServerData serverData;
    private final GameProfile localGameProfile;
    private final Screen callbackScreen;
    private final Minecraft minecraft;
    private ClientLevel level;
    private ClientLevel.ClientLevelData levelData;
    private final Map<UUID, PlayerInfo> playerInfoMap = Maps.newHashMap();
    private final Set<PlayerInfo> listedPlayers = new ReferenceOpenHashSet<PlayerInfo>();
    private final ClientAdvancements advancements;
    private final ClientSuggestionProvider suggestionsProvider;
    private final DebugQueryHandler debugQueryHandler = new DebugQueryHandler(this);
    private int serverChunkRadius = 3;
    private int serverSimulationDistance = 3;
    private final RandomSource random = RandomSource.createThreadSafe();
    private CommandDispatcher<SharedSuggestionProvider> commands = new CommandDispatcher();
    private final RecipeManager recipeManager = new RecipeManager();
    private final UUID id = UUID.randomUUID();
    private Set<ResourceKey<Level>> levels;
    private LayeredRegistryAccess<ClientRegistryLayer> registryAccess = ClientRegistryLayer.createRegistryAccess();
    private FeatureFlagSet enabledFeatures = FeatureFlags.DEFAULT_FLAGS;
    private final WorldSessionTelemetryManager telemetryManager;
    @Nullable
    private LocalChatSession chatSession;
    private SignedMessageChain.Encoder signedMessageEncoder = SignedMessageChain.Encoder.UNSIGNED;
    private LastSeenMessagesTracker lastSeenMessages = new LastSeenMessagesTracker(20);
    private MessageSignatureCache messageSignatureCache = MessageSignatureCache.createDefault();

    public ClientPacketListener(Minecraft minecraft, Screen screen, Connection connection, @Nullable ServerData serverData, GameProfile gameProfile, WorldSessionTelemetryManager worldSessionTelemetryManager) {
        this.minecraft = minecraft;
        this.callbackScreen = screen;
        this.connection = connection;
        this.serverData = serverData;
        this.localGameProfile = gameProfile;
        this.advancements = new ClientAdvancements(minecraft, worldSessionTelemetryManager);
        this.suggestionsProvider = new ClientSuggestionProvider(this, minecraft);
        this.telemetryManager = worldSessionTelemetryManager;
    }

    public ClientSuggestionProvider getSuggestionsProvider() {
        return this.suggestionsProvider;
    }

    public void close() {
        this.level = null;
        this.telemetryManager.onDisconnect();
    }

    public RecipeManager getRecipeManager() {
        return this.recipeManager;
    }

    public void handleLogin(ClientboundLoginPacket clientboundLoginPacket) {
        ClientLevel.ClientLevelData clientLevelData;
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundLoginPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.gameMode = new MultiPlayerGameMode(this.minecraft, this);
        this.registryAccess = this.registryAccess.replaceFrom((Object)ClientRegistryLayer.REMOTE, new RegistryAccess.Frozen[]{clientboundLoginPacket.registryHolder()});
        if (!this.connection.isMemoryConnection()) {
            this.registryAccess.compositeAccess().registries().forEach(registryEntry -> registryEntry.value().resetTags());
        }
        ArrayList list = Lists.newArrayList(clientboundLoginPacket.levels());
        Collections.shuffle(list);
        this.levels = Sets.newLinkedHashSet(list);
        ResourceKey resourceKey = clientboundLoginPacket.dimension();
        Holder.Reference holder = this.registryAccess.compositeAccess().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(clientboundLoginPacket.dimensionType());
        this.serverChunkRadius = clientboundLoginPacket.chunkRadius();
        this.serverSimulationDistance = clientboundLoginPacket.simulationDistance();
        boolean bl = clientboundLoginPacket.isDebug();
        boolean bl2 = clientboundLoginPacket.isFlat();
        this.levelData = clientLevelData = new ClientLevel.ClientLevelData(Difficulty.NORMAL, clientboundLoginPacket.hardcore(), bl2);
        this.level = new ClientLevel(this, clientLevelData, (ResourceKey<Level>)resourceKey, (Holder<DimensionType>)holder, this.serverChunkRadius, this.serverSimulationDistance, this.minecraft::getProfiler, this.minecraft.levelRenderer, bl, clientboundLoginPacket.seed());
        this.minecraft.setLevel(this.level);
        if (this.minecraft.player == null) {
            this.minecraft.player = this.minecraft.gameMode.createPlayer(this.level, new StatsCounter(), new ClientRecipeBook());
            this.minecraft.player.setYRot(-180.0f);
            if (this.minecraft.getSingleplayerServer() != null) {
                this.minecraft.getSingleplayerServer().setUUID(this.minecraft.player.getUUID());
            }
        }
        this.minecraft.debugRenderer.clear();
        this.minecraft.player.resetPos();
        int i = clientboundLoginPacket.playerId();
        this.minecraft.player.setId(i);
        this.level.addPlayer(i, this.minecraft.player);
        this.minecraft.player.input = new KeyboardInput(this.minecraft.options);
        this.minecraft.gameMode.adjustPlayer(this.minecraft.player);
        this.minecraft.cameraEntity = this.minecraft.player;
        this.minecraft.setScreen(new ReceivingLevelScreen());
        this.minecraft.player.setReducedDebugInfo(clientboundLoginPacket.reducedDebugInfo());
        this.minecraft.player.setShowDeathScreen(clientboundLoginPacket.showDeathScreen());
        this.minecraft.player.setLastDeathLocation(clientboundLoginPacket.lastDeathLocation());
        this.minecraft.player.setPortalCooldown(clientboundLoginPacket.portalCooldown());
        this.minecraft.gameMode.setLocalMode(clientboundLoginPacket.gameType(), clientboundLoginPacket.previousGameType());
        this.minecraft.options.setServerRenderDistance(clientboundLoginPacket.chunkRadius());
        this.minecraft.options.broadcastOptions();
        this.connection.send((Packet)new ServerboundCustomPayloadPacket(ServerboundCustomPayloadPacket.BRAND, new FriendlyByteBuf(Unpooled.buffer()).writeUtf(ClientBrandRetriever.getClientModName())));
        this.chatSession = null;
        this.lastSeenMessages = new LastSeenMessagesTracker(20);
        this.messageSignatureCache = MessageSignatureCache.createDefault();
        if (this.connection.isEncrypted()) {
            this.minecraft.getProfileKeyPairManager().prepareKeyPair().thenAcceptAsync(optional -> optional.ifPresent(this::setKeyPair), (Executor)((Object)this.minecraft));
        }
        this.telemetryManager.onPlayerInfoReceived(clientboundLoginPacket.gameType(), clientboundLoginPacket.hardcore());
        this.minecraft.quickPlayLog().log(this.minecraft);
    }

    public void handleAddEntity(ClientboundAddEntityPacket clientboundAddEntityPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundAddEntityPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        EntityType entityType = clientboundAddEntityPacket.getType();
        Entity entity = entityType.create((Level)this.level);
        if (entity != null) {
            entity.recreateFromPacket(clientboundAddEntityPacket);
            int i = clientboundAddEntityPacket.getId();
            this.level.putNonPlayerEntity(i, entity);
            this.postAddEntitySoundInstance(entity);
        } else {
            LOGGER.warn("Skipping Entity with id {}", (Object)entityType);
        }
    }

    private void postAddEntitySoundInstance(Entity entity) {
        if (entity instanceof AbstractMinecart) {
            this.minecraft.getSoundManager().play(new MinecartSoundInstance((AbstractMinecart)entity));
        } else if (entity instanceof Bee) {
            boolean bl = ((Bee)entity).isAngry();
            BeeSoundInstance beeSoundInstance = bl ? new BeeAggressiveSoundInstance((Bee)entity) : new BeeFlyingSoundInstance((Bee)entity);
            this.minecraft.getSoundManager().queueTickingSound(beeSoundInstance);
        }
    }

    public void handleAddExperienceOrb(ClientboundAddExperienceOrbPacket clientboundAddExperienceOrbPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundAddExperienceOrbPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        double d = clientboundAddExperienceOrbPacket.getX();
        double e = clientboundAddExperienceOrbPacket.getY();
        double f = clientboundAddExperienceOrbPacket.getZ();
        ExperienceOrb entity = new ExperienceOrb((Level)this.level, d, e, f, clientboundAddExperienceOrbPacket.getValue());
        entity.syncPacketPositionCodec(d, e, f);
        entity.setYRot(0.0f);
        entity.setXRot(0.0f);
        entity.setId(clientboundAddExperienceOrbPacket.getId());
        this.level.putNonPlayerEntity(clientboundAddExperienceOrbPacket.getId(), (Entity)entity);
    }

    public void handleSetEntityMotion(ClientboundSetEntityMotionPacket clientboundSetEntityMotionPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetEntityMotionPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundSetEntityMotionPacket.getId());
        if (entity == null) {
            return;
        }
        entity.lerpMotion((double)clientboundSetEntityMotionPacket.getXa() / 8000.0, (double)clientboundSetEntityMotionPacket.getYa() / 8000.0, (double)clientboundSetEntityMotionPacket.getZa() / 8000.0);
    }

    public void handleSetEntityData(ClientboundSetEntityDataPacket clientboundSetEntityDataPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetEntityDataPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundSetEntityDataPacket.id());
        if (entity != null) {
            entity.getEntityData().assignValues(clientboundSetEntityDataPacket.packedItems());
        }
    }

    public void handleAddPlayer(ClientboundAddPlayerPacket clientboundAddPlayerPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundAddPlayerPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        PlayerInfo playerInfo = this.getPlayerInfo(clientboundAddPlayerPacket.getPlayerId());
        if (playerInfo == null) {
            LOGGER.warn("Server attempted to add player prior to sending player info (Player id {})", (Object)clientboundAddPlayerPacket.getPlayerId());
            return;
        }
        double d = clientboundAddPlayerPacket.getX();
        double e = clientboundAddPlayerPacket.getY();
        double f = clientboundAddPlayerPacket.getZ();
        float g = (float)(clientboundAddPlayerPacket.getyRot() * 360) / 256.0f;
        float h = (float)(clientboundAddPlayerPacket.getxRot() * 360) / 256.0f;
        int i = clientboundAddPlayerPacket.getEntityId();
        RemotePlayer remotePlayer = new RemotePlayer(this.minecraft.level, playerInfo.getProfile());
        remotePlayer.setId(i);
        remotePlayer.syncPacketPositionCodec(d, e, f);
        remotePlayer.absMoveTo(d, e, f, g, h);
        remotePlayer.setOldPosAndRot();
        this.level.addPlayer(i, remotePlayer);
    }

    public void handleTeleportEntity(ClientboundTeleportEntityPacket clientboundTeleportEntityPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundTeleportEntityPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundTeleportEntityPacket.getId());
        if (entity == null) {
            return;
        }
        double d = clientboundTeleportEntityPacket.getX();
        double e = clientboundTeleportEntityPacket.getY();
        double f = clientboundTeleportEntityPacket.getZ();
        entity.syncPacketPositionCodec(d, e, f);
        if (!entity.isControlledByLocalInstance()) {
            float g = (float)(clientboundTeleportEntityPacket.getyRot() * 360) / 256.0f;
            float h = (float)(clientboundTeleportEntityPacket.getxRot() * 360) / 256.0f;
            entity.lerpTo(d, e, f, g, h, 3, true);
            entity.setOnGround(clientboundTeleportEntityPacket.isOnGround());
        }
    }

    public void handleSetCarriedItem(ClientboundSetCarriedItemPacket clientboundSetCarriedItemPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetCarriedItemPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        if (Inventory.isHotbarSlot((int)clientboundSetCarriedItemPacket.getSlot())) {
            this.minecraft.player.getInventory().selected = clientboundSetCarriedItemPacket.getSlot();
        }
    }

    public void handleMoveEntity(ClientboundMoveEntityPacket clientboundMoveEntityPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundMoveEntityPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = clientboundMoveEntityPacket.getEntity((Level)this.level);
        if (entity == null) {
            return;
        }
        if (!entity.isControlledByLocalInstance()) {
            if (clientboundMoveEntityPacket.hasPosition()) {
                VecDeltaCodec vecDeltaCodec = entity.getPositionCodec();
                Vec3 vec3 = vecDeltaCodec.decode((long)clientboundMoveEntityPacket.getXa(), (long)clientboundMoveEntityPacket.getYa(), (long)clientboundMoveEntityPacket.getZa());
                vecDeltaCodec.setBase(vec3);
                float f = clientboundMoveEntityPacket.hasRotation() ? (float)(clientboundMoveEntityPacket.getyRot() * 360) / 256.0f : entity.getYRot();
                float g = clientboundMoveEntityPacket.hasRotation() ? (float)(clientboundMoveEntityPacket.getxRot() * 360) / 256.0f : entity.getXRot();
                entity.lerpTo(vec3.x(), vec3.y(), vec3.z(), f, g, 3, false);
            } else if (clientboundMoveEntityPacket.hasRotation()) {
                float h = (float)(clientboundMoveEntityPacket.getyRot() * 360) / 256.0f;
                float i = (float)(clientboundMoveEntityPacket.getxRot() * 360) / 256.0f;
                entity.lerpTo(entity.getX(), entity.getY(), entity.getZ(), h, i, 3, false);
            }
            entity.setOnGround(clientboundMoveEntityPacket.isOnGround());
        }
    }

    public void handleRotateMob(ClientboundRotateHeadPacket clientboundRotateHeadPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundRotateHeadPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = clientboundRotateHeadPacket.getEntity((Level)this.level);
        if (entity == null) {
            return;
        }
        float f = (float)(clientboundRotateHeadPacket.getYHeadRot() * 360) / 256.0f;
        entity.lerpHeadTo(f, 3);
    }

    public void handleRemoveEntities(ClientboundRemoveEntitiesPacket clientboundRemoveEntitiesPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundRemoveEntitiesPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        clientboundRemoveEntitiesPacket.getEntityIds().forEach(i -> this.level.removeEntity(i, Entity.RemovalReason.DISCARDED));
    }

    public void handleMovePlayer(ClientboundPlayerPositionPacket clientboundPlayerPositionPacket) {
        double i;
        double h;
        double g;
        double f;
        double e;
        double d;
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundPlayerPositionPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        LocalPlayer player = this.minecraft.player;
        Vec3 vec3 = player.getDeltaMovement();
        boolean bl = clientboundPlayerPositionPacket.getRelativeArguments().contains(RelativeMovement.X);
        boolean bl2 = clientboundPlayerPositionPacket.getRelativeArguments().contains(RelativeMovement.Y);
        boolean bl3 = clientboundPlayerPositionPacket.getRelativeArguments().contains(RelativeMovement.Z);
        if (bl) {
            d = vec3.x();
            e = player.getX() + clientboundPlayerPositionPacket.getX();
            player.xOld += clientboundPlayerPositionPacket.getX();
            player.xo += clientboundPlayerPositionPacket.getX();
        } else {
            d = 0.0;
            player.xOld = e = clientboundPlayerPositionPacket.getX();
            player.xo = e;
        }
        if (bl2) {
            f = vec3.y();
            g = player.getY() + clientboundPlayerPositionPacket.getY();
            player.yOld += clientboundPlayerPositionPacket.getY();
            player.yo += clientboundPlayerPositionPacket.getY();
        } else {
            f = 0.0;
            player.yOld = g = clientboundPlayerPositionPacket.getY();
            player.yo = g;
        }
        if (bl3) {
            h = vec3.z();
            i = player.getZ() + clientboundPlayerPositionPacket.getZ();
            player.zOld += clientboundPlayerPositionPacket.getZ();
            player.zo += clientboundPlayerPositionPacket.getZ();
        } else {
            h = 0.0;
            player.zOld = i = clientboundPlayerPositionPacket.getZ();
            player.zo = i;
        }
        player.setPos(e, g, i);
        player.setDeltaMovement(d, f, h);
        float j = clientboundPlayerPositionPacket.getYRot();
        float k = clientboundPlayerPositionPacket.getXRot();
        if (clientboundPlayerPositionPacket.getRelativeArguments().contains(RelativeMovement.X_ROT)) {
            player.setXRot(player.getXRot() + k);
            player.xRotO += k;
        } else {
            player.setXRot(k);
            player.xRotO = k;
        }
        if (clientboundPlayerPositionPacket.getRelativeArguments().contains(RelativeMovement.Y_ROT)) {
            player.setYRot(player.getYRot() + j);
            player.yRotO += j;
        } else {
            player.setYRot(j);
            player.yRotO = j;
        }
        this.connection.send((Packet)new ServerboundAcceptTeleportationPacket(clientboundPlayerPositionPacket.getId()));
        this.connection.send((Packet)new ServerboundMovePlayerPacket.PosRot(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), false));
    }

    public void handleChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket clientboundSectionBlocksUpdatePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSectionBlocksUpdatePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        clientboundSectionBlocksUpdatePacket.runUpdates((blockPos, blockState) -> this.level.setServerVerifiedBlockState((BlockPos)blockPos, (BlockState)blockState, 19));
    }

    public void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket clientboundLevelChunkWithLightPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundLevelChunkWithLightPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        int i = clientboundLevelChunkWithLightPacket.getX();
        int j = clientboundLevelChunkWithLightPacket.getZ();
        this.updateLevelChunk(i, j, clientboundLevelChunkWithLightPacket.getChunkData());
        ClientboundLightUpdatePacketData clientboundLightUpdatePacketData = clientboundLevelChunkWithLightPacket.getLightData();
        this.level.queueLightUpdate(() -> {
            this.applyLightData(i, j, clientboundLightUpdatePacketData);
            LevelChunk levelChunk = this.level.getChunkSource().getChunk(i, j, false);
            if (levelChunk != null) {
                this.enableChunkLight(levelChunk, i, j);
            }
        });
    }

    public void handleChunksBiomes(ClientboundChunksBiomesPacket clientboundChunksBiomesPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundChunksBiomesPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        for (ClientboundChunksBiomesPacket.ChunkBiomeData chunkBiomeData : clientboundChunksBiomesPacket.chunkBiomeData()) {
            this.level.getChunkSource().replaceBiomes(chunkBiomeData.pos().x, chunkBiomeData.pos().z, chunkBiomeData.getReadBuffer());
        }
        for (ClientboundChunksBiomesPacket.ChunkBiomeData chunkBiomeData : clientboundChunksBiomesPacket.chunkBiomeData()) {
            this.level.onChunkLoaded(new ChunkPos(chunkBiomeData.pos().x, chunkBiomeData.pos().z));
        }
        for (ClientboundChunksBiomesPacket.ChunkBiomeData chunkBiomeData : clientboundChunksBiomesPacket.chunkBiomeData()) {
            for (int i = -1; i <= 1; ++i) {
                for (int j = -1; j <= 1; ++j) {
                    for (int k = this.level.getMinSection(); k < this.level.getMaxSection(); ++k) {
                        this.minecraft.levelRenderer.setSectionDirty(chunkBiomeData.pos().x + i, k, chunkBiomeData.pos().z + j);
                    }
                }
            }
        }
    }

    private void updateLevelChunk(int i, int j, ClientboundLevelChunkPacketData clientboundLevelChunkPacketData) {
        this.level.getChunkSource().replaceWithPacketData(i, j, clientboundLevelChunkPacketData.getReadBuffer(), clientboundLevelChunkPacketData.getHeightmaps(), clientboundLevelChunkPacketData.getBlockEntitiesTagsConsumer(i, j));
    }

    private void enableChunkLight(LevelChunk levelChunk, int i, int j) {
        LevelLightEngine levelLightEngine = this.level.getChunkSource().getLightEngine();
        LevelChunkSection[] levelChunkSections = levelChunk.getSections();
        ChunkPos chunkPos = levelChunk.getPos();
        for (int k = 0; k < levelChunkSections.length; ++k) {
            LevelChunkSection levelChunkSection = levelChunkSections[k];
            int l = this.level.getSectionYFromSectionIndex(k);
            levelLightEngine.updateSectionStatus(SectionPos.of((ChunkPos)chunkPos, (int)l), levelChunkSection.hasOnlyAir());
            this.level.setSectionDirtyWithNeighbors(i, l, j);
        }
    }

    public void handleForgetLevelChunk(ClientboundForgetLevelChunkPacket clientboundForgetLevelChunkPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundForgetLevelChunkPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        int i = clientboundForgetLevelChunkPacket.getX();
        int j = clientboundForgetLevelChunkPacket.getZ();
        ClientChunkCache clientChunkCache = this.level.getChunkSource();
        clientChunkCache.drop(i, j);
        this.queueLightRemoval(clientboundForgetLevelChunkPacket);
    }

    private void queueLightRemoval(ClientboundForgetLevelChunkPacket clientboundForgetLevelChunkPacket) {
        ChunkPos chunkPos = new ChunkPos(clientboundForgetLevelChunkPacket.getX(), clientboundForgetLevelChunkPacket.getZ());
        this.level.queueLightUpdate(() -> {
            int i;
            LevelLightEngine levelLightEngine = this.level.getLightEngine();
            levelLightEngine.setLightEnabled(chunkPos, false);
            for (i = levelLightEngine.getMinLightSection(); i < levelLightEngine.getMaxLightSection(); ++i) {
                SectionPos sectionPos = SectionPos.of((ChunkPos)chunkPos, (int)i);
                levelLightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, null);
                levelLightEngine.queueSectionData(LightLayer.SKY, sectionPos, null);
            }
            for (i = this.level.getMinSection(); i < this.level.getMaxSection(); ++i) {
                levelLightEngine.updateSectionStatus(SectionPos.of((ChunkPos)chunkPos, (int)i), true);
            }
        });
    }

    public void handleBlockUpdate(ClientboundBlockUpdatePacket clientboundBlockUpdatePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundBlockUpdatePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.level.setServerVerifiedBlockState(clientboundBlockUpdatePacket.getPos(), clientboundBlockUpdatePacket.getBlockState(), 19);
    }

    public void handleDisconnect(ClientboundDisconnectPacket clientboundDisconnectPacket) {
        this.connection.disconnect(clientboundDisconnectPacket.getReason());
    }

    public void onDisconnect(Component component) {
        this.minecraft.clearLevel();
        this.telemetryManager.onDisconnect();
        if (this.callbackScreen != null) {
            if (this.callbackScreen instanceof RealmsScreen) {
                this.minecraft.setScreen(new DisconnectedRealmsScreen(this.callbackScreen, GENERIC_DISCONNECT_MESSAGE, component));
            } else {
                this.minecraft.setScreen(new DisconnectedScreen(this.callbackScreen, GENERIC_DISCONNECT_MESSAGE, component));
            }
        } else {
            this.minecraft.setScreen(new DisconnectedScreen(new JoinMultiplayerScreen(new TitleScreen()), GENERIC_DISCONNECT_MESSAGE, component));
        }
    }

    public void send(Packet<?> packet) {
        this.connection.send(packet);
    }

    public void handleTakeItemEntity(ClientboundTakeItemEntityPacket clientboundTakeItemEntityPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundTakeItemEntityPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundTakeItemEntityPacket.getItemId());
        Object livingEntity = (LivingEntity)this.level.getEntity(clientboundTakeItemEntityPacket.getPlayerId());
        if (livingEntity == null) {
            livingEntity = this.minecraft.player;
        }
        if (entity != null) {
            if (entity instanceof ExperienceOrb) {
                this.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1f, (this.random.nextFloat() - this.random.nextFloat()) * 0.35f + 0.9f, false);
            } else {
                this.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, (this.random.nextFloat() - this.random.nextFloat()) * 1.4f + 2.0f, false);
            }
            this.minecraft.particleEngine.add(new ItemPickupParticle(this.minecraft.getEntityRenderDispatcher(), this.minecraft.renderBuffers(), this.level, entity, (Entity)livingEntity));
            if (entity instanceof ItemEntity) {
                ItemEntity itemEntity = (ItemEntity)entity;
                ItemStack itemStack = itemEntity.getItem();
                if (!itemStack.isEmpty()) {
                    itemStack.shrink(clientboundTakeItemEntityPacket.getAmount());
                }
                if (itemStack.isEmpty()) {
                    this.level.removeEntity(clientboundTakeItemEntityPacket.getItemId(), Entity.RemovalReason.DISCARDED);
                }
            } else if (!(entity instanceof ExperienceOrb)) {
                this.level.removeEntity(clientboundTakeItemEntityPacket.getItemId(), Entity.RemovalReason.DISCARDED);
            }
        }
    }

    public void handleSystemChat(ClientboundSystemChatPacket clientboundSystemChatPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSystemChatPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.getChatListener().handleSystemMessage(clientboundSystemChatPacket.content(), clientboundSystemChatPacket.overlay());
    }

    public void handlePlayerChat(ClientboundPlayerChatPacket clientboundPlayerChatPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundPlayerChatPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Optional optional = clientboundPlayerChatPacket.body().unpack(this.messageSignatureCache);
        Optional optional2 = clientboundPlayerChatPacket.chatType().resolve((RegistryAccess)this.registryAccess.compositeAccess());
        if (optional.isEmpty() || optional2.isEmpty()) {
            this.connection.disconnect(INVALID_PACKET);
            return;
        }
        UUID uUID = clientboundPlayerChatPacket.sender();
        PlayerInfo playerInfo = this.getPlayerInfo(uUID);
        if (playerInfo == null) {
            this.connection.disconnect(CHAT_VALIDATION_FAILED_ERROR);
            return;
        }
        RemoteChatSession remoteChatSession = playerInfo.getChatSession();
        SignedMessageLink signedMessageLink = remoteChatSession != null ? new SignedMessageLink(clientboundPlayerChatPacket.index(), uUID, remoteChatSession.sessionId()) : SignedMessageLink.unsigned((UUID)uUID);
        PlayerChatMessage playerChatMessage = new PlayerChatMessage(signedMessageLink, clientboundPlayerChatPacket.signature(), (SignedMessageBody)optional.get(), clientboundPlayerChatPacket.unsignedContent(), clientboundPlayerChatPacket.filterMask());
        if (!playerInfo.getMessageValidator().updateAndValidate(playerChatMessage)) {
            this.connection.disconnect(CHAT_VALIDATION_FAILED_ERROR);
            return;
        }
        this.minecraft.getChatListener().handlePlayerChatMessage(playerChatMessage, playerInfo.getProfile(), (ChatType.Bound)optional2.get());
        this.messageSignatureCache.push(playerChatMessage);
    }

    public void handleDisguisedChat(ClientboundDisguisedChatPacket clientboundDisguisedChatPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundDisguisedChatPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Optional optional = clientboundDisguisedChatPacket.chatType().resolve((RegistryAccess)this.registryAccess.compositeAccess());
        if (optional.isEmpty()) {
            this.connection.disconnect(INVALID_PACKET);
            return;
        }
        this.minecraft.getChatListener().handleDisguisedChatMessage(clientboundDisguisedChatPacket.message(), (ChatType.Bound)optional.get());
    }

    public void handleDeleteChat(ClientboundDeleteChatPacket clientboundDeleteChatPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundDeleteChatPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Optional optional = clientboundDeleteChatPacket.messageSignature().unpack(this.messageSignatureCache);
        if (optional.isEmpty()) {
            this.connection.disconnect(INVALID_PACKET);
            return;
        }
        this.lastSeenMessages.ignorePending((MessageSignature)optional.get());
        if (!this.minecraft.getChatListener().removeFromDelayedMessageQueue((MessageSignature)optional.get())) {
            this.minecraft.gui.getChat().deleteMessage((MessageSignature)optional.get());
        }
    }

    public void handleAnimate(ClientboundAnimatePacket clientboundAnimatePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundAnimatePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundAnimatePacket.getId());
        if (entity == null) {
            return;
        }
        if (clientboundAnimatePacket.getAction() == 0) {
            LivingEntity livingEntity = (LivingEntity)entity;
            livingEntity.swing(InteractionHand.MAIN_HAND);
        } else if (clientboundAnimatePacket.getAction() == 3) {
            LivingEntity livingEntity = (LivingEntity)entity;
            livingEntity.swing(InteractionHand.OFF_HAND);
        } else if (clientboundAnimatePacket.getAction() == 2) {
            Player player = (Player)entity;
            player.stopSleepInBed(false, false);
        } else if (clientboundAnimatePacket.getAction() == 4) {
            this.minecraft.particleEngine.createTrackingEmitter(entity, (ParticleOptions)ParticleTypes.CRIT);
        } else if (clientboundAnimatePacket.getAction() == 5) {
            this.minecraft.particleEngine.createTrackingEmitter(entity, (ParticleOptions)ParticleTypes.ENCHANTED_HIT);
        }
    }

    public void handleHurtAnimation(ClientboundHurtAnimationPacket clientboundHurtAnimationPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundHurtAnimationPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundHurtAnimationPacket.id());
        if (entity == null) {
            return;
        }
        entity.animateHurt(clientboundHurtAnimationPacket.yaw());
    }

    public void handleSetTime(ClientboundSetTimePacket clientboundSetTimePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetTimePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.level.setGameTime(clientboundSetTimePacket.getGameTime());
        this.minecraft.level.setDayTime(clientboundSetTimePacket.getDayTime());
        this.telemetryManager.setTime(clientboundSetTimePacket.getGameTime());
    }

    public void handleSetSpawn(ClientboundSetDefaultSpawnPositionPacket clientboundSetDefaultSpawnPositionPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetDefaultSpawnPositionPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.level.setDefaultSpawnPos(clientboundSetDefaultSpawnPositionPacket.getPos(), clientboundSetDefaultSpawnPositionPacket.getAngle());
        Screen screen = this.minecraft.screen;
        if (screen instanceof ReceivingLevelScreen) {
            ReceivingLevelScreen receivingLevelScreen = (ReceivingLevelScreen)screen;
            receivingLevelScreen.loadingPacketsReceived();
        }
    }

    public void handleSetEntityPassengersPacket(ClientboundSetPassengersPacket clientboundSetPassengersPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetPassengersPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundSetPassengersPacket.getVehicle());
        if (entity == null) {
            LOGGER.warn("Received passengers for unknown entity");
            return;
        }
        boolean bl = entity.hasIndirectPassenger((Entity)this.minecraft.player);
        entity.ejectPassengers();
        for (int i : clientboundSetPassengersPacket.getPassengers()) {
            Entity entity2 = this.level.getEntity(i);
            if (entity2 == null) continue;
            entity2.startRiding(entity, true);
            if (entity2 != this.minecraft.player || bl) continue;
            if (entity instanceof Boat) {
                this.minecraft.player.yRotO = entity.getYRot();
                this.minecraft.player.setYRot(entity.getYRot());
                this.minecraft.player.setYHeadRot(entity.getYRot());
            }
            MutableComponent component = Component.translatable((String)"mount.onboard", (Object[])new Object[]{this.minecraft.options.keyShift.getTranslatedKeyMessage()});
            this.minecraft.gui.setOverlayMessage((Component)component, false);
            this.minecraft.getNarrator().sayNow((Component)component);
        }
    }

    public void handleEntityLinkPacket(ClientboundSetEntityLinkPacket clientboundSetEntityLinkPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetEntityLinkPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundSetEntityLinkPacket.getSourceId());
        if (entity instanceof Mob) {
            ((Mob)entity).setDelayedLeashHolderId(clientboundSetEntityLinkPacket.getDestId());
        }
    }

    private static ItemStack findTotem(Player player) {
        for (InteractionHand interactionHand : InteractionHand.values()) {
            ItemStack itemStack = player.getItemInHand(interactionHand);
            if (!itemStack.is(Items.TOTEM_OF_UNDYING)) continue;
            return itemStack;
        }
        return new ItemStack((ItemLike)Items.TOTEM_OF_UNDYING);
    }

    public void handleEntityEvent(ClientboundEntityEventPacket clientboundEntityEventPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundEntityEventPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = clientboundEntityEventPacket.getEntity((Level)this.level);
        if (entity != null) {
            switch (clientboundEntityEventPacket.getEventId()) {
                case 63: {
                    this.minecraft.getSoundManager().play(new SnifferSoundInstance((Sniffer)entity));
                    break;
                }
                case 21: {
                    this.minecraft.getSoundManager().play(new GuardianAttackSoundInstance((Guardian)entity));
                    break;
                }
                case 35: {
                    int i = 40;
                    this.minecraft.particleEngine.createTrackingEmitter(entity, (ParticleOptions)ParticleTypes.TOTEM_OF_UNDYING, 30);
                    this.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TOTEM_USE, entity.getSoundSource(), 1.0f, 1.0f, false);
                    if (entity != this.minecraft.player) break;
                    this.minecraft.gameRenderer.displayItemActivation(ClientPacketListener.findTotem(this.minecraft.player));
                    break;
                }
                default: {
                    entity.handleEntityEvent(clientboundEntityEventPacket.getEventId());
                }
            }
        }
    }

    public void handleDamageEvent(ClientboundDamageEventPacket clientboundDamageEventPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundDamageEventPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundDamageEventPacket.entityId());
        if (entity == null) {
            return;
        }
        entity.handleDamageEvent(clientboundDamageEventPacket.getSource((Level)this.level));
    }

    public void handleSetHealth(ClientboundSetHealthPacket clientboundSetHealthPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetHealthPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.player.hurtTo(clientboundSetHealthPacket.getHealth());
        this.minecraft.player.getFoodData().setFoodLevel(clientboundSetHealthPacket.getFood());
        this.minecraft.player.getFoodData().setSaturation(clientboundSetHealthPacket.getSaturation());
    }

    public void handleSetExperience(ClientboundSetExperiencePacket clientboundSetExperiencePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetExperiencePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.player.setExperienceValues(clientboundSetExperiencePacket.getExperienceProgress(), clientboundSetExperiencePacket.getTotalExperience(), clientboundSetExperiencePacket.getExperienceLevel());
    }

    public void handleRespawn(ClientboundRespawnPacket clientboundRespawnPacket) {
        List list;
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundRespawnPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        ResourceKey resourceKey = clientboundRespawnPacket.getDimension();
        Holder.Reference holder = this.registryAccess.compositeAccess().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(clientboundRespawnPacket.getDimensionType());
        LocalPlayer localPlayer = this.minecraft.player;
        int i = localPlayer.getId();
        if (resourceKey != localPlayer.level().dimension()) {
            ClientLevel.ClientLevelData clientLevelData;
            Scoreboard scoreboard = this.level.getScoreboard();
            Map<String, MapItemSavedData> map = this.level.getAllMapData();
            boolean bl = clientboundRespawnPacket.isDebug();
            boolean bl2 = clientboundRespawnPacket.isFlat();
            this.levelData = clientLevelData = new ClientLevel.ClientLevelData(this.levelData.getDifficulty(), this.levelData.isHardcore(), bl2);
            this.level = new ClientLevel(this, clientLevelData, (ResourceKey<Level>)resourceKey, (Holder<DimensionType>)holder, this.serverChunkRadius, this.serverSimulationDistance, this.minecraft::getProfiler, this.minecraft.levelRenderer, bl, clientboundRespawnPacket.getSeed());
            this.level.setScoreboard(scoreboard);
            this.level.addMapData(map);
            this.minecraft.setLevel(this.level);
            this.minecraft.setScreen(new ReceivingLevelScreen());
        }
        String string = localPlayer.getServerBrand();
        this.minecraft.cameraEntity = null;
        if (localPlayer.hasContainerOpen()) {
            localPlayer.closeContainer();
        }
        LocalPlayer localPlayer2 = clientboundRespawnPacket.shouldKeep((byte)2) ? this.minecraft.gameMode.createPlayer(this.level, localPlayer.getStats(), localPlayer.getRecipeBook(), localPlayer.isShiftKeyDown(), localPlayer.isSprinting()) : this.minecraft.gameMode.createPlayer(this.level, localPlayer.getStats(), localPlayer.getRecipeBook());
        localPlayer2.setId(i);
        this.minecraft.player = localPlayer2;
        if (resourceKey != localPlayer.level().dimension()) {
            this.minecraft.getMusicManager().stopPlaying();
        }
        this.minecraft.cameraEntity = localPlayer2;
        if (clientboundRespawnPacket.shouldKeep((byte)2) && (list = localPlayer.getEntityData().getNonDefaultValues()) != null) {
            localPlayer2.getEntityData().assignValues(list);
        }
        if (clientboundRespawnPacket.shouldKeep((byte)1)) {
            localPlayer2.getAttributes().assignValues(localPlayer.getAttributes());
        }
        localPlayer2.resetPos();
        localPlayer2.setServerBrand(string);
        this.level.addPlayer(i, localPlayer2);
        localPlayer2.setYRot(-180.0f);
        localPlayer2.input = new KeyboardInput(this.minecraft.options);
        this.minecraft.gameMode.adjustPlayer(localPlayer2);
        localPlayer2.setReducedDebugInfo(localPlayer.isReducedDebugInfo());
        localPlayer2.setShowDeathScreen(localPlayer.shouldShowDeathScreen());
        localPlayer2.setLastDeathLocation(clientboundRespawnPacket.getLastDeathLocation());
        localPlayer2.setPortalCooldown(clientboundRespawnPacket.getPortalCooldown());
        localPlayer2.spinningEffectIntensity = localPlayer.spinningEffectIntensity;
        localPlayer2.oSpinningEffectIntensity = localPlayer.oSpinningEffectIntensity;
        if (this.minecraft.screen instanceof DeathScreen || this.minecraft.screen instanceof DeathScreen.TitleConfirmScreen) {
            this.minecraft.setScreen(null);
        }
        this.minecraft.gameMode.setLocalMode(clientboundRespawnPacket.getPlayerGameType(), clientboundRespawnPacket.getPreviousPlayerGameType());
    }

    public void handleExplosion(ClientboundExplodePacket clientboundExplodePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundExplodePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Explosion explosion = new Explosion((Level)this.minecraft.level, null, clientboundExplodePacket.getX(), clientboundExplodePacket.getY(), clientboundExplodePacket.getZ(), clientboundExplodePacket.getPower(), clientboundExplodePacket.getToBlow());
        explosion.finalizeExplosion(true);
        this.minecraft.player.setDeltaMovement(this.minecraft.player.getDeltaMovement().add((double)clientboundExplodePacket.getKnockbackX(), (double)clientboundExplodePacket.getKnockbackY(), (double)clientboundExplodePacket.getKnockbackZ()));
    }

    public void handleHorseScreenOpen(ClientboundHorseScreenOpenPacket clientboundHorseScreenOpenPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundHorseScreenOpenPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundHorseScreenOpenPacket.getEntityId());
        if (entity instanceof AbstractHorse) {
            LocalPlayer localPlayer = this.minecraft.player;
            AbstractHorse abstractHorse = (AbstractHorse)entity;
            SimpleContainer simpleContainer = new SimpleContainer(clientboundHorseScreenOpenPacket.getSize());
            HorseInventoryMenu horseInventoryMenu = new HorseInventoryMenu(clientboundHorseScreenOpenPacket.getContainerId(), localPlayer.getInventory(), (Container)simpleContainer, abstractHorse);
            localPlayer.containerMenu = horseInventoryMenu;
            this.minecraft.setScreen(new HorseInventoryScreen(horseInventoryMenu, localPlayer.getInventory(), abstractHorse));
        }
    }

    public void handleOpenScreen(ClientboundOpenScreenPacket clientboundOpenScreenPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundOpenScreenPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        MenuScreens.create(clientboundOpenScreenPacket.getType(), this.minecraft, clientboundOpenScreenPacket.getContainerId(), clientboundOpenScreenPacket.getTitle());
    }

    public void handleContainerSetSlot(ClientboundContainerSetSlotPacket clientboundContainerSetSlotPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundContainerSetSlotPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        LocalPlayer player = this.minecraft.player;
        ItemStack itemStack = clientboundContainerSetSlotPacket.getItem();
        int i = clientboundContainerSetSlotPacket.getSlot();
        this.minecraft.getTutorial().onGetItem(itemStack);
        if (clientboundContainerSetSlotPacket.getContainerId() == -1) {
            if (!(this.minecraft.screen instanceof CreativeModeInventoryScreen)) {
                player.containerMenu.setCarried(itemStack);
            }
        } else if (clientboundContainerSetSlotPacket.getContainerId() == -2) {
            player.getInventory().setItem(i, itemStack);
        } else {
            boolean bl = false;
            Screen screen = this.minecraft.screen;
            if (screen instanceof CreativeModeInventoryScreen) {
                CreativeModeInventoryScreen creativeModeInventoryScreen = (CreativeModeInventoryScreen)screen;
                boolean bl2 = bl = !creativeModeInventoryScreen.isInventoryOpen();
            }
            if (clientboundContainerSetSlotPacket.getContainerId() == 0 && InventoryMenu.isHotbarSlot((int)i)) {
                ItemStack itemStack2;
                if (!itemStack.isEmpty() && ((itemStack2 = player.inventoryMenu.getSlot(i).getItem()).isEmpty() || itemStack2.getCount() < itemStack.getCount())) {
                    itemStack.setPopTime(5);
                }
                player.inventoryMenu.setItem(i, clientboundContainerSetSlotPacket.getStateId(), itemStack);
            } else if (!(clientboundContainerSetSlotPacket.getContainerId() != player.containerMenu.containerId || clientboundContainerSetSlotPacket.getContainerId() == 0 && bl)) {
                player.containerMenu.setItem(i, clientboundContainerSetSlotPacket.getStateId(), itemStack);
            }
        }
    }

    public void handleContainerContent(ClientboundContainerSetContentPacket clientboundContainerSetContentPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundContainerSetContentPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        LocalPlayer player = this.minecraft.player;
        if (clientboundContainerSetContentPacket.getContainerId() == 0) {
            player.inventoryMenu.initializeContents(clientboundContainerSetContentPacket.getStateId(), clientboundContainerSetContentPacket.getItems(), clientboundContainerSetContentPacket.getCarriedItem());
        } else if (clientboundContainerSetContentPacket.getContainerId() == player.containerMenu.containerId) {
            player.containerMenu.initializeContents(clientboundContainerSetContentPacket.getStateId(), clientboundContainerSetContentPacket.getItems(), clientboundContainerSetContentPacket.getCarriedItem());
        }
    }

    public void handleOpenSignEditor(ClientboundOpenSignEditorPacket clientboundOpenSignEditorPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundOpenSignEditorPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        BlockPos blockPos = clientboundOpenSignEditorPacket.getPos();
        BlockEntity blockEntity = this.level.getBlockEntity(blockPos);
        if (blockEntity instanceof SignBlockEntity) {
            SignBlockEntity signBlockEntity = (SignBlockEntity)blockEntity;
            this.minecraft.player.openTextEdit(signBlockEntity, clientboundOpenSignEditorPacket.isFrontText());
        } else {
            BlockState blockState = this.level.getBlockState(blockPos);
            SignBlockEntity signBlockEntity2 = new SignBlockEntity(blockPos, blockState);
            signBlockEntity2.setLevel((Level)this.level);
            this.minecraft.player.openTextEdit(signBlockEntity2, clientboundOpenSignEditorPacket.isFrontText());
        }
    }

    public void handleBlockEntityData(ClientboundBlockEntityDataPacket clientboundBlockEntityDataPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundBlockEntityDataPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        BlockPos blockPos = clientboundBlockEntityDataPacket.getPos();
        this.minecraft.level.getBlockEntity(blockPos, clientboundBlockEntityDataPacket.getType()).ifPresent(blockEntity -> {
            CompoundTag compoundTag = clientboundBlockEntityDataPacket.getTag();
            if (compoundTag != null) {
                blockEntity.load(compoundTag);
            }
            if (blockEntity instanceof CommandBlockEntity && this.minecraft.screen instanceof CommandBlockEditScreen) {
                ((CommandBlockEditScreen)this.minecraft.screen).updateGui();
            }
        });
    }

    public void handleContainerSetData(ClientboundContainerSetDataPacket clientboundContainerSetDataPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundContainerSetDataPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        LocalPlayer player = this.minecraft.player;
        if (player.containerMenu != null && player.containerMenu.containerId == clientboundContainerSetDataPacket.getContainerId()) {
            player.containerMenu.setData(clientboundContainerSetDataPacket.getId(), clientboundContainerSetDataPacket.getValue());
        }
    }

    public void handleSetEquipment(ClientboundSetEquipmentPacket clientboundSetEquipmentPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetEquipmentPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundSetEquipmentPacket.getEntity());
        if (entity != null) {
            clientboundSetEquipmentPacket.getSlots().forEach(pair -> entity.setItemSlot((EquipmentSlot)pair.getFirst(), (ItemStack)pair.getSecond()));
        }
    }

    public void handleContainerClose(ClientboundContainerClosePacket clientboundContainerClosePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundContainerClosePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.player.clientSideCloseContainer();
    }

    public void handleBlockEvent(ClientboundBlockEventPacket clientboundBlockEventPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundBlockEventPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.level.blockEvent(clientboundBlockEventPacket.getPos(), clientboundBlockEventPacket.getBlock(), clientboundBlockEventPacket.getB0(), clientboundBlockEventPacket.getB1());
    }

    public void handleBlockDestruction(ClientboundBlockDestructionPacket clientboundBlockDestructionPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundBlockDestructionPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.level.destroyBlockProgress(clientboundBlockDestructionPacket.getId(), clientboundBlockDestructionPacket.getPos(), clientboundBlockDestructionPacket.getProgress());
    }

    public void handleGameEvent(ClientboundGameEventPacket clientboundGameEventPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundGameEventPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        LocalPlayer player = this.minecraft.player;
        ClientboundGameEventPacket.Type type = clientboundGameEventPacket.getEvent();
        float f = clientboundGameEventPacket.getParam();
        int i = Mth.floor((float)(f + 0.5f));
        if (type == ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE) {
            player.displayClientMessage((Component)Component.translatable((String)"block.minecraft.spawn.not_valid"), false);
        } else if (type == ClientboundGameEventPacket.START_RAINING) {
            this.level.getLevelData().setRaining(true);
            this.level.setRainLevel(0.0f);
        } else if (type == ClientboundGameEventPacket.STOP_RAINING) {
            this.level.getLevelData().setRaining(false);
            this.level.setRainLevel(1.0f);
        } else if (type == ClientboundGameEventPacket.CHANGE_GAME_MODE) {
            this.minecraft.gameMode.setLocalMode(GameType.byId((int)i));
        } else if (type == ClientboundGameEventPacket.WIN_GAME) {
            if (i == 0) {
                this.minecraft.player.connection.send((Packet<?>)new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                this.minecraft.setScreen(new ReceivingLevelScreen());
            } else if (i == 1) {
                this.minecraft.setScreen(new WinScreen(true, () -> {
                    this.minecraft.player.connection.send((Packet<?>)new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                    this.minecraft.setScreen(null);
                }));
            }
        } else if (type == ClientboundGameEventPacket.DEMO_EVENT) {
            Options options = this.minecraft.options;
            if (f == 0.0f) {
                this.minecraft.setScreen(new DemoIntroScreen());
            } else if (f == 101.0f) {
                this.minecraft.gui.getChat().addMessage((Component)Component.translatable((String)"demo.help.movement", (Object[])new Object[]{options.keyUp.getTranslatedKeyMessage(), options.keyLeft.getTranslatedKeyMessage(), options.keyDown.getTranslatedKeyMessage(), options.keyRight.getTranslatedKeyMessage()}));
            } else if (f == 102.0f) {
                this.minecraft.gui.getChat().addMessage((Component)Component.translatable((String)"demo.help.jump", (Object[])new Object[]{options.keyJump.getTranslatedKeyMessage()}));
            } else if (f == 103.0f) {
                this.minecraft.gui.getChat().addMessage((Component)Component.translatable((String)"demo.help.inventory", (Object[])new Object[]{options.keyInventory.getTranslatedKeyMessage()}));
            } else if (f == 104.0f) {
                this.minecraft.gui.getChat().addMessage((Component)Component.translatable((String)"demo.day.6", (Object[])new Object[]{options.keyScreenshot.getTranslatedKeyMessage()}));
            }
        } else if (type == ClientboundGameEventPacket.ARROW_HIT_PLAYER) {
            this.level.playSound(player, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.18f, 0.45f);
        } else if (type == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
            this.level.setRainLevel(f);
        } else if (type == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
            this.level.setThunderLevel(f);
        } else if (type == ClientboundGameEventPacket.PUFFER_FISH_STING) {
            this.level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.PUFFER_FISH_STING, SoundSource.NEUTRAL, 1.0f, 1.0f);
        } else if (type == ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT) {
            this.level.addParticle((ParticleOptions)ParticleTypes.ELDER_GUARDIAN, player.getX(), player.getY(), player.getZ(), 0.0, 0.0, 0.0);
            if (i == 1) {
                this.level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0f, 1.0f);
            }
        } else if (type == ClientboundGameEventPacket.IMMEDIATE_RESPAWN) {
            this.minecraft.player.setShowDeathScreen(f == 0.0f);
        }
    }

    public void handleMapItemData(ClientboundMapItemDataPacket clientboundMapItemDataPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundMapItemDataPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        MapRenderer mapRenderer = this.minecraft.gameRenderer.getMapRenderer();
        int i = clientboundMapItemDataPacket.getMapId();
        String string = MapItem.makeKey((int)i);
        MapItemSavedData mapItemSavedData = this.minecraft.level.getMapData(string);
        if (mapItemSavedData == null) {
            mapItemSavedData = MapItemSavedData.createForClient((byte)clientboundMapItemDataPacket.getScale(), (boolean)clientboundMapItemDataPacket.isLocked(), (ResourceKey)this.minecraft.level.dimension());
            this.minecraft.level.overrideMapData(string, mapItemSavedData);
        }
        clientboundMapItemDataPacket.applyToMap(mapItemSavedData);
        mapRenderer.update(i, mapItemSavedData);
    }

    public void handleLevelEvent(ClientboundLevelEventPacket clientboundLevelEventPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundLevelEventPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        if (clientboundLevelEventPacket.isGlobalEvent()) {
            this.minecraft.level.globalLevelEvent(clientboundLevelEventPacket.getType(), clientboundLevelEventPacket.getPos(), clientboundLevelEventPacket.getData());
        } else {
            this.minecraft.level.levelEvent(clientboundLevelEventPacket.getType(), clientboundLevelEventPacket.getPos(), clientboundLevelEventPacket.getData());
        }
    }

    public void handleUpdateAdvancementsPacket(ClientboundUpdateAdvancementsPacket clientboundUpdateAdvancementsPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundUpdateAdvancementsPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.advancements.update(clientboundUpdateAdvancementsPacket);
    }

    public void handleSelectAdvancementsTab(ClientboundSelectAdvancementsTabPacket clientboundSelectAdvancementsTabPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSelectAdvancementsTabPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        ResourceLocation resourceLocation = clientboundSelectAdvancementsTabPacket.getTab();
        if (resourceLocation == null) {
            this.advancements.setSelectedTab(null, false);
        } else {
            Advancement advancement = this.advancements.getAdvancements().get(resourceLocation);
            this.advancements.setSelectedTab(advancement, false);
        }
    }

    public void handleCommands(ClientboundCommandsPacket clientboundCommandsPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundCommandsPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.commands = new CommandDispatcher(clientboundCommandsPacket.getRoot(CommandBuildContext.simple((HolderLookup.Provider)this.registryAccess.compositeAccess(), (FeatureFlagSet)this.enabledFeatures)));
    }

    public void handleStopSoundEvent(ClientboundStopSoundPacket clientboundStopSoundPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundStopSoundPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.getSoundManager().stop(clientboundStopSoundPacket.getName(), clientboundStopSoundPacket.getSource());
    }

    public void handleCommandSuggestions(ClientboundCommandSuggestionsPacket clientboundCommandSuggestionsPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundCommandSuggestionsPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.suggestionsProvider.completeCustomSuggestions(clientboundCommandSuggestionsPacket.getId(), clientboundCommandSuggestionsPacket.getSuggestions());
    }

    public void handleUpdateRecipes(ClientboundUpdateRecipesPacket clientboundUpdateRecipesPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundUpdateRecipesPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.recipeManager.replaceRecipes((Iterable)clientboundUpdateRecipesPacket.getRecipes());
        ClientRecipeBook clientRecipeBook = this.minecraft.player.getRecipeBook();
        clientRecipeBook.setupCollections(this.recipeManager.getRecipes(), this.minecraft.level.registryAccess());
        this.minecraft.populateSearchTree(SearchRegistry.RECIPE_COLLECTIONS, clientRecipeBook.getCollections());
    }

    public void handleLookAt(ClientboundPlayerLookAtPacket clientboundPlayerLookAtPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundPlayerLookAtPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Vec3 vec3 = clientboundPlayerLookAtPacket.getPosition((Level)this.level);
        if (vec3 != null) {
            this.minecraft.player.lookAt(clientboundPlayerLookAtPacket.getFromAnchor(), vec3);
        }
    }

    public void handleTagQueryPacket(ClientboundTagQueryPacket clientboundTagQueryPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundTagQueryPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        if (!this.debugQueryHandler.handleResponse(clientboundTagQueryPacket.getTransactionId(), clientboundTagQueryPacket.getTag())) {
            LOGGER.debug("Got unhandled response to tag query {}", (Object)clientboundTagQueryPacket.getTransactionId());
        }
    }

    public void handleAwardStats(ClientboundAwardStatsPacket clientboundAwardStatsPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundAwardStatsPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        for (Map.Entry entry : clientboundAwardStatsPacket.getStats().entrySet()) {
            Stat stat = (Stat)entry.getKey();
            int i = (Integer)entry.getValue();
            this.minecraft.player.getStats().setValue((Player)this.minecraft.player, stat, i);
        }
        if (this.minecraft.screen instanceof StatsUpdateListener) {
            ((StatsUpdateListener)((Object)this.minecraft.screen)).onStatsUpdated();
        }
    }

    public void handleAddOrRemoveRecipes(ClientboundRecipePacket clientboundRecipePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundRecipePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        ClientRecipeBook clientRecipeBook = this.minecraft.player.getRecipeBook();
        clientRecipeBook.setBookSettings(clientboundRecipePacket.getBookSettings());
        ClientboundRecipePacket.State state = clientboundRecipePacket.getState();
        switch (state) {
            case REMOVE: {
                for (ResourceLocation resourceLocation : clientboundRecipePacket.getRecipes()) {
                    this.recipeManager.byKey(resourceLocation).ifPresent(arg_0 -> ((ClientRecipeBook)clientRecipeBook).remove(arg_0));
                }
                break;
            }
            case INIT: {
                for (ResourceLocation resourceLocation : clientboundRecipePacket.getRecipes()) {
                    this.recipeManager.byKey(resourceLocation).ifPresent(arg_0 -> ((ClientRecipeBook)clientRecipeBook).add(arg_0));
                }
                for (ResourceLocation resourceLocation : clientboundRecipePacket.getHighlights()) {
                    this.recipeManager.byKey(resourceLocation).ifPresent(arg_0 -> ((ClientRecipeBook)clientRecipeBook).addHighlight(arg_0));
                }
                break;
            }
            case ADD: {
                for (ResourceLocation resourceLocation : clientboundRecipePacket.getRecipes()) {
                    this.recipeManager.byKey(resourceLocation).ifPresent(recipe -> {
                        clientRecipeBook.add((Recipe)recipe);
                        clientRecipeBook.addHighlight((Recipe)recipe);
                        if (recipe.showNotification()) {
                            RecipeToast.addOrUpdate(this.minecraft.getToasts(), recipe);
                        }
                    });
                }
                break;
            }
        }
        clientRecipeBook.getCollections().forEach(recipeCollection -> recipeCollection.updateKnownRecipes(clientRecipeBook));
        if (this.minecraft.screen instanceof RecipeUpdateListener) {
            ((RecipeUpdateListener)((Object)this.minecraft.screen)).recipesUpdated();
        }
    }

    public void handleUpdateMobEffect(ClientboundUpdateMobEffectPacket clientboundUpdateMobEffectPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundUpdateMobEffectPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundUpdateMobEffectPacket.getEntityId());
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        MobEffect mobEffect = clientboundUpdateMobEffectPacket.getEffect();
        if (mobEffect == null) {
            return;
        }
        MobEffectInstance mobEffectInstance = new MobEffectInstance(mobEffect, clientboundUpdateMobEffectPacket.getEffectDurationTicks(), (int)clientboundUpdateMobEffectPacket.getEffectAmplifier(), clientboundUpdateMobEffectPacket.isEffectAmbient(), clientboundUpdateMobEffectPacket.isEffectVisible(), clientboundUpdateMobEffectPacket.effectShowsIcon(), null, Optional.ofNullable(clientboundUpdateMobEffectPacket.getFactorData()));
        ((LivingEntity)entity).forceAddEffect(mobEffectInstance, null);
    }

    public void handleUpdateTags(ClientboundUpdateTagsPacket clientboundUpdateTagsPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundUpdateTagsPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        clientboundUpdateTagsPacket.getTags().forEach(this::updateTagsForRegistry);
        if (!this.connection.isMemoryConnection()) {
            Blocks.rebuildCache();
        }
        CreativeModeTabs.searchTab().rebuildSearchTree();
    }

    public void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket clientboundUpdateEnabledFeaturesPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundUpdateEnabledFeaturesPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.enabledFeatures = FeatureFlags.REGISTRY.fromNames((Iterable)clientboundUpdateEnabledFeaturesPacket.features());
    }

    private <T> void updateTagsForRegistry(ResourceKey<? extends Registry<? extends T>> resourceKey, TagNetworkSerialization.NetworkPayload networkPayload) {
        if (networkPayload.isEmpty()) {
            return;
        }
        Registry registry = (Registry)this.registryAccess.compositeAccess().registry(resourceKey).orElseThrow(() -> new IllegalStateException("Unknown registry " + resourceKey));
        ResourceKey resourceKey2 = resourceKey;
        HashMap map = new HashMap();
        TagNetworkSerialization.deserializeTagsFromNetwork(resourceKey2, (Registry)registry, (TagNetworkSerialization.NetworkPayload)networkPayload, map::put);
        registry.bindTags(map);
    }

    public void handlePlayerCombatEnd(ClientboundPlayerCombatEndPacket clientboundPlayerCombatEndPacket) {
    }

    public void handlePlayerCombatEnter(ClientboundPlayerCombatEnterPacket clientboundPlayerCombatEnterPacket) {
    }

    public void handlePlayerCombatKill(ClientboundPlayerCombatKillPacket clientboundPlayerCombatKillPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundPlayerCombatKillPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundPlayerCombatKillPacket.getPlayerId());
        if (entity == this.minecraft.player) {
            if (this.minecraft.player.shouldShowDeathScreen()) {
                this.minecraft.setScreen(new DeathScreen(clientboundPlayerCombatKillPacket.getMessage(), this.level.getLevelData().isHardcore()));
            } else {
                this.minecraft.player.respawn();
            }
        }
    }

    public void handleChangeDifficulty(ClientboundChangeDifficultyPacket clientboundChangeDifficultyPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundChangeDifficultyPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.levelData.setDifficulty(clientboundChangeDifficultyPacket.getDifficulty());
        this.levelData.setDifficultyLocked(clientboundChangeDifficultyPacket.isLocked());
    }

    public void handleSetCamera(ClientboundSetCameraPacket clientboundSetCameraPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetCameraPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = clientboundSetCameraPacket.getEntity((Level)this.level);
        if (entity != null) {
            this.minecraft.setCameraEntity(entity);
        }
    }

    public void handleInitializeBorder(ClientboundInitializeBorderPacket clientboundInitializeBorderPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundInitializeBorderPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        WorldBorder worldBorder = this.level.getWorldBorder();
        worldBorder.setCenter(clientboundInitializeBorderPacket.getNewCenterX(), clientboundInitializeBorderPacket.getNewCenterZ());
        long l = clientboundInitializeBorderPacket.getLerpTime();
        if (l > 0L) {
            worldBorder.lerpSizeBetween(clientboundInitializeBorderPacket.getOldSize(), clientboundInitializeBorderPacket.getNewSize(), l);
        } else {
            worldBorder.setSize(clientboundInitializeBorderPacket.getNewSize());
        }
        worldBorder.setAbsoluteMaxSize(clientboundInitializeBorderPacket.getNewAbsoluteMaxSize());
        worldBorder.setWarningBlocks(clientboundInitializeBorderPacket.getWarningBlocks());
        worldBorder.setWarningTime(clientboundInitializeBorderPacket.getWarningTime());
    }

    public void handleSetBorderCenter(ClientboundSetBorderCenterPacket clientboundSetBorderCenterPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetBorderCenterPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.level.getWorldBorder().setCenter(clientboundSetBorderCenterPacket.getNewCenterX(), clientboundSetBorderCenterPacket.getNewCenterZ());
    }

    public void handleSetBorderLerpSize(ClientboundSetBorderLerpSizePacket clientboundSetBorderLerpSizePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetBorderLerpSizePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.level.getWorldBorder().lerpSizeBetween(clientboundSetBorderLerpSizePacket.getOldSize(), clientboundSetBorderLerpSizePacket.getNewSize(), clientboundSetBorderLerpSizePacket.getLerpTime());
    }

    public void handleSetBorderSize(ClientboundSetBorderSizePacket clientboundSetBorderSizePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetBorderSizePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.level.getWorldBorder().setSize(clientboundSetBorderSizePacket.getSize());
    }

    public void handleSetBorderWarningDistance(ClientboundSetBorderWarningDistancePacket clientboundSetBorderWarningDistancePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetBorderWarningDistancePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.level.getWorldBorder().setWarningBlocks(clientboundSetBorderWarningDistancePacket.getWarningBlocks());
    }

    public void handleSetBorderWarningDelay(ClientboundSetBorderWarningDelayPacket clientboundSetBorderWarningDelayPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetBorderWarningDelayPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.level.getWorldBorder().setWarningTime(clientboundSetBorderWarningDelayPacket.getWarningDelay());
    }

    public void handleTitlesClear(ClientboundClearTitlesPacket clientboundClearTitlesPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundClearTitlesPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.gui.clear();
        if (clientboundClearTitlesPacket.shouldResetTimes()) {
            this.minecraft.gui.resetTitleTimes();
        }
    }

    public void handleServerData(ClientboundServerDataPacket clientboundServerDataPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundServerDataPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        if (this.serverData == null) {
            return;
        }
        this.serverData.motd = clientboundServerDataPacket.getMotd();
        clientboundServerDataPacket.getIconBytes().ifPresent(this.serverData::setIconBytes);
        this.serverData.setEnforcesSecureChat(clientboundServerDataPacket.enforcesSecureChat());
        ServerList.saveSingleServer(this.serverData);
        if (!clientboundServerDataPacket.enforcesSecureChat()) {
            SystemToast systemToast = SystemToast.multiline(this.minecraft, SystemToast.SystemToastIds.UNSECURE_SERVER_WARNING, UNSECURE_SERVER_TOAST_TITLE, UNSERURE_SERVER_TOAST);
            this.minecraft.getToasts().addToast(systemToast);
        }
    }

    public void handleCustomChatCompletions(ClientboundCustomChatCompletionsPacket clientboundCustomChatCompletionsPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundCustomChatCompletionsPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.suggestionsProvider.modifyCustomCompletions(clientboundCustomChatCompletionsPacket.action(), clientboundCustomChatCompletionsPacket.entries());
    }

    public void setActionBarText(ClientboundSetActionBarTextPacket clientboundSetActionBarTextPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetActionBarTextPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.gui.setOverlayMessage(clientboundSetActionBarTextPacket.getText(), false);
    }

    public void setTitleText(ClientboundSetTitleTextPacket clientboundSetTitleTextPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetTitleTextPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.gui.setTitle(clientboundSetTitleTextPacket.getText());
    }

    public void setSubtitleText(ClientboundSetSubtitleTextPacket clientboundSetSubtitleTextPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetSubtitleTextPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.gui.setSubtitle(clientboundSetSubtitleTextPacket.getText());
    }

    public void setTitlesAnimation(ClientboundSetTitlesAnimationPacket clientboundSetTitlesAnimationPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetTitlesAnimationPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.gui.setTimes(clientboundSetTitlesAnimationPacket.getFadeIn(), clientboundSetTitlesAnimationPacket.getStay(), clientboundSetTitlesAnimationPacket.getFadeOut());
    }

    public void handleTabListCustomisation(ClientboundTabListPacket clientboundTabListPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundTabListPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.gui.getTabList().setHeader(clientboundTabListPacket.getHeader().getString().isEmpty() ? null : clientboundTabListPacket.getHeader());
        this.minecraft.gui.getTabList().setFooter(clientboundTabListPacket.getFooter().getString().isEmpty() ? null : clientboundTabListPacket.getFooter());
    }

    public void handleRemoveMobEffect(ClientboundRemoveMobEffectPacket clientboundRemoveMobEffectPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundRemoveMobEffectPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = clientboundRemoveMobEffectPacket.getEntity((Level)this.level);
        if (entity instanceof LivingEntity) {
            ((LivingEntity)entity).removeEffectNoUpdate(clientboundRemoveMobEffectPacket.getEffect());
        }
    }

    public void handlePlayerInfoRemove(ClientboundPlayerInfoRemovePacket clientboundPlayerInfoRemovePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundPlayerInfoRemovePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        for (UUID uUID : clientboundPlayerInfoRemovePacket.profileIds()) {
            this.minecraft.getPlayerSocialManager().removePlayer(uUID);
            PlayerInfo playerInfo = this.playerInfoMap.remove(uUID);
            if (playerInfo == null) continue;
            this.listedPlayers.remove(playerInfo);
        }
    }

    public void handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket clientboundPlayerInfoUpdatePacket) {
        PlayerInfo playerInfo;
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundPlayerInfoUpdatePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : clientboundPlayerInfoUpdatePacket.newEntries()) {
            playerInfo = new PlayerInfo(entry.profile(), this.enforcesSecureChat());
            if (this.playerInfoMap.putIfAbsent(entry.profileId(), playerInfo) != null) continue;
            this.minecraft.getPlayerSocialManager().addPlayer(playerInfo);
        }
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : clientboundPlayerInfoUpdatePacket.entries()) {
            playerInfo = this.playerInfoMap.get(entry.profileId());
            if (playerInfo == null) {
                LOGGER.warn("Ignoring player info update for unknown player {}", (Object)entry.profileId());
                continue;
            }
            for (ClientboundPlayerInfoUpdatePacket.Action action : clientboundPlayerInfoUpdatePacket.actions()) {
                this.applyPlayerInfoUpdate(action, entry, playerInfo);
            }
        }
    }

    private void applyPlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket.Action action, ClientboundPlayerInfoUpdatePacket.Entry entry, PlayerInfo playerInfo) {
        switch (action) {
            case INITIALIZE_CHAT: {
                this.initializeChatSession(entry, playerInfo);
                break;
            }
            case UPDATE_GAME_MODE: {
                if (playerInfo.getGameMode() != entry.gameMode() && this.minecraft.player != null && this.minecraft.player.getUUID().equals(entry.profileId())) {
                    this.minecraft.player.onGameModeChanged(entry.gameMode());
                }
                playerInfo.setGameMode(entry.gameMode());
                break;
            }
            case UPDATE_LISTED: {
                if (entry.listed()) {
                    this.listedPlayers.add(playerInfo);
                    break;
                }
                this.listedPlayers.remove(playerInfo);
                break;
            }
            case UPDATE_LATENCY: {
                playerInfo.setLatency(entry.latency());
                break;
            }
            case UPDATE_DISPLAY_NAME: {
                playerInfo.setTabListDisplayName(entry.displayName());
            }
        }
    }

    private void initializeChatSession(ClientboundPlayerInfoUpdatePacket.Entry entry, PlayerInfo playerInfo) {
        GameProfile gameProfile = playerInfo.getProfile();
        SignatureValidator signatureValidator = this.minecraft.getProfileKeySignatureValidator();
        if (signatureValidator == null) {
            LOGGER.warn("Ignoring chat session from {} due to missing Services public key", (Object)gameProfile.getName());
            playerInfo.clearChatSession(this.enforcesSecureChat());
            return;
        }
        RemoteChatSession.Data data = entry.chatSession();
        if (data != null) {
            try {
                RemoteChatSession remoteChatSession = data.validate(gameProfile, signatureValidator, ProfilePublicKey.EXPIRY_GRACE_PERIOD);
                playerInfo.setChatSession(remoteChatSession);
            } catch (ProfilePublicKey.ValidationException validationException) {
                LOGGER.error("Failed to validate profile key for player: '{}'", (Object)gameProfile.getName(), (Object)validationException);
                playerInfo.clearChatSession(this.enforcesSecureChat());
            }
        } else {
            playerInfo.clearChatSession(this.enforcesSecureChat());
        }
    }

    private boolean enforcesSecureChat() {
        return this.serverData != null && this.serverData.enforcesSecureChat();
    }

    public void handleKeepAlive(ClientboundKeepAlivePacket clientboundKeepAlivePacket) {
        this.sendWhen((Packet<ServerGamePacketListener>)new ServerboundKeepAlivePacket(clientboundKeepAlivePacket.getId()), () -> !RenderSystem.isFrozenAtPollEvents(), Duration.ofMinutes(1L));
    }

    private void sendWhen(Packet<ServerGamePacketListener> packet, BooleanSupplier booleanSupplier, Duration duration) {
        if (booleanSupplier.getAsBoolean()) {
            this.send(packet);
        } else {
            this.deferredPackets.add(new DeferredPacket(packet, booleanSupplier, Util.getMillis() + duration.toMillis()));
        }
    }

    private void sendDeferredPackets() {
        Iterator<DeferredPacket> iterator = this.deferredPackets.iterator();
        while (iterator.hasNext()) {
            DeferredPacket deferredPacket = iterator.next();
            if (deferredPacket.sendCondition().getAsBoolean()) {
                this.send(deferredPacket.packet);
                iterator.remove();
                continue;
            }
            if (deferredPacket.expirationTime() > Util.getMillis()) continue;
            iterator.remove();
        }
    }

    public void handlePlayerAbilities(ClientboundPlayerAbilitiesPacket clientboundPlayerAbilitiesPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundPlayerAbilitiesPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        LocalPlayer player = this.minecraft.player;
        player.getAbilities().flying = clientboundPlayerAbilitiesPacket.isFlying();
        player.getAbilities().instabuild = clientboundPlayerAbilitiesPacket.canInstabuild();
        player.getAbilities().invulnerable = clientboundPlayerAbilitiesPacket.isInvulnerable();
        player.getAbilities().mayfly = clientboundPlayerAbilitiesPacket.canFly();
        player.getAbilities().setFlyingSpeed(clientboundPlayerAbilitiesPacket.getFlyingSpeed());
        player.getAbilities().setWalkingSpeed(clientboundPlayerAbilitiesPacket.getWalkingSpeed());
    }

    public void handleSoundEvent(ClientboundSoundPacket clientboundSoundPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSoundPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.level.playSeededSound(this.minecraft.player, clientboundSoundPacket.getX(), clientboundSoundPacket.getY(), clientboundSoundPacket.getZ(), (Holder<SoundEvent>)clientboundSoundPacket.getSound(), clientboundSoundPacket.getSource(), clientboundSoundPacket.getVolume(), clientboundSoundPacket.getPitch(), clientboundSoundPacket.getSeed());
    }

    public void handleSoundEntityEvent(ClientboundSoundEntityPacket clientboundSoundEntityPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSoundEntityPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundSoundEntityPacket.getId());
        if (entity == null) {
            return;
        }
        this.minecraft.level.playSeededSound(this.minecraft.player, entity, (Holder<SoundEvent>)clientboundSoundEntityPacket.getSound(), clientboundSoundEntityPacket.getSource(), clientboundSoundEntityPacket.getVolume(), clientboundSoundEntityPacket.getPitch(), clientboundSoundEntityPacket.getSeed());
    }

    public void handleResourcePack(ClientboundResourcePackPacket clientboundResourcePackPacket) {
        URL uRL = ClientPacketListener.parseResourcePackUrl(clientboundResourcePackPacket.getUrl());
        if (uRL == null) {
            this.send(ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD);
            return;
        }
        String string = clientboundResourcePackPacket.getHash();
        boolean bl = clientboundResourcePackPacket.isRequired();
        if (this.serverData != null && this.serverData.getResourcePackStatus() == ServerData.ServerPackStatus.ENABLED) {
            this.send(ServerboundResourcePackPacket.Action.ACCEPTED);
            this.downloadCallback(this.minecraft.getDownloadedPackSource().downloadAndSelectResourcePack(uRL, string, true));
        } else if (this.serverData == null || this.serverData.getResourcePackStatus() == ServerData.ServerPackStatus.PROMPT || bl && this.serverData.getResourcePackStatus() == ServerData.ServerPackStatus.DISABLED) {
            this.minecraft.execute(() -> this.minecraft.setScreen(new ConfirmScreen(bl2 -> {
                this.minecraft.setScreen(null);
                if (bl2) {
                    if (this.serverData != null) {
                        this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
                    }
                    this.send(ServerboundResourcePackPacket.Action.ACCEPTED);
                    this.downloadCallback(this.minecraft.getDownloadedPackSource().downloadAndSelectResourcePack(uRL, string, true));
                } else {
                    this.send(ServerboundResourcePackPacket.Action.DECLINED);
                    if (bl) {
                        this.connection.disconnect((Component)Component.translatable((String)"multiplayer.requiredTexturePrompt.disconnect"));
                    } else if (this.serverData != null) {
                        this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
                    }
                }
                if (this.serverData != null) {
                    ServerList.saveSingleServer(this.serverData);
                }
            }, (Component)(bl ? Component.translatable((String)"multiplayer.requiredTexturePrompt.line1") : Component.translatable((String)"multiplayer.texturePrompt.line1")), ClientPacketListener.preparePackPrompt((Component)(bl ? Component.translatable((String)"multiplayer.requiredTexturePrompt.line2").withStyle(new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.BOLD}) : Component.translatable((String)"multiplayer.texturePrompt.line2")), clientboundResourcePackPacket.getPrompt()), bl ? CommonComponents.GUI_PROCEED : CommonComponents.GUI_YES, (Component)(bl ? Component.translatable((String)"menu.disconnect") : CommonComponents.GUI_NO))));
        } else {
            this.send(ServerboundResourcePackPacket.Action.DECLINED);
            if (bl) {
                this.connection.disconnect((Component)Component.translatable((String)"multiplayer.requiredTexturePrompt.disconnect"));
            }
        }
    }

    private static Component preparePackPrompt(Component component, @Nullable Component component2) {
        if (component2 == null) {
            return component;
        }
        return Component.translatable((String)"multiplayer.texturePrompt.serverPrompt", (Object[])new Object[]{component, component2});
    }

    @Nullable
    private static URL parseResourcePackUrl(String string) {
        try {
            URL uRL = new URL(string);
            String string2 = uRL.getProtocol();
            if ("http".equals(string2) || "https".equals(string2)) {
                return uRL;
            }
        } catch (MalformedURLException malformedURLException) {
            return null;
        }
        return null;
    }

    private void downloadCallback(CompletableFuture<?> completableFuture) {
        ((CompletableFuture)completableFuture.thenRun(() -> this.send(ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED))).exceptionally(throwable -> {
            this.send(ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD);
            return null;
        });
    }

    private void send(ServerboundResourcePackPacket.Action action) {
        this.connection.send((Packet)new ServerboundResourcePackPacket(action));
    }

    public void handleBossUpdate(ClientboundBossEventPacket clientboundBossEventPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundBossEventPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.minecraft.gui.getBossOverlay().update(clientboundBossEventPacket);
    }

    public void handleItemCooldown(ClientboundCooldownPacket clientboundCooldownPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundCooldownPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        if (clientboundCooldownPacket.getDuration() == 0) {
            this.minecraft.player.getCooldowns().removeCooldown(clientboundCooldownPacket.getItem());
        } else {
            this.minecraft.player.getCooldowns().addCooldown(clientboundCooldownPacket.getItem(), clientboundCooldownPacket.getDuration());
        }
    }

    public void handleMoveVehicle(ClientboundMoveVehiclePacket clientboundMoveVehiclePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundMoveVehiclePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.minecraft.player.getRootVehicle();
        if (entity != this.minecraft.player && entity.isControlledByLocalInstance()) {
            entity.absMoveTo(clientboundMoveVehiclePacket.getX(), clientboundMoveVehiclePacket.getY(), clientboundMoveVehiclePacket.getZ(), clientboundMoveVehiclePacket.getYRot(), clientboundMoveVehiclePacket.getXRot());
            this.connection.send((Packet)new ServerboundMoveVehiclePacket(entity));
        }
    }

    public void handleOpenBook(ClientboundOpenBookPacket clientboundOpenBookPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundOpenBookPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        ItemStack itemStack = this.minecraft.player.getItemInHand(clientboundOpenBookPacket.getHand());
        if (itemStack.is(Items.WRITTEN_BOOK)) {
            this.minecraft.setScreen(new BookViewScreen(new BookViewScreen.WrittenBookAccess(itemStack)));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void handleCustomPayload(ClientboundCustomPayloadPacket clientboundCustomPayloadPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundCustomPayloadPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        ResourceLocation resourceLocation = clientboundCustomPayloadPacket.getIdentifier();
        FriendlyByteBuf friendlyByteBuf = null;
        try {
            friendlyByteBuf = clientboundCustomPayloadPacket.getData();
            if (ClientboundCustomPayloadPacket.BRAND.equals((Object)resourceLocation)) {
                String string = friendlyByteBuf.readUtf();
                this.minecraft.player.setServerBrand(string);
                this.telemetryManager.onServerBrandReceived(string);
            } else if (ClientboundCustomPayloadPacket.DEBUG_PATHFINDING_PACKET.equals((Object)resourceLocation)) {
                int i = friendlyByteBuf.readInt();
                float f = friendlyByteBuf.readFloat();
                Path path = Path.createFromStream((FriendlyByteBuf)friendlyByteBuf);
                this.minecraft.debugRenderer.pathfindingRenderer.addPath(i, path, f);
            } else if (ClientboundCustomPayloadPacket.DEBUG_NEIGHBORSUPDATE_PACKET.equals((Object)resourceLocation)) {
                long l = friendlyByteBuf.readVarLong();
                BlockPos blockPos = friendlyByteBuf.readBlockPos();
                ((NeighborsUpdateRenderer)this.minecraft.debugRenderer.neighborsUpdateRenderer).addUpdate(l, blockPos);
            } else if (ClientboundCustomPayloadPacket.DEBUG_STRUCTURES_PACKET.equals((Object)resourceLocation)) {
                DimensionType dimensionType = (DimensionType)this.registryAccess.compositeAccess().registryOrThrow(Registries.DIMENSION_TYPE).get(friendlyByteBuf.readResourceLocation());
                BoundingBox boundingBox = new BoundingBox(friendlyByteBuf.readInt(), friendlyByteBuf.readInt(), friendlyByteBuf.readInt(), friendlyByteBuf.readInt(), friendlyByteBuf.readInt(), friendlyByteBuf.readInt());
                int j = friendlyByteBuf.readInt();
                ArrayList<BoundingBox> list = Lists.newArrayList();
                ArrayList<Boolean> list2 = Lists.newArrayList();
                for (int k = 0; k < j; ++k) {
                    list.add(new BoundingBox(friendlyByteBuf.readInt(), friendlyByteBuf.readInt(), friendlyByteBuf.readInt(), friendlyByteBuf.readInt(), friendlyByteBuf.readInt(), friendlyByteBuf.readInt()));
                    list2.add(friendlyByteBuf.readBoolean());
                }
                this.minecraft.debugRenderer.structureRenderer.addBoundingBox(boundingBox, list, list2, dimensionType);
            } else if (ClientboundCustomPayloadPacket.DEBUG_WORLDGENATTEMPT_PACKET.equals((Object)resourceLocation)) {
                ((WorldGenAttemptRenderer)this.minecraft.debugRenderer.worldGenAttemptRenderer).addPos(friendlyByteBuf.readBlockPos(), friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat());
            } else if (ClientboundCustomPayloadPacket.DEBUG_VILLAGE_SECTIONS.equals((Object)resourceLocation)) {
                int m;
                int i = friendlyByteBuf.readInt();
                for (m = 0; m < i; ++m) {
                    this.minecraft.debugRenderer.villageSectionsDebugRenderer.setVillageSection(friendlyByteBuf.readSectionPos());
                }
                m = friendlyByteBuf.readInt();
                for (int j = 0; j < m; ++j) {
                    this.minecraft.debugRenderer.villageSectionsDebugRenderer.setNotVillageSection(friendlyByteBuf.readSectionPos());
                }
            } else if (ClientboundCustomPayloadPacket.DEBUG_POI_ADDED_PACKET.equals((Object)resourceLocation)) {
                BlockPos blockPos2 = friendlyByteBuf.readBlockPos();
                String string2 = friendlyByteBuf.readUtf();
                int j = friendlyByteBuf.readInt();
                BrainDebugRenderer.PoiInfo poiInfo = new BrainDebugRenderer.PoiInfo(blockPos2, string2, j);
                this.minecraft.debugRenderer.brainDebugRenderer.addPoi(poiInfo);
            } else if (ClientboundCustomPayloadPacket.DEBUG_POI_REMOVED_PACKET.equals((Object)resourceLocation)) {
                BlockPos blockPos2 = friendlyByteBuf.readBlockPos();
                this.minecraft.debugRenderer.brainDebugRenderer.removePoi(blockPos2);
            } else if (ClientboundCustomPayloadPacket.DEBUG_POI_TICKET_COUNT_PACKET.equals((Object)resourceLocation)) {
                BlockPos blockPos2 = friendlyByteBuf.readBlockPos();
                int m = friendlyByteBuf.readInt();
                this.minecraft.debugRenderer.brainDebugRenderer.setFreeTicketCount(blockPos2, m);
            } else if (ClientboundCustomPayloadPacket.DEBUG_GOAL_SELECTOR.equals((Object)resourceLocation)) {
                BlockPos blockPos2 = friendlyByteBuf.readBlockPos();
                int m = friendlyByteBuf.readInt();
                int j = friendlyByteBuf.readInt();
                ArrayList<GoalSelectorDebugRenderer.DebugGoal> list = Lists.newArrayList();
                for (int n = 0; n < j; ++n) {
                    int k = friendlyByteBuf.readInt();
                    boolean bl = friendlyByteBuf.readBoolean();
                    String string3 = friendlyByteBuf.readUtf(255);
                    list.add(new GoalSelectorDebugRenderer.DebugGoal(blockPos2, k, string3, bl));
                }
                this.minecraft.debugRenderer.goalSelectorRenderer.addGoalSelector(m, list);
            } else if (ClientboundCustomPayloadPacket.DEBUG_RAIDS.equals((Object)resourceLocation)) {
                int i = friendlyByteBuf.readInt();
                ArrayList<BlockPos> collection = Lists.newArrayList();
                for (int j = 0; j < i; ++j) {
                    collection.add(friendlyByteBuf.readBlockPos());
                }
                this.minecraft.debugRenderer.raidDebugRenderer.setRaidCenters(collection);
            } else if (ClientboundCustomPayloadPacket.DEBUG_BRAIN.equals((Object)resourceLocation)) {
                int x;
                int w;
                int v;
                int u;
                int t;
                double d = friendlyByteBuf.readDouble();
                double e = friendlyByteBuf.readDouble();
                double g = friendlyByteBuf.readDouble();
                PositionImpl position = new PositionImpl(d, e, g);
                UUID uUID = friendlyByteBuf.readUUID();
                int o = friendlyByteBuf.readInt();
                String string4 = friendlyByteBuf.readUtf();
                String string5 = friendlyByteBuf.readUtf();
                int p = friendlyByteBuf.readInt();
                float h = friendlyByteBuf.readFloat();
                float q = friendlyByteBuf.readFloat();
                String string6 = friendlyByteBuf.readUtf();
                Path path2 = (Path)friendlyByteBuf.readNullable(Path::createFromStream);
                boolean bl2 = friendlyByteBuf.readBoolean();
                int r = friendlyByteBuf.readInt();
                BrainDebugRenderer.BrainDump brainDump = new BrainDebugRenderer.BrainDump(uUID, o, string4, string5, p, h, q, (Position)position, string6, path2, bl2, r);
                int s = friendlyByteBuf.readVarInt();
                for (t = 0; t < s; ++t) {
                    String string7 = friendlyByteBuf.readUtf();
                    brainDump.activities.add(string7);
                }
                t = friendlyByteBuf.readVarInt();
                for (u = 0; u < t; ++u) {
                    String string8 = friendlyByteBuf.readUtf();
                    brainDump.behaviors.add(string8);
                }
                u = friendlyByteBuf.readVarInt();
                for (v = 0; v < u; ++v) {
                    String string9 = friendlyByteBuf.readUtf();
                    brainDump.memories.add(string9);
                }
                v = friendlyByteBuf.readVarInt();
                for (w = 0; w < v; ++w) {
                    BlockPos blockPos3 = friendlyByteBuf.readBlockPos();
                    brainDump.pois.add(blockPos3);
                }
                w = friendlyByteBuf.readVarInt();
                for (x = 0; x < w; ++x) {
                    BlockPos blockPos4 = friendlyByteBuf.readBlockPos();
                    brainDump.potentialPois.add(blockPos4);
                }
                x = friendlyByteBuf.readVarInt();
                for (int y = 0; y < x; ++y) {
                    String string10 = friendlyByteBuf.readUtf();
                    brainDump.gossips.add(string10);
                }
                this.minecraft.debugRenderer.brainDebugRenderer.addOrUpdateBrainDump(brainDump);
            } else if (ClientboundCustomPayloadPacket.DEBUG_BEE.equals((Object)resourceLocation)) {
                int aa;
                double d = friendlyByteBuf.readDouble();
                double e = friendlyByteBuf.readDouble();
                double g = friendlyByteBuf.readDouble();
                PositionImpl position = new PositionImpl(d, e, g);
                UUID uUID = friendlyByteBuf.readUUID();
                int o = friendlyByteBuf.readInt();
                BlockPos blockPos5 = (BlockPos)friendlyByteBuf.readNullable(FriendlyByteBuf::readBlockPos);
                BlockPos blockPos6 = (BlockPos)friendlyByteBuf.readNullable(FriendlyByteBuf::readBlockPos);
                int p = friendlyByteBuf.readInt();
                Path path3 = (Path)friendlyByteBuf.readNullable(Path::createFromStream);
                BeeDebugRenderer.BeeInfo beeInfo = new BeeDebugRenderer.BeeInfo(uUID, o, (Position)position, path3, blockPos5, blockPos6, p);
                int z = friendlyByteBuf.readVarInt();
                for (aa = 0; aa < z; ++aa) {
                    String string11 = friendlyByteBuf.readUtf();
                    beeInfo.goals.add(string11);
                }
                aa = friendlyByteBuf.readVarInt();
                for (int ab = 0; ab < aa; ++ab) {
                    BlockPos blockPos7 = friendlyByteBuf.readBlockPos();
                    beeInfo.blacklistedHives.add(blockPos7);
                }
                this.minecraft.debugRenderer.beeDebugRenderer.addOrUpdateBeeInfo(beeInfo);
            } else if (ClientboundCustomPayloadPacket.DEBUG_HIVE.equals((Object)resourceLocation)) {
                BlockPos blockPos2 = friendlyByteBuf.readBlockPos();
                String string2 = friendlyByteBuf.readUtf();
                int j = friendlyByteBuf.readInt();
                int ac = friendlyByteBuf.readInt();
                boolean bl3 = friendlyByteBuf.readBoolean();
                BeeDebugRenderer.HiveInfo hiveInfo = new BeeDebugRenderer.HiveInfo(blockPos2, string2, j, ac, bl3, this.level.getGameTime());
                this.minecraft.debugRenderer.beeDebugRenderer.addOrUpdateHiveInfo(hiveInfo);
            } else if (ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_CLEAR.equals((Object)resourceLocation)) {
                this.minecraft.debugRenderer.gameTestDebugRenderer.clear();
            } else if (ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_ADD_MARKER.equals((Object)resourceLocation)) {
                BlockPos blockPos2 = friendlyByteBuf.readBlockPos();
                int m = friendlyByteBuf.readInt();
                String string12 = friendlyByteBuf.readUtf();
                int ac = friendlyByteBuf.readInt();
                this.minecraft.debugRenderer.gameTestDebugRenderer.addMarker(blockPos2, m, string12, ac);
            } else if (ClientboundCustomPayloadPacket.DEBUG_GAME_EVENT.equals((Object)resourceLocation)) {
                GameEvent gameEvent = (GameEvent)BuiltInRegistries.GAME_EVENT.get(new ResourceLocation(friendlyByteBuf.readUtf()));
                Vec3 vec3 = new Vec3(friendlyByteBuf.readDouble(), friendlyByteBuf.readDouble(), friendlyByteBuf.readDouble());
                this.minecraft.debugRenderer.gameEventListenerRenderer.trackGameEvent(gameEvent, vec3);
            } else if (ClientboundCustomPayloadPacket.DEBUG_GAME_EVENT_LISTENER.equals((Object)resourceLocation)) {
                ResourceLocation resourceLocation2 = friendlyByteBuf.readResourceLocation();
                PositionSource positionSource = ((PositionSourceType)BuiltInRegistries.POSITION_SOURCE_TYPE.getOptional(resourceLocation2).orElseThrow(() -> new IllegalArgumentException("Unknown position source type " + resourceLocation2))).read(friendlyByteBuf);
                int j = friendlyByteBuf.readVarInt();
                this.minecraft.debugRenderer.gameEventListenerRenderer.trackListener(positionSource, j);
            } else {
                LOGGER.warn("Unknown custom packed identifier: {}", (Object)resourceLocation);
            }
        } finally {
            if (friendlyByteBuf != null) {
                friendlyByteBuf.release();
            }
        }
    }

    public void handleAddObjective(ClientboundSetObjectivePacket clientboundSetObjectivePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetObjectivePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Scoreboard scoreboard = this.level.getScoreboard();
        String string = clientboundSetObjectivePacket.getObjectiveName();
        if (clientboundSetObjectivePacket.getMethod() == 0) {
            scoreboard.addObjective(string, ObjectiveCriteria.DUMMY, clientboundSetObjectivePacket.getDisplayName(), clientboundSetObjectivePacket.getRenderType());
        } else if (scoreboard.hasObjective(string)) {
            Objective objective = scoreboard.getObjective(string);
            if (clientboundSetObjectivePacket.getMethod() == 1) {
                scoreboard.removeObjective(objective);
            } else if (clientboundSetObjectivePacket.getMethod() == 2) {
                objective.setRenderType(clientboundSetObjectivePacket.getRenderType());
                objective.setDisplayName(clientboundSetObjectivePacket.getDisplayName());
            }
        }
    }

    public void handleSetScore(ClientboundSetScorePacket clientboundSetScorePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetScorePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Scoreboard scoreboard = this.level.getScoreboard();
        String string = clientboundSetScorePacket.getObjectiveName();
        switch (clientboundSetScorePacket.getMethod()) {
            case CHANGE: {
                Objective objective = scoreboard.getOrCreateObjective(string);
                Score score = scoreboard.getOrCreatePlayerScore(clientboundSetScorePacket.getOwner(), objective);
                score.setScore(clientboundSetScorePacket.getScore());
                break;
            }
            case REMOVE: {
                scoreboard.resetPlayerScore(clientboundSetScorePacket.getOwner(), scoreboard.getObjective(string));
            }
        }
    }

    public void handleSetDisplayObjective(ClientboundSetDisplayObjectivePacket clientboundSetDisplayObjectivePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetDisplayObjectivePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Scoreboard scoreboard = this.level.getScoreboard();
        String string = clientboundSetDisplayObjectivePacket.getObjectiveName();
        Objective objective = string == null ? null : scoreboard.getOrCreateObjective(string);
        scoreboard.setDisplayObjective(clientboundSetDisplayObjectivePacket.getSlot(), objective);
    }

    public void handleSetPlayerTeamPacket(ClientboundSetPlayerTeamPacket clientboundSetPlayerTeamPacket) {
        PlayerTeam playerTeam;
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetPlayerTeamPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Scoreboard scoreboard = this.level.getScoreboard();
        ClientboundSetPlayerTeamPacket.Action action = clientboundSetPlayerTeamPacket.getTeamAction();
        if (action == ClientboundSetPlayerTeamPacket.Action.ADD) {
            playerTeam = scoreboard.addPlayerTeam(clientboundSetPlayerTeamPacket.getName());
        } else {
            playerTeam = scoreboard.getPlayerTeam(clientboundSetPlayerTeamPacket.getName());
            if (playerTeam == null) {
                LOGGER.warn("Received packet for unknown team {}: team action: {}, player action: {}", clientboundSetPlayerTeamPacket.getName(), clientboundSetPlayerTeamPacket.getTeamAction(), clientboundSetPlayerTeamPacket.getPlayerAction());
                return;
            }
        }
        Optional optional = clientboundSetPlayerTeamPacket.getParameters();
        optional.ifPresent(parameters -> {
            Team.CollisionRule collisionRule;
            playerTeam.setDisplayName(parameters.getDisplayName());
            playerTeam.setColor(parameters.getColor());
            playerTeam.unpackOptions(parameters.getOptions());
            Team.Visibility visibility = Team.Visibility.byName((String)parameters.getNametagVisibility());
            if (visibility != null) {
                playerTeam.setNameTagVisibility(visibility);
            }
            if ((collisionRule = Team.CollisionRule.byName((String)parameters.getCollisionRule())) != null) {
                playerTeam.setCollisionRule(collisionRule);
            }
            playerTeam.setPlayerPrefix(parameters.getPlayerPrefix());
            playerTeam.setPlayerSuffix(parameters.getPlayerSuffix());
        });
        ClientboundSetPlayerTeamPacket.Action action2 = clientboundSetPlayerTeamPacket.getPlayerAction();
        if (action2 == ClientboundSetPlayerTeamPacket.Action.ADD) {
            for (String string : clientboundSetPlayerTeamPacket.getPlayers()) {
                scoreboard.addPlayerToTeam(string, playerTeam);
            }
        } else if (action2 == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
            for (String string : clientboundSetPlayerTeamPacket.getPlayers()) {
                scoreboard.removePlayerFromTeam(string, playerTeam);
            }
        }
        if (action == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
            scoreboard.removePlayerTeam(playerTeam);
        }
    }

    public void handleParticleEvent(ClientboundLevelParticlesPacket clientboundLevelParticlesPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundLevelParticlesPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        if (clientboundLevelParticlesPacket.getCount() == 0) {
            double d = clientboundLevelParticlesPacket.getMaxSpeed() * clientboundLevelParticlesPacket.getXDist();
            double e = clientboundLevelParticlesPacket.getMaxSpeed() * clientboundLevelParticlesPacket.getYDist();
            double f = clientboundLevelParticlesPacket.getMaxSpeed() * clientboundLevelParticlesPacket.getZDist();
            try {
                this.level.addParticle(clientboundLevelParticlesPacket.getParticle(), clientboundLevelParticlesPacket.isOverrideLimiter(), clientboundLevelParticlesPacket.getX(), clientboundLevelParticlesPacket.getY(), clientboundLevelParticlesPacket.getZ(), d, e, f);
            } catch (Throwable throwable) {
                LOGGER.warn("Could not spawn particle effect {}", (Object)clientboundLevelParticlesPacket.getParticle());
            }
        } else {
            for (int i = 0; i < clientboundLevelParticlesPacket.getCount(); ++i) {
                double g = this.random.nextGaussian() * (double)clientboundLevelParticlesPacket.getXDist();
                double h = this.random.nextGaussian() * (double)clientboundLevelParticlesPacket.getYDist();
                double j = this.random.nextGaussian() * (double)clientboundLevelParticlesPacket.getZDist();
                double k = this.random.nextGaussian() * (double)clientboundLevelParticlesPacket.getMaxSpeed();
                double l = this.random.nextGaussian() * (double)clientboundLevelParticlesPacket.getMaxSpeed();
                double m = this.random.nextGaussian() * (double)clientboundLevelParticlesPacket.getMaxSpeed();
                try {
                    this.level.addParticle(clientboundLevelParticlesPacket.getParticle(), clientboundLevelParticlesPacket.isOverrideLimiter(), clientboundLevelParticlesPacket.getX() + g, clientboundLevelParticlesPacket.getY() + h, clientboundLevelParticlesPacket.getZ() + j, k, l, m);
                    continue;
                } catch (Throwable throwable2) {
                    LOGGER.warn("Could not spawn particle effect {}", (Object)clientboundLevelParticlesPacket.getParticle());
                    return;
                }
            }
        }
    }

    public void handlePing(ClientboundPingPacket clientboundPingPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundPingPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.send((Packet<?>)new ServerboundPongPacket(clientboundPingPacket.getId()));
    }

    public void handleUpdateAttributes(ClientboundUpdateAttributesPacket clientboundUpdateAttributesPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundUpdateAttributesPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        Entity entity = this.level.getEntity(clientboundUpdateAttributesPacket.getEntityId());
        if (entity == null) {
            return;
        }
        if (!(entity instanceof LivingEntity)) {
            throw new IllegalStateException("Server tried to update attributes of a non-living entity (actually: " + entity + ")");
        }
        AttributeMap attributeMap = ((LivingEntity)entity).getAttributes();
        for (ClientboundUpdateAttributesPacket.AttributeSnapshot attributeSnapshot : clientboundUpdateAttributesPacket.getValues()) {
            AttributeInstance attributeInstance = attributeMap.getInstance(attributeSnapshot.getAttribute());
            if (attributeInstance == null) {
                LOGGER.warn("Entity {} does not have attribute {}", (Object)entity, (Object)BuiltInRegistries.ATTRIBUTE.getKey((Object)attributeSnapshot.getAttribute()));
                continue;
            }
            attributeInstance.setBaseValue(attributeSnapshot.getBase());
            attributeInstance.removeModifiers();
            for (AttributeModifier attributeModifier : attributeSnapshot.getModifiers()) {
                attributeInstance.addTransientModifier(attributeModifier);
            }
        }
    }

    public void handlePlaceRecipe(ClientboundPlaceGhostRecipePacket clientboundPlaceGhostRecipePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundPlaceGhostRecipePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        AbstractContainerMenu abstractContainerMenu = this.minecraft.player.containerMenu;
        if (abstractContainerMenu.containerId != clientboundPlaceGhostRecipePacket.getContainerId()) {
            return;
        }
        this.recipeManager.byKey(clientboundPlaceGhostRecipePacket.getRecipe()).ifPresent(recipe -> {
            if (this.minecraft.screen instanceof RecipeUpdateListener) {
                RecipeBookComponent recipeBookComponent = ((RecipeUpdateListener)((Object)this.minecraft.screen)).getRecipeBookComponent();
                recipeBookComponent.setupGhostRecipe((Recipe<?>)recipe, (List<Slot>)abstractContainerMenu.slots);
            }
        });
    }

    public void handleLightUpdatePacket(ClientboundLightUpdatePacket clientboundLightUpdatePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundLightUpdatePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        int i = clientboundLightUpdatePacket.getX();
        int j = clientboundLightUpdatePacket.getZ();
        ClientboundLightUpdatePacketData clientboundLightUpdatePacketData = clientboundLightUpdatePacket.getLightData();
        this.level.queueLightUpdate(() -> this.applyLightData(i, j, clientboundLightUpdatePacketData));
    }

    private void applyLightData(int i, int j, ClientboundLightUpdatePacketData clientboundLightUpdatePacketData) {
        LevelLightEngine levelLightEngine = this.level.getChunkSource().getLightEngine();
        BitSet bitSet = clientboundLightUpdatePacketData.getSkyYMask();
        BitSet bitSet2 = clientboundLightUpdatePacketData.getEmptySkyYMask();
        Iterator<byte[]> iterator = clientboundLightUpdatePacketData.getSkyUpdates().iterator();
        this.readSectionList(i, j, levelLightEngine, LightLayer.SKY, bitSet, bitSet2, iterator);
        BitSet bitSet3 = clientboundLightUpdatePacketData.getBlockYMask();
        BitSet bitSet4 = clientboundLightUpdatePacketData.getEmptyBlockYMask();
        Iterator<byte[]> iterator2 = clientboundLightUpdatePacketData.getBlockUpdates().iterator();
        this.readSectionList(i, j, levelLightEngine, LightLayer.BLOCK, bitSet3, bitSet4, iterator2);
        levelLightEngine.setLightEnabled(new ChunkPos(i, j), true);
    }

    public void handleMerchantOffers(ClientboundMerchantOffersPacket clientboundMerchantOffersPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundMerchantOffersPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        AbstractContainerMenu abstractContainerMenu = this.minecraft.player.containerMenu;
        if (clientboundMerchantOffersPacket.getContainerId() == abstractContainerMenu.containerId && abstractContainerMenu instanceof MerchantMenu) {
            MerchantMenu merchantMenu = (MerchantMenu)abstractContainerMenu;
            merchantMenu.setOffers(new MerchantOffers(clientboundMerchantOffersPacket.getOffers().createTag()));
            merchantMenu.setXp(clientboundMerchantOffersPacket.getVillagerXp());
            merchantMenu.setMerchantLevel(clientboundMerchantOffersPacket.getVillagerLevel());
            merchantMenu.setShowProgressBar(clientboundMerchantOffersPacket.showProgress());
            merchantMenu.setCanRestock(clientboundMerchantOffersPacket.canRestock());
        }
    }

    public void handleSetChunkCacheRadius(ClientboundSetChunkCacheRadiusPacket clientboundSetChunkCacheRadiusPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetChunkCacheRadiusPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.serverChunkRadius = clientboundSetChunkCacheRadiusPacket.getRadius();
        this.minecraft.options.setServerRenderDistance(this.serverChunkRadius);
        this.level.getChunkSource().updateViewRadius(clientboundSetChunkCacheRadiusPacket.getRadius());
    }

    public void handleSetSimulationDistance(ClientboundSetSimulationDistancePacket clientboundSetSimulationDistancePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetSimulationDistancePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.serverSimulationDistance = clientboundSetSimulationDistancePacket.simulationDistance();
        this.level.setServerSimulationDistance(this.serverSimulationDistance);
    }

    public void handleSetChunkCacheCenter(ClientboundSetChunkCacheCenterPacket clientboundSetChunkCacheCenterPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundSetChunkCacheCenterPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.level.getChunkSource().updateViewCenter(clientboundSetChunkCacheCenterPacket.getX(), clientboundSetChunkCacheCenterPacket.getZ());
    }

    public void handleBlockChangedAck(ClientboundBlockChangedAckPacket clientboundBlockChangedAckPacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundBlockChangedAckPacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        this.level.handleBlockChangedAck(clientboundBlockChangedAckPacket.sequence());
    }

    public void handleBundlePacket(ClientboundBundlePacket clientboundBundlePacket) {
        PacketUtils.ensureRunningOnSameThread((Packet)clientboundBundlePacket, (PacketListener)this, (BlockableEventLoop)this.minecraft);
        for (Packet packet : clientboundBundlePacket.subPackets()) {
            packet.handle((PacketListener)this);
        }
    }

    private void readSectionList(int i, int j, LevelLightEngine levelLightEngine, LightLayer lightLayer, BitSet bitSet, BitSet bitSet2, Iterator<byte[]> iterator) {
        for (int k = 0; k < levelLightEngine.getLightSectionCount(); ++k) {
            int l = levelLightEngine.getMinLightSection() + k;
            boolean bl = bitSet.get(k);
            boolean bl2 = bitSet2.get(k);
            if (!bl && !bl2) continue;
            levelLightEngine.queueSectionData(lightLayer, SectionPos.of((int)i, (int)l, (int)j), bl ? new DataLayer((byte[])iterator.next().clone()) : new DataLayer());
            this.level.setSectionDirtyWithNeighbors(i, l, j);
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }

    public Collection<PlayerInfo> getListedOnlinePlayers() {
        return this.listedPlayers;
    }

    public Collection<PlayerInfo> getOnlinePlayers() {
        return this.playerInfoMap.values();
    }

    public Collection<UUID> getOnlinePlayerIds() {
        return this.playerInfoMap.keySet();
    }

    @Nullable
    public PlayerInfo getPlayerInfo(UUID uUID) {
        return this.playerInfoMap.get(uUID);
    }

    @Nullable
    public PlayerInfo getPlayerInfo(String string) {
        for (PlayerInfo playerInfo : this.playerInfoMap.values()) {
            if (!playerInfo.getProfile().getName().equals(string)) continue;
            return playerInfo;
        }
        return null;
    }

    public GameProfile getLocalGameProfile() {
        return this.localGameProfile;
    }

    public ClientAdvancements getAdvancements() {
        return this.advancements;
    }

    public CommandDispatcher<SharedSuggestionProvider> getCommands() {
        return this.commands;
    }

    public ClientLevel getLevel() {
        return this.level;
    }

    public DebugQueryHandler getDebugQueryHandler() {
        return this.debugQueryHandler;
    }

    public UUID getId() {
        return this.id;
    }

    public Set<ResourceKey<Level>> levels() {
        return this.levels;
    }

    public RegistryAccess registryAccess() {
        return this.registryAccess.compositeAccess();
    }

    public void markMessageAsProcessed(PlayerChatMessage playerChatMessage, boolean bl) {
        MessageSignature messageSignature = playerChatMessage.signature();
        if (messageSignature != null && this.lastSeenMessages.addPending(messageSignature, bl) && this.lastSeenMessages.offset() > 64) {
            this.sendChatAcknowledgement();
        }
    }

    private void sendChatAcknowledgement() {
        int i = this.lastSeenMessages.getAndClearOffset();
        if (i > 0) {
            this.send((Packet<?>)new ServerboundChatAckPacket(i));
        }
    }

    public void sendChat(String string) {
        Instant instant = Instant.now();
        long l = Crypt.SaltSupplier.getLong();
        LastSeenMessagesTracker.Update update = this.lastSeenMessages.generateAndApplyUpdate();
        MessageSignature messageSignature = this.signedMessageEncoder.pack(new SignedMessageBody(string, instant, l, update.lastSeen()));
        this.send((Packet<?>)new ServerboundChatPacket(string, instant, l, messageSignature, update.update()));
    }

    public void sendCommand(String string2) {
        Instant instant = Instant.now();
        long l = Crypt.SaltSupplier.getLong();
        LastSeenMessagesTracker.Update update = this.lastSeenMessages.generateAndApplyUpdate();
        ArgumentSignatures argumentSignatures = ArgumentSignatures.signCommand((SignableCommand)SignableCommand.of(this.parseCommand(string2)), string -> {
            SignedMessageBody signedMessageBody = new SignedMessageBody(string, instant, l, update.lastSeen());
            return this.signedMessageEncoder.pack(signedMessageBody);
        });
        this.send((Packet<?>)new ServerboundChatCommandPacket(string2, instant, l, argumentSignatures, update.update()));
    }

    public boolean sendUnsignedCommand(String string) {
        if (SignableCommand.of(this.parseCommand(string)).arguments().isEmpty()) {
            LastSeenMessagesTracker.Update update = this.lastSeenMessages.generateAndApplyUpdate();
            this.send((Packet<?>)new ServerboundChatCommandPacket(string, Instant.now(), 0L, ArgumentSignatures.EMPTY, update.update()));
            return true;
        }
        return false;
    }

    private ParseResults<SharedSuggestionProvider> parseCommand(String string) {
        return this.commands.parse(string, (SharedSuggestionProvider)this.suggestionsProvider);
    }

    public void tick() {
        ProfileKeyPairManager profileKeyPairManager;
        if (this.connection.isEncrypted() && (profileKeyPairManager = this.minecraft.getProfileKeyPairManager()).shouldRefreshKeyPair()) {
            profileKeyPairManager.prepareKeyPair().thenAcceptAsync(optional -> optional.ifPresent(this::setKeyPair), (Executor)((Object)this.minecraft));
        }
        this.sendDeferredPackets();
        this.telemetryManager.tick();
    }

    public void setKeyPair(ProfileKeyPair profileKeyPair) {
        if (!this.localGameProfile.getId().equals(this.minecraft.getUser().getProfileId())) {
            return;
        }
        if (this.chatSession != null && this.chatSession.keyPair().equals((Object)profileKeyPair)) {
            return;
        }
        this.chatSession = LocalChatSession.create((ProfileKeyPair)profileKeyPair);
        this.signedMessageEncoder = this.chatSession.createMessageEncoder(this.localGameProfile.getId());
        this.send((Packet<?>)new ServerboundChatSessionUpdatePacket(this.chatSession.asRemote().asData()));
    }

    @Nullable
    public ServerData getServerData() {
        return this.serverData;
    }

    public FeatureFlagSet enabledFeatures() {
        return this.enabledFeatures;
    }

    public boolean isFeatureEnabled(FeatureFlagSet featureFlagSet) {
        return featureFlagSet.isSubsetOf(this.enabledFeatures());
    }

    @Environment(value=EnvType.CLIENT)
    record DeferredPacket(Packet<ServerGamePacketListener> packet, BooleanSupplier sendCondition, long expirationTime) {
    }
}

