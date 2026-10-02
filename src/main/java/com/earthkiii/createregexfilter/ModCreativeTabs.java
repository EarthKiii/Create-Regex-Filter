package com.earthkiii.createregexfilter;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {

	private static final DeferredRegister<CreativeModeTab> TABS =
		DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateRegexFilter.MOD_ID);

	/** Placed right after Create's own tabs (its last one is "palettes"). */
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
		.title(Component.translatable("itemGroup." + CreateRegexFilter.MOD_ID))
		.withTabsBefore(ResourceLocation.fromNamespaceAndPath("create", "palettes"))
		.icon(() -> ModRegistry.REGEX_FILTER.get().getDefaultInstance())
		.displayItems((parameters, output) -> output.accept(ModRegistry.REGEX_FILTER.get()))
		.build());

	private ModCreativeTabs() {
	}

	static void register(IEventBus modBus) {
		TABS.register(modBus);
	}
}
