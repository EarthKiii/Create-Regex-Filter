package com.earthkiii.createregexfilter.filter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.earthkiii.createregexfilter.filter.RegexFilterConfig.Target;
import com.earthkiii.createregexfilter.lang.LocalizedNames;
import com.earthkiii.createregexfilter.lang.Translations;
import com.earthkiii.createregexfilter.regex.CompiledRegex;
import com.earthkiii.createregexfilter.regex.RegexCompiler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The matching logic and result caches of one filter configuration, shared by every Regex Filter
 * with that configuration. Create rebuilds a filter's {@code FilterItemStack} more often than one
 * might think (on every block entity sync to clients, every stock keeper refresh, every blueprint
 * craft), and sharing the matcher keeps its caches across those rebuilds and across filters.
 * <p>
 * Cost of a test, from the hot path down:
 * <ol>
 * <li>most stacks, including damaged or enchanted ones: one map lookup by item, as their name and
 * mod ID only depend on the item (see {@link LocalizedNames#nameDependsOnlyOnItem});</li>
 * <li>renamed items, potions, ...: the name is looked up by its text component, then in the regex's
 * result memo (see {@code Re2Regex});</li>
 * <li>only for a string never seen before is the regex engine run, in linear time.</li>
 * </ol>
 */
public final class FilterMatcher {

	/** Past this many distinct configurations the matchers are dropped and rebuilt, which bounds their memory. */
	private static final int MAX_SHARED_MATCHERS = 1024;
	private static final Map<RegexFilterConfig, FilterMatcher> MATCHERS = new ConcurrentHashMap<>();

	private final RegexFilterConfig config;
	@Nullable
	private final CompiledRegex regex;

	private final Map<Item, Boolean> itemResults = new ConcurrentHashMap<>();
	private final Map<Fluid, Boolean> fluidResults = new ConcurrentHashMap<>();
	private volatile int cachedGeneration;

	private FilterMatcher(RegexFilterConfig config) {
		this.config = config;
		regex = config.compile() instanceof RegexCompiler.Valid valid ? valid.regex() : null;
		cachedGeneration = Translations.generation();
	}

	public static FilterMatcher of(RegexFilterConfig config) {
		// Start loading the language now, so it is usually ready by the time the first item arrives.
		if (config.target() == Target.ITEM_NAME)
			Translations.request(config.language());

		FilterMatcher matcher = MATCHERS.get(config);
		if (matcher != null)
			return matcher;
		if (MATCHERS.size() >= MAX_SHARED_MATCHERS)
			MATCHERS.clear();
		return MATCHERS.computeIfAbsent(config, FilterMatcher::new);
	}

	public RegexFilterConfig config() {
		return config;
	}

	public boolean test(ItemStack stack) {
		// An invalid pattern lets nothing through, even in deny mode: a typo must not open the floodgates.
		if (regex == null || stack.isEmpty())
			return false;

		if (config.target() == Target.ITEM_NAME && !LocalizedNames.nameDependsOnlyOnItem(stack))
			return passes(Translations.get(config.language()).of(stack));

		checkGeneration();
		Item item = stack.getItem();
		Boolean cached = itemResults.get(item);
		if (cached != null)
			return cached;
		boolean result = passes(subject(stack));
		itemResults.put(item, result);
		return result;
	}

	public boolean test(FluidStack stack) {
		if (regex == null || stack.isEmpty())
			return false;

		if (config.target() == Target.ITEM_NAME && !stack.isComponentsPatchEmpty())
			return passes(Translations.get(config.language()).of(stack));

		checkGeneration();
		Fluid fluid = stack.getFluid();
		Boolean cached = fluidResults.get(fluid);
		if (cached != null)
			return cached;
		boolean result = passes(subject(stack));
		fluidResults.put(fluid, result);
		return result;
	}

	/** The string the pattern is matched against. */
	public String subject(ItemStack stack) {
		if (config.target() == Target.MOD_ID)
			return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
		return Translations.get(config.language()).of(stack);
	}

	public String subject(FluidStack stack) {
		if (config.target() == Target.MOD_ID)
			return BuiltInRegistries.FLUID.getKey(stack.getFluid()).getNamespace();
		return Translations.get(config.language()).of(stack);
	}

	private boolean passes(String subject) {
		return regex.find(subject) != config.deny();
	}

	/** Names can change when resources (and so translations) are reloaded. */
	private void checkGeneration() {
		int generation = Translations.generation();
		if (generation != cachedGeneration) {
			cachedGeneration = generation;
			itemResults.clear();
			fluidResults.clear();
		}
	}
}
