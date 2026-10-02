package com.earthkiii.createregexfilter.client;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;

import com.earthkiii.createregexfilter.CreateRegexFilter;
import com.earthkiii.createregexfilter.ModRegistry;
import com.earthkiii.createregexfilter.lang.Translations;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = CreateRegexFilter.MOD_ID, dist = Dist.CLIENT)
public final class CreateRegexFilterClient {

	public CreateRegexFilterClient(IEventBus modBus) {
		Translations.setClientLoader(ClientTranslationLoader::load);

		modBus.addListener((RegisterMenuScreensEvent event) ->
			event.register(ModRegistry.REGEX_FILTER_MENU.get(), RegexFilterScreen::new));

		// Language changes and resource pack changes both reload resources.
		modBus.addListener((RegisterClientReloadListenersEvent event) ->
			event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> Translations.invalidate()));

		// "Hold [Shift] for Summary", like Create's own items.
		modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
			var item = ModRegistry.REGEX_FILTER.get();
			TooltipModifier.REGISTRY.register(item, new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE));
		}));
	}
}
