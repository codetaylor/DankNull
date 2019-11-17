package p455w0rd.danknull.init;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import p455w0rd.danknull.network.PacketConfigSync;
import p455w0rd.danknull.util.NonNullListSerializable;
import p455w0rd.danknull.util.WeakHashMapSerializable;

/**
 * @author p455w0rd
 *
 */
public class ModConfig {

	public static Configuration CONFIG;

	public static final String CLIENT_CAT = "General";
	public static final String SERVER_CAT = "Server Rules";
	public static final boolean DEBUG_RESET = false;

	public static final String NAME_CREATIVE_BLACKLIST = "CreativeBlacklist";
	public static final String NAME_CREATIVE_WHITELIST = "CreativeWhitelist";
	public static final String NAME_OREDICT_BLACKLIST = "OreDictBlacklist";
	public static final String NAME_OREDICT_WHITELIST = "OreDictWhitelist";
	public static final String NAME_DISABLE_OREDICT = "DisableOreDictMode";
	public static final String NAME_ENABLE_COLORED_LIGHTING = "EnableColorShaders";
	public static final String NAME_ALLOW_DOCK_INSERTION = "AllowDockInsertion";
	static boolean init = false;

	public static void init() {
		if (CONFIG == null) {
			final File configFile = new File(ModGlobals.CONFIG_FILE);
			if (DEBUG_RESET) {
				configFile.delete();
			}
			CONFIG = new Configuration(configFile);
			CONFIG.load();
			Options.initDefaults();
		}

		Options.callItDevNull = CONFIG.getBoolean("CallItDevNull", CLIENT_CAT, false, "Call it a /dev/null in-game (Requested by TheMattaBase)");
		Options.superShine = CONFIG.getBoolean("SuperShine", CLIENT_CAT, false, "Make items ultra shiny!");
		Options.creativeBlacklist = CONFIG.getString(NAME_CREATIVE_BLACKLIST, SERVER_CAT, "", "A semicolon separated list of items that are not allowed to be placed into the creative /dank/null\nFormat: modid:name:meta (meta optional: modid:name is acceptable) - Example: minecraft:diamond;minecraft:coal:1").trim();
		Options.creativeWhitelist = CONFIG.getString(NAME_CREATIVE_WHITELIST, SERVER_CAT, "", "A semicolon separated list of items that are allowed to be placed into the creative /dank/null\nSame format as Blacklist and whitelist superceeds blacklist.\nIf whitelist is non-empty, then ONLY whitelisted items can be added to the Creative /dank/null").trim();
		Options.oreBlacklist = CONFIG.getString(NAME_OREDICT_BLACKLIST, SERVER_CAT, "itemSkull", "A semicolon separated list of Ore Dictionary entries (strings) which WILL NOT be allowed to be used with /dank/null's Ore Dictionary functionality.");
		Options.oreWhitelist = CONFIG.getString(NAME_OREDICT_WHITELIST, SERVER_CAT, "", "A semicolon separated list of Ore Dictionary entries (strings) which WILL BE allowed to be used with /dank/null's Ore Dictionary functionality. Whitelist superceeds blacklist.\nIf whitelist is non-empty, then ONLY Ore Dictionary items matching the entries will\nbe able to take advantage of /dank/null's Ore Dictionary functionality.");
		Options.disableOreDictMode = CONFIG.getBoolean(NAME_DISABLE_OREDICT, SERVER_CAT, false, "If set to true, then Ore Dictionary Mode will not be available (overrides Ore Dictionary White/Black lists)");
		Options.showHUD = CONFIG.getBoolean("showHUD", CLIENT_CAT, true, "Show the /dank/null HUD overlay?");
		Options.allowDockInserting = CONFIG.getBoolean(NAME_ALLOW_DOCK_INSERTION, SERVER_CAT, true, "If true, you will be able to pipe items into the /dank/null Docking Station");
		if (CONFIG.hasChanged()) {
			CONFIG.save();
		}
	}

