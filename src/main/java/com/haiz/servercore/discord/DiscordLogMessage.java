package com.haiz.servercore.discord;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.util.List;
import java.util.Objects;

public record DiscordLogMessage(MessageEmbed embed, List<ActionRow> components) {
    public DiscordLogMessage {
        Objects.requireNonNull(embed, "embed");
        components = components == null ? List.of() : List.copyOf(components);
    }

    public static DiscordLogMessage of(MessageEmbed embed) {
        return new DiscordLogMessage(embed, List.of());
    }
}
