/*
 * Configurate
 * Copyright (C) zml and Configurate contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spongepowered.configurate.yaml;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNodeIntermediary;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationVisitor;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.events.CommentEvent;
import org.yaml.snakeyaml.events.DocumentEndEvent;
import org.yaml.snakeyaml.events.DocumentStartEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.ImplicitTuple;
import org.yaml.snakeyaml.events.MappingEndEvent;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.events.SequenceEndEvent;
import org.yaml.snakeyaml.events.SequenceStartEvent;
import org.yaml.snakeyaml.events.StreamEndEvent;
import org.yaml.snakeyaml.events.StreamStartEvent;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

final class YamlVisitor implements ConfigurationVisitor<YamlVisitor.State, Void, ConfigurateException> {

    private static final Pattern COMMENT_SPLIT = Pattern.compile("\r?\n");
    private static final CommentEvent COMMENT_BLANK_LINE = new CommentEvent(CommentType.BLOCK, "", null, null);
    private static final StreamStartEvent STREAM_START = new StreamStartEvent(null, null);
    private static final StreamEndEvent STREAM_END = new StreamEndEvent(null, null);
    private static final DocumentEndEvent DOCUMENT_END = new DocumentEndEvent(null, null, false);
    private static final SequenceEndEvent SEQUENCE_END = new SequenceEndEvent(null, null);
    private static final MappingEndEvent MAPPING_END = new MappingEndEvent(null, null);

    private final Resolver resolver;
    private final DumperOptions dumper;
    private final boolean enableComments;
    private final TagRepository tags;

    YamlVisitor(final Resolver resolver, final DumperOptions dumper, final boolean enableComments, final TagRepository tags) {
        this.resolver = resolver;
        this.dumper = dumper;
        this.enableComments = enableComments;
        this.tags = tags;
    }

    @Override
    public State newState() throws ConfigurateException {
        throw new ConfigurateException("States cannot be created as a writer must be provided");
    }

    @Override
    public void beginVisit(final ConfigurationNode node, final State state) throws ConfigurateException {
        state.start = node;
        state.emit(STREAM_START);
        state.emit(new DocumentStartEvent(null, null, this.dumper.isExplicitStart(),
                this.dumper.getVersion(), this.dumper.getTags()));
    }

    @Override
    public void enterNode(final ConfigurationNode node, final State state) throws ConfigurateException {
        if (node instanceof CommentedConfigurationNodeIntermediary<?> && this.enableComments) {
            final @Nullable String comment = ((CommentedConfigurationNodeIntermediary<?>) node).comment();
            if (comment != null) {
                for (final String line : COMMENT_SPLIT.split(comment)) {
                    if (line.isEmpty()) {
                        state.emit(COMMENT_BLANK_LINE);
                    } else {
                        if (!Character.isWhitespace(line.codePointAt(0))) {
                            state.emit(new CommentEvent(CommentType.BLOCK, " " + line, null, null));
                        } else {
                            state.emit(new CommentEvent(CommentType.BLOCK, line, null, null));
                        }
                    }
                }
            }
        }

        if (node != state.start && node.key() != null && node.parent().isMap()) { // emit key
            final String value = String.valueOf(node.key());
            final Tag implicit = this.resolver.resolve(NodeId.scalar, value, true);
            final Tag explicit = this.resolver.resolve(NodeId.scalar, value, true);
            final ImplicitTuple implicity = new ImplicitTuple(true, true);
            // final TagRepository.AnalyzedTag analysis = this.tags.analyze(node); // TODO: Handle tags properly
            state.emit(new ScalarEvent(null, implicit.getValue(), implicity, value, null, null,
                    DumperOptions.ScalarStyle.PLAIN));
        }
    }

    @Override
    public void enterMappingNode(final ConfigurationNode node, final State state) throws ConfigurateException {
        final Tag implicit = this.resolver.resolve(NodeId.mapping, null, true);
        state.emit(new MappingStartEvent(
            anchor(node),
            implicit.toString(),
            true,
            null,
            null,
            NodeStyle.asSnakeYaml(determineStyle(node, state))
        ));
    }

    @Override
    public void enterListNode(final ConfigurationNode node, final State state) throws ConfigurateException {
        final Tag implicit = this.resolver.resolve(NodeId.sequence, null, true);
        state.emit(new SequenceStartEvent(anchor(node), implicit.getValue(), true,
                null, null, NodeStyle.asSnakeYaml(determineStyle(node, state))));
    }

    @Override
    public void enterScalarNode(final ConfigurationNode node, final State state) throws ConfigurateException {
        final String value = String.valueOf(node.getString());
        final Tag implicit = this.resolver.resolve(NodeId.scalar, value, true);
        final Tag explicit = this.resolver.resolve(NodeId.scalar, value, true);
        final ImplicitTuple implicity = new ImplicitTuple(true, true);
        state.emit(new ScalarEvent(anchor(node), implicit.getValue(), implicity, value, null, null,
                ScalarStyle.asSnakeYaml(node.hint(YamlConfigurationLoader.SCALAR_STYLE))));
    }

    // TODO: emit alias events for enterReferenceNode

    @Override
    public void exitMappingNode(final ConfigurationNode node, final State state) throws ConfigurateException {
        state.emit(MAPPING_END);
    }

    @Override
    public void exitListNode(final ConfigurationNode node, final State state) throws ConfigurateException {
        state.emit(SEQUENCE_END);
    }

    @Override
    public Void endVisit(final State state) throws ConfigurateException {
        state.emit(DOCUMENT_END);
        state.emit(STREAM_END);
        return null;
    }

    private @Nullable NodeStyle determineStyle(final ConfigurationNode node, final State state) {
        // some basic rules:
        // - if a node has any children with comments, convert it to block style
        // - when the default style is `AUTO` and `flowLevel` == 0,
        final @Nullable NodeStyle style = node.hint(YamlConfigurationLoader.NODE_STYLE);
        return style == null ? state.defaultStyle : style;
    }

    private @Nullable String anchor(final ConfigurationNode node) {
        return node.ownHint(YamlConfigurationLoader.ANCHOR_ID);
    }

    static class State {
        private final Emitter emit;
        @Nullable ConfigurationNode start;
        final @Nullable NodeStyle defaultStyle;

        State(final DumperOptions options, final Writer writer, final @Nullable NodeStyle defaultStyle) {
            this.emit = new Emitter(writer, options);
            this.defaultStyle = defaultStyle;
        }

        public void emit(final Event event) throws ConfigurateException {
            try {
                this.emit.emit(event);
            } catch (final YAMLException | IOException ex) {
                throw new ConfigurateException(ex);
            }
        }
    }

}
