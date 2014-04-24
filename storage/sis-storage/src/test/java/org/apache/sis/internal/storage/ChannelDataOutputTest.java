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
package org.apache.sis.internal.storage;

import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.imageio.stream.ImageOutputStream;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ChannelDataOutput}.
 * First we write into two different output streams, then we compare theirs written byte array.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public strictfp class ChannelDataOutputTest extends ChannelDataTestCase {
    /**
     * The {@link DataOutput} implementation to test. This implementation will write data to
     * {@link #testedStreamBackingArray}. The content of that array will be compared to
     * {@link #expectedData} for verifying result correctness.
     */
    ChannelDataOutput testedStream;

    /**
     * A stream to use as a reference implementation. Any data written in {@link #testedStream}
     * will also be written in {@code referenceStream}, for later comparison.
     */
    DataOutput referenceStream;

    /**
     * Byte array which is filled by the {@linkplain #testedStream} implementation during write operations.
     * The content of this array will be compared to {@linkplain #expectedData}.
     */
    byte[] testedStreamBackingArray;

    /**
     * Object which is filled by {@link #referenceStream} implementation during write operations.
     * <b>Do not write to this stream</b> - this field is kept only for invocation of
     * {@link ByteArrayOutputStream#toByteArray()}.
     */
    ByteArrayOutputStream expectedData;

    /**
     * Initializes all non-final fields before to execute a test.
     *
     * @param  testName     The name of the test method to be executed.
     * @param  streamLength Length of stream to create.
     * @param  bufferLength Length of the {@code ByteBuffer} to use for the tests.
     * @throws IOException Should never happen.
     */
    void initialize(final String testName, final int streamLength, final int bufferLength) throws IOException {
        expectedData             = new ByteArrayOutputStream(streamLength);
        referenceStream          = new DataOutputStream(expectedData);
        testedStreamBackingArray = new byte[streamLength];
        testedStream             = new ChannelDataOutput(testName,
                new ByteArrayChannel(testedStreamBackingArray), ByteBuffer.allocate(bufferLength));
    }

    /**
     * Fills a stream with random data and compares the result with a reference output stream.
     * We allocate a small buffer for the {@code ChannelDataOutput} in order to force frequent
     * interactions between the buffer and the channel.
     *
     * @throws IOException Should never happen.
     */
    @Test
    public void testAllWriteMethods() throws IOException {
        initialize("testAllWriteMethods", STREAM_LENGTH, random.nextInt(BUFFER_MAX_CAPACITY) + Double.BYTES);
        writeInStreams();
        testedStream.flush();
        ((Closeable) referenceStream).close();
        final byte[] expectedArray = expectedData.toByteArray();
        assertArrayEquals(expectedArray, Arrays.copyOf(testedStreamBackingArray, expectedArray.length));
    }

    /**
     * Tests write operations followed by seek operations.
     *
     * @throws IOException Should never happen.
     */
    @Test
    @DependsOnMethod("testAllWriteMethods")
    public void testWriteAndSeek() throws IOException {
        initialize("testWriteAndSeek", STREAM_LENGTH, random.nextInt(BUFFER_MAX_CAPACITY) + Double.BYTES);
        writeInStreams();
        ((Closeable) referenceStream).close();
        final byte[] expectedArray = expectedData.toByteArray();
        final int seekRange = expectedArray.length - Long.BYTES;
        final ByteBuffer arrayView = ByteBuffer.wrap(expectedArray);
        for (int i=0; i<100; i++) {
            final int position = random.nextInt(seekRange);
            testedStream.seek(position);
            assertEquals("getStreamPosition()", position, testedStream.getStreamPosition());
            final long v = random.nextLong();
            testedStream.writeLong(v);
            arrayView.putLong(position, v);
        }
        testedStream.flush();
        assertArrayEquals(expectedArray, Arrays.copyOf(testedStreamBackingArray, expectedArray.length));
    }

    /**
     * Tests the argument checks performed by various methods. For example this method
     * tests {@link ChannelDataOutput#seek(long)} with an invalid seek position.
     *
     * @throws IOException Should never happen.
     */
    @Test
    public void testArgumentChecks() throws IOException {
        initialize("testArgumentChecks", 20, 20);
        try {
            testedStream.setBitOffset(9);
            fail("Shall not accept invalid bitOffset.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("bitOffset"));
        }
        try {
            testedStream.seek(1);
            fail("Shall not seek further than stream length.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("position"));
        }
        try {
            testedStream.reset();
            fail("Shall not accept reset without mark.");
        } catch (IOException e) {
            assertFalse(e.getMessage().isEmpty());
        }
        /*
         * flushBefore(int).
         */
        testedStream.writeShort(random.nextInt());
        testedStream.flushBefore(0); // Valid.
        try {
            testedStream.flushBefore(3);
            fail("Shall not flush at a position greater than buffer limit.");
        } catch (IndexOutOfBoundsException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("position"));
        }
        testedStream.flush();
        try {
            testedStream.flushBefore(0);
            fail("Shall not flush at a position before buffer base.");
        } catch (IndexOutOfBoundsException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("position"));
        }
    }

    /**
     * Writes the same random data in both {@link #testedStream} and {@link #referenceStream}.
     *
     * @throws IOException Should never happen.
     */
    private void writeInStreams() throws IOException {
        final int numOperations = (testedStream instanceof DataOutput) ? 19 : 14;
        while (testedStream.getStreamPosition() < testedStreamBackingArray.length - ARRAY_MAX_LENGTH) {
            final int operation = random.nextInt(numOperations);
            switch (operation) {
                case 0: {
                    final byte v = (byte) random.nextInt(1 << Byte.SIZE);
                    referenceStream.writeByte(v);
                    testedStream.writeByte(v);
                    break;
                }
                case 1: {
                    final short v = (short) random.nextInt(1 << Short.SIZE);
                    referenceStream.writeShort(v);
                    testedStream.writeShort(v);
                    break;
                }
                case 2: {
                    final char v = (char) random.nextInt(1 << Character.SIZE);
                    referenceStream.writeChar(v);
                    testedStream.writeChar(v);
                    break;
                }
                case 3: {
                    final int v = random.nextInt();
                    referenceStream.writeInt(v);
                    testedStream.writeInt(v);
                    break;
                }
                case 4: {
                    final long v = random.nextLong();
                    referenceStream.writeLong(v);
                    testedStream.writeLong(v);
                    break;
                }
                case 5: {
                    final float v = random.nextFloat();
                    referenceStream.writeFloat(v);
                    testedStream.writeFloat(v);
                    break;
                }
                case 6: {
                    final double v = random.nextDouble();
                    referenceStream.writeDouble(v);
                    testedStream.writeDouble(v);
                    break;
                }
                case 7: {
                    final byte[] tmp = new byte[random.nextInt(ARRAY_MAX_LENGTH / Byte.BYTES)];
                    random.nextBytes(tmp);
                    referenceStream.write(tmp);
                    testedStream.write(tmp);
                    break;
                }
                case 8: {
                    final char[] tmp = new char[random.nextInt(ARRAY_MAX_LENGTH / Character.BYTES)];
                    for (int i=0; i<tmp.length; i++) {
                        referenceStream.writeChar(tmp[i] = (char) random.nextInt(1 << Character.SIZE));
                    }
                    testedStream.writeChars(tmp);
                    break;
                }
                case 9: {
                    final short[] tmp = new short[random.nextInt(ARRAY_MAX_LENGTH / Short.BYTES)];
                    for (int i=0; i<tmp.length; i++) {
                        referenceStream.writeShort(tmp[i] = (short) random.nextInt(1 << Short.SIZE));
                    }
                    testedStream.writeShorts(tmp);
                    break;
                }
                case 10: {
                    final int[] tmp = new int[random.nextInt(ARRAY_MAX_LENGTH / Integer.BYTES)];
                    for (int i=0; i<tmp.length; i++) {
                        referenceStream.writeInt(tmp[i] = random.nextInt());
                    }
                    testedStream.writeInts(tmp);
                    break;
                }
                case 11: {
                    final long[] tmp = new long[random.nextInt(ARRAY_MAX_LENGTH / Long.BYTES)];
                    for (int i=0; i<tmp.length; i++) {
                        referenceStream.writeLong(tmp[i] = random.nextLong());
                    }
                    testedStream.writeLongs(tmp);
                    break;
                }
                case 12: {
                    final float[] tmp = new float[random.nextInt(ARRAY_MAX_LENGTH / Float.BYTES)];
                    for (int i=0; i<tmp.length; i++) {
                        referenceStream.writeFloat(tmp[i] = random.nextFloat());
                    }
                    testedStream.writeFloats(tmp);
                    break;
                }
                case 13: {
                    final double[] tmp = new double[random.nextInt(ARRAY_MAX_LENGTH / Double.BYTES)];
                    for (int i=0; i<tmp.length; i++) {
                        referenceStream.writeDouble(tmp[i] = random.nextDouble());
                    }
                    testedStream.writeDoubles(tmp);
                    break;
                }
                /*
                 * Cases below this point are executed only by ChannelImageOutputStreamTest.
                 */
                case 14: {
                    final long v = random.nextLong();
                    final int numBits = random.nextInt(Byte.SIZE);
                    ((ImageOutputStream) referenceStream).writeBits(v, numBits);
                    testedStream.writeBits(v, numBits);
                    break;
                }
                case 15: {
                    final boolean v = random.nextBoolean();
                    referenceStream.writeBoolean(v);
                    ((DataOutput) testedStream).writeBoolean(v);
                    break;
                }
                case 16: {
                    final String s = "Byte sequence";
                    referenceStream.writeBytes(s);
                    ((DataOutput) testedStream).writeBytes(s);
                    break;
                }
                case 17: {
                    final String s = "Character sequence";
                    referenceStream.writeChars(s);
                    ((DataOutput) testedStream).writeChars(s);
                    break;
                }
                case 18: {
                    final String s = "お元気ですか";
                    final byte[] array = s.getBytes("UTF-8");
                    assertEquals(s.length() * 3, array.length); // Sanity check.
                    referenceStream.writeUTF(s);
                    ((DataOutput) testedStream).writeUTF(s);
                    break;
                }
                default: throw new AssertionError(operation);
            }
        }
    }
}
