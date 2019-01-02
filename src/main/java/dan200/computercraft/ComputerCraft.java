/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaAPIFactory;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.media.IMediaProvider;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import dan200.computercraft.api.permissions.ITurtlePermissionProvider;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.redstone.IBundledRedstoneProvider;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.core.apis.AddressPredicate;
import dan200.computercraft.core.apis.ApiFactories;
import dan200.computercraft.core.filesystem.ComboMount;
import dan200.computercraft.core.filesystem.FileMount;
import dan200.computercraft.core.filesystem.FileSystemMount;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.shared.*;
import dan200.computercraft.shared.computer.blocks.BlockCommandComputer;
import dan200.computercraft.shared.computer.blocks.BlockComputer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ClientComputerRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerComputerRegistry;
import dan200.computercraft.shared.media.items.ItemDiskExpanded;
import dan200.computercraft.shared.media.items.ItemDiskLegacy;
import dan200.computercraft.shared.media.items.ItemPrintout;
import dan200.computercraft.shared.media.items.ItemTreasureDisk;
import dan200.computercraft.shared.peripheral.common.BlockPeripheral;
import dan200.computercraft.shared.peripheral.diskdrive.TileDiskDrive;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.BlockWiredModemFull;
import dan200.computercraft.shared.peripheral.modem.wireless.BlockAdvancedModem;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.peripherals.PocketModem;
import dan200.computercraft.shared.pocket.peripherals.PocketSpeaker;
import dan200.computercraft.shared.proxy.ICCTurtleProxy;
import dan200.computercraft.shared.proxy.IComputerCraftProxy;
import dan200.computercraft.shared.turtle.blocks.BlockTurtle;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.upgrades.*;
import dan200.computercraft.shared.util.CreativeTabMain;
import dan200.computercraft.shared.util.IDAssigner;
import dan200.computercraft.shared.wired.CapabilityWiredElement;
import dan200.computercraft.shared.wired.WiredNode;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mod(
    modid = ComputerCraft.MOD_ID, name = "CC: Tweaked", version = "${version}",
    guiFactory = "dan200.computercraft.client.gui.GuiConfigCC$Factory",
    dependencies = "required:forge@[14.23.4.2746,)"
)
public class ComputerCraft
{
    public static final String MOD_ID = "computercraft";

    // GUI IDs
    public static final int diskDriveGUIID = 100;
    public static final int computerGUIID = 101;
    public static final int printerGUIID = 102;
    public static final int turtleGUIID = 103;
    // ComputerCraftEdu uses ID 104
    public static final int printoutGUIID = 105;
    public static final int pocketComputerGUIID = 106;
    public static final int viewComputerGUIID = 110;

    // Configuration options
    private static final String[] DEFAULT_HTTP_WHITELIST = new String[] { "*" };
    private static final String[] DEFAULT_HTTP_BLACKLIST = new String[] {
        "127.0.0.0/8",
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "fd00::/8",
    };

    public static boolean http_enable = true;
    public static boolean http_websocket_enable = true;
    public static AddressPredicate http_whitelist = new AddressPredicate( DEFAULT_HTTP_WHITELIST );
    public static AddressPredicate http_blacklist = new AddressPredicate( DEFAULT_HTTP_BLACKLIST );
    public static boolean disable_lua51_features = false;
    public static String default_computer_settings = "";
    public static boolean debug_enable = false;
    public static int computer_threads = 1;
    public static boolean logPeripheralErrors = false;

    public static boolean enableCommandBlock = false;
    public static boolean turtlesNeedFuel = true;
    public static int turtleFuelLimit = 20000;
    public static int advancedTurtleFuelLimit = 100000;
    public static boolean turtlesObeyBlockProtection = true;
    public static boolean turtlesCanPush = true;
    public static EnumSet<TurtleAction> turtleDisabledActions = EnumSet.noneOf( TurtleAction.class );

    public static final int terminalWidth_computer = 51;
    public static final int terminalHeight_computer = 19;

    public static final int terminalWidth_turtle = 39;
    public static final int terminalHeight_turtle = 13;

