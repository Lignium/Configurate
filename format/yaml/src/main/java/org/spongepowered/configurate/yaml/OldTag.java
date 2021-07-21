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

import com.google.auto.value.AutoValue;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * A YAML 1.1/1.2 tag
 *
 * @apiNote Design based on §3.2.1.1 of the YAML 1.1 spec
 * @since 4.2.0
 */
@AutoValue
abstract class OldTag {

    // Standard tags:
    // str (plain ? unresolved), seq, map

    // types:
    // scalar: associated Pattern for matching

    // actions:
    // go from Event -> node, potentially increasing depth
    // given a Node, emit a series of events

    // Non-scalar custom tags probably won't be exposed to API?

    /**
     * Create a new builder for a {@link OldTag}.
     *
     * @return a new builder
     * @since 4.1.0
     */
    public static OldTag.Builder builder() {
        return new AutoValue_Tag.Builder();
    }

    OldTag() {}

    /**
     * The canonical tag URI.
     *
     * @return tag uri, with `tag:` schema
     * @since 4.1.0
     */
    public abstract URI uri();

    /**
     * The native type that maps to this tag.
     *
     * @return native type for tag
     * @since 4.1.0
     */
    public abstract Type nativeType();

    /**
     * Pattern to test scalar values against when resolving this tag.
     *
     * @return match pattern
     * @apiNote See §3.3.2 of YAML 1.1 spec
     * @since 4.1.0
     */
    public abstract Pattern targetPattern();

    /**
     * Whether this tag is a global tag with a full namespace or a local one.
     *
     * @return if this is a global tag
     * @since 4.1.0
     */
    public final boolean global() {
        return uri().getScheme().equals("tag");
    }

    /**
     * A builder for {@link OldTag Tags}.
     *
     * @since 4.1.0
     */
    @AutoValue.Builder
    public abstract static class Builder {

        /**
         * Set the URI used to refer to the tag.
         *
         * @param url canonical tag URI
         * @return this builder
         * @since 4.1.0
         */
        public abstract Builder uri(URI url);

        /**
         * Set the URI used to refer to the tag, parsing a new URL from
         * the argument.
         *
         * @param tagUrl canonical tag URI
         * @return this builder
         * @since 4.1.0
         */
        public final Builder uri(final String tagUrl) {
            try {
                if (tagUrl.startsWith("!")) {
                    return this.uri(new URI(tagUrl.substring(1)));
                } else {
                    return this.uri(new URI(tagUrl));
                }
            } catch (final URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * The Java type that will be used to represent this value in the node
         * structure.
         *
         * @param type type for the value
         * @return this builder
         * @since 4.1.0
         */
        public abstract Builder nativeType(Type type);

        /**
         * Pattern to match an undefined scalar string to this tag as an
         * <em>implicit tag</em>.
         *
         * @param targetPattern pattern to match
         * @return this builder
         * @since 4.1.0
         */
        public abstract Builder targetPattern(Pattern targetPattern);

        /**
         * Create a new tag from the provided parameters.
         *
         * @return a new tag
         * @since 4.1.0
         */
        public abstract OldTag build();

    }

}
