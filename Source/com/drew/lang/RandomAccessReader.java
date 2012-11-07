/*
 * Copyright 2002-2012 Drew Noakes
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 * More information about this project is available at:
 *
 *    http://drewnoakes.com/code/exif/
 *    http://code.google.com/p/metadata-extractor/
 */

package com.drew.lang;

import com.drew.lang.annotations.NotNull;

import java.io.UnsupportedEncodingException;

/**
 * Base class for random access data reading operations of common data types.
 * <p/>
 * By default, the reader operates with Motorola byte order (big endianness).  This can be changed by calling
 * <code>setMotorolaByteOrder(boolean)</code>.
 * <p/>
 * Concrete implementations include:
 * <ul>
 *     <li>{@link ByteArrayReader}</li>
 *     <li>{@link RandomAccessReader}</li>
 *     <li>{@link RandomAccessStreamReader}</li>
 * </ul>
 *
 * @author Drew Noakes http://drewnoakes.com
 */
public abstract class RandomAccessReader
{
    private boolean _isMotorolaByteOrder = true;

    /**
     * Gets the byte value at the specified byte index.
     * <p/>
     * Implementations should not perform any bounds checking in this method. That should be performed
     * in <code>validateIndex</code> and <code>isValidIndex</code>.
     *
     * @param index The index from which to read the byte
     * @return The read byte value
     * @throws BufferBoundsException If the index is invalid, or otherwise unable to be read
     */
    protected abstract byte getByte(int index) throws BufferBoundsException;

    /**
     * Returns the required number of bytes from the specified index from the underlying source.
     *
     * @param index The index from which the bytes begins in the underlying source
     * @param count The number of bytes to be returned
     * @return The requested bytes
     * @throws BufferBoundsException If the index is invalid, or if the requested number of bytes were unavailable
     * or otherwise unable to be read
     */
    @NotNull
    public abstract byte[] getBytes(int index, int count) throws BufferBoundsException;

    /**
     * Ensures that the buffered bytes extend to cover the specified index. If not, an attempt is made
     * to read to that point.
     * <p/>
     * If the stream ends before the point is reached, a {@link com.drew.lang.BufferBoundsException} is raised.
     *
     * @param index the index from which the required bytes start
     * @param bytesRequested the number of bytes which are required
     * @throws com.drew.lang.BufferBoundsException if the stream ends before the required number of bytes are acquired
     */
    protected abstract void validateIndex(int index, int bytesRequested) throws BufferBoundsException;

    protected abstract boolean isValidIndex(int index, int bytesRequested) throws BufferBoundsException;

    /**
     * Returns the length of the data source in bytes.
     * <p/>
     * This is a simple operation for implementations (such as {@link RandomAccessFileReader} and
     * {@link ByteArrayReader}) that have the entire data source available.
     * <p/>
     * Users of this method must be aware that sequentially accessed implementations such as
     * {@link RandomAccessStreamReader} will have to read and buffer the entire data source in
     * order to determine the length.
     *
     * @return the length of the data source, in bytes.
     */
    public abstract long getLength() throws BufferBoundsException;

    /**
     * Sets the endianness of this reader.
     * <ul>
     * <li><code>true</code> for Motorola (or big) endianness</li>
     * <li><code>false</code> for Intel (or little) endianness</li>
     * </ul>
     *
     * @param motorolaByteOrder <code>true</code> for motorola/big endian, <code>false</code> for intel/little endian
     */
    public void setMotorolaByteOrder(boolean motorolaByteOrder)
    {
        _isMotorolaByteOrder = motorolaByteOrder;
    }

    /**
     * Gets the endianness of this reader.
     * <ul>
     * <li><code>true</code> for Motorola (or big) endianness</li>
     * <li><code>false</code> for Intel (or little) endianness</li>
     * </ul>
     */
    public boolean isMotorolaByteOrder()
    {
        return _isMotorolaByteOrder;
    }

    /**
     * Returns an unsigned 8-bit int calculated from one byte of data at the specified index.
     *
     * @param index position within the data buffer to read byte
     * @return the 8 bit int value, between 0 and 255
     * @throws BufferBoundsException the buffer does not contain enough bytes to service the request, or index is negative
     */
    public short getUInt8(int index) throws BufferBoundsException
    {
        validateIndex(index, 1);

        return (short) (getByte(index) & 0xFF);
    }

