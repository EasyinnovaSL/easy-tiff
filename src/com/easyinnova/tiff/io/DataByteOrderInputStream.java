/**
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
import java.util.ArrayList;
import java.util.List;

import com.easyinnova.tiff.model.types.*;
import com.easyinnova.tiff.model.types.Byte;
import com.easyinnova.tiff.model.types.Long;
import com.easyinnova.tiff.model.types.Short;


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

  public Byte readByte() throws IOException {
    int ch = in.read();
    if (ch < 0) {
    throw new EOFException();
    }
    return new Byte(ch);
  }
  
  public Ascii readAscii() throws IOException {
    int ch = in.read();
    if (ch < 0) {
    throw new EOFException();
    }
    return new Ascii(ch);
  }
  
  public SByte readSByte() throws IOException {
    int ch = in.read();
    if (ch < 0) {
    throw new EOFException();
    }
    return new SByte(ch);
  }

  public Short readShort() throws IOException {
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
    return new Short(val);
  }
  
  public SShort readSShort() throws IOException {
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
    return new SShort(val);
  }
  
  public Long readLong() throws IOException {
    
    int ch1 = in.read();
    int ch2 = in.read();
    int ch3 = in.read();
    int ch4 = in.read();
    if ((ch1 | ch2 | ch3 | ch4) < 0) {
        throw new EOFException();
    }
    short val;
    if(ByteOrder) {
      val = (short)((ch1 << 32) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }else{
      val = (short)((ch4 << 32) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }         
    return new Long(val);
  }
  
  public SLong readSLong() throws IOException {
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
    return new SLong(val);
  }

  
  public Undefined readUndefined() throws IOException {
    int ch = in.read();
    if (ch < 0) {
    throw new EOFException();
    }
    return new Undefined(ch);
  }

 



}