    public static final int terminalWidth_pocketComputer = 26;
    public static final int terminalHeight_pocketComputer = 20;

    public static int modem_range = 64;
    public static int modem_highAltitudeRange = 384;
    public static int modem_rangeDuringStorm = 64;
    public static int modem_highAltitudeRangeDuringStorm = 384;

    public static int computerSpaceLimit = 1000 * 1000;
    public static int floppySpaceLimit = 125 * 1000;
    public static int maximumFilesOpen = 128;

    public static int maxNotesPerTick = 8;

    // Blocks and Items
    public static class Blocks
    {
        public static BlockComputer computer;
        public static BlockPeripheral peripheral;
        public static BlockCable cable;
        public static BlockTurtle turtle;
        public static BlockTurtle turtleExpanded;
        public static BlockTurtle turtleAdvanced;
        public static BlockCommandComputer commandComputer;
        public static BlockAdvancedModem advancedModem;
        public static BlockWiredModemFull wiredModemFull;
    }

    public static class Items
    {
        public static ItemDiskLegacy disk;
        public static ItemDiskExpanded diskExpanded;
        public static ItemPrintout printout;
        public static ItemTreasureDisk treasureDisk;
        public static ItemPocketComputer pocketComputer;
    }

    public static class Upgrades
    {
        public static TurtleModem wirelessModem;
        public static TurtleCraftingTable craftingTable;
        public static TurtleSword diamondSword;
        public static TurtleShovel diamondShovel;
        public static TurtleTool diamondPickaxe;
        public static TurtleAxe diamondAxe;
        public static TurtleHoe diamondHoe;
        public static TurtleModem advancedModem;
        public static TurtleSpeaker turtleSpeaker;
    }

    public static class PocketUpgrades
    {
        public static PocketModem wirelessModem;
        public static PocketModem advancedModem;
        public static PocketSpeaker pocketSpeaker;
    }

    public static class Config
    {
        public static Configuration config;

        public static Property http_enable;
        public static Property http_websocket_enable;
        public static Property http_whitelist;
        public static Property http_blacklist;
        public static Property disable_lua51_features;
        public static Property default_computer_settings;
        public static Property debug_enable;
        public static Property computer_threads;
        public static Property logPeripheralErrors;

        public static Property enableCommandBlock;
        public static Property turtlesNeedFuel;
        public static Property turtleFuelLimit;
        public static Property advancedTurtleFuelLimit;
        public static Property turtlesObeyBlockProtection;
        public static Property turtlesCanPush;
        public static Property turtleDisabledActions;

        public static Property modem_range;
        public static Property modem_highAltitudeRange;
        public static Property modem_rangeDuringStorm;
        public static Property modem_highAltitudeRangeDuringStorm;

        public static Property computerSpaceLimit;
        public static Property floppySpaceLimit;
        public static Property maximumFilesOpen;
        public static Property maxNotesPerTick;

    }

    // Registries
    public static ClientComputerRegistry clientComputerRegistry = new ClientComputerRegistry();
    public static ServerComputerRegistry serverComputerRegistry = new ServerComputerRegistry();

    // Networking
    public static SimpleNetworkWrapper networkWrapper;

    // Creative
    public static CreativeTabMain mainCreativeTab;

    // Logging
    public static Logger log;

    // Peripheral providers. This is still here to ensure compatibility with Plethora and Computronics
    public static List<IPeripheralProvider> peripheralProviders = new ArrayList<>();

    // Implementation
    @Mod.Instance( value = ComputerCraft.MOD_ID )
    public static ComputerCraft instance;

    @SidedProxy( clientSide = "dan200.computercraft.client.proxy.ComputerCraftProxyClient", serverSide = "dan200.computercraft.server.proxy.ComputerCraftProxyServer" )
    public static IComputerCraftProxy proxy;

    @SidedProxy( clientSide = "dan200.computercraft.client.proxy.CCTurtleProxyClient", serverSide = "dan200.computercraft.server.proxy.CCTurtleProxyServer" )
    public static ICCTurtleProxy turtleProxy;

