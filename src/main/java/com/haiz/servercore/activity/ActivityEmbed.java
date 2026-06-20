package com.haiz.servercore.activity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ActivityEmbed {
    private final String title;
    private final String description;
    private final int color;
    private final List<Field> fields = new ArrayList<>();
    private String footer = "Haiz Activity | Minecraft Stats";

    public ActivityEmbed(String title, String description, int color) {
        this.title = title;
        this.description = description;
        this.color = color;
    }

    public ActivityEmbed addField(String name, String value, boolean inline) {
        fields.add(new Field(name, value, inline));
        return this;
    }

    public ActivityEmbed footer(String footer) {
        this.footer = footer;
        return this;
    }

    public String toJson() {
        StringBuilder json = new StringBuilder("{\"embeds\":[{");
        json.append("\"title\":\"").append(escape(title)).append("\",");
        json.append("\"description\":\"").append(escape(description)).append("\",");
        json.append("\"color\":").append(color).append(',');
        json.append("\"fields\":[");
        for (int index = 0; index < fields.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            Field field = fields.get(index);
            json.append("{\"name\":\"").append(escape(field.name())).append("\",")
                    .append("\"value\":\"").append(escape(field.value())).append("\",")
                    .append("\"inline\":").append(field.inline()).append('}');
        }
        json.append("],\"footer\":{\"text\":\"").append(escape(footer)).append("\"},");
        json.append("\"timestamp\":\"").append(Instant.now()).append("\"}]}");
        return json.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 32) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private record Field(String name, String value, boolean inline) {
    }
}
