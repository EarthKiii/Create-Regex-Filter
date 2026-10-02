package com.earthkiii.createregexfilter.compat.emi;

import java.util.Map;
import java.util.function.Consumer;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import com.earthkiii.createregexfilter.client.RegexFilterScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

/**
 * Lets items be dragged from EMI into the filter's test slot, and keeps EMI's panels clear of the
 * filter drawn to the right of the screen. Only loaded by EMI, when it is installed.
 */
@EmiEntrypoint
public class RegexFilterEmiPlugin implements EmiPlugin {

	@Override
	public void register(EmiRegistry registry) {
		registry.addDragDropHandler(RegexFilterScreen.class, new EmiDragDropHandler.BoundsBased<RegexFilterScreen>(
			screen -> Map.of(slotBounds(screen.getSampleSlotArea()), (Consumer<EmiIngredient>) dropped -> drop(screen, dropped))));

		registry.addExclusionArea(RegexFilterScreen.class, (screen, consumer) -> {
			for (Rect2i area : screen.getExtraAreas())
				consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
		});
	}

	/** The slot's 16x16 item area plus its 1 px frame, like EMI's own slot targets. */
	private static Bounds slotBounds(Rect2i area) {
		return new Bounds(area.getX() - 1, area.getY() - 1, area.getWidth() + 2, area.getHeight() + 2);
	}

	/** A dragged ingredient can be a whole tag: the first item of it is used. Fluids are ignored. */
	private static void drop(RegexFilterScreen screen, EmiIngredient dropped) {
		for (EmiStack stack : dropped.getEmiStacks()) {
			ItemStack item = stack.getItemStack();
			if (!item.isEmpty()) {
				screen.setSample(item);
				return;
			}
		}
	}
}