    @Mod.EventHandler
    public void preInit( FMLPreInitializationEvent event )
    {
        log = event.getModLog();

        // Load config
        Config.config = new Configuration( event.getSuggestedConfigurationFile() );
        loadConfig();

        // Setup network
        networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel( ComputerCraft.MOD_ID );

        proxy.preInit();
        turtleProxy.preInit();
    }

    public static void loadConfig()
    {
        Config.config.load();

        Config.http_enable = Config.config.get( Configuration.CATEGORY_GENERAL, "http_enable", http_enable );
        Config.http_enable.setComment( "Enable the \"http\" API on Computers (see \"http_whitelist\" and \"http_blacklist\" for more fine grained control than this)" );

        Config.http_websocket_enable = Config.config.get( Configuration.CATEGORY_GENERAL, "http_websocket_enable", http_websocket_enable );
        Config.http_websocket_enable.setComment( "Enable use of http websockets. This requires the \"http_enable\" option to also be true." );

        {
            ConfigCategory category = Config.config.getCategory( Configuration.CATEGORY_GENERAL );
            Property currentProperty = category.get( "http_whitelist" );
            if( currentProperty != null && !currentProperty.isList() ) category.remove( "http_whitelist" );

            Config.http_whitelist = Config.config.get( Configuration.CATEGORY_GENERAL, "http_whitelist", DEFAULT_HTTP_WHITELIST );

            if( currentProperty != null && !currentProperty.isList() )
            {
                Config.http_whitelist.setValues( currentProperty.getString().split( ";" ) );
            }
        }
        Config.http_whitelist.setComment( "A list of wildcards for domains or IP ranges that can be accessed through the \"http\" API on Computers.\n" +
            "Set this to \"*\" to access to the entire internet. Example: \"*.pastebin.com\" will restrict access to just subdomains of pastebin.com.\n" +
            "You can use domain names (\"pastebin.com\"), wilcards (\"*.pastebin.com\") or CIDR notation (\"127.0.0.0/8\")." );

        Config.http_blacklist = Config.config.get( Configuration.CATEGORY_GENERAL, "http_blacklist", DEFAULT_HTTP_BLACKLIST );
        Config.http_blacklist.setComment( "A list of wildcards for domains or IP ranges that cannot be accessed through the \"http\" API on Computers.\n" +
            "If this is empty then all whitelisted domains will be accessible. Example: \"*.github.com\" will block access to all subdomains of github.com.\n" +
            "You can use domain names (\"pastebin.com\"), wilcards (\"*.pastebin.com\") or CIDR notation (\"127.0.0.0/8\")." );

        Config.disable_lua51_features = Config.config.get( Configuration.CATEGORY_GENERAL, "disable_lua51_features", disable_lua51_features );
        Config.disable_lua51_features.setComment( "Set this to true to disable Lua 5.1 functions that will be removed in a future update. Useful for ensuring forward compatibility of your programs now." );

        Config.default_computer_settings = Config.config.get( Configuration.CATEGORY_GENERAL, "default_computer_settings", default_computer_settings );
        Config.default_computer_settings.setComment( "A comma seperated list of default system settings to set on new computers. Example: \"shell.autocomplete=false,lua.autocomplete=false,edit.autocomplete=false\" will disable all autocompletion" );

        Config.debug_enable = Config.config.get( Configuration.CATEGORY_GENERAL, "debug_enable", debug_enable );
        Config.debug_enable.setComment( "Enable Lua's debug library. Whilst this should be safe for general use, it may allow players to interact with other computers. Enable at your own risk." );

        Config.computer_threads = Config.config.get( Configuration.CATEGORY_GENERAL, "computer_threads", computer_threads );
        Config.computer_threads
            .setMinValue( 1 )
            .setRequiresWorldRestart( true )
            .setComment( "Set the number of threads computers can run on. A higher number means more computers can run at once, but may induce lag.\n" +
                "Please note that some mods may not work with a thread count higher than 1. Use with caution." );

        Config.logPeripheralErrors = Config.config.get( Configuration.CATEGORY_GENERAL, "logPeripheralErrors", logPeripheralErrors );
        Config.logPeripheralErrors.setComment( "Log exceptions thrown by peripherals and other Lua objects.\n" +
            "This makes it easier for mod authors to debug problems, but may result in log spam should people use buggy methods." );

        Config.enableCommandBlock = Config.config.get( Configuration.CATEGORY_GENERAL, "enableCommandBlock", enableCommandBlock );
        Config.enableCommandBlock.setComment( "Enable Command Block peripheral support" );

        Config.modem_range = Config.config.get( Configuration.CATEGORY_GENERAL, "modem_range", modem_range );
        Config.modem_range.setComment( "The range of Wireless Modems at low altitude in clear weather, in meters" );

        Config.modem_highAltitudeRange = Config.config.get( Configuration.CATEGORY_GENERAL, "modem_highAltitudeRange", modem_highAltitudeRange );
        Config.modem_highAltitudeRange.setComment( "The range of Wireless Modems at maximum altitude in clear weather, in meters" );

        Config.modem_rangeDuringStorm = Config.config.get( Configuration.CATEGORY_GENERAL, "modem_rangeDuringStorm", modem_rangeDuringStorm );
        Config.modem_rangeDuringStorm.setComment( "The range of Wireless Modems at low altitude in stormy weather, in meters" );

        Config.modem_highAltitudeRangeDuringStorm = Config.config.get( Configuration.CATEGORY_GENERAL, "modem_highAltitudeRangeDuringStorm", modem_highAltitudeRangeDuringStorm );
        Config.modem_highAltitudeRangeDuringStorm.setComment( "The range of Wireless Modems at maximum altitude in stormy weather, in meters" );

        Config.computerSpaceLimit = Config.config.get( Configuration.CATEGORY_GENERAL, "computerSpaceLimit", computerSpaceLimit );
        Config.computerSpaceLimit.setComment( "The disk space limit for computers and turtles, in bytes" );

        Config.floppySpaceLimit = Config.config.get( Configuration.CATEGORY_GENERAL, "floppySpaceLimit", floppySpaceLimit );
        Config.floppySpaceLimit.setComment( "The disk space limit for floppy disks, in bytes" );

        Config.turtlesNeedFuel = Config.config.get( Configuration.CATEGORY_GENERAL, "turtlesNeedFuel", turtlesNeedFuel );
        Config.turtlesNeedFuel.setComment( "Set whether Turtles require fuel to move" );

        Config.maximumFilesOpen = Config.config.get( Configuration.CATEGORY_GENERAL, "maximumFilesOpen", maximumFilesOpen );
        Config.maximumFilesOpen.setComment( "Set how many files a computer can have open at the same time. Set to 0 for unlimited." );

        Config.turtleFuelLimit = Config.config.get( Configuration.CATEGORY_GENERAL, "turtleFuelLimit", turtleFuelLimit );
        Config.turtleFuelLimit.setComment( "The fuel limit for Turtles" );

        Config.advancedTurtleFuelLimit = Config.config.get( Configuration.CATEGORY_GENERAL, "advancedTurtleFuelLimit", advancedTurtleFuelLimit );
        Config.advancedTurtleFuelLimit.setComment( "The fuel limit for Advanced Turtles" );

        Config.turtlesObeyBlockProtection = Config.config.get( Configuration.CATEGORY_GENERAL, "turtlesObeyBlockProtection", turtlesObeyBlockProtection );
        Config.turtlesObeyBlockProtection.setComment( "If set to true, Turtles will be unable to build, dig, or enter protected areas (such as near the server spawn point)" );

        Config.turtlesCanPush = Config.config.get( Configuration.CATEGORY_GENERAL, "turtlesCanPush", turtlesCanPush );
        Config.turtlesCanPush.setComment( "If set to true, Turtles will push entities out of the way instead of stopping if there is space to do so" );

        Config.turtleDisabledActions = Config.config.get( Configuration.CATEGORY_GENERAL, "turtle_disabled_actions", new String[0] );
        Config.turtleDisabledActions.setComment( "A list of turtle actions which are disabled." );

        Config.maxNotesPerTick = Config.config.get( Configuration.CATEGORY_GENERAL, "maxNotesPerTick", maxNotesPerTick );
        Config.maxNotesPerTick.setComment( "Maximum amount of notes a speaker can play at once" );

        for( Property property : Config.config.getCategory( Configuration.CATEGORY_GENERAL ).getOrderedValues() )
        {
            property.setLanguageKey( "gui.computercraft:config." + CaseFormat.LOWER_CAMEL.to( CaseFormat.LOWER_UNDERSCORE, property.getName() ) );
        }

        syncConfig();
    }

