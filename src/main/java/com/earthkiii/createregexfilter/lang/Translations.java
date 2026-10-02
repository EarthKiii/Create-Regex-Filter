package com.earthkiii.createregexfilter.lang;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.earthkiii.createregexfilter.CreateRegexFilter;
import net.minecraft.locale.Language;

/**
 * Item and fluid names in any language, loaded on demand and cached until resources are reloaded.
 * <p>
 * Loading a language reads the language files of every mod, which can take a moment in a big pack, so
 * it happens on a background thread. The filter GUI requests a language as soon as it is selected, so
 * it is usually loaded before any filter needs it; a filter that needs a language that is still
 * loading waits for it.
 * <p>
 * On a physical client (including the singleplayer / LAN server) languages come from the client's
 * resources, which have every vanilla language and the player's resource packs. On a dedicated server
 * they come from the server's packs, see {@link ServerTranslationLoader}.
 */
public final class Translations {

	private static final Map<String, CompletableFuture<LocalizedNames>> LANGUAGES = new ConcurrentHashMap<>();
	private static final AtomicInteger GENERATION = new AtomicInteger();
	private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "Regex Filter language loader");
		thread.setDaemon(true);
		return thread;
	});

	private static volatile Function<String, TranslationTable> clientLoader;

	private Translations() {
	}

	/** Called on physical clients only. */
	public static void setClientLoader(Function<String, TranslationTable> loader) {
		clientLoader = loader;
		invalidate();
	}

	/** Starts loading a language if needed, without waiting for it. */
	public static CompletableFuture<LocalizedNames> request(String languageCode) {
		return LANGUAGES.computeIfAbsent(languageCode,
			code -> CompletableFuture.supplyAsync(() -> new LocalizedNames(load(code)), LOADER));
	}

	/** Returns a language, waiting for it to load if needed. */
	public static LocalizedNames get(String languageCode) {
		return request(languageCode).join();
	}

	/**
	 * Increases every time the cached languages are dropped. Callers that cache results derived from
	 * names compare it to know when to drop theirs.
	 */
	public static int generation() {
		return GENERATION.get();
	}

	public static void invalidate() {
		GENERATION.incrementAndGet();
		LANGUAGES.clear();
	}

	private static TranslationTable load(String code) {
		Function<String, TranslationTable> loader = clientLoader;
		try {
			return loader != null ? loader.apply(code) : ServerTranslationLoader.load(code);
		} catch (Exception e) {
			CreateRegexFilter.LOGGER.error("Could not load language {}, using the default language instead", code, e);
			return TranslationTable.of(Language.getInstance());
		}
	}
}
