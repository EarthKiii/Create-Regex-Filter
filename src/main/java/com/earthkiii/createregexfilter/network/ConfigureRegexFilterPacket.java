package com.earthkiii.createregexfilter.network;

import com.earthkiii.createregexfilter.CreateRegexFilter;
import com.earthkiii.createregexfilter.filter.RegexFilterConfig;
import com.earthkiii.createregexfilter.filter.RegexFilterMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent by the filter screen whenever a setting changes. It carries the whole configuration, so
 * packets are idempotent. The server stores it in the open menu, which writes it to the item when
 * the menu closes (like Create's own filters).
 */
public record ConfigureRegexFilterPacket(RegexFilterConfig config) implements CustomPacketPayload {

	public static final Type<ConfigureRegexFilterPacket> TYPE = new Type<>(CreateRegexFilter.asResource("configure_regex_filter"));

	public static final StreamCodec<ByteBuf, ConfigureRegexFilterPacket> STREAM_CODEC =
		RegexFilterConfig.STREAM_CODEC.map(ConfigureRegexFilterPacket::new, ConfigureRegexFilterPacket::config);

	public static void register(RegisterPayloadHandlersEvent event) {
		event.registrar("1").playToServer(TYPE, STREAM_CODEC, ConfigureRegexFilterPacket::handle);
	}

	private static void handle(ConfigureRegexFilterPacket packet, IPayloadContext context) {
		if (context.player().containerMenu instanceof RegexFilterMenu menu)
			menu.applyConfig(packet.config());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