    public static void syncConfig()
    {

        http_enable = Config.http_enable.getBoolean();
        http_websocket_enable = Config.http_websocket_enable.getBoolean();
        http_whitelist = new AddressPredicate( Config.http_whitelist.getStringList() );
        http_blacklist = new AddressPredicate( Config.http_blacklist.getStringList() );
        disable_lua51_features = Config.disable_lua51_features.getBoolean();
        default_computer_settings = Config.default_computer_settings.getString();
        debug_enable = Config.debug_enable.getBoolean();
        computer_threads = Config.computer_threads.getInt();
        logPeripheralErrors = Config.logPeripheralErrors.getBoolean();

        enableCommandBlock = Config.enableCommandBlock.getBoolean();

        modem_range = Math.min( Config.modem_range.getInt(), 100000 );
        modem_highAltitudeRange = Math.min( Config.modem_highAltitudeRange.getInt(), 100000 );
        modem_rangeDuringStorm = Math.min( Config.modem_rangeDuringStorm.getInt(), 100000 );
        modem_highAltitudeRangeDuringStorm = Math.min( Config.modem_highAltitudeRangeDuringStorm.getInt(), 100000 );

        computerSpaceLimit = Config.computerSpaceLimit.getInt();
        floppySpaceLimit = Config.floppySpaceLimit.getInt();
        maximumFilesOpen = Math.max( 0, Config.maximumFilesOpen.getInt() );

        turtlesNeedFuel = Config.turtlesNeedFuel.getBoolean();
        turtleFuelLimit = Config.turtleFuelLimit.getInt();
        advancedTurtleFuelLimit = Config.advancedTurtleFuelLimit.getInt();
        turtlesObeyBlockProtection = Config.turtlesObeyBlockProtection.getBoolean();
        turtlesCanPush = Config.turtlesCanPush.getBoolean();

        turtleDisabledActions.clear();
        Converter<String, String> converter = CaseFormat.LOWER_CAMEL.converterTo( CaseFormat.UPPER_UNDERSCORE );
        for( String value : Config.turtleDisabledActions.getStringList() )
        {
            try
            {
                turtleDisabledActions.add( TurtleAction.valueOf( converter.convert( value ) ) );
            }
            catch( IllegalArgumentException e )
            {
                ComputerCraft.log.error( "Unknown turtle action " + value );
            }
        }

        maxNotesPerTick = Math.max( 1, Config.maxNotesPerTick.getInt() );

        Config.config.save();
    }