    /**
     * Returns a signed 8-bit int calculated from one byte of data at the specified index.
     *
     * @param index position within the data buffer to read byte
     * @return the 8 bit int value, between 0x00 and 0xFF
     * @throws BufferBoundsException the buffer does not contain enough bytes to service the request, or index is negative
     */
    public byte getInt8(int index) throws BufferBoundsException
    {
        validateIndex(index, 1);

        return getByte(index);
    }

    /**
     * Returns an unsigned 16-bit int calculated from two bytes of data at the specified index.
     *
     * @param index position within the data buffer to read first byte
     * @return the 16 bit int value, between 0x0000 and 0xFFFF
     * @throws BufferBoundsException the buffer does not contain enough bytes to service the request, or index is negative
     */
    public int getUInt16(int index) throws BufferBoundsException
    {
        validateIndex(index, 2);

        if (_isMotorolaByteOrder) {
            // Motorola - MSB first
            return (getByte(index    ) << 8 & 0xFF00) |
                   (getByte(index + 1)      & 0xFF);
        } else {
            // Intel ordering - LSB first
            return (getByte(index + 1) << 8 & 0xFF00) |
                   (getByte(index    )      & 0xFF);
        }
    }

    /**
     * Returns a signed 16-bit int calculated from two bytes of data at the specified index (MSB, LSB).
     *
     * @param index position within the data buffer to read first byte
     * @return the 16 bit int value, between 0x0000 and 0xFFFF
     * @throws BufferBoundsException the buffer does not contain enough bytes to service the request, or index is negative
     */
    public short getInt16(int index) throws BufferBoundsException
    {
        validateIndex(index, 2);

        if (_isMotorolaByteOrder) {
            // Motorola - MSB first
            return (short) (((short)getByte(index    ) << 8 & (short)0xFF00) |
                            ((short)getByte(index + 1)      & (short)0xFF));
        } else {
            // Intel ordering - LSB first
            return (short) (((short)getByte(index + 1) << 8 & (short)0xFF00) |
                            ((short)getByte(index    )      & (short)0xFF));
        }
    }

    /**
     * Get a 32-bit unsigned integer from the buffer, returning it as a long.
     *
     * @param index position within the data buffer to read first byte
     * @return the unsigned 32-bit int value as a long, between 0x00000000 and 0xFFFFFFFF
     * @throws BufferBoundsException the buffer does not contain enough bytes to service the request, or index is negative
     */
    public long getUInt32(int index) throws BufferBoundsException
    {
        validateIndex(index, 4);

        if (_isMotorolaByteOrder) {
            // Motorola - MSB first (big endian)
            return (((long)getByte(index    )) << 24 & 0xFF000000L) |
                   (((long)getByte(index + 1)) << 16 & 0xFF0000L) |
                   (((long)getByte(index + 2)) << 8  & 0xFF00L) |
                   (((long)getByte(index + 3))       & 0xFFL);
        } else {
            // Intel ordering - LSB first (little endian)
            return (((long)getByte(index + 3)) << 24 & 0xFF000000L) |
                   (((long)getByte(index + 2)) << 16 & 0xFF0000L) |
                   (((long)getByte(index + 1)) << 8  & 0xFF00L) |
                   (((long)getByte(index    ))       & 0xFFL);
        }
    }

    /**
     * Returns a signed 32-bit integer from four bytes of data at the specified index the buffer.
     *
     * @param index position within the data buffer to read first byte
     * @return the signed 32 bit int value, between 0x00000000 and 0xFFFFFFFF
     * @throws BufferBoundsException the buffer does not contain enough bytes to service the request, or index is negative
     */
    public int getInt32(int index) throws BufferBoundsException
    {
        validateIndex(index, 4);

        if (_isMotorolaByteOrder) {
            // Motorola - MSB first (big endian)
            return (getByte(index    ) << 24 & 0xFF000000) |
                   (getByte(index + 1) << 16 & 0xFF0000) |
                   (getByte(index + 2) << 8  & 0xFF00) |
                   (getByte(index + 3)       & 0xFF);
        } else {
            // Intel ordering - LSB first (little endian)
            return (getByte(index + 3) << 24 & 0xFF000000) |
                   (getByte(index + 2) << 16 & 0xFF0000) |
                   (getByte(index + 1) << 8  & 0xFF00) |
                   (getByte(index    )       & 0xFF);
        }
    }

