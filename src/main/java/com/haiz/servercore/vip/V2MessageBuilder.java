package com.haiz.servercore.vip;

import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitário para construir mensagens usando Discord Components v2.
 *
 * Components v2 substitui embeds por componentes estruturados:
 * - Container (type 17): agrupa visualmente componentes
 * - Text Display (type 10): texto markdown
 * - Section (type 9): texto com accessory (thumbnail/botão)
 * - Separator (type 14): espaçamento vertical
 * - Media Gallery (type 12): imagens
 * - Action Row (type 1): botões/selects
 *
 * Flag IS_COMPONENTS_V2 = 1 << 15 = 32768
 */
public final class V2MessageBuilder {

    private static final int FLAG_COMPONENTS_V2 = 1 << 15;
    private static final int TYPE_ACTION_ROW = 1;
    private static final int TYPE_BUTTON = 2;
    private static final int TYPE_SECTION = 9;
    private static final int TYPE_TEXT_DISPLAY = 10;
    private static final int TYPE_THUMBNAIL = 11;
    private static final int TYPE_MEDIA_GALLERY = 12;
    private static final int TYPE_SEPARATOR = 14;
    private static final int TYPE_CONTAINER = 17;

    private final List<DataObject> components = new ArrayList<>();
    private final List<DataObject> attachments = new ArrayList<>();

    private V2MessageBuilder() {}

    public static V2MessageBuilder create() {
        return new V2MessageBuilder();
    }

    // ── Container ─────────────────────────────────────────────────────────

    /**
     * Adiciona um container com cor de accent e filhos.
     */
    public V2MessageBuilder addContainer(int accentColor, boolean spoiler, ChildBuilder children) {
        DataObject container = DataObject.empty()
                .put("type", TYPE_CONTAINER)
                .put("accent_color", accentColor);

        List<DataObject> childComponents = new ArrayList<>();
        children.build(childComponents);
        container.put("components", DataArray.fromCollection(childComponents));

        if (spoiler) {
            container.put("spoiler", true);
        }

        components.add(container);
        return this;
    }

    public V2MessageBuilder addContainer(int accentColor, ChildBuilder children) {
        return addContainer(accentColor, false, children);
    }

    // ── Text Display ──────────────────────────────────────────────────────

    public V2MessageBuilder addText(String markdown) {
        components.add(DataObject.empty()
                .put("type", TYPE_TEXT_DISPLAY)
                .put("content", markdown));
        return this;
    }

    // ── Section ───────────────────────────────────────────────────────────

    /**
     * Adiciona uma Section com texto e um accessory (Thumbnail ou Button).
     */
    public V2MessageBuilder addSection(String markdown, DataObject accessory) {
        DataObject section = DataObject.empty()
                .put("type", TYPE_SECTION)
                .put("components", DataArray.fromCollection(List.of(
                        DataObject.empty()
                                .put("type", TYPE_TEXT_DISPLAY)
                                .put("content", markdown)
                )))
                .put("accessory", accessory);
        components.add(section);
        return this;
    }

    // ── Separator ─────────────────────────────────────────────────────────

    public static DataObject separator() {
        return separator(false);
    }

    public static DataObject separator(boolean large) {
        return DataObject.empty()
                .put("type", TYPE_SEPARATOR)
                .put("spacing", large ? 2 : 1)
                .put("divider", true);
    }

    public V2MessageBuilder addSeparator() {
        components.add(separator());
        return this;
    }

    public V2MessageBuilder addSeparator(boolean large) {
        components.add(separator(large));
        return this;
    }

    // ── Media Gallery ─────────────────────────────────────────────────────

    public V2MessageBuilder addMedia(String url) {
        return addMedia(url, null);
    }

    public V2MessageBuilder addMedia(String url, String description) {
        DataObject item = DataObject.empty()
                .put("url", url);
        if (description != null) {
            item.put("description", description);
        }
        components.add(DataObject.empty()
                .put("type", TYPE_MEDIA_GALLERY)
                .put("items", DataArray.fromCollection(List.of(item))));
        return this;
    }

    // ── Thumbnail ─────────────────────────────────────────────────────────

    public static DataObject thumbnail(String url) {
        return thumbnail(url, null);
    }

    public static DataObject thumbnail(String url, String description) {
        DataObject thumb = DataObject.empty()
                .put("type", TYPE_THUMBNAIL)
                .put("media", DataObject.empty().put("url", url));
        if (description != null) {
            thumb.put("description", description);
        }
        return thumb;
    }

    // ── Button ────────────────────────────────────────────────────────────

    public static DataObject button(int style, String customId, String label) {
        DataObject btn = DataObject.empty()
                .put("type", TYPE_BUTTON)
                .put("style", style)
                .put("label", label);
        if (customId != null) {
            btn.put("custom_id", customId);
        }
        return btn;
    }

    public static DataObject buttonLink(String url, String label) {
        return DataObject.empty()
                .put("type", TYPE_BUTTON)
                .put("style", 5)
                .put("label", label)
                .put("url", url);
    }

    public static DataObject buttonDisabled(int style, String label) {
        return DataObject.empty()
                .put("type", TYPE_BUTTON)
                .put("style", style)
                .put("label", label)
                .put("disabled", true);
    }

    // ── Action Row (para botões dentro de container) ───────────────────────

    public static DataObject actionRow(DataObject... buttons) {
        DataArray arr = DataArray.empty();
        for (DataObject btn : buttons) {
            arr.add(btn);
        }
        return DataObject.empty()
                .put("type", TYPE_ACTION_ROW)
                .put("components", arr);
    }

    // ── String Select ─────────────────────────────────────────────────────

    public static DataObject stringSelect(String customId, String placeholder, List<DataObject> options) {
        DataArray opts = DataArray.empty();
        for (DataObject opt : options) {
            opts.add(opt);
        }
        return DataObject.empty()
                .put("type", 3)
                .put("custom_id", customId)
                .put("placeholder", placeholder)
                .put("options", opts);
    }

    public static DataObject selectOption(String label, String value, String description) {
        return selectOption(label, value, description, null, null);
    }

    public static DataObject selectOption(String label, String value, String description,
                                           String emojiName, Long emojiId) {
        DataObject opt = DataObject.empty()
                .put("label", label)
                .put("value", value);
        if (description != null) opt.put("description", description);
        if (emojiName != null) {
            DataObject emoji = DataObject.empty().put("name", emojiName);
            if (emojiId != null) emoji.put("id", Long.toUnsignedString(emojiId));
            opt.put("emoji", emoji);
        }
        return opt;
    }

    // ── Build ─────────────────────────────────────────────────────────────

    /**
     * Constrói o DataObject da mensagem para envio via REST API.
     * Retorna o JSON que deve ser enviado como body do request.
     */
    public DataObject build() {
        DataObject message = DataObject.empty()
                .put("flags", FLAG_COMPONENTS_V2)
                .put("components", DataArray.fromCollection(components));
        return message;
    }

    /**
     * Constrói apenas o array de components (para uso em contextos diferentes).
     */
    public DataArray buildComponents() {
        return DataArray.fromCollection(components);
    }

    /**
     * Retorna o número de componentes adicionados.
     */
    public int componentCount() {
        return components.size();
    }

    // ── Functional interface para construir filhos de um container ─────────

    @FunctionalInterface
    public interface ChildBuilder {
        void build(List<DataObject> components);
    }
}
