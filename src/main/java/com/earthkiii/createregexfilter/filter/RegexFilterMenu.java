package com.earthkiii.createregexfilter.filter;

import com.simibubi.create.content.logistics.filter.AbstractFilterMenu;

import com.earthkiii.createregexfilter.ModRegistry;
import com.earthkiii.createregexfilter.lang.Translations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Menu of the Regex Filter. Has one ghost slot for a sample item, which only exists to preview the
 * pattern in the screen and is not saved on the filter.
 */
public class RegexFilterMenu extends AbstractFilterMenu {

	public static final int SAMPLE_SLOT_X = 16;
	public static final int SAMPLE_SLOT_Y = 24;

	public RegexFilterConfig config;

	public RegexFilterMenu(int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
		this(ModRegistry.REGEX_FILTER_MENU.get(), id, inv, extraData);
	}

	protected RegexFilterMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	protected RegexFilterMenu(MenuType<?> type, int id, Inventory inv, ItemStack filter) {
		super(type, id, inv, filter);
	}

	public static RegexFilterMenu create(int id, Inventory inv, ItemStack filter) {
		return new RegexFilterMenu(ModRegistry.REGEX_FILTER_MENU.get(), id, inv, filter);
	}

	/** Called on the server with the settings the screen sent. */
	public void applyConfig(RegexFilterConfig config) {
		this.config = config;
		if (config.target() == RegexFilterConfig.Target.ITEM_NAME)
			Translations.request(config.language());
	}

	public ItemStack getSample() {
		return ghostInventory.getStackInSlot(0);
	}

	@Override
	protected int getPlayerInventoryXOffset() {
		// Matches RegexFilterScreen: the player inventory is centered under the 248 px wide background,
		// and the window is shifted 11 px left to balance the tab on its right side.
		return 11 + (248 - 176) / 2 + 8;
	}

	@Override
	protected int getPlayerInventoryYOffset() {
		return 79 + 4 + 18;
	}

	@Override
	protected void addFilterSlots() {
		addSlot(new SlotItemHandler(ghostInventory, 0, SAMPLE_SLOT_X, SAMPLE_SLOT_Y));
	}

	@Override
	protected ItemStackHandler createGhostInventory() {
		return new ItemStackHandler(1);
	}

	@Override
	protected void initAndReadInventory(ItemStack filter) {
		super.initAndReadInventory(filter);
		config = filter.getOrDefault(ModRegistry.REGEX_FILTER_CONFIG.get(), RegexFilterConfig.DEFAULT);
	}

	@Override
	public void clearContents() {
		super.clearContents();
		// Keep the language: it is a preference of the player more than a part of the pattern.
		// (A mod ID filter has none, so it gets the default one.)
		config = RegexFilterConfig.DEFAULT.withLanguage(config.language());
	}

	@Override
	protected void saveData(ItemStack filter) {
		// Not calling super: it would store the sample item as the filter's item list.
		if (config.pattern().isEmpty())
			filter.remove(ModRegistry.REGEX_FILTER_CONFIG.get());
		else
			filter.set(ModRegistry.REGEX_FILTER_CONFIG.get(), config);
	}
}
