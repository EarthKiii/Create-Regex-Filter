package com.earthkiii.createregexfilter.client;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;
import static com.earthkiii.createregexfilter.CreateRegexFilter.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.menu.GhostItemSubmitPacket;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.item.TooltipHelper;

import com.earthkiii.createregexfilter.CreateRegexFilter;
import com.earthkiii.createregexfilter.ModRegistry;
import com.earthkiii.createregexfilter.filter.RegexFilterConfig;
import com.earthkiii.createregexfilter.filter.RegexFilterConfig.Target;
import com.earthkiii.createregexfilter.filter.RegexFilterMenu;
import com.earthkiii.createregexfilter.lang.LocalizedNames;
import com.earthkiii.createregexfilter.lang.Translations;
import com.earthkiii.createregexfilter.network.ConfigureRegexFilterPacket;
import com.earthkiii.createregexfilter.regex.RegexCompiler;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.neoforge.network.PacketDistributor;

public class RegexFilterScreen extends AbstractSimiContainerScreen<RegexFilterMenu> {

	private static final ResourceLocation BACKGROUND = CreateRegexFilter.asResource("textures/gui/regex_filter.png");
	static final int BG_WIDTH = 248;
	static final int BG_HEIGHT = 79;

	private static final int TITLE_COLOR = 0x3B2750;
	private static final int TEXT_COLOR = 0xFFFFFF;
	private static final int INVALID_TEXT_COLOR = 0xFF8080;
	private static final int PASS_TINT = 0x7040C040;
	private static final int BLOCK_TINT = 0x70D04040;

	// Layout, relative to the top left corner of the background. See textures/gui/regex_filter.png.
	private static final int PATTERN_X = 43, PATTERN_Y = 28, PATTERN_WIDTH = 111;
	private static final int PATTERN_FIELD_X = 38, PATTERN_FIELD_Y = 22, PATTERN_FIELD_WIDTH = 121, FIELD_HEIGHT = 20;
	private static final int CASE_X = 162, CASE_Y = 23;
	private static final int LANGUAGE_X = 184, LANGUAGE_Y = 22, LANGUAGE_WIDTH = 43;
	private static final int BUTTON_Y = BG_HEIGHT - 24;
	private static final int STATUS_X = 98, STATUS_Y = BUTTON_Y + 5, STATUS_WIDTH = 84;

	private final List<String> languageCodes = new ArrayList<>();
	private EditBox patternBox;
	private SelectionScrollInput languageSelector;
	private Label languageLabel;
	private IconButton itemNameButton, modIdButton, allowButton, denyButton, caseButton;
	private List<Rect2i> extraAreas = List.of();
	private boolean languageInitialized;

	private RegexCompiler.Result compiled = RegexFilterConfig.DEFAULT.compile();
	private Preview preview = Preview.NONE;

	public RegexFilterScreen(RegexFilterMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
	}

	@Override
	protected void init() {
		setWindowSize(Math.max(BG_WIDTH, PLAYER_INVENTORY.getWidth()), BG_HEIGHT + 4 + PLAYER_INVENTORY.getHeight());
		setWindowOffset(-11, 7);
		super.init();

		int x = leftPos;
		int y = topPos;

		// A filter that was never configured starts in the player's own language.
		if (!languageInitialized) {
			languageInitialized = true;
			if (!menu.contentHolder.has(ModRegistry.REGEX_FILTER_CONFIG.get()))
				updateConfig(menu.config.withLanguage(ownLanguage()));
		}

		patternBox = new EditBox(font, x + PATTERN_X, y + PATTERN_Y, PATTERN_WIDTH, 9, Component.translatable(lang("gui.pattern")));
		patternBox.setBordered(false);
		patternBox.setMaxLength(RegexCompiler.MAX_PATTERN_LENGTH);
		patternBox.setValue(menu.config.pattern());
		patternBox.setHint(Component.translatable(lang("gui.pattern.hint")).withStyle(ChatFormatting.GRAY));
		patternBox.setResponder(pattern -> updateConfig(menu.config.withPattern(pattern)));
		addRenderableWidget(patternBox);

		caseButton = new IconButton(x + CASE_X, y + CASE_Y, textIcon("Aa"));
		caseButton.withCallback(() -> updateConfig(menu.config.withIgnoreCase(!menu.config.ignoreCase())));
		addRenderableWidget(caseButton);

		initLanguageSelector(x, y);

		itemNameButton = targetButton(x + 14, Target.ITEM_NAME, itemIcon(new ItemStack(Items.NAME_TAG)), "gui.target.item_name");
		modIdButton = targetButton(x + 32, Target.MOD_ID, textIcon("@"), "gui.target.mod_id");
		allowButton = modeButton(x + 56, false, AllIcons.I_WHITELIST, "gui.mode.allow");
		denyButton = modeButton(x + 74, true, AllIcons.I_BLACKLIST, "gui.mode.deny");

		IconButton resetButton = new IconButton(x + BG_WIDTH - 62, y + BUTTON_Y, AllIcons.I_TRASH);
		resetButton.withCallback(() -> {
			boolean hadLanguage = menu.config.target() == Target.ITEM_NAME;
			menu.clearContents();
			menu.sendClearPacket();
			// A mod ID filter has no language to keep: start over in the player's language, like a new filter.
			if (!hadLanguage)
				updateConfig(menu.config.withLanguage(ownLanguage()));
			patternBox.setValue("");
			setFocused(patternBox);
			refresh();
		});
		IconButton confirmButton = new IconButton(x + BG_WIDTH - 33, y + BUTTON_Y, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> minecraft.player.closeContainer());
		addRenderableWidgets(resetButton, confirmButton);

		extraAreas = ImmutableList.of(new Rect2i(x + BG_WIDTH, y + BG_HEIGHT - 40, 80, 48));

		setFocused(patternBox);
		refresh();
	}

