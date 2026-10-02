package com.earthkiii.createregexfilter.lang;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Display names of items and fluids in one language.
 * <p>
 * Most stacks are named after their item alone, even when they carry data (damage, enchantments,
 * ...), so those names are cached per item; see {@link #nameDependsOnlyOnItem(ItemStack)}. The
 * other names (renamed items, potions, written books, ...) are cached by the name component itself,
 * which skips rendering it again when the same name comes back.
 */
public final class LocalizedNames {

	/** Past this many distinct name components the cache is dropped and rebuilt, which bounds its memory. */
	private static final int MAX_CACHED_COMPONENTS = 4096;

	/** Whether an item class names its stacks from their data, by overriding one of the two naming methods. */
	private static final ClassValue<Boolean> NAMES_FROM_STACK_DATA = new ClassValue<>() {
		@Override
		protected Boolean computeValue(Class<?> type) {
			try {
				return type.getMethod("getName", ItemStack.class).getDeclaringClass() != Item.class
					|| type.getMethod("getDescriptionId", ItemStack.class).getDeclaringClass() != Item.class;
			} catch (NoSuchMethodException e) {
				return true;
			}
		}
	};

	private final TranslationTable table;
	private final Map<Item, String> itemNames = new ConcurrentHashMap<>();
	private final Map<Fluid, String> fluidNames = new ConcurrentHashMap<>();
	private final Map<Component, String> componentNames = new ConcurrentHashMap<>();

	public LocalizedNames(TranslationTable table) {
		this.table = table;
	}

	/**
	 * Whether this stack's name is its item's default name. {@link ItemStack#getHoverName()} uses the
	 * custom name, then the item name component, then {@link Item#getName(ItemStack)}: so it is when
	 * neither component differs from the item's defaults and the item class does not override how it
	 * names stacks (as potions, written books or player heads do).
	 */
	public static boolean nameDependsOnlyOnItem(ItemStack stack) {
		if (stack.isComponentsPatchEmpty())
			return true;
		// Compared through get(): getComponentsPatch() would make the stack copy its data on its next change.
		Item item = stack.getItem();
		return stack.get(DataComponents.CUSTOM_NAME) == null
			&& Objects.equals(stack.get(DataComponents.ITEM_NAME), item.components().get(DataComponents.ITEM_NAME))
			&& !NAMES_FROM_STACK_DATA.get(item.getClass());
	}

	public String of(ItemStack stack) {
		if (nameDependsOnlyOnItem(stack))
			return itemNames.computeIfAbsent(stack.getItem(), item -> ComponentFlattener.flatten(stack.getHoverName(), table));
		return render(stack.getHoverName());
	}

	public String of(FluidStack stack) {
		if (stack.isComponentsPatchEmpty())
			return fluidNames.computeIfAbsent(stack.getFluid(), fluid -> ComponentFlattener.flatten(stack.getHoverName(), table));
		return render(stack.getHoverName());
	}

	private String render(Component name) {
		String cached = componentNames.get(name);
		if (cached != null)
			return cached;
		String rendered = ComponentFlattener.flatten(name, table);
		if (componentNames.size() >= MAX_CACHED_COMPONENTS)
			componentNames.clear();
		componentNames.put(name, rendered);
		return rendered;
	}
}
