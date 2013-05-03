/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.metadata;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link MetadataTreeTable} class.
 * Unless otherwise specified, all tests use the {@link MetadataStandard#ISO_19115} constant.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@DependsOn(MetadataTreeNodeTest.class)
public final strictfp class MetadataTreeTableTest extends TestCase {
    /**
     * Creates a table to be tested for the given value policy.
     */
    private static MetadataTreeTable create(final ValueExistencePolicy valuePolicy) {
        return new MetadataTreeTable(MetadataStandard.ISO_19115, MetadataTreeNodeTest.metadataWithHierarchy(), valuePolicy);
    }

    /**
     * Asserts that the given metadata object has the expected string representation.
     */
    private static void assertExpectedString(final MetadataTreeTable metadata) {
        assertMultilinesEquals("toString()",
                "DefaultCitation\n" +
                "  ├─Title…………………………………………………………………………………… Some title\n" +
                "  ├─Alternate title (1 of 2)………………………………… First alternate title\n" +
                "  ├─Alternate title (2 of 2)………………………………… Second alternate title\n" +
                "  ├─Edition……………………………………………………………………………… Some edition\n" +
                "  ├─Cited responsible party (1 of 2)\n" +
                "  │   ├─Organisation name………………………………………… Some organisation\n" +
                "  │   └─Role…………………………………………………………………………… Distributor\n" +
                "  ├─Cited responsible party (2 of 2)\n" +
                "  │   ├─Individual name……………………………………………… Some person of contact\n" +
                "  │   ├─Contact info\n" +
                "  │   │   └─Address\n" +
                "  │   │       └─Electronic mail address…… Some email\n" +
                "  │   └─Role…………………………………………………………………………… Point of contact\n" +
                "  ├─Presentation form (1 of 2)…………………………… Map digital\n" +
                "  ├─Presentation form (2 of 2)…………………………… Map hardcopy\n" +
                "  └─Other citation details……………………………………… Some other details\n",
                metadata.toString());
    }

    /**
     * Tests {@link MetadataTreeTable#toString()}.
     */
    @Test
    public void testToString() {
        assertExpectedString(create(ValueExistencePolicy.NON_EMPTY));
    }

    /**
     * Tests serialization.
     *
     * @throws Exception If an error occurred during the serialization process.
     */
    @Test
    @DependsOnMethod("testToString")
    public void testSerialization() throws Exception {
        final Object original = create(ValueExistencePolicy.NON_EMPTY);
        final Object deserialized;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
            out.writeObject(original);
        }
        // Now reads the object we just serialized.
        final byte[] data = buffer.toByteArray();
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            deserialized = in.readObject();
        }
        assertExpectedString((MetadataTreeTable) deserialized);
    }
}
