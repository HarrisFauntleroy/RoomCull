package com.harrisfauntleroy.roomcull;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Main mod class for the Room Culling mod.
 *
 * <p>This mod implements room-based occlusion culling to improve performance in indoor
 * environments. When a player is inside a room (defined by placing a Room Block), objects outside
 * that room are automatically culled from rendering, reducing the visual complexity and improving
 * frame rates.
 *
 * <p>The mod adds:
 *
 * <ul>
 *   <li>{@link RoomBlock} - A functional block that detects room boundaries
 *   <li>{@link RoomBlockEntity} - Handles room scanning and boundary detection
 *   <li>{@link RoomManager} - Manages occlusion culling across all rooms
 *   <li>Mixin-based integration with Minecraft's rendering system
 * </ul>
 *
 * @author Harris Fauntleroy
 * @since 1.0.0
 */
// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RoomCullMod.MODID)
public class RoomCullMod {
    /** The unique identifier for this mod. */
    public static final String MODID = "roomcull";

    /** Logger instance for this mod. */
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Registry for blocks added by this mod. */
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);

    /** Registry for items added by this mod. */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    /** Registry for creative mode tabs added by this mod. */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** Registry for block entity types added by this mod. */
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    /** The room block that detects boundaries and enables occlusion culling. */
    public static final DeferredBlock<Block> ROOM_BLOCK =
            BLOCKS.register("room_block", RoomBlock::new);

    /** Item form of the room block for placement. */
    public static final DeferredItem<BlockItem> ROOM_BLOCK_ITEM =
            ITEMS.register(
                    "room_block", () -> new BlockItem(ROOM_BLOCK.get(), new Item.Properties()));

    /** Block entity type for room blocks that handles boundary detection. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RoomBlockEntity>>
            ROOM_BLOCK_ENTITY =
                    BLOCK_ENTITY_TYPES.register(
                            "room_block_entity",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    RoomBlockEntity::new, ROOM_BLOCK.get())
                                            .build(null));

    /** Creative tab for room culling items. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ROOM_CULL_TAB =
            CREATIVE_MODE_TABS.register(
                    "room_cull_tab",
                    () ->
                            CreativeModeTab.builder()
                                    .title(Component.translatable("itemGroup.roomcull"))
                                    .withTabsBefore(CreativeModeTabs.REDSTONE_BLOCKS)
                                    .icon(() -> ROOM_BLOCK_ITEM.get().getDefaultInstance())
                                    .displayItems(
                                            (parameters, output) -> {
                                                output.accept(ROOM_BLOCK_ITEM.get());
                                            })
                                    .build());

    /**
     * Main constructor for the Room Culling mod.
     *
     * <p>This constructor is called by NeoForge during mod loading and handles:
     *
     * <ul>
     *   <li>Registering deferred registries for blocks, items, and block entities
     *   <li>Setting up event listeners for mod lifecycle events
     *   <li>Configuring mod configuration
     * </ul>
     *
     * @param modEventBus the mod-specific event bus for registration events
     * @param modContainer the container holding this mod's metadata
     */
    public RoomCullMod(final IEventBus modEventBus, final ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so block entities get registered
        BLOCK_ENTITY_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond
        // directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class,
        // like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Room culling setup
        LOGGER.info("Room Culling mod initialized");
    }

    // Add the room block to the redstone blocks tab
    private void addCreative(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(ROOM_BLOCK_ITEM);
        }
    }

    /**
     * Called when the server is starting up.
     *
     * @param event the server starting event
     */
    @SubscribeEvent
    public void onServerStarting(final ServerStartingEvent event) {
        // Room culling server startup
        LOGGER.info("Room Culling server started");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class
    // annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        /**
         * Called when the client is being set up.
         *
         * @param event the client setup event
         */
        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event) {
            // Room culling client setup
            LOGGER.info("Room Culling client initialized");

            // Register the room block entity renderer
            event.enqueueWork(
                    () -> {
                        BlockEntityRenderers.register(
                                ROOM_BLOCK_ENTITY.get(), RoomBlockEntityRenderer::new);
                    });
        }
    }
}