	private void initLanguageSelector(int x, int y) {
		languageCodes.clear();
		List<Component> options = new ArrayList<>();
		for (Map.Entry<String, LanguageInfo> entry : minecraft.getLanguageManager().getLanguages().entrySet()) {
			languageCodes.add(entry.getKey());
			options.add(Component.literal(entry.getKey() + "  ").append(entry.getValue().toComponent()));
		}
		// A language this client does not know (e.g. set by a player with another resource pack).
		if (!languageCodes.contains(menu.config.language())) {
			languageCodes.add(menu.config.language());
			options.add(Component.literal(menu.config.language()));
		}

		languageLabel = new Label(x + LANGUAGE_X + 5, y + LANGUAGE_Y + 6, CommonComponents.EMPTY).withShadow();
		languageSelector = new SelectionScrollInput(x + LANGUAGE_X, y + LANGUAGE_Y, LANGUAGE_WIDTH, FIELD_HEIGHT);
		languageSelector.forOptions(options);
		languageSelector.titled(Component.translatable(lang("gui.language")));
		languageSelector.addHint(Component.translatable(lang("gui.language.hint")));
		languageSelector.withShiftStep(10);
		languageSelector.setState(languageCodes.indexOf(menu.config.language()));
		languageSelector.calling(index -> selectLanguage(languageCodes.get(index)));
		// Click: back to the player's own language.
		languageSelector.withCallback(() -> {
			String own = ownLanguage();
			int index = languageCodes.indexOf(own);
			if (index >= 0 && !own.equals(menu.config.language())) {
				languageSelector.setState(index);
				selectLanguage(own);
			}
		});
		addRenderableWidgets(languageSelector, languageLabel);
	}

	private void selectLanguage(String code) {
		Translations.request(code);
		updateConfig(menu.config.withLanguage(code));
	}

	private IconButton targetButton(int x, Target target, ScreenElement icon, String key) {
		IconButton button = tooltipButton(x, icon, key);
		button.withCallback(() -> {
			RegexFilterConfig config = menu.config.withTarget(target);
			// Mod ID filters clear their language; coming back to names starts in the player's language.
			if (target == Target.ITEM_NAME && menu.config.target() != Target.ITEM_NAME)
				config = config.withLanguage(ownLanguage());
			updateConfig(config);
		});
		return button;
	}

	private IconButton modeButton(int x, boolean deny, ScreenElement icon, String key) {
		IconButton button = tooltipButton(x, icon, key);
		button.withCallback(() -> updateConfig(menu.config.withDeny(deny)));
		return button;
	}

	private IconButton tooltipButton(int x, ScreenElement icon, String key) {
		IconButton button = new IconButton(x, topPos + BUTTON_Y, icon);
		button.setToolTip(Component.translatable(lang(key)));
		addRenderableWidget(button);
		return button;
	}

	private String ownLanguage() {
		return minecraft.getLanguageManager().getSelected();
	}

	/** Area of the test item slot in screen coordinates, for JEI and EMI drag and drop. */
	public Rect2i getSampleSlotArea() {
		return new Rect2i(leftPos + RegexFilterMenu.SAMPLE_SLOT_X, topPos + RegexFilterMenu.SAMPLE_SLOT_Y, 16, 16);
	}

	/** Sets the test item, as if it had been clicked into the slot. */
	public void setSample(ItemStack stack) {
		ItemStack sample = stack.copyWithCount(1);
		menu.ghostInventory.setStackInSlot(0, sample);
		CatnipServices.NETWORK.sendToServer(new GhostItemSubmitPacket(sample, 0));
		updatePreview();
	}