    @Mod.EventHandler
    public void init( FMLInitializationEvent event )
    {
        proxy.init();
        turtleProxy.init();
    }

    @Mod.EventHandler
    public void onServerStarting( FMLServerStartingEvent event )
    {
        proxy.initServer( event.getServer() );
    }

    @Mod.EventHandler
    public void onServerStart( FMLServerStartedEvent event )
    {
        if( FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER )
        {
            ComputerCraft.serverComputerRegistry.reset();
            WirelessNetwork.resetNetworks();
            Tracking.reset();
        }
    }

    @Mod.EventHandler
    public void onServerStopped( FMLServerStoppedEvent event )
    {
        if( FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER )
        {
            ComputerCraft.serverComputerRegistry.reset();
            WirelessNetwork.resetNetworks();
            Tracking.reset();
        }
    }

    public static String getVersion()
    {
        return "${version}";
    }

    public static void openDiskDriveGUI( EntityPlayer player, TileDiskDrive drive )
    {
        BlockPos pos = drive.getPos();
        player.openGui( ComputerCraft.instance, ComputerCraft.diskDriveGUIID, player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ() );
    }

    public static void openComputerGUI( EntityPlayer player, TileComputer computer )
    {
        BlockPos pos = computer.getPos();
        player.openGui( ComputerCraft.instance, ComputerCraft.computerGUIID, player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ() );
    }

