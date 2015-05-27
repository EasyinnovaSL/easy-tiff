/**
 * <h1>DataByteOrderInputStream.java</h1>
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version; or, at your choice, under the terms of the
 * Mozilla Public License, v. 2.0. SPDX GPL-3.0+ or MPL-2.0+.
 * </p>
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License and the Mozilla Public License for more details.
 * </p>
 * <p>
 * You should have received a copy of the GNU General Public License and the Mozilla Public License
 * along with this program. If not, see <a
 * href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a> and at <a
 * href="http://mozilla.org/MPL/2.0">http://mozilla.org/MPL/2.0</a> .
 * </p>
 * <p>
 * NB: for the © statement, include Easy Innova SL or other company/Person contributing the code.
 * </p>
 * <p>
 * © 2015 Easy Innova, SL
 * </p>
 *
 * @author Xavier Tarrés Bonet
 * @version 1.0
 * @since 26/5/2015
 *
 */
package com.easyinnova.tiff.io;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;



/**
 * The Class DataByteOrderInputStream.
 */
public class DataByteOrderInputStream extends FilterInputStream implements TiffDataIntput {

  /** The big endian constant. */
  public static boolean BIG_ENDIAN = true;

  /** The littel endian constant. */
  public static boolean LITTEL_ENDIAN = false;

  private boolean ByteOrder;

  /**
   * Instantiates a new data byte order input stream.
   *
   * @param in the in
   */
  protected DataByteOrderInputStream(InputStream in) {
    super(in);
    ByteOrder = BIG_ENDIAN;
  }

  /**
   * Instantiates a new data byte order input stream.
   *
   * @param in the in
   * @param order the byte order
   */
  protected DataByteOrderInputStream(InputStream in, boolean order) {
    super(in);
    ByteOrder = order;
  }

  /**
   * @return the byteOrder
   */

  public boolean getByteOrder() {
    return ByteOrder;
  }

  /**
   * @param byteOrder the byteOrder to set
   */
  public void setByteOrder(boolean byteOrder) {
    ByteOrder = byteOrder;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#readFully(byte[])
   */
  @Override
  public void readFully(byte[] b) throws IOException {
    readFully(b, 0, b.length);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#readFully(byte[], int, int)
   */
  @Override
  public void readFully(byte[] b, int off, int len) throws IOException {
    if (len < 0) {
      throw new IndexOutOfBoundsException();
    }
    int n = 0;
    while (n < len) {
      int count = in.read(b, off + n, len - n);
      if (count < 0)
        throw new EOFException();
      n += count;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#skipBytes(int)
   */
  @Override
  public int skipBytes(int n) throws IOException {
    int total = 0;
     int cur = 0;
     
    while ((total<n) && ((cur = (int) in.skip(n-total)) > 0)) {
     total += cur;
    }
    
  return total;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#readBoolean()
   */
  @Override
  public boolean readBoolean() throws IOException {
    int ch = in.read();
    if (ch < 0) {
     throw new EOFException();
    }
    return (ch != 0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#readByte()
   */
  @Override
  public byte readByte() throws IOException {
    int ch = in.read();
    if (ch < 0) {
    throw new EOFException();
    }
    return (byte)(ch);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#readUnsignedByte()
   */
  @Override
  public int readUnsignedByte() throws IOException {
    int ch = in.read();
    if (ch < 0) {
      throw new EOFException();
    }
    return ch;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#readShort()
   */
  @Override
  public short readShort() throws IOException {
    int ch1 = in.read();
    int ch2 = in.read();
    if ((ch1 | ch2) < 0) {
        throw new EOFException();
    }
    short val;
    if(ByteOrder) {
      val = (short)((ch1 << 8) + (ch2 << 0));
    }else{
      val = (short)((ch2 << 8) + (ch1 << 0));
    }         
    return val;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#readUnsignedShort()
   */
  @Override
  public int readUnsignedShort() throws IOException {
    int ch1 = in.read();
    int ch2 = in.read();
    if ((ch1 | ch2) < 0) {
      throw new EOFException();
    }
    int val;
    if(ByteOrder) {
      val = ((ch1 << 8) + (ch2 << 0));
    }else{
      val = ((ch2 << 8) + (ch1 << 0));
    }         
    return val;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#readChar()
   */
  @Override
  public char readChar() throws IOException {
    int ch1 = in.read();
    int ch2 = in.read();
    if ((ch1 | ch2) < 0) {
      throw new EOFException();
    }
    return (char)((ch1 << 8) + (ch2 << 0));
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#readInt()
   */
  @Override
  public int readInt() throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#readLong()
   */
  @Override
  public long readLong() throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#readFloat()
   */
  @Override
  public float readFloat() throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#readDouble()
   */
  @Override
  public double readDouble() throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#readLine()
   */
  @Override
  public String readLine() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.DataInput#readUTF()
   */
  @Override
  public String readUTF() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }



}