	@SideOnly(Side.SERVER)
	public static void sendConfigsToClient(final EntityPlayerMP player) {
		final WeakHashMapSerializable<String, Object> map = new WeakHashMapSerializable<>();
		map.put(ModConfig.NAME_CREATIVE_BLACKLIST, Options.creativeBlacklist);
		map.put(ModConfig.NAME_CREATIVE_WHITELIST, Options.creativeWhitelist);
		map.put(ModConfig.NAME_OREDICT_BLACKLIST, Options.oreBlacklist);
		map.put(ModConfig.NAME_OREDICT_WHITELIST, Options.oreWhitelist);
		map.put(ModConfig.NAME_DISABLE_OREDICT, Options.disableOreDictMode);
		ModNetworking.getInstance().sendTo(new PacketConfigSync(map), player);
	}

	public static boolean isOreDictBlacklistEnabled() {
		return !Options.getOreBlacklist().isEmpty() && !isOreDictWhitelistEnabled();
	}

	public static boolean isOreDictWhitelistEnabled() {
		return !Options.getOreWhitelist().isEmpty();
	}

	public static boolean isOreBlacklisted(final String oreName) {
		for (final String currentOre : Options.getOreBlacklist()) {
			if (currentOre.equals(oreName)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isOreWhitelisted(final String oreName) {
		for (final String currentOre : Options.getOreWhitelist()) {
			if (currentOre.equals(oreName)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isValidOre(final String oreName) {
		boolean isValid = true;
		if (isOreConfigEnabled()) {
			isValid = isOreDictWhitelistEnabled() && isOreWhitelisted(oreName);
			if (!isValid) {
				isValid = isOreDictBlacklistEnabled() && !isOreBlacklisted(oreName);
			}
		}
		return isValid;
	}

	public static boolean isOreConfigEnabled() {
		return !Options.disableOreDictMode && isOreDictBlacklistEnabled() || isOreDictWhitelistEnabled();
	}

	public static boolean isItemOreDictBlacklisted(final ItemStack stack) {
		if (isOreConfigEnabled() && isOreDictBlacklistEnabled()) {
			for (final int id : OreDictionary.getOreIDs(stack)) {
				final String currentOreName = OreDictionary.getOreName(id);
				if (isValidOre(currentOreName)) {
					for (final String oreName : Options.getOreBlacklist()) {
						if (isValidOre(oreName) && oreName.equals(currentOreName)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public static boolean isItemOreDictWhitelisted(final ItemStack stack) {
		if (isOreConfigEnabled() && isOreDictWhitelistEnabled()) {
			for (final int id : OreDictionary.getOreIDs(stack)) {
				final String currentOreName = OreDictionary.getOreName(id);
				if (isValidOre(currentOreName)) {
					for (final String oreName : Options.getOreWhitelist()) {
						if (isValidOre(oreName) && oreName.equals(OreDictionary.getOreName(id))) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public static class Options {

		public static boolean callItDevNull = false;
		public static boolean superShine = false;
		public static String creativeBlacklist;
		public static String creativeWhitelist;
		public static String oreBlacklist;
		public static String oreWhitelist;
		public static boolean showHUD;
		private static NonNullListSerializable<ItemStack> creativeItemBlacklist; // cache it..
		private static NonNullListSerializable<ItemStack> creativeItemWhitelist;
		private static ArrayList<String> oreStringBlacklist;
		private static ArrayList<String> oreStringWhitelist;
		public static Boolean disableOreDictMode;
		public static boolean allowDockInserting;

		public static void initDefaults() {
			if (!ModConfig.init) {
				callItDevNull = false;
				superShine = false;
				creativeBlacklist = "";
				creativeWhitelist = "";
				oreBlacklist = "";
				oreWhitelist = "";
				showHUD = true;
				creativeItemBlacklist = null; // cache it..
				creativeItemWhitelist = null;
				oreStringBlacklist = Lists.<String>newArrayList();
				oreStringWhitelist = Lists.<String>newArrayList();
				disableOreDictMode = false;
				allowDockInserting = true;
				ModConfig.init = true;
			}
		}

		public static List<String> getOreBlacklist() {
			initDefaults();
			String[] tmpList = null;
			if (oreStringBlacklist.isEmpty() && !oreBlacklist.isEmpty() && getOreWhitelist().isEmpty()) {
				tmpList = oreBlacklist.split(";");
			}
			if (tmpList != null) {
				for (final String string : tmpList) {
					if (OreDictionary.doesOreNameExist(string)) {
						oreStringBlacklist.add(string);
					}
				}
			}
			return oreStringBlacklist;
		}

		public static List<String> getOreWhitelist() {
			initDefaults();
			String[] tmpList = null;
			if (oreStringWhitelist.isEmpty() && !oreWhitelist.isEmpty()) {
				tmpList = oreWhitelist.split(";");
			}
			if (tmpList != null) {
				for (final String string : tmpList) {
					if (OreDictionary.doesOreNameExist(string)) {
						oreStringWhitelist.add(string);
					}
				}
			}
			return oreStringWhitelist;
		}

		public static NonNullListSerializable<ItemStack> getCreativeBlacklistedItems() throws Exception {
			initDefaults();
			if (creativeItemBlacklist == null && getCreativeWhitelistedItems().isEmpty()) {
				creativeItemBlacklist = (NonNullListSerializable<ItemStack>) NonNullListSerializable.<ItemStack>create();
				if (!creativeBlacklist.isEmpty()) {
					final List<String> itemStringList = Lists.newArrayList(creativeBlacklist.split(";"));
					for (final String itemString : itemStringList) {
						final String[] params = itemString.split(":");
						final int numColons = params.length - 1;
						if (numColons > 2 || numColons <= 0) {
							throw new Exception(new Throwable("Invalid format for item blacklisting, check " + ModGlobals.CONFIG_FILE + " for an example"));
						}
						if (numColons == 1) { //no meta
							final Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(params[0], params[1]));
							if (item == null) {
								ModLogger.warn("Item \"" + params[0] + ":" + params[1] + "\" not found");
							}
							else {
								creativeItemBlacklist.add(new ItemStack(item));
							}
						}
						else if (numColons == 2) {
							final Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(params[0], params[1]));
							if (item == null) {
								ModLogger.warn("Item \"" + params[0] + ":" + params[1] + "\" not found");
							}
							else {
								int meta = -1;
								try {
									meta = Integer.parseInt(params[2]);
								}
								catch (final NumberFormatException e) {
									meta = -1;
								}
								if (meta < 0) {
									ModLogger.warn("Invalid metadata for item \"" + params[0] + ":" + params[1] + "\" (" + params[2] + ")");
								}
								else {
									creativeItemWhitelist.add(new ItemStack(item, 1, meta));
								}
							}
						}
					}
				}
			}
			return creativeItemBlacklist;
		}

		public static NonNullListSerializable<ItemStack> getCreativeWhitelistedItems() throws Exception {
			initDefaults();
			if (creativeItemWhitelist == null) {
				creativeItemWhitelist = (NonNullListSerializable<ItemStack>) NonNullListSerializable.<ItemStack>create();
				if (!creativeWhitelist.isEmpty()) {
					final List<String> itemStringList = Lists.newArrayList(creativeWhitelist.split(";"));
					for (final String itemString : itemStringList) {
						final String[] params = itemString.split(":");
						final int numColons = params.length - 1;
						if (numColons > 2 || numColons <= 0) {
							throw new Exception(new Throwable("Invalid format for item whitelisting, check " + ModGlobals.CONFIG_FILE + " for an example"));
						}
						if (numColons == 1) { //no meta
							final Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(params[0], params[1]));
							if (item == null) {
								ModLogger.warn("Item \"" + params[0] + ":" + params[1] + "\" not found");
							}
							else {
								creativeItemWhitelist.add(new ItemStack(item));
							}
						}
						else if (numColons == 2) {
							final Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(params[0], params[1]));
							if (item == null) {
								ModLogger.warn("Item \"" + params[0] + ":" + params[1] + "\" not found");
							}
							else {
								int meta = -1;
								try {
									meta = Integer.parseInt(params[2]);
								}
								catch (final NumberFormatException e) {
									meta = -1;
								}
								if (meta < 0) {
									ModLogger.warn("Invalid metadata for item \"" + params[0] + ":" + params[1] + "\" (" + params[2] + ")");
								}
								else {
									creativeItemWhitelist.add(new ItemStack(item, 1, meta));
								}
							}
						}
					}
				}
			}
			return creativeItemWhitelist;
		}
	}

}
