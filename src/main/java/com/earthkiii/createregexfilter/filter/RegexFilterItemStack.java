package com.earthkiii.createregexfilter.filter;

import com.simibubi.create.content.logistics.filter.FilterItemStack;

import com.earthkiii.createregexfilter.ModRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What Create asks a Regex Filter to match with. It is rebuilt whenever Create re-reads the filter
 * item, so it only points at the {@link FilterMatcher} shared by all filters with the same settings.
 */
public class RegexFilterItemStack extends FilterItemStack {

	private final FilterMatcher matcher;

	public RegexFilterItemStack(ItemStack filter) {
		super(filter);
		matcher = FilterMatcher.of(filter.getOrDefault(ModRegistry.REGEX_FILTER_CONFIG.get(), RegexFilterConfig.DEFAULT));
	}

	public RegexFilterConfig config() {
		return matcher.config();
	}

	public FilterMatcher matcher() {
		return matcher;
	}

	@Override
	public boolean test(Level world, ItemStack stack, boolean matchNBT) {
		return matcher.test(stack);
	}

	@Override
	public boolean test(Level world, FluidStack stack, boolean matchNBT) {
		return matcher.test(stack);
	}

	/** The string the pattern is matched against. */
	public String subject(ItemStack stack) {
		return matcher.subject(stack);
	}

	public String subject(FluidStack stack) {
		return matcher.subject(stack);
	}
}