    public static void openPrinterGUI( EntityPlayer player, TilePrinter printer )
    {
        BlockPos pos = printer.getPos();
        player.openGui( ComputerCraft.instance, ComputerCraft.printerGUIID, player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ() );
    }

    public static void openTurtleGUI( EntityPlayer player, TileTurtle turtle )
    {
        BlockPos pos = turtle.getPos();
        player.openGui( instance, ComputerCraft.turtleGUIID, player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ() );
    }

    public static void openPrintoutGUI( EntityPlayer player, EnumHand hand )
    {
        player.openGui( ComputerCraft.instance, ComputerCraft.printoutGUIID, player.getEntityWorld(), hand.ordinal(), 0, 0 );
    }

    public static void openPocketComputerGUI( EntityPlayer player, EnumHand hand )
    {
        player.openGui( ComputerCraft.instance, ComputerCraft.pocketComputerGUIID, player.getEntityWorld(), hand.ordinal(), 0, 0 );
    }

    public static void openComputerGUI( EntityPlayer player, ServerComputer computer )
    {
        ComputerFamily family = computer.getFamily();
        int width = 0, height = 0;
        Terminal terminal = computer.getTerminal();
        if( terminal != null )
        {
            width = terminal.getWidth();
            height = terminal.getHeight();
        }

        // Pack useful terminal information into the various coordinate bits.
        // These are extracted in ComputerCraftProxyCommon.getClientGuiElement
        player.openGui( ComputerCraft.instance, ComputerCraft.viewComputerGUIID, player.getEntityWorld(),
            computer.getInstanceID(), family.ordinal(), (width & 0xFFFF) << 16 | (height & 0xFFFF)
        );
    }

    private static File getBaseDir()
    {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getDataDirectory();
    }

    private static File getResourcePackDir()
    {
        return new File( getBaseDir(), "resourcepacks" );
    }

    public static File getWorldDir( World world )
    {
        return proxy.getWorldDir( world );
    }

    public static void sendToPlayer( EntityPlayer player, IMessage packet )
    {
        networkWrapper.sendTo( packet, (EntityPlayerMP) player );
    }

    public static void sendToAllPlayers( IMessage packet )
    {
        networkWrapper.sendToAll( packet );
    }

    public static void sendToServer( IMessage packet )
    {
        networkWrapper.sendToServer( packet );
    }

    public static void sendToAllAround( IMessage packet, NetworkRegistry.TargetPoint point )
    {
        networkWrapper.sendToAllAround( packet, point );
    }

    public static boolean canPlayerUseCommands( EntityPlayer player )
    {
        MinecraftServer server = player.getServer();
        if( server != null )
        {
            return server.getPlayerList().canSendCommands( player.getGameProfile() );
        }
        return false;
    }

    @Deprecated
    public static void registerPermissionProvider( ITurtlePermissionProvider provider )
    {
        TurtlePermissions.register( provider );
    }

    @Deprecated
    public static void registerPocketUpgrade( IPocketUpgrade upgrade )
    {
        dan200.computercraft.shared.PocketUpgrades.register( upgrade );
    }

    @Deprecated
    public static void registerPeripheralProvider( IPeripheralProvider provider )
    {
        Peripherals.register( provider );
    }

    @Deprecated
    public static void registerBundledRedstoneProvider( IBundledRedstoneProvider provider )
    {
        BundledRedstone.register( provider );
    }

    @Deprecated
    public static void registerMediaProvider( IMediaProvider provider )
    {
        MediaProviders.register( provider );
    }

