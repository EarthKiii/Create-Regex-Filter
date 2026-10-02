package com.earthkiii.createregexfilter.compat.jei;

import java.util.List;

import com.earthkiii.createregexfilter.CreateRegexFilter;
import com.earthkiii.createregexfilter.client.RegexFilterScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Lets items be dragged from JEI into the filter's test slot. Only loaded by JEI, when it is installed.
 * (JEI exclusion areas are already handled by Create for all its container screens, this one included.)
 */
@JeiPlugin
public class RegexFilterJeiPlugin implements IModPlugin {

	private static final ResourceLocation ID = CreateRegexFilter.asResource("jei_plugin");

	@Override
	public ResourceLocation getPluginUid() {
		return ID;
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addGhostIngredientHandler(RegexFilterScreen.class, new SampleSlotHandler());
	}

	private static class SampleSlotHandler implements IGhostIngredientHandler<RegexFilterScreen> {

		@Override
		public <I> List<Target<I>> getTargetsTyped(RegexFilterScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
			if (ingredient.getType() != VanillaTypes.ITEM_STACK)
				return List.of();

			Rect2i area = screen.getSampleSlotArea();
			return List.of(new Target<>() {
				@Override
				public Rect2i getArea() {
					return area;
				}

				@Override
				public void accept(I dropped) {
					screen.setSample((ItemStack) dropped);
				}
			});
		}

		@Override
		public void onComplete() {
		}
	}
}