    /**
     * Get a signed 64-bit integer from the buffer.
     *
     * @param index position within the data buffer to read first byte
     * @return the 64 bit int value, between 0x0000000000000000 and 0xFFFFFFFFFFFFFFFF
     * @throws BufferBoundsException the buffer does not contain enough bytes to service the request, or index is negative
     */
    public long getInt64(int index) throws BufferBoundsException
    {
        validateIndex(index, 8);

        if (_isMotorolaByteOrder) {
            // Motorola - MSB first
            return ((long)getByte(index    ) << 56 & 0xFF00000000000000L) |
                   ((long)getByte(index + 1) << 48 & 0xFF000000000000L) |
                   ((long)getByte(index + 2) << 40 & 0xFF0000000000L) |
                   ((long)getByte(index + 3) << 32 & 0xFF00000000L) |
                   ((long)getByte(index + 4) << 24 & 0xFF000000L) |
                   ((long)getByte(index + 5) << 16 & 0xFF0000L) |
                   ((long)getByte(index + 6) << 8  & 0xFF00L) |
                   ((long)getByte(index + 7)       & 0xFFL);
        } else {
            // Intel ordering - LSB first
            return ((long)getByte(index + 7) << 56 & 0xFF00000000000000L) |
                   ((long)getByte(index + 6) << 48 & 0xFF000000000000L) |
                   ((long)getByte(index + 5) << 40 & 0xFF0000000000L) |
                   ((long)getByte(index + 4) << 32 & 0xFF00000000L) |
                   ((long)getByte(index + 3) << 24 & 0xFF000000L) |
                   ((long)getByte(index + 2) << 16 & 0xFF0000L) |
                   ((long)getByte(index + 1) << 8  & 0xFF00L) |
                   ((long)getByte(index    )       & 0xFFL);
        }
    }

    public float getS15Fixed16(int index) throws BufferBoundsException
    {
        validateIndex(index, 4);

        if (_isMotorolaByteOrder) {
            float res = (getByte(index    ) & 0xFF) << 8 |
                        (getByte(index + 1) & 0xFF);
            int d =     (getByte(index + 2) & 0xFF) << 8 |
                        (getByte(index + 3) & 0xFF);
            return (float)(res + d/65536.0);
        } else {
            // this particular branch is untested
            float res = (getByte(index + 3) & 0xFF) << 8 |
                        (getByte(index + 2) & 0xFF);
            int d =     (getByte(index + 1) & 0xFF) << 8 |
                        (getByte(index    ) & 0xFF);
            return (float)(res + d/65536.0);
        }
    }

    public float getFloat32(int index) throws BufferBoundsException
    {
        return Float.intBitsToFloat(getInt32(index));
    }

    public double getDouble64(int index) throws BufferBoundsException
    {
        return Double.longBitsToDouble(getInt64(index));
    }

    @NotNull
    public String getString(int index, int bytesRequested) throws BufferBoundsException
    {
        return new String(getBytes(index, bytesRequested));
    }

    @NotNull
    public String getString(int index, int bytesRequested, String charset) throws BufferBoundsException
    {
        byte[] bytes = getBytes(index, bytesRequested);
        try {
            return new String(bytes, charset);
        } catch (UnsupportedEncodingException e) {
            return new String(bytes);
        }
    }

    /**
     * Creates a String from the _data buffer starting at the specified index,
     * and ending where <code>byte=='\0'</code> or where <code>length==maxLength</code>.
     *
     * @param index          The index within the buffer at which to start reading the string.
     * @param maxLengthBytes The maximum number of bytes to read.  If a zero-byte is not reached within this limit,
     *                       reading will stop and the string will be truncated to this length.
     * @return The read string.
     * @throws BufferBoundsException The buffer does not contain enough bytes to satisfy this request.
     */
    @NotNull
    public String getNullTerminatedString(int index, int maxLengthBytes) throws BufferBoundsException
    {
        // NOTE currently only really suited to single-byte character strings

        byte[] bytes = getBytes(index, maxLengthBytes);

        // Count the number of non-null bytes
        int length = 0;
        while (length < bytes.length && bytes[length] != '\0')
            length++;

        return new String(bytes, 0, length);
    }
}