    @Deprecated
    public static void registerAPIFactory( ILuaAPIFactory factory )
    {
        ApiFactories.register( factory );
    }

    @Deprecated
    public static IWiredNode createWiredNodeForElement( IWiredElement element )
    {
        return new WiredNode( element );
    }

    @Deprecated
    public static IWiredElement getWiredElementAt( IBlockAccess world, BlockPos pos, EnumFacing side )
    {
        TileEntity tile = world.getTileEntity( pos );
        return tile != null && tile.hasCapability( CapabilityWiredElement.CAPABILITY, side )
            ? tile.getCapability( CapabilityWiredElement.CAPABILITY, side )
            : null;
    }

    @Deprecated
    public static int getDefaultBundledRedstoneOutput( World world, BlockPos pos, EnumFacing side )
    {
        return BundledRedstone.getDefaultOutput( world, pos, side );
    }

    @Deprecated
    public static IPacketNetwork getWirelessNetwork()
    {
        return WirelessNetwork.getUniversal();
    }

    @Deprecated
    public static int createUniqueNumberedSaveDir( World world, String parentSubPath )
    {
        return IDAssigner.getNextIDFromDirectory( new File( getWorldDir( world ), parentSubPath ) );
    }

    @Deprecated
    public static IWritableMount createSaveDirMount( World world, String subPath, long capacity )
    {
        try
        {
            return new FileMount( new File( getWorldDir( world ), subPath ), capacity );
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Deprecated
    public static IMount createResourceMount( Class<?> modClass, String domain, String subPath )
    {
        // Start building list of mounts
        List<IMount> mounts = new ArrayList<>();
        subPath = "assets/" + domain + "/" + subPath;

        // Mount from debug dir
        File codeDir = getDebugCodeDir( modClass );
        if( codeDir != null )
        {
            File subResource = new File( codeDir, subPath );
            if( subResource.exists() )
            {
                IMount resourcePackMount = new FileMount( subResource, 0 );
                mounts.add( resourcePackMount );
            }
        }

        // Mount from mod jar
        File modJar = getContainingJar( modClass );
        if( modJar != null )
        {
            try
            {
                FileSystem fs = FileSystems.newFileSystem( modJar.toPath(), ComputerCraft.class.getClassLoader() );
                mounts.add( new FileSystemMount( fs, subPath ) );
            }
            catch( IOException | RuntimeException | ServiceConfigurationError e )
            {
                ComputerCraft.log.error( "Could not load mount from mod jar", e );
                // Ignore
            }
        }

        // Mount from resource packs
        File resourcePackDir = getResourcePackDir();
        if( resourcePackDir.exists() && resourcePackDir.isDirectory() )
        {
            String[] resourcePacks = resourcePackDir.list();
            for( String resourcePackName : resourcePacks )
            {
                try
                {
                    File resourcePack = new File( resourcePackDir, resourcePackName );
                    if( !resourcePack.isDirectory() )
                    {
                        // Mount a resource pack from a jar
                        FileSystem fs = FileSystems.newFileSystem( resourcePack.toPath(), ComputerCraft.class.getClassLoader() );
                        if( Files.exists( fs.getPath( subPath ) ) ) mounts.add( new FileSystemMount( fs, subPath ) );
                    }
                    else
                    {
                        // Mount a resource pack from a folder
                        File subResource = new File( resourcePack, subPath );
                        if( subResource.exists() )
                        {
                            IMount resourcePackMount = new FileMount( subResource, 0 );
                            mounts.add( resourcePackMount );
                        }
                    }
                }
                catch( IOException | RuntimeException | ServiceConfigurationError e )
                {
                    ComputerCraft.log.error( "Could not load resource pack '" + resourcePackName + "'", e );
                }
            }
        }

        // Return the combination of all the mounts found
        if( mounts.size() >= 2 )
        {
            IMount[] mountArray = new IMount[mounts.size()];
            mounts.toArray( mountArray );
            return new ComboMount( mountArray );
        }
        else if( mounts.size() == 1 )
        {
            return mounts.get( 0 );
        }
        else
        {
            return null;
        }
    }

    public static InputStream getResourceFile( Class<?> modClass, String domain, String subPath )
    {
        // Start searching in possible locations
        subPath = "assets/" + domain + "/" + subPath;

        // Look in resource packs
        File resourcePackDir = getResourcePackDir();
        if( resourcePackDir.exists() && resourcePackDir.isDirectory() )
        {
            String[] resourcePacks = resourcePackDir.list();
            for( String resourcePackPath : resourcePacks )
            {
                File resourcePack = new File( resourcePackDir, resourcePackPath );
                if( resourcePack.isDirectory() )
                {
                    // Mount a resource pack from a folder
                    File subResource = new File( resourcePack, subPath );
                    if( subResource.exists() && subResource.isFile() )
                    {
                        try
                        {
                            return new FileInputStream( subResource );
                        }
                        catch( FileNotFoundException ignored )
                        {
                        }
                    }
                }
                else
                {
                    ZipFile zipFile = null;
                    try
                    {
                        final ZipFile zip = zipFile = new ZipFile( resourcePack );
                        ZipEntry entry = zipFile.getEntry( subPath );
                        if( entry != null )
                        {
                            // Return a custom InputStream which will close the original zip when finished.
                            return new FilterInputStream( zipFile.getInputStream( entry ) )
                            {
                                @Override
                                public void close() throws IOException
                                {
                                    super.close();
                                    zip.close();
                                }
                            };
                        }
                        else
                        {
                            IOUtils.closeQuietly( zipFile );
                        }
                    }
                    catch( IOException e )
                    {
                        if( zipFile != null ) IOUtils.closeQuietly( zipFile );
                    }
                }
            }
        }

        // Look in debug dir
        File codeDir = getDebugCodeDir( modClass );
        if( codeDir != null )
        {
            File subResource = new File( codeDir, subPath );
            if( subResource.exists() && subResource.isFile() )
            {
                try
                {
                    return new FileInputStream( subResource );
                }
                catch( FileNotFoundException ignored )
                {
                }
            }
        }

        // Look in class loader
        return modClass.getClassLoader().getResourceAsStream( subPath );
    }

    private static File getContainingJar( Class<?> modClass )
    {
        String path = modClass.getProtectionDomain().getCodeSource().getLocation().getPath();
        int bangIndex = path.indexOf( "!" );
        if( bangIndex >= 0 )
        {
            path = path.substring( 0, bangIndex );
        }

        URL url;
        try
        {
            url = new URL( path );
        }
        catch( MalformedURLException e1 )
        {
            return null;
        }

        File file;
        try
        {
            file = new File( url.toURI() );
        }
        catch( URISyntaxException e )
        {
            file = new File( url.getPath() );
        }
        return file;
    }

    private static File getDebugCodeDir( Class<?> modClass )
    {
        String path = modClass.getProtectionDomain().getCodeSource().getLocation().getPath();
        int bangIndex = path.indexOf( "!" );
        return bangIndex >= 0 ? null : new File( new File( path ).getParentFile(), "../.." );
    }

    @Deprecated
    public static void registerTurtleUpgrade( ITurtleUpgrade upgrade )
    {
        TurtleUpgrades.register( upgrade );
    }

    //region Compatibility
    @Deprecated
    public static IMedia getMedia( ItemStack stack )
    {
        return MediaProviders.get( stack );
    }

    @Deprecated
    public static IPocketUpgrade getPocketUpgrade( ItemStack stack )
    {
        return dan200.computercraft.shared.PocketUpgrades.get( stack );
    }

    @Deprecated
    public static ITurtleUpgrade getTurtleUpgrade( ItemStack stack )
    {
        return TurtleUpgrades.get( stack );
    }

    @Deprecated
    public static IPeripheral getPeripheralAt( World world, BlockPos pos, EnumFacing side )
    {
        return Peripherals.getPeripheral( world, pos, side );
    }
    //endregion
}