	private static ScreenElement itemIcon(ItemStack stack) {
		return (graphics, x, y) -> graphics.renderItem(stack, x, y);
	}

	private ScreenElement textIcon(String text) {
		return (graphics, x, y) -> graphics.drawString(font, text, x + (16 - font.width(text) + 1) / 2, y + 4, 0xFFFFFF, true);
	}

	private void updateConfig(RegexFilterConfig config) {
		if (config.equals(menu.config))
			return;
		menu.config = config;
		PacketDistributor.sendToServer(new ConfigureRegexFilterPacket(config));
		refresh();
	}

	/** Updates everything derived from the configuration. */
	private void refresh() {
		if (patternBox == null)
			return;
		RegexFilterConfig config = menu.config;
		compiled = config.compile();
		patternBox.setTextColor(compiled instanceof RegexCompiler.Valid ? TEXT_COLOR : INVALID_TEXT_COLOR);

		// Mod ID filters have no language: the box stays empty and cannot be scrolled.
		boolean usesLanguage = config.target() == Target.ITEM_NAME;
		languageSelector.active = usesLanguage;
		languageLabel.text = Component.literal(config.language());
		int languageIndex = languageCodes.indexOf(config.language());
		if (languageIndex >= 0)
			languageSelector.setState(languageIndex);

		itemNameButton.green = config.target() == Target.ITEM_NAME;
		modIdButton.green = config.target() == Target.MOD_ID;
		allowButton.green = !config.deny();
		denyButton.green = config.deny();
		caseButton.green = config.ignoreCase();
		updatePreview();
	}

	@Override
	protected void containerTick() {
		if (!ItemStack.matches(menu.player.getMainHandItem(), menu.contentHolder))
			menu.player.closeContainer();
		super.containerTick();
		updateButtonTooltips();
		updatePreview();
	}

	/** Adds "Hold [Shift]" and, when held, the description, like Create's filter screens. */
	private void updateButtonTooltips() {
		IconButton[] buttons = {itemNameButton, modIdButton, allowButton, denyButton, caseButton};
		String[] keys = {"gui.target.item_name", "gui.target.mod_id", "gui.mode.allow", "gui.mode.deny", "gui.ignore_case"};
		for (int i = 0; i < buttons.length; i++) {
			IconButton button = buttons[i];
			MutableComponent title = Component.translatable(lang(keys[i]));
			if (button == caseButton)
				title.append(": ").append(menu.config.ignoreCase() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
			button.setToolTip(title);
			button.getToolTip().add(TooltipHelper.holdShift(Palette.YELLOW, hasShiftDown()));
			if (hasShiftDown() && button.isHoveredOrFocused())
				button.getToolTip().addAll(TooltipHelper.cutTextComponent(
					Component.translatable(lang(keys[i] + ".description")), Palette.ALL_GRAY));
		}
	}

	private void updatePreview() {
		ItemStack sample = menu.getSample();
		if (sample.isEmpty()) {
			preview = Preview.NONE;
			return;
		}
		if (!(compiled instanceof RegexCompiler.Valid valid)) {
			preview = new Preview(PreviewState.INVALID, "");
			return;
		}

		RegexFilterConfig config = menu.config;
		String subject;
		if (config.target() == Target.MOD_ID) {
			subject = BuiltInRegistries.ITEM.getKey(sample.getItem()).getNamespace();
		} else {
			LocalizedNames names = Translations.request(config.language()).getNow(null);
			if (names == null) {
				preview = new Preview(PreviewState.LOADING, "");
				return;
			}
			subject = names.of(sample);
		}
		boolean passes = valid.regex().find(subject) != config.deny();
		preview = new Preview(passes ? PreviewState.PASS : PreviewState.BLOCK, subject);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		renderPlayerInventory(graphics, getLeftOfCentered(PLAYER_INVENTORY.getWidth()), topPos + BG_HEIGHT + 4);

		int x = leftPos;
		int y = topPos;
		graphics.blit(BACKGROUND, x, y, 0, 0, BG_WIDTH, BG_HEIGHT);
		graphics.drawString(font, title, x + (BG_WIDTH - 8) / 2 - font.width(title) / 2, y + 4, TITLE_COLOR, false);

		if (preview.state() == PreviewState.PASS || preview.state() == PreviewState.BLOCK) {
			int slotX = x + RegexFilterMenu.SAMPLE_SLOT_X;
			int slotY = y + RegexFilterMenu.SAMPLE_SLOT_Y;
			graphics.fill(slotX, slotY, slotX + 16, slotY + 16, preview.state() == PreviewState.PASS ? PASS_TINT : BLOCK_TINT);
		}

		String status = font.plainSubstrByWidth(statusLine().getString(), STATUS_WIDTH);
		graphics.drawString(font, status, x + STATUS_X, y + STATUS_Y, statusColor(), false);

		GuiGameElement.of(menu.contentHolder).<GuiGameElement.GuiRenderBuilder>at(x + BG_WIDTH + 8, y + BG_HEIGHT - 52, -200)
			.scale(4)
			.render(graphics);
	}

	private Component statusLine() {
		if (menu.config.pattern().isEmpty())
			return Component.translatable(lang("gui.status.empty"));
		if (compiled instanceof RegexCompiler.Invalid)
			return Component.translatable(lang("gui.status.invalid"));
		return Component.translatable(lang("gui.status.valid"));
	}

	/** Dark colors, for the light gray bar the status is drawn on. */
	private int statusColor() {
		if (menu.config.pattern().isEmpty())
			return 0x555555;
		return compiled instanceof RegexCompiler.Invalid ? 0xAA0000 : 0x00800B;
	}

	@Override
	protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderForeground(graphics, mouseX, mouseY, partialTicks);

		int x = leftPos;
		int y = topPos;
		boolean overStatus = isIn(mouseX, mouseY, x + STATUS_X, y + BUTTON_Y, STATUS_WIDTH, 18);
		boolean overField = isIn(mouseX, mouseY, x + PATTERN_FIELD_X, y + PATTERN_FIELD_Y, PATTERN_FIELD_WIDTH, FIELD_HEIGHT);
		if (overStatus || overField && compiled instanceof RegexCompiler.Invalid && !patternBox.isFocused())
			graphics.renderComponentTooltip(font, statusTooltip(), mouseX, mouseY);
	}

