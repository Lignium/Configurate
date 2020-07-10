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
package org.spongepowered.configurate.objectmapping;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.ConfigSerializable;
import org.spongepowered.configurate.serialize.Scalars;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.util.UnmodifiableCollections;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class TypeSerializersTest {

    private <T> TypeSerializer<T> getSerializer(final TypeToken<T> type) {
        final @Nullable TypeSerializer<T> ret = TypeSerializerCollection.defaults().get(type);
        assertNotNull(ret);
        return ret;
    }

    @Test
    public void testStringSerializer() throws ObjectMappingException {
        final TypeToken<String> stringType = TypeToken.of(String.class);
        final TypeSerializer<String> stringSerializer = getSerializer(stringType);
        final BasicConfigurationNode node = BasicConfigurationNode.root().setValue("foobar");

        assertEquals("foobar", stringSerializer.deserialize(stringType, node));
        stringSerializer.serialize(stringType, "foobarbaz", node);
        assertEquals("foobarbaz", node.getString());
    }

    @Test
    public void testAsBoolean() throws Exception {
        final boolean actual = true;
        final String[] trueEvaluating = new String[] {"true", "yes", "1", "t", "y"};
        final String[] falseEvaluating = new String[] {"false", "no", "0", "f", "n"};
        assertEquals(actual, Scalars.BOOLEAN.deserialize(actual));
        for (String val : trueEvaluating) {
            assertEquals(true, Scalars.BOOLEAN.deserialize(val));
        }

        for (String val : falseEvaluating) {
            assertEquals(false, Scalars.BOOLEAN.deserialize(val));
        }
    }

    @Test
    public void testBooleanSerializer() throws ObjectMappingException {
        final TypeToken<Boolean> booleanType = TypeToken.of(Boolean.class);

        final TypeSerializer<Boolean> booleanSerializer = getSerializer(booleanType);
        final BasicConfigurationNode node = BasicConfigurationNode.root();
        node.getNode("direct").setValue(true);
        node.getNode("fromstring").setValue("true");

        assertEquals(true, booleanSerializer.deserialize(booleanType, node.getNode("direct")));
        assertEquals(true, booleanSerializer.deserialize(booleanType, node.getNode("fromstring")));
    }

    private enum TestEnum {
        FIRST,
        SECOND,
        Third,
        third
    }

    @Test
    public void testEnumValueSerializer() throws ObjectMappingException {
        final TypeToken<TestEnum> enumType = TypeToken.of(TestEnum.class);

        final TypeSerializer<TestEnum> enumSerializer = getSerializer(enumType);

        final BasicConfigurationNode node = BasicConfigurationNode.root();
        node.getNode("present_val").setValue("first");
        node.getNode("another_present_val").setValue("SECOND");
        node.getNode("casematters_val").setValue("tHiRd");
        node.getNode("casematters_val_lowercase").setValue("third");
        node.getNode("invalid_val").setValue("3rd");

        assertEquals(TestEnum.FIRST, enumSerializer.deserialize(enumType, node.getNode("present_val")));
        assertEquals(TestEnum.SECOND, enumSerializer.deserialize(enumType, node.getNode("another_present_val")));
        assertEquals(TestEnum.Third, enumSerializer.deserialize(enumType, node.getNode("casematters_val")));
        assertEquals(TestEnum.third, enumSerializer.deserialize(enumType, node.getNode("casematters_val_lowercase")));
        Assertions.assertThrows(ObjectMappingException.class, () -> {
            enumSerializer.deserialize(enumType, node.getNode("invalid_val"));
        });
    }

    @Test
    public void testListSerializer() throws ObjectMappingException {
        final TypeToken<List<String>> stringListType = new TypeToken<List<String>>() {};
        final TypeSerializer<List<String>> stringListSerializer = getSerializer(stringListType);
        final BasicConfigurationNode value = BasicConfigurationNode.root();
        value.appendListNode().setValue("hi");
        value.appendListNode().setValue("there");
        value.appendListNode().setValue("beautiful");
        value.appendListNode().setValue("people");

        assertEquals(Arrays.asList("hi", "there", "beautiful", "people"), stringListSerializer.deserialize(stringListType, value));
        value.setValue(null);

        stringListSerializer.serialize(stringListType, Arrays.asList("hi", "there", "lame", "people"), value);
        assertEquals("hi", value.getNode(0).getString());
        assertEquals("there", value.getNode(1).getString());
        assertEquals("lame", value.getNode(2).getString());
        assertEquals("people", value.getNode(3).getString());
    }

    @Test
    public void testSetSerializer() throws ObjectMappingException {
        final TypeToken<Set<String>> stringListType = new TypeToken<Set<String>>() {};
        final TypeSerializer<Set<String>> stringListSerializer = getSerializer(stringListType);
        final BasicConfigurationNode value = BasicConfigurationNode.root();
        value.appendListNode().setValue("hi");
        value.appendListNode().setValue("there");
        value.appendListNode().setValue("beautiful");
        value.appendListNode().setValue("people");

        assertEquals(UnmodifiableCollections.toSet("hi", "there", "beautiful", "people"), stringListSerializer.deserialize(stringListType, value));
        value.setValue(null);

        stringListSerializer.serialize(stringListType, UnmodifiableCollections.toSet("hi", "there", "lame", "people"), value);
        assertEquals("hi", value.getNode(0).getString());
        assertEquals("there", value.getNode(1).getString());
        assertEquals("lame", value.getNode(2).getString());
        assertEquals("people", value.getNode(3).getString());
    }

    @Test
    public void testListSerializerPreservesEmptyList() throws ObjectMappingException {
        final TypeToken<List<String>> listStringType = new TypeToken<List<String>>() {};
        final TypeSerializer<List<String>> listStringSerializer =
                getSerializer(listStringType);

        final BasicConfigurationNode value = BasicConfigurationNode.root();

        listStringSerializer.serialize(listStringType, Collections.emptyList(), value);

        assertTrue(value.isList());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testListRawTypes() {
        final TypeToken<List> rawType = TypeToken.of(List.class);
        final TypeSerializer<List> serial = getSerializer(rawType);

        final BasicConfigurationNode value = BasicConfigurationNode.root();

        value.appendListNode().setValue(1);
        value.appendListNode().setValue("dog");
        value.appendListNode().setValue(2.4);

        Assertions.assertTrue(Assertions.assertThrows(Exception.class, () -> {
            serial.deserialize(rawType, value);
        }).getMessage().startsWith("Raw types"));
    }

    @Test
    public void testMapSerializer() throws ObjectMappingException {
        final TypeToken<Map<String, Integer>> mapStringIntType = new TypeToken<Map<String, Integer>>() {};
        final TypeSerializer<Map<String, Integer>> mapStringIntSerializer =
                getSerializer(mapStringIntType);

        final BasicConfigurationNode value = BasicConfigurationNode.root();
        value.getNode("fish").setValue(5);
        value.getNode("bugs").setValue("124880");
        value.getNode("time").setValue("-1");

        final Map<String, Integer> expectedValues = ImmutableMap.of("fish", 5, "bugs", 124880, "time", -1);

        assertEquals(expectedValues, mapStringIntSerializer.deserialize(mapStringIntType, value));

        value.setValue(null);

        mapStringIntSerializer.serialize(mapStringIntType, expectedValues, value);
        assertEquals(5, value.getNode("fish").getInt());
        assertEquals(124880, value.getNode("bugs").getInt());
        assertEquals(-1, value.getNode("time").getInt());
    }

    @Test
    public void testInvalidMapValueTypes() throws ObjectMappingException {
        final TypeToken<Map<TestEnum, Integer>> mapTestEnumIntType = new TypeToken<Map<TestEnum, Integer>>() {};
        final TypeSerializer<Map<TestEnum, Integer>> mapTestEnumIntSerializer =
                getSerializer(mapTestEnumIntType);

        final BasicConfigurationNode value = BasicConfigurationNode.root();
        value.getNode("FIRST").setValue(5);
        value.getNode("SECOND").setValue(8);

        final @Nullable Map<TestEnum, Integer> des = mapTestEnumIntSerializer.deserialize(mapTestEnumIntType, value);
        final BasicConfigurationNode serialVal = BasicConfigurationNode.root(ConfigurationOptions.defaults()
                .withNativeTypes(UnmodifiableCollections.toSet(String.class, Integer.class)));
        mapTestEnumIntSerializer.serialize(mapTestEnumIntType, des, serialVal);
        assertEquals(value.getValue(), serialVal.getValue());
        //assertEquals(value, serialVal);
    }

    @Test
    public void testMapSerializerRemovesDeletedKeys() throws ObjectMappingException {
        final TypeToken<Map<String, Integer>> mapStringIntType = new TypeToken<Map<String, Integer>>() {};
        final TypeSerializer<Map<String, Integer>> mapStringIntSerializer = getSerializer(mapStringIntType);

        final BasicConfigurationNode value = BasicConfigurationNode.root();
        value.getNode("fish").setValue(5);
        value.getNode("bugs").setValue("124880");
        value.getNode("time").setValue("-1");

        @SuppressWarnings("unchecked")
        final @Nullable Map<String, Integer> deserialized = mapStringIntSerializer.deserialize(mapStringIntType, value);
        requireNonNull(deserialized).remove("fish");

        mapStringIntSerializer.serialize(mapStringIntType, deserialized, value);
        assertTrue(value.getNode("fish").isVirtual());
        assertFalse(value.getNode("bugs").isVirtual());
    }

    @Test
    public void testMapSerializerPreservesEmptyMap() throws ObjectMappingException {
        final TypeToken<Map<String, Integer>> mapStringIntType = new TypeToken<Map<String, Integer>>() {};
        final TypeSerializer<Map<String, Integer>> mapStringIntSerializer =
                getSerializer(mapStringIntType);

        final BasicConfigurationNode value = BasicConfigurationNode.root();

        mapStringIntSerializer.serialize(mapStringIntType, Collections.emptyMap(), value);

        assertTrue(value.isMap());
    }

    @Test
    public void testMapSerializerPreservesChildComments() throws ObjectMappingException {
        final TypeToken<Map<String, Integer>> mapStringIntType = new TypeToken<Map<String, Integer>>() {};
        final TypeSerializer<Map<String, Integer>> mapStringIntSerializer =
                getSerializer(mapStringIntType);

        final CommentedConfigurationNode commentNode = CommentedConfigurationNode.root();

        commentNode.getNode("hi").setComment("test").setValue(3);

        mapStringIntSerializer.serialize(mapStringIntType, ImmutableMap.of("hi", 5, "no", 2), commentNode);

        assertEquals(5, commentNode.getNode("hi").getValue());
        assertEquals("test", commentNode.getNode("hi").getComment());

    }

    @ConfigSerializable
    private static class TestObject {
        @Setting("int") private int value;
        @Setting private String name;
    }

    @Test
    public void testAnnotatedObjectSerializer() throws ObjectMappingException {
        final TypeToken<TestObject> testNodeType = TypeToken.of(TestObject.class);
        final TypeSerializer<TestObject> testObjectSerializer = getSerializer(testNodeType);
        final BasicConfigurationNode node = BasicConfigurationNode.root();
        node.getNode("int").setValue("42");
        node.getNode("name").setValue("Bob");

        final TestObject object = testObjectSerializer.deserialize(testNodeType, node);
        assertEquals(42, object.value);
        assertEquals("Bob", object.name);
    }

    @Test
    public void testUriSerializer() throws ObjectMappingException {
        final TypeToken<URI> uriType = TypeToken.of(URI.class);
        final TypeSerializer<URI> uriSerializer = getSerializer(uriType);

        final String uriString = "http://google.com";
        final URI testUri = URI.create(uriString);

        final BasicConfigurationNode node = BasicConfigurationNode.root(ConfigurationOptions.defaults()
                .withNativeTypes(UnmodifiableCollections.toSet(String.class, Integer.class)))
                .setValue(uriString);
        assertEquals(testUri, uriSerializer.deserialize(uriType, node));

        uriSerializer.serialize(uriType, testUri, node);
        assertEquals(uriString, node.getValue());
    }

    @Test
    public void testUrlSerializer() throws ObjectMappingException, MalformedURLException {
        final TypeToken<URL> urlType = TypeToken.of(URL.class);
        final TypeSerializer<URL> urlSerializer = getSerializer(urlType);

        final String urlString = "http://google.com";
        final URL testUrl = new URL(urlString);

        final BasicConfigurationNode node = BasicConfigurationNode.root(ConfigurationOptions.defaults()
                .withNativeTypes(UnmodifiableCollections.toSet(String.class, Integer.class)))
                .setValue(urlString);
        assertEquals(testUrl, urlSerializer.deserialize(urlType, node));

        urlSerializer.serialize(urlType, testUrl, node);
        assertEquals(urlString, node.getValue());
    }

    @Test
    public void testUuidSerializer() throws ObjectMappingException {
        final TypeToken<UUID> uuidType = TypeToken.of(UUID.class);
        final TypeSerializer<UUID> uuidSerializer = getSerializer(uuidType);

        final UUID testUuid = UUID.randomUUID();

        final BasicConfigurationNode serializeTo = BasicConfigurationNode.root(ConfigurationOptions.defaults()
                .withNativeTypes(UnmodifiableCollections.toSet(String.class, Integer.class)));
        uuidSerializer.serialize(uuidType, testUuid, serializeTo);
        assertEquals(testUuid.toString(), serializeTo.getValue());

        assertEquals(testUuid, uuidSerializer.deserialize(uuidType, serializeTo));

    }

    @Test
    public void testPatternSerializer() throws ObjectMappingException {
        final TypeToken<Pattern> patternType = TypeToken.of(Pattern.class);
        final TypeSerializer<Pattern> patternSerializer = getSerializer(patternType);

        final Pattern testPattern = Pattern.compile("(na )+batman");
        final BasicConfigurationNode serializeTo = BasicConfigurationNode.root(ConfigurationOptions.defaults()
                .withNativeTypes(UnmodifiableCollections.toSet(String.class, Integer.class)));
        patternSerializer.serialize(patternType, testPattern, serializeTo);
        assertEquals("(na )+batman", serializeTo.getValue());
        assertEquals(testPattern.pattern(), patternSerializer.deserialize(patternType, serializeTo).pattern());
    }

    @Test
    public void testCharSerializer() throws ObjectMappingException {
        final TypeToken<Character> charType = TypeToken.of(Character.class);
        final TypeSerializer<Character> charSerializer = getSerializer(charType);

        final BasicConfigurationNode serializeTo = BasicConfigurationNode.root();

        serializeTo.setValue("e");
        assertEquals(Character.valueOf('e'), charSerializer.deserialize(charType, serializeTo));

        serializeTo.setValue('P');
        assertEquals(Character.valueOf('P'), charSerializer.deserialize(charType, serializeTo));

        serializeTo.setValue(0x2a);
        assertEquals(Character.valueOf('*'), charSerializer.deserialize(charType, serializeTo));

        charSerializer.serialize(charType, 'z', serializeTo);
        assertEquals('z', serializeTo.getValue());
    }

    @Test
    public void testArraySerializer() throws ObjectMappingException {
        final TypeToken<String[]> arrayType = TypeToken.of(String[].class);
        final TypeSerializer<String[]> arraySerializer = getSerializer(arrayType);

        final String[] testArray = new String[] {"hello", "world"};
        final BasicConfigurationNode serializeTo = BasicConfigurationNode.root();
        arraySerializer.serialize(arrayType, testArray, serializeTo);
        assertEquals(Arrays.asList("hello", "world"), serializeTo.getValue());
        assertArrayEquals(testArray, arraySerializer.deserialize(arrayType, serializeTo));
    }

    @Test
    public void testArraySerializerBooleanPrimitive() throws ObjectMappingException {
        final TypeToken<boolean[]> booleanArrayType = TypeToken.of(boolean[].class);
        final TypeSerializer<boolean[]> booleanArraySerializer = getSerializer(booleanArrayType);

        final boolean[] testArray = new boolean[] {true, false, true, true, false};
        final BasicConfigurationNode serializeTo = BasicConfigurationNode.root();
        booleanArraySerializer.serialize(booleanArrayType, testArray, serializeTo);
        assertEquals(Arrays.asList(true, false, true, true, false), serializeTo.getValue());
        assertArrayEquals(testArray, booleanArraySerializer.deserialize(booleanArrayType, serializeTo));
    }

    @Test
    public void testArraySerializerBytePrimitive() throws ObjectMappingException {
        final TypeToken<byte[]> byteArrayType = TypeToken.of(byte[].class);
        final TypeSerializer<byte[]> byteArraySerializer = getSerializer(byteArrayType);

        final byte[] testArray = new byte[] {1, 5, 3, -7, 9, 0};
        final BasicConfigurationNode serializeTo = BasicConfigurationNode.root();
        byteArraySerializer.serialize(byteArrayType, testArray, serializeTo);
        assertEquals(Arrays.asList((byte) 1, (byte) 5, (byte) 3, (byte) -7, (byte) 9, (byte) 0), serializeTo.getValue());
        assertArrayEquals(testArray, byteArraySerializer.deserialize(byteArrayType, serializeTo));
    }

    @Test
    public void testArraySerializerCharPrimitive() throws ObjectMappingException {
        final TypeToken<char[]> charArrayType = TypeToken.of(char[].class);
        final TypeSerializer<char[]> charArraySerializer = getSerializer(charArrayType);

        final char[] testArray = new char[] {'s', 'l', 'e', 'e', 'p'};
        final BasicConfigurationNode serializeTo = BasicConfigurationNode.root();
        charArraySerializer.serialize(charArrayType, testArray, serializeTo);
        assertEquals(Arrays.asList('s', 'l', 'e', 'e', 'p'), serializeTo.getValue());
        assertArrayEquals(testArray, charArraySerializer.deserialize(charArrayType, serializeTo));
    }

    @Test
    public void testArraySerializerShortPrimitive() throws ObjectMappingException {
        final TypeToken<short[]> shortArrayType = TypeToken.of(short[].class);
        final TypeSerializer<short[]> shortArraySerializer = getSerializer(shortArrayType);

        final short[] testArray = new short[] {1, 5, 3, 7, 9};
        final BasicConfigurationNode serializeTo = BasicConfigurationNode.root();
        shortArraySerializer.serialize(shortArrayType, testArray, serializeTo);
        assertEquals(Arrays.asList((short) 1, (short) 5, (short) 3, (short) 7, (short) 9), serializeTo.getValue());
        assertArrayEquals(testArray, shortArraySerializer.deserialize(shortArrayType, serializeTo));
    }

    @Test
    public void testArraySerializerIntPrimitive() throws ObjectMappingException {
        final TypeToken<int[]> intArrayType = TypeToken.of(int[].class);
        final TypeSerializer<int[]> intArraySerializer = getSerializer(intArrayType);

        final int[] testArray = new int[] {1, 5, 3, 7, 9};
        final BasicConfigurationNode serializeTo = BasicConfigurationNode.root();
        intArraySerializer.serialize(intArrayType, testArray, serializeTo);
        assertEquals(Arrays.asList(1, 5, 3, 7, 9), serializeTo.getValue());
        assertArrayEquals(testArray, intArraySerializer.deserialize(intArrayType, serializeTo));
    }

    @Test
    public void testArraySerializerLongPrimitive() throws ObjectMappingException {
        final TypeToken<long[]> longArrayType = TypeToken.of(long[].class);
        final TypeSerializer<long[]> longArraySerializer = getSerializer(longArrayType);

        final long[] testArray = new long[] {1, 5, 3, 7, 9};
        final BasicConfigurationNode serializeTo = BasicConfigurationNode.root();
        longArraySerializer.serialize(longArrayType, testArray, serializeTo);
        assertEquals(Arrays.asList(1L, 5L, 3L, 7L, 9L), serializeTo.getValue());
        assertArrayEquals(testArray, longArraySerializer.deserialize(longArrayType, serializeTo));
    }

    @Test
    public void testArraySerializerFloatPrimitive() throws ObjectMappingException {
        final TypeToken<float[]> floatArrayType = TypeToken.of(float[].class);
        final TypeSerializer<float[]> floatArraySerializer = getSerializer(floatArrayType);

        final float[] testArray = new float[] {1.02f, 5.66f, 3.2f, 7.9f, 9f};
        final BasicConfigurationNode serializeTo = BasicConfigurationNode.root();
        floatArraySerializer.serialize(floatArrayType, testArray, serializeTo);
        assertEquals(Arrays.asList(1.02f, 5.66f, 3.2f, 7.9f, 9f), serializeTo.getValue());
        assertArrayEquals(testArray, floatArraySerializer.deserialize(floatArrayType, serializeTo));
    }

    @Test
    public void testArraySerializerDoublePrimitive() throws ObjectMappingException {
        final TypeToken<double[]> doubleArrayType = TypeToken.of(double[].class);
        final TypeSerializer<double[]> doubleArraySerializer = getSerializer(doubleArrayType);

        final double[] testArray = new double[] {1.02d, 5.66d, 3.2d, 7.9d, 9d};
        final BasicConfigurationNode serializeTo = BasicConfigurationNode.root();
        doubleArraySerializer.serialize(doubleArrayType, testArray, serializeTo);
        assertEquals(Arrays.asList(1.02d, 5.66d, 3.2d, 7.9d, 9d), serializeTo.getValue());
        assertArrayEquals(testArray, doubleArraySerializer.deserialize(doubleArrayType, serializeTo));
    }

    @Test
    public void testConfigurationNodeSerializer() throws ObjectMappingException {
        final TypeToken<ConfigurationNode> nodeType = TypeToken.of(ConfigurationNode.class);
        final TypeSerializer<ConfigurationNode> nodeSerializer = getSerializer(nodeType);
        assertNotNull(nodeSerializer);

        final BasicConfigurationNode sourceNode = BasicConfigurationNode.root(n -> {
            n.getNode("hello").setValue("world");
            n.getNode("lorg").act(c -> {
                c.appendListNode().setValue("doggo");
                c.appendListNode().setValue("pupper");
            });
        });

        final ConfigurationNode ret = nodeSerializer.deserialize(nodeType, sourceNode);
        assertEquals(sourceNode, ret);

        final BasicConfigurationNode dest = BasicConfigurationNode.root();
        nodeSerializer.serialize(nodeType, ret, dest);

        assertEquals(sourceNode, dest);

    }

}
