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
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.RepresentationHint;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.loader.CommentHandler;
import org.spongepowered.configurate.loader.CommentHandlers;
import org.spongepowered.configurate.loader.ParsingException;
import org.spongepowered.configurate.util.UnmodifiableCollections;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.BufferedReader;
import java.io.Writer;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;

/**
 * A loader for YAML-formatted configurations, using the SnakeYAML library for
 * parsing and generation.
 *
 * @since 4.0.0
 */
public final class YamlConfigurationLoader extends AbstractConfigurationLoader<CommentedConfigurationNode> {

    /**
     * The identifier for a YAML anchor that can be used to refer to the node
     * this hint is set on.
     *
     * @since 4.1.0
     */
    public static final RepresentationHint<String> ANCHOR_ID = RepresentationHint.of("configurate:anchor-id", String.class);

    /**
     * The YAML scalar style this node should attempt to use.
     *
     * <p>If the chosen scalar style would produce syntactically invalid YAML, a
     * valid one will replace it.</p>
     *
     * @since 4.1.0
     */
    public static final RepresentationHint<ScalarStyle> SCALAR_STYLE = RepresentationHint.of("configurate:scalar-style", ScalarStyle.class);

    /**
     * The YAML node style to use for collection nodes. A {@code null} value
     * will instruct the emitter to fall back to the
     * {@link Builder#nodeStyle()} setting.
     *
     * @since 4.1.0
     */
    public static final RepresentationHint<NodeStyle> NODE_STYLE = RepresentationHint.of("configurate:node-style", NodeStyle.class);

    /**
     * The explicitly specified tag for a node.
     *
     * <p>This can override default type conversion for a YAML document.</p>
     *
     * @since 4.1.0
     */
    public static final RepresentationHint<Tag> TAG = RepresentationHint.of("configurate:tag", Tag.class);

    /**
     * Whether comments will be enabled by default.
     *
     * <p>Comments will be introduced as an experimental feature, defaulting to
     * {@code false} at first, but changed to {@code true} in a later release.</p>
     */
    private static final boolean COMMENTS_DEFAULT = Boolean.parseBoolean(System.getProperty("configurate.yaml.commentsEnabled", "false"));

    /**
     * YAML native types from <a href="https://yaml.org/type/">YAML 1.1 Global tags</a>.
     *
     * <p>using SnakeYaml representation: https://bitbucket.org/asomov/snakeyaml/wiki/Documentation#markdown-header-yaml-tags-and-java-types
     */
    private static final Set<Class<?>> NATIVE_TYPES = UnmodifiableCollections.toSet(
            Boolean.class, Integer.class, Long.class, BigInteger.class, Double.class, // numeric
            byte[].class, String.class, Date.class, java.sql.Date.class, Timestamp.class); // complex types

    /**
     * Creates a new {@link YamlConfigurationLoader} builder.
     *
     * @return a new builder
     * @since 4.0.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link YamlConfigurationLoader}.
     *
     * @since 4.0.0
     */
    public static final class Builder extends AbstractConfigurationLoader.Builder<Builder, YamlConfigurationLoader> {
        private final DumperOptions options = new DumperOptions();
        private @Nullable NodeStyle style;
        private boolean enableComments = COMMENTS_DEFAULT;

        Builder() {
            indent(4);
            defaultOptions(o -> o.nativeTypes(NATIVE_TYPES));
        }

        /**
         * Sets the level of indentation the resultant loader should use.
         *
         * @param indent the indent level
         * @return this builder (for chaining)
         * @since 4.0.0
         */
        public Builder indent(final int indent) {
            this.options.setIndent(indent);
            return this;
        }

        /**
         * Gets the level of indentation to be used by the resultant loader.
         *
         * @return the indent level
         * @since 4.0.0
         */
        public int indent() {
            return this.options.getIndent();
        }

        /**
         * Sets the node style the built loader should use.
         *
         * <dl><dt>Flow</dt>
         * <dd>the compact, json-like representation.<br>
         * Example: <code>
         *     {value: [list, of, elements], another: value}
         * </code></dd>
         *
         * <dt>Block</dt>
         * <dd>expanded, traditional YAML<br>
         * Example: <code>
         *     value:
         *     - list
         *     - of
         *     - elements
         *     another: value
         * </code></dd>
         * </dl>
         *
         * <p>A {@code null} value will tell the loader to pick a value
         * automatically based on the contents of each non-scalar node.</p>
         *
         * @param style the node style to use
         * @return this builder (for chaining)
         * @since 4.0.0
         */
        public Builder nodeStyle(final @Nullable NodeStyle style) {
            this.style = style;
            return this;
        }

        /**
         * Gets the node style to be used by the resultant loader.
         *
         * @return the node style
         * @since 4.0.0
         */
        public @Nullable NodeStyle nodeStyle() {
            return this.style;
        }

        /**
         * Set whether comment handling is enabled on this loader.
         *
         * <p>Comment handling is available as an <em>experimental</em> feature
         * in 4.1.0. There may be edge cases where parsing or writing while
         * comments are enabled that can cause parse or emit errors, or badly
         * formatted output data.</p>
         *
         * <p>When comment handling is enabled, comments will be read from files
         * and written back to files where possible.</p>
         *
         * @param enableComments whether comment handling should be enabled
         * @return this builder (for chaining)
         * @since 4.1.0
         */
        public Builder commentsEnabled(final boolean enableComments) {
            this.enableComments = enableComments;
            return this;
        }

        /**
         * Get whether comment handling is enabled.
         *
         * @return whether comment handling is enabled
         * @since 4.1.0
         * @see #commentsEnabled(boolean) for details on comment handling
         */
        public boolean commentsEnabled() {
            return this.enableComments;
        }

        @Override
        public YamlConfigurationLoader build() {
            return new YamlConfigurationLoader(this);
        }
    }

    private final DumperOptions options;
    private final YamlVisitor visitor;
    private final @Nullable NodeStyle defaultNodeStyle;
    private final boolean enableComments;

    private YamlConfigurationLoader(final Builder builder) {
        super(builder, new CommentHandler[] {CommentHandlers.HASH});
        final DumperOptions opts = builder.options;
        opts.setDefaultFlowStyle(NodeStyle.asSnakeYaml(builder.nodeStyle()));
        this.defaultNodeStyle = builder.nodeStyle();
        this.enableComments = builder.commentsEnabled();
        this.options = opts;
        this.visitor = new YamlVisitor(new Resolver(), this.options, this.enableComments, Yaml11Tags.REPOSITORY);
    }

    @Override
    protected void loadInternal(final CommentedConfigurationNode node, final BufferedReader reader) throws ParsingException {
        // Match the superclass implementation, except we substitute our own scanner implementation
        final YamlParser parser = new YamlParser(new StreamReader(reader), Yaml11Tags.REPOSITORY);
        parser.singleDocumentStream(node);
    }

    @Override
    protected void saveInternal(final ConfigurationNode node, final Writer writer) throws ConfigurateException {
        final YamlVisitor.State state = new YamlVisitor.State(this.options, writer, this.defaultNodeStyle);
        node.visit(this.visitor, state);
    }

    @Override
    public CommentedConfigurationNode createNode(final ConfigurationOptions options) {
        return CommentedConfigurationNode.root(options);
    }

}
