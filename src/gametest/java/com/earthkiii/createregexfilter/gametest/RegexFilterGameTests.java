package com.earthkiii.createregexfilter.gametest;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.logistics.filter.FilterItemStack;

import com.earthkiii.createregexfilter.CreateRegexFilter;
import com.earthkiii.createregexfilter.ModRegistry;
import com.earthkiii.createregexfilter.filter.RegexFilterConfig;
import com.earthkiii.createregexfilter.filter.RegexFilterConfig.Target;
import com.earthkiii.createregexfilter.filter.RegexFilterItemStack;
import com.earthkiii.createregexfilter.filter.RegexFilterMenu;
import com.earthkiii.createregexfilter.lang.ComponentFlattener;
import com.earthkiii.createregexfilter.lang.LocalizedNames;
import com.earthkiii.createregexfilter.lang.TranslationTable;
import com.earthkiii.createregexfilter.regex.RegexCompiler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.server.network.Filterable;
import net.minecraft.locale.Language;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Runs on a real (dedicated-style) server with Create loaded: {@code ./gradlew runGameTestServer}.
 * Filters are evaluated through Create's own entry point, {@link FilterItemStack#of(ItemStack)}, the
 * same call Create's filter slots make.
 */
@GameTestHolder(CreateRegexFilter.MOD_ID)
@PrefixGameTestTemplate(false)
public class RegexFilterGameTests {

	private static final String EMPTY = "empty";

	/** A case-sensitive filter; see {@link #ignoreCase(GameTestHelper)} for the other mode. */
	private static FilterItemStack filter(GameTestHelper helper, String pattern, Target target, String language, boolean deny) {
		return filter(helper, new RegexFilterConfig(pattern, target, language, deny, false));
	}

	private static FilterItemStack filter(GameTestHelper helper, RegexFilterConfig config) {
		ItemStack stack = new ItemStack(ModRegistry.REGEX_FILTER.get());
		stack.set(ModRegistry.REGEX_FILTER_CONFIG.get(), config);
		FilterItemStack filter = FilterItemStack.of(stack);
		helper.assertTrue(filter instanceof RegexFilterItemStack, "Create did not use the regex filter's matcher: " + filter);
		return filter;
	}

	private static ItemStack create(String path) {
		Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", path));
		return new ItemStack(item);
	}

	private static void expect(GameTestHelper helper, FilterItemStack filter, ItemStack stack, boolean expected) {
		boolean actual = filter.test(helper.getLevel(), stack);
		String subject = ((RegexFilterItemStack) filter).subject(stack);
		helper.assertTrue(actual == expected, "Expected " + ((RegexFilterItemStack) filter).config() + " to "
			+ (expected ? "pass" : "block") + " " + stack + " (subject \"" + subject + "\")");
	}