	private List<Component> statusTooltip() {
		List<Component> lines = new ArrayList<>();
		ChatFormatting color = menu.config.pattern().isEmpty() ? ChatFormatting.GRAY
			: compiled instanceof RegexCompiler.Invalid ? ChatFormatting.RED : ChatFormatting.GREEN;
		lines.add(statusLine().copy().withStyle(color));
		// Not wrapped with TooltipHelper: it reads '_' as highlight markers, and errors quote the pattern.
		if (compiled instanceof RegexCompiler.Invalid invalid)
			lines.add(Component.literal(invalid.error()).withStyle(ChatFormatting.GRAY));
		lines.add(CommonComponents.EMPTY);
		// Same two yellows as Create's item descriptions; _underscored_ words are highlighted.
		for (String help : List.of("gui.help.find", "gui.help.case", "gui.help.syntax", "gui.help.examples"))
			lines.addAll(TooltipHelper.cutTextComponent(Component.translatable(lang(help)), Palette.STANDARD_CREATE));
		lines.addAll(TooltipHelper.cutTextComponent(
			Component.translatable(lang("gui.help.length"), RegexCompiler.MAX_PATTERN_LENGTH), Palette.STANDARD_CREATE));
		return lines;
	}

	@Override
	protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.index == 36) {
			graphics.renderComponentTooltip(font, sampleTooltip(), mouseX, mouseY);
			return;
		}
		super.renderTooltip(graphics, mouseX, mouseY);
	}

	private List<Component> sampleTooltip() {
		List<Component> lines = new ArrayList<>();
		ItemStack sample = menu.getSample();
		if (sample.isEmpty()) {
			lines.add(Component.translatable(lang("gui.sample")).withStyle(ChatFormatting.GOLD));
			lines.addAll(TooltipHelper.cutTextComponent(Component.translatable(lang("gui.sample.description")), Palette.ALL_GRAY));
			return lines;
		}

		lines.add(sample.getHoverName());
		switch (preview.state()) {
			case LOADING -> lines.add(Component.translatable(lang("gui.sample.loading")).withStyle(ChatFormatting.GRAY));
			case INVALID -> lines.add(Component.translatable(lang("gui.status.invalid")).withStyle(ChatFormatting.RED));
			case PASS, BLOCK -> {
				String key = menu.config.target() == Target.MOD_ID ? "gui.sample.mod_id" : "gui.sample.name";
				lines.add(Component.translatable(lang(key), Component.literal(preview.subject()).withStyle(ChatFormatting.WHITE))
					.withStyle(ChatFormatting.GRAY));
				lines.add(preview.state() == PreviewState.PASS
					? Component.translatable(lang("gui.sample.passes")).withStyle(ChatFormatting.GREEN)
					: Component.translatable(lang("gui.sample.blocked")).withStyle(ChatFormatting.RED));
			}
			default -> {
			}
		}
		return lines;
	}

	private static boolean isIn(int mouseX, int mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
			setFocused(null);
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

	private enum PreviewState {
		NONE, LOADING, INVALID, PASS, BLOCK
	}

	private record Preview(PreviewState state, String subject) {
		static final Preview NONE = new Preview(PreviewState.NONE, "");
	}
}
