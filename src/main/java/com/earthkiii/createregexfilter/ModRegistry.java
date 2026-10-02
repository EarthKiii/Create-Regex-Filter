package com.earthkiii.createregexfilter;

import com.earthkiii.createregexfilter.filter.RegexFilterConfig;
import com.earthkiii.createregexfilter.filter.RegexFilterItem;
import com.earthkiii.createregexfilter.filter.RegexFilterMenu;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistry {

	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateRegexFilter.MOD_ID);
	private static final DeferredRegister.DataComponents DATA_COMPONENTS =
		DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CreateRegexFilter.MOD_ID);
	private static final DeferredRegister<MenuType<?>> MENU_TYPES =
		DeferredRegister.create(Registries.MENU, CreateRegexFilter.MOD_ID);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<RegexFilterConfig>> REGEX_FILTER_CONFIG =
		DATA_COMPONENTS.registerComponentType("regex_filter", builder -> builder
			.persistent(RegexFilterConfig.CODEC)
			.networkSynchronized(RegexFilterConfig.STREAM_CODEC)
			.cacheEncoding());

	public static final DeferredItem<RegexFilterItem> REGEX_FILTER =
		ITEMS.register("regex_filter", () -> new RegexFilterItem(new Item.Properties()));

	public static final DeferredHolder<MenuType<?>, MenuType<RegexFilterMenu>> REGEX_FILTER_MENU =
		MENU_TYPES.register("regex_filter", () -> IMenuTypeExtension.create(RegexFilterMenu::new));

	private ModRegistry() {
	}

	static void register(IEventBus modBus) {
		ITEMS.register(modBus);
		DATA_COMPONENTS.register(modBus);
		MENU_TYPES.register(modBus);
	}
}
