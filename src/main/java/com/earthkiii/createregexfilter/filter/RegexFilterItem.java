package com.earthkiii.createregexfilter.filter;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;

import com.earthkiii.createregexfilter.CreateRegexFilter;
import com.earthkiii.createregexfilter.ModRegistry;
import com.earthkiii.createregexfilter.filter.RegexFilterConfig.Target;
import com.earthkiii.createregexfilter.regex.RegexCompiler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * A Create filter that matches item (or fluid) names or mod IDs against a regular expression.
 * Create's filter slots, stock keeper categories, item copying recipe etc. work with any
 * {@link FilterItem}, and ask it for a {@link FilterItemStack} to do the actual matching.
 */
public class RegexFilterItem extends FilterItem {

	public RegexFilterItem(Properties properties) {
		super(properties);
	}

	@Override
	public List<Component> makeSummary(ItemStack filter) {
		RegexFilterConfig config = filter.get(ModRegistry.REGEX_FILTER_CONFIG.get());
		if (config == null || config.pattern().isEmpty())
			return List.of();

		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable(config.deny() ? CreateRegexFilter.lang("summary.deny") : CreateRegexFilter.lang("summary.allow"))
			.withStyle(ChatFormatting.GOLD));
		lines.add(Component.literal("/" + config.pattern() + "/").withStyle(ChatFormatting.WHITE));
		Component target = config.target() == Target.ITEM_NAME
			? Component.translatable(CreateRegexFilter.lang("target.item_name.with_language"), config.language())
			: Component.translatable(CreateRegexFilter.lang("target.mod_id"));
		lines.add(Component.translatable(CreateRegexFilter.lang("summary.target"), target).withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable(CreateRegexFilter.lang(config.ignoreCase() ? "summary.ignore_case" : "summary.match_case"))
			.withStyle(ChatFormatting.GRAY));
		if (config.compile() instanceof RegexCompiler.Invalid)
			lines.add(Component.translatable(CreateRegexFilter.lang("summary.invalid")).withStyle(ChatFormatting.RED));
		return lines;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return RegexFilterMenu.create(id, inv, player.getMainHandItem());
	}

	@Override
	public DataComponentType<?> getComponentType() {
		return ModRegistry.REGEX_FILTER_CONFIG.get();
	}

	@Override
	public FilterItemStack makeStackWrapper(ItemStack filter) {
		return new RegexFilterItemStack(filter);
	}

	@Override
	public ItemStack[] getFilterItems(ItemStack stack) {
		return new ItemStack[0];
	}
}
