package com.earthkiii.createregexfilter;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.earthkiii.createregexfilter.lang.Translations;
import com.earthkiii.createregexfilter.network.ConfigureRegexFilterPacket;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@Mod(CreateRegexFilter.MOD_ID)
public final class CreateRegexFilter {

	public static final String MOD_ID = "create_regex_filter";
	public static final Logger LOGGER = LogUtils.getLogger();

	public CreateRegexFilter(IEventBus modBus) {
		ModRegistry.register(modBus);
		ModCreativeTabs.register(modBus);
		modBus.addListener(ConfigureRegexFilterPacket::register);

		// A dedicated server reads languages from its packs, which /reload can change.
		NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
			if (event.getPlayer() == null)
				Translations.invalidate();
		});
		NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> Translations.invalidate());
	}

	public static ResourceLocation asResource(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Translation key under this mod's namespace, e.g. {@code create_regex_filter.gui.pattern}. */
	public static String lang(String key) {
		return MOD_ID + "." + key;
	}
}