	@GameTest(template = EMPTY)
	public static void modIdAllowAndDeny(GameTestHelper helper) {
		FilterItemStack allow = filter(helper, "^minecraft$", Target.MOD_ID, "en_us", false);
		expect(helper, allow, new ItemStack(Items.IRON_INGOT), true);
		expect(helper, allow, create("andesite_alloy"), false);

		FilterItemStack deny = filter(helper, "^minecraft$", Target.MOD_ID, "en_us", true);
		expect(helper, deny, new ItemStack(Items.IRON_INGOT), false);
		expect(helper, deny, create("andesite_alloy"), true);
		expect(helper, deny, ItemStack.EMPTY, false);
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void englishItemNames(GameTestHelper helper) {
		FilterItemStack ingots = filter(helper, "^(Iron|Gold) Ingot$", Target.ITEM_NAME, "en_us", false);
		expect(helper, ingots, new ItemStack(Items.IRON_INGOT), true);
		expect(helper, ingots, new ItemStack(Items.GOLD_INGOT), true);
		expect(helper, ingots, new ItemStack(Items.COPPER_INGOT), false);
		expect(helper, ingots, create("andesite_alloy"), false);
		helper.succeed();
	}

	/** Every example in the README's table, with Ignore Case on as it is there. */
	@GameTest(template = EMPTY)
	public static void readmeExamples(GameTestHelper helper) {
		FilterItemStack ingots = filter(helper, new RegexFilterConfig("ingot$", Target.ITEM_NAME, "en_us", false, true));
		expect(helper, ingots, new ItemStack(Items.IRON_INGOT), true);
		expect(helper, ingots, new ItemStack(Items.GOLD_INGOT), true);
		expect(helper, ingots, create("brass_ingot"), true);

		FilterItemStack ironOrGold = filter(helper, new RegexFilterConfig("^(iron|gold)\\b", Target.ITEM_NAME, "en_us", false, true));
		expect(helper, ironOrGold, new ItemStack(Items.IRON_INGOT), true);
		expect(helper, ironOrGold, new ItemStack(Items.IRON_SWORD), true);
		expect(helper, ironOrGold, new ItemStack(Items.GOLD_NUGGET), true);
		expect(helper, ironOrGold, new ItemStack(Items.GOLDEN_APPLE), false);

		FilterItemStack ores = filter(helper, new RegexFilterConfig("\\bore$", Target.ITEM_NAME, "en_us", false, true));
		expect(helper, ores, new ItemStack(Items.IRON_ORE), true);
		expect(helper, ores, new ItemStack(Items.DEEPSLATE_GOLD_ORE), true);
		expect(helper, ores, new ItemStack(Items.HEAVY_CORE), false);

		FilterItemStack fromCreate = filter(helper, new RegexFilterConfig("^create$", Target.MOD_ID, "", false, true));
		expect(helper, fromCreate, create("andesite_alloy"), true);
		expect(helper, fromCreate, new ItemStack(Items.IRON_INGOT), false);

		FilterItemStack others = filter(helper, new RegexFilterConfig("^(minecraft|create)$", Target.MOD_ID, "", true, true));
		expect(helper, others, new ItemStack(Items.IRON_INGOT), false);
		expect(helper, others, create("andesite_alloy"), false);
		expect(helper, others, new ItemStack(ModRegistry.REGEX_FILTER.get()), true);
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void ignoreCase(GameTestHelper helper) {
		RegexFilterConfig lower = new RegexFilterConfig("iron ingot", Target.ITEM_NAME, "en_us", false, true);
		expect(helper, filter(helper, lower), new ItemStack(Items.IRON_INGOT), true);
		expect(helper, filter(helper, lower.withIgnoreCase(false)), new ItemStack(Items.IRON_INGOT), false);

		RegexFilterConfig regex = new RegexFilterConfig("^(iron|gold) ingot$", Target.ITEM_NAME, "en_us", false, true);
		expect(helper, filter(helper, regex), new ItemStack(Items.GOLD_INGOT), true);
		expect(helper, filter(helper, regex), new ItemStack(Items.COPPER_INGOT), false);

		FilterItemStack french = filter(helper, new RegexFilterConfig("ANDÉSITE", Target.ITEM_NAME, "fr_fr", false, true));
		expect(helper, french, create("andesite_alloy"), true);

		// The inline flag is rejected in favour of the toggle, so it lets nothing through.
		expect(helper, filter(helper, new RegexFilterConfig("(?i)iron", Target.ITEM_NAME, "en_us", true, true)),
			new ItemStack(Items.IRON_INGOT), false);
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void frenchItemNamesFromModJars(GameTestHelper helper) {
		// Create ships fr_fr, and the server loads it from the mod jar even though it runs in English.
		FilterItemStack french = filter(helper, "^Alliage d'andésite$", Target.ITEM_NAME, "fr_fr", false);
		expect(helper, french, create("andesite_alloy"), true);
		FilterItemStack english = filter(helper, "^Alliage d'andésite$", Target.ITEM_NAME, "en_us", false);
		expect(helper, english, create("andesite_alloy"), false);

		// A dedicated server has no vanilla fr_fr: vanilla names fall back to English, like missing keys in game.
		FilterItemStack vanilla = filter(helper, "^Iron Ingot$", Target.ITEM_NAME, "fr_fr", false);
		expect(helper, vanilla, new ItemStack(Items.IRON_INGOT), true);
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void customNamesAndComponents(GameTestHelper helper) {
		ItemStack renamed = new ItemStack(Items.COBBLESTONE);
		renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Magic Rock"));
		FilterItemStack magic = filter(helper, "^Magic", Target.ITEM_NAME, "en_us", false);
		expect(helper, magic, renamed, true);
		expect(helper, magic, new ItemStack(Items.COBBLESTONE), false);

		// The mod ID does not depend on components.
		FilterItemStack mod = filter(helper, "minecraft", Target.MOD_ID, "en_us", false);
		expect(helper, mod, renamed, true);
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void invalidPatternsLetNothingThrough(GameTestHelper helper) {
		expect(helper, filter(helper, "(unclosed", Target.ITEM_NAME, "en_us", false), new ItemStack(Items.STONE), false);
		expect(helper, filter(helper, "(unclosed", Target.ITEM_NAME, "en_us", true), new ItemStack(Items.STONE), false);
		expect(helper, filter(helper, "((a{1000}){1000}){1000}", Target.ITEM_NAME, "en_us", true), new ItemStack(Items.STONE), false);
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void pathologicalPatternIsFast(GameTestHelper helper) {
		ItemStack evil = new ItemStack(Items.PAPER);
		evil.set(DataComponents.CUSTOM_NAME, Component.literal("a".repeat(40) + "!"));
		FilterItemStack filter = filter(helper, "(a+)+$", Target.ITEM_NAME, "en_us", false);
		long start = System.nanoTime();
		expect(helper, filter, evil, false);
		long millis = (System.nanoTime() - start) / 1_000_000;
		helper.assertTrue(millis < 250, "Pathological pattern took " + millis + " ms");
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void fluids(GameTestHelper helper) {
		FluidStack water = new FluidStack(Fluids.WATER, 1000);
		helper.assertTrue(filter(helper, "^minecraft$", Target.MOD_ID, "en_us", false).test(helper.getLevel(), water),
			"mod ID filter should pass water");
		helper.assertTrue(filter(helper, "^Water$", Target.ITEM_NAME, "en_us", false).test(helper.getLevel(), water),
			"name filter should pass water");
		helper.assertFalse(filter(helper, "^Lava$", Target.ITEM_NAME, "en_us", false).test(helper.getLevel(), water),
			"name filter should block water");
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void configPersistence(GameTestHelper helper) {
		RegexFilterConfig config = new RegexFilterConfig("ore$", Target.MOD_ID, "fr_fr", true, false);
		// Mod ID filters do not keep a language; switching back to names uses a valid one again.
		helper.assertValueEqual(config.language(), "", "language of a mod ID filter");
		RegexFilterConfig names = new RegexFilterConfig("x", Target.ITEM_NAME, "fr_fr", false, true);
		helper.assertValueEqual(names.withTarget(Target.MOD_ID).language(), "", "language after switching to mod ID");
		helper.assertValueEqual(names.withTarget(Target.MOD_ID).withTarget(Target.ITEM_NAME).language(), "en_us",
			"language after switching back to names");
		Tag saved = RegexFilterConfig.CODEC.encodeStart(NbtOps.INSTANCE, config).getOrThrow();
		helper.assertValueEqual(RegexFilterConfig.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow(), config, "decoded config");
		// Data without the setting (written before it existed) ignores case, like a new filter.
		CompoundTag withoutCase = ((CompoundTag) saved).copy();
		withoutCase.remove("ignore_case");
		helper.assertTrue(RegexFilterConfig.CODEC.parse(NbtOps.INSTANCE, withoutCase).getOrThrow().ignoreCase(),
			"missing ignore_case defaults to on");
		helper.assertTrue(RegexFilterConfig.DEFAULT.ignoreCase(), "new filters ignore case");

		// Untrusted values are clamped instead of trusted.
		RegexFilterConfig hostile = new RegexFilterConfig("x".repeat(10_000), null, "../../evil", false, true);
		helper.assertValueEqual(hostile.pattern().length(), RegexCompiler.MAX_PATTERN_LENGTH, "clamped pattern length");
		helper.assertValueEqual(hostile.target(), Target.ITEM_NAME, "default target");
		helper.assertValueEqual(hostile.language(), "en_us", "default language");

		ItemStack stack = new ItemStack(ModRegistry.REGEX_FILTER.get());
		stack.set(ModRegistry.REGEX_FILTER_CONFIG.get(), config);
		Tag item = stack.save(helper.getLevel().registryAccess());
		ItemStack loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) item);
		helper.assertValueEqual(loaded.get(ModRegistry.REGEX_FILTER_CONFIG.get()), config, "config after item save/load");
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void menuSavesToHeldFilter(GameTestHelper helper) {
		// A plain mock player: a mock ServerPlayer would make Create send it login packets it cannot receive.
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistry.REGEX_FILTER.get()));
		ItemStack held = player.getMainHandItem();

		RegexFilterMenu menu = RegexFilterMenu.create(1, player.getInventory(), held);
		menu.applyConfig(new RegexFilterConfig("Ingot$", Target.ITEM_NAME, "en_us", false, false));
		menu.removed(player);
		helper.assertValueEqual(held.get(ModRegistry.REGEX_FILTER_CONFIG.get()).pattern(), "Ingot$", "saved pattern");
		expect(helper, FilterItemStack.of(held), new ItemStack(Items.IRON_INGOT), true);

		// The trash button clears the pattern, which removes the data (a blank filter again).
		RegexFilterMenu reopened = RegexFilterMenu.create(2, player.getInventory(), held);
		reopened.clearContents();
		reopened.removed(player);
		helper.assertFalse(held.has(ModRegistry.REGEX_FILTER_CONFIG.get()), "cleared filter should have no data");
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void itemsWithDataUseTheirRealName(GameTestHelper helper) {
		ItemStack damaged = new ItemStack(Items.IRON_SWORD);
		damaged.setDamageValue(42);
		ItemStack enchanted = new ItemStack(Items.IRON_SWORD);
		enchanted.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
			.getOrThrow(Enchantments.SHARPNESS), 3);
		helper.assertTrue(LocalizedNames.nameDependsOnlyOnItem(damaged), "a damaged sword is named after its item");
		helper.assertTrue(LocalizedNames.nameDependsOnlyOnItem(enchanted), "an enchanted sword is named after its item");
		FilterItemStack swords = filter(helper, "^Iron Sword$", Target.ITEM_NAME, "en_us", false);
		expect(helper, swords, damaged, true);
		expect(helper, swords, enchanted, true);

		// Names that come from the stack's data must never be shared through the per-item cache.
		ItemStack swiftness = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
		ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
		helper.assertFalse(LocalizedNames.nameDependsOnlyOnItem(swiftness), "potions are named from their contents");
		FilterItemStack potions = filter(helper, "Swiftness", Target.ITEM_NAME, "en_us", false);
		for (int round = 0; round < 3; round++) {
			expect(helper, potions, swiftness, true);
			expect(helper, potions, water, false);
		}

		ItemStack plans = writtenBook("Secret Plans");
		ItemStack recipes = writtenBook("Recipes");
		FilterItemStack books = filter(helper, "^Secret", Target.ITEM_NAME, "en_us", false);
		expect(helper, books, plans, true);
		expect(helper, books, recipes, false);

		ItemStack excalibur = new ItemStack(Items.IRON_SWORD);
		excalibur.set(DataComponents.ITEM_NAME, Component.literal("Excalibur"));
		helper.assertFalse(LocalizedNames.nameDependsOnlyOnItem(excalibur), "an item name override changes the name");
		expect(helper, swords, excalibur, false);
		expect(helper, filter(helper, "^Excalibur$", Target.ITEM_NAME, "en_us", false), excalibur, true);

		ItemStack renamed = damaged.copy();
		renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Old Faithful"));
		expect(helper, swords, renamed, false);
		expect(helper, swords, damaged, true);
		helper.succeed();
	}

	private static ItemStack writtenBook(String title) {
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT,
			new WrittenBookContent(Filterable.passThrough(title), "EarthKiii", 0, List.of(), true));
		return book;
	}

	@GameTest(template = EMPTY)
	public static void filtersWithTheSameSettingsShareOneMatcher(GameTestHelper helper) {
		FilterItemStack a = filter(helper, "ingot$", Target.ITEM_NAME, "en_us", false);
		FilterItemStack b = filter(helper, "ingot$", Target.ITEM_NAME, "en_us", false);
		FilterItemStack other = filter(helper, "ingot$", Target.ITEM_NAME, "en_us", true);
		helper.assertTrue(((RegexFilterItemStack) a).matcher() == ((RegexFilterItemStack) b).matcher(),
			"equal settings should share a matcher");
		helper.assertFalse(((RegexFilterItemStack) a).matcher() == ((RegexFilterItemStack) other).matcher(),
			"different settings should not share a matcher");
		helper.succeed();
	}

	/** Not a pass/fail test: logs what a test costs on damaged tools, against rendering the name every time. */
	@GameTest(template = EMPTY)
	public static void measureDamagedToolCost(GameTestHelper helper) {
		List<ItemStack> tools = new ArrayList<>();
		for (Item item : List.of(Items.IRON_SWORD, Items.IRON_PICKAXE, Items.DIAMOND_AXE, Items.GOLDEN_SHOVEL)) {
			for (int damage = 1; damage <= 25; damage++) {
				ItemStack tool = new ItemStack(item);
				tool.setDamageValue(damage);
				tools.add(tool);
			}
		}
		FilterItemStack filter = filter(helper, "^(Iron|Diamond) ", Target.ITEM_NAME, "en_us", false);
		TranslationTable english = TranslationTable.of(Language.getInstance());
		int rounds = 2000;
		long sink = 0;

		for (int warmup = 0; warmup < 2; warmup++) {
			long start = System.nanoTime();
			for (int round = 0; round < rounds; round++)
				for (ItemStack tool : tools)
					sink += filter.test(helper.getLevel(), tool) ? 1 : 0;
			long cached = System.nanoTime() - start;

			start = System.nanoTime();
			for (int round = 0; round < rounds; round++)
				for (ItemStack tool : tools)
					sink += ComponentFlattener.flatten(tool.getHoverName(), english).length();
			long rendering = System.nanoTime() - start;

			long tests = (long) rounds * tools.size();
			CreateRegexFilter.LOGGER.info("PERF damaged tools, run {}: {} ns per filter test, {} ns just to render the name (the old path)",
				warmup + 1, cached / tests, rendering / tests);
		}
		helper.assertTrue(sink > 0, "measurement ran");
		helper.succeed();
	}
}
