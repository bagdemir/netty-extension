/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @author tags. See the COPYRIGHT.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.netty.buffer;

import static org.jboss.netty.buffer.ChannelBuffers.EMPTY_BUFFER;
import static org.jboss.netty.buffer.ChannelBuffers.LITTLE_ENDIAN;
import static org.jboss.netty.buffer.ChannelBuffers.buffer;
import static org.jboss.netty.buffer.ChannelBuffers.compare;
import static org.jboss.netty.buffer.ChannelBuffers.copiedBuffer;
import static org.jboss.netty.buffer.ChannelBuffers.directBuffer;
import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;
import static org.jboss.netty.buffer.ChannelBuffers.hexDump;
import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;
import static org.jboss.netty.buffer.AggregateChannelBuffer.wrappedCheckedBuffer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

/**
 * @author frederic bregier
 *
 */
public class AggregateChannelBufferTest extends AbstractChannelBufferTest {
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(AggregateChannelBufferTest.class);
    }


    private List<ChannelBuffer> buffers;
    private ChannelBuffer buffer;

    protected ChannelBuffer newBuffer(int length) {
        buffers = new ArrayList<ChannelBuffer>();
        for (int i = 0; i < length; i += 10) {
            buffers.add(ChannelBuffers.EMPTY_BUFFER);
            buffers.add(ChannelBuffers.wrappedBuffer(new byte[1]));
            buffers.add(ChannelBuffers.EMPTY_BUFFER);
            buffers.add(ChannelBuffers.wrappedBuffer(new byte[2]));
            buffers.add(ChannelBuffers.EMPTY_BUFFER);
            buffers.add(ChannelBuffers.wrappedBuffer(new byte[3]));
            buffers.add(ChannelBuffers.EMPTY_BUFFER);
            buffers.add(ChannelBuffers.wrappedBuffer(new byte[4]));
            buffers.add(ChannelBuffers.EMPTY_BUFFER);
            buffers.add(ChannelBuffers.wrappedBuffer(new byte[5]));
            buffers.add(ChannelBuffers.EMPTY_BUFFER);
            buffers.add(ChannelBuffers.wrappedBuffer(new byte[6]));
            buffers.add(ChannelBuffers.EMPTY_BUFFER);
            buffers.add(ChannelBuffers.wrappedBuffer(new byte[7]));
            buffers.add(ChannelBuffers.EMPTY_BUFFER);
            buffers.add(ChannelBuffers.wrappedBuffer(new byte[8]));
            buffers.add(ChannelBuffers.EMPTY_BUFFER);
            buffers.add(ChannelBuffers.wrappedBuffer(new byte[9]));
            buffers.add(ChannelBuffers.EMPTY_BUFFER);
        }

        buffer = wrappedCheckedBuffer(buffers.toArray(new ChannelBuffer[buffers.size()]));
        buffer.writerIndex(length);
        buffer = wrappedCheckedBuffer(buffer);
        assertEquals(length, buffer.capacity());
        assertEquals(length, buffer.readableBytes());
        assertFalse(buffer.writable());
        buffer.writerIndex(0);
        return buffer;
    }

    protected ChannelBuffer[] components() {
        return buffers.toArray(new ChannelBuffer[buffers.size()]);
    }

    @Test
    public void testCompositeWrappedBuffer() {
        ChannelBuffer header = dynamicBuffer(12);
        ChannelBuffer payload = dynamicBuffer(512);

        header.writeBytes(new byte[12]);
        payload.writeBytes(new byte[512]);

        ChannelBuffer buffer = wrappedCheckedBuffer(header, payload);

        assertTrue(header.readableBytes() == 12);
        assertTrue(payload.readableBytes() == 512);

        assertEquals(12 + 512, buffer.readableBytes());

        assertEquals(12 + 512, buffer.toByteBuffer(0, 12 + 512).remaining());
    }

    @Test
    public void testHashCode() {
        Map<byte[], Integer> map = new LinkedHashMap<byte[], Integer>();
        map.put(new byte[0], 1);
        map.put(new byte[] { 1 }, 32);
        map.put(new byte[] { 2 }, 33);
        map.put(new byte[] { 0, 1 }, 962);
        map.put(new byte[] { 1, 2 }, 994);
        map.put(new byte[] { 0, 1, 2, 3, 4, 5 }, 63504931);
        map.put(new byte[] { 6, 7, 8, 9, 0, 1 }, (int) 97180294697L);
        map.put(new byte[] { -1, -1, -1, (byte) 0xE1 }, 1);

        for (Entry<byte[], Integer> e: map.entrySet()) {
            assertEquals(
                    e.getValue().intValue(),//FIXME
                    ChannelBuffers.hashCode(wrappedCheckedBuffer(wrappedBuffer(e.getKey()))));
        }
    }

    @Test
    public void testEquals() {
        ChannelBuffer a, b;
      //FIXME
        // Different length.
        a = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1  }));
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2 }));
        assertFalse(ChannelBuffers.equals(a, b));

        // Same content, same firstIndex, short length.
        a = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3 }));
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3 }));
        assertTrue(ChannelBuffers.equals(a, b));

        // Same content, different firstIndex, short length.
        a = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3 }));
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 0, 1, 2, 3, 4 }, 1, 3));
        assertTrue(ChannelBuffers.equals(a, b));

        // Different content, same firstIndex, short length.
        a = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3 }));
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 4 }));
        assertFalse(ChannelBuffers.equals(a, b));

        // Different content, different firstIndex, short length.
        a = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3 }));
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 0, 1, 2, 4, 5 }, 1, 3));
        assertFalse(ChannelBuffers.equals(a, b));

        // Same content, same firstIndex, long length.
        a = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }));
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }));
        assertTrue(ChannelBuffers.equals(a, b));

        // Same content, different firstIndex, long length.
        a = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }));
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, 1, 10));
        assertTrue(ChannelBuffers.equals(a, b));

        // Different content, same firstIndex, long length.
        a = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }));
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4, 6, 7, 8, 5, 9, 10 }));
        assertFalse(ChannelBuffers.equals(a, b));

        // Different content, different firstIndex, long length.
        a = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }));
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 0, 1, 2, 3, 4, 6, 7, 8, 5, 9, 10, 11 }, 1, 10));
        assertFalse(ChannelBuffers.equals(a, b));

        // new tests with mixed type of buffer
        // Different length.
        a = wrappedBuffer(new byte[] { 1  });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2 }));
        assertFalse(ChannelBuffers.equals(a, b));

        // Same content, same firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3 }));
        assertTrue(ChannelBuffers.equals(a, b));

        // Same content, different firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 0, 1, 2, 3, 4 }, 1, 3));
        assertTrue(ChannelBuffers.equals(a, b));

        // Different content, same firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 4 }));
        assertFalse(ChannelBuffers.equals(a, b));

        // Different content, different firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 0, 1, 2, 4, 5 }, 1, 3));
        assertFalse(ChannelBuffers.equals(a, b));

        // Same content, same firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }));
        assertTrue(ChannelBuffers.equals(a, b));

        // Same content, different firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, 1, 10));
        assertTrue(ChannelBuffers.equals(a, b));

        // Different content, same firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4, 6, 7, 8, 5, 9, 10 }));
        assertFalse(ChannelBuffers.equals(a, b));

        // Different content, different firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 0, 1, 2, 3, 4, 6, 7, 8, 5, 9, 10, 11 }, 1, 10));
        assertFalse(ChannelBuffers.equals(a, b));

        // Same tests with several buffers in wrappedCheckedBuffer
        // Different length.
        a = wrappedBuffer(new byte[] { 1  });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1 }),
                wrappedBuffer(new byte[] { 2 }));
        assertFalse(ChannelBuffers.equals(a, b));

        // Same content, same firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1 }),
                wrappedBuffer(new byte[] { 2 }),wrappedBuffer(new byte[] { 3 }));
        assertTrue(ChannelBuffers.equals(a, b));

        // Same content, different firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 0, 1, 2, 3, 4 }, 1, 2),
                wrappedBuffer(new byte[] { 0, 1, 2, 3, 4 }, 3, 1));
        assertTrue(ChannelBuffers.equals(a, b));

        // Different content, same firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2 }),
                wrappedBuffer(new byte[] { 4 }));
        assertFalse(ChannelBuffers.equals(a, b));

        // Different content, different firstIndex, short length.
        a = wrappedBuffer(new byte[] { 1, 2, 3 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 0, 1, 2, 4, 5 }, 1, 2),
                wrappedBuffer(new byte[] { 0, 1, 2, 4, 5 }, 3, 1));
        assertFalse(ChannelBuffers.equals(a, b));

        // Same content, same firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3 }),
                wrappedBuffer(new byte[] { 4, 5, 6 }),
                wrappedBuffer(new byte[] { 7, 8, 9, 10 }));
        assertTrue(ChannelBuffers.equals(a, b));

        // Same content, different firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, 1, 5),
                wrappedBuffer(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, 6, 5));
        assertTrue(ChannelBuffers.equals(a, b));

        // Different content, same firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4, 6 }),
                wrappedBuffer(new byte[] { 7, 8, 5, 9, 10 }));
        assertFalse(ChannelBuffers.equals(a, b));

        // Different content, different firstIndex, long length.
        a = wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        b = wrappedCheckedBuffer(wrappedBuffer(new byte[] { 0, 1, 2, 3, 4, 6, 7, 8, 5, 9, 10, 11 }, 1, 5),
                wrappedBuffer(new byte[] { 0, 1, 2, 3, 4, 6, 7, 8, 5, 9, 10, 11 }, 6, 5));
        assertFalse(ChannelBuffers.equals(a, b));

    }

    @Test
    public void testCompare() {
        List<ChannelBuffer> expected = new ArrayList<ChannelBuffer>();
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1 })));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2 })));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8,  9, 10 })));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8,  9, 10, 11, 12 })));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 2 })));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 2, 3 })));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 })));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 })));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 2, 3, 4 }, 1, 1)));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4 }, 2, 2)));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 }, 1, 10)));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 }, 2, 12)));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 2, 3, 4, 5 }, 2, 1)));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5 }, 3, 2)));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 }, 2, 10)));
        expected.add(wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 }, 3, 12)));

        for (int i = 0; i < expected.size(); i ++) {
            for (int j = 0; j < expected.size(); j ++) {
                if (i == j) {
                    assertEquals(0, compare(expected.get(i), expected.get(j)));
                } else if (i < j) {
                    assertTrue(compare(expected.get(i), expected.get(j)) < 0);
                } else {
                    assertTrue(compare(expected.get(i), expected.get(j)) > 0);
                }
            }
        }
    }

    @Test
    public void shouldReturnEmptyBufferWhenLengthIsZero() {
        assertSame(EMPTY_BUFFER, buffer(0));
        assertSame(EMPTY_BUFFER, buffer(LITTLE_ENDIAN, 0));
        assertSame(EMPTY_BUFFER, directBuffer(0));

        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(wrappedBuffer(new byte[0])));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(wrappedBuffer(LITTLE_ENDIAN, new byte[0])));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(wrappedBuffer(new byte[8], 0, 0)));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(wrappedBuffer(LITTLE_ENDIAN, new byte[8], 0, 0)));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(wrappedBuffer(new byte[8], 8, 0)));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(wrappedBuffer(LITTLE_ENDIAN, new byte[8], 8, 0)));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(wrappedBuffer(ByteBuffer.allocateDirect(0))));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(wrappedBuffer(EMPTY_BUFFER)));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(wrappedBuffer(new byte[0][])));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(wrappedBuffer(new byte[][] { new byte[0] })));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(wrappedBuffer(new ByteBuffer[0])));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(wrappedBuffer(new ByteBuffer[] { ByteBuffer.allocate(0) })));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(wrappedBuffer(new ByteBuffer[] { ByteBuffer.allocate(0), ByteBuffer.allocate(0) })));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(new ChannelBuffer[0]));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(new ChannelBuffer[] { buffer(0) }));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(new ChannelBuffer[] { buffer(0), buffer(0) }));

        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(new byte[0])));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(LITTLE_ENDIAN, new byte[0])));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(new byte[8], 0, 0)));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(LITTLE_ENDIAN, new byte[8], 0, 0)));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(new byte[8], 8, 0)));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(LITTLE_ENDIAN, new byte[8], 8, 0)));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(ByteBuffer.allocateDirect(0))));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(EMPTY_BUFFER)));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(new byte[0][])));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(new byte[][] { new byte[0] })));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(new ByteBuffer[0])));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(new ByteBuffer[] { ByteBuffer.allocate(0) })));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(new ByteBuffer[] { ByteBuffer.allocate(0), ByteBuffer.allocate(0) })));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(new ChannelBuffer[0])));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(new ChannelBuffer[] { buffer(0) })));
        assertSame(EMPTY_BUFFER, wrappedCheckedBuffer(copiedBuffer(new ChannelBuffer[] { buffer(0), buffer(0) })));
    }

    @Test
    public void shouldAllowEmptyBufferToCreateAggregateBuffer() {
        ChannelBuffer buf = wrappedCheckedBuffer(
                EMPTY_BUFFER,
                wrappedBuffer(LITTLE_ENDIAN, new byte[16]),
                EMPTY_BUFFER);
        assertEquals(16, buf.capacity());
    }

    @Test
    public void testWrappedCheckedBuffer() {
        assertEquals(16, wrappedCheckedBuffer(wrappedBuffer(ByteBuffer.allocateDirect(16))).capacity());

        assertEquals(
                wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3 })),
                wrappedCheckedBuffer(wrappedBuffer(new byte[][] { new byte[] { 1, 2, 3 } })));

        assertEquals(
                wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3 })),
                wrappedCheckedBuffer(wrappedBuffer(
                        new byte[] { 1 },
                        new byte[] { 2 },
                        new byte[] { 3 })));

        assertEquals(
                wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3 })),
                wrappedCheckedBuffer(new ChannelBuffer[] {
                        wrappedBuffer(new byte[] { 1, 2, 3 })
                }));

        assertEquals(
                wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3 })),
                wrappedCheckedBuffer(
                        wrappedBuffer(new byte[] { 1 }),
                        wrappedBuffer(new byte[] { 2 }),
                        wrappedBuffer(new byte[] { 3 })));

        assertEquals(
                wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3 })),
                wrappedCheckedBuffer(wrappedBuffer(new ByteBuffer[] {
                        ByteBuffer.wrap(new byte[] { 1, 2, 3 })
                })));

        assertEquals(
                wrappedCheckedBuffer(wrappedBuffer(new byte[] { 1, 2, 3 })),
                wrappedCheckedBuffer(wrappedBuffer(
                        ByteBuffer.wrap(new byte[] { 1 }),
                        ByteBuffer.wrap(new byte[] { 2 }),
                        ByteBuffer.wrap(new byte[] { 3 }))));
    }

    @Test
    public void testHexDump() {
        assertEquals("", hexDump(wrappedCheckedBuffer(EMPTY_BUFFER)));

        assertEquals("123456", hexDump(wrappedCheckedBuffer(wrappedBuffer(
                new byte[] {
                        0x12, 0x34, 0x56
                }))));

        assertEquals("1234567890abcdef", hexDump(wrappedCheckedBuffer(wrappedBuffer(
                new byte[] {
                        0x12, 0x34, 0x56, 0x78,
                        (byte) 0x90, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
                }))));
    }
}
