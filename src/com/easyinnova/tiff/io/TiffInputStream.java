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

import com.easyinnova.tiff.model.types.Ascii;
import com.easyinnova.tiff.model.types.Byte;
import com.easyinnova.tiff.model.types.Double;
import com.easyinnova.tiff.model.types.Float;
import com.easyinnova.tiff.model.types.Long;
import com.easyinnova.tiff.model.types.Rational;
import com.easyinnova.tiff.model.types.SByte;
import com.easyinnova.tiff.model.types.SLong;
import com.easyinnova.tiff.model.types.SRational;
import com.easyinnova.tiff.model.types.SShort;
import com.easyinnova.tiff.model.types.Short;
import com.easyinnova.tiff.model.types.Undefined;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * The Class DataByteOrderInputStream.
 */
public class TiffInputStream extends RandomAccessFileInputStream implements TiffDataIntput {

  /** The Byte order. */
  private ByteOrder byteOrder;

  /**
   * Instantiates a new data byte order input stream.
   * @param file file
   * @throws FileNotFoundException sdf
   */
  public TiffInputStream(File file) throws FileNotFoundException {
  super(file);
    byteOrder = ByteOrder.BIG_ENDIAN;
  }
  
  /**
   * Gets the byte order.
   *
   * @return the byteOrder
   */
  public ByteOrder getByteOrder() {
    return byteOrder;
  }

  /**
   * Sets the byte order.
   *
   * @param byteOrder the byteOrder to set
   */
  public void setByteOrder(ByteOrder byteOrder) {
    this.byteOrder = byteOrder;
  }

  /**
   * Read byte.
   *
   * @param position the position
   * @return the byte
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Byte readByte(long position) throws IOException {
    seek(position);
    return readByte();
  }

  @Override
  public Byte readByte() throws IOException {
    int ch = read();
    if (ch < 0) {
    throw new EOFException();
    }
    return new Byte(ch);
  }
  
  /**
   * Read ascii.
   *
   * @param position the position
   * @return the ascii
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Ascii readAscii(long position) throws IOException {
    seek(position);
    return readAscii();
  }

  @Override
  public Ascii readAscii() throws IOException {
    int ch = read();
    if (ch < 0) {
    throw new EOFException();
    }
    return new Ascii(ch);
  }
  
  /**
   * Read s byte.
   *
   * @param position the position
   * @return the s byte
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public SByte readSByte(long position) throws IOException {
    seek(position);
    return readSByte();
  }

  @Override
  public SByte readSByte() throws IOException {
    
    int ch = read();
    if (ch < 0) {
    throw new EOFException();
    }
    return new SByte(ch);
  }

  /**
   * Read short.
   *
   * @param position the position
   * @return the short
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Short readShort(long position) throws IOException {
    seek(position);
    return readShort();
  }

  @Override
  public Short readShort() throws IOException {
    int ch1 = read();
    int ch2 = read();
    if ((ch1 | ch2) < 0) {
        throw new EOFException();
    }
    short val;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      val = (short)((ch1 << 8) + (ch2 << 0));
    }else{
      val = (short)((ch2 << 8) + (ch1 << 0));
    }         
    return new Short(val);
  }
  
  /**
   * Read s short.
   *
   * @param position the position
   * @return the s short
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public SShort readSShort(long position) throws IOException {
    seek(position);
    return readSShort();
  }

  @Override
  public SShort readSShort() throws IOException {
    int ch1 = read();
    int ch2 = read();
    if ((ch1 | ch2) < 0) {
      throw new EOFException();
    }
    short val;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      val = (short)((ch1 << 8) + (ch2 << 0));
    }else{
      val = (short)((ch2 << 8) + (ch1 << 0));
    }         
    return new SShort(val);
  }
  
  /**
   * Read long.
   *
   * @param position the position
   * @return the long
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Long readLong(long position) throws IOException {
    seek(position);
    return readLong();
  }

  @Override
  public Long readLong() throws IOException {
    
    int ch1 = read();
    int ch2 = read();
    int ch3 = read();
    int ch4 = read();
    if ((ch1 | ch2 | ch3 | ch4) < 0) {
        throw new EOFException();
    }
    int val;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      val = (int)((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }else{
      val = (int)((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }         
    return new Long(val);
  }
  
  /**
   * Read s long.
   *
   * @param position the position
   * @return the s long
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public SLong readSLong(long position) throws IOException {
    seek(position);
    return readSLong();
  }

  @Override
  public SLong readSLong() throws IOException {
    int ch1 = read();
    int ch2 = read();
    int ch3 = read();
    int ch4 = read();
    if ((ch1 | ch2 | ch3 | ch4) < 0) {
        throw new EOFException();
    }
    int val;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      val = (int)((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }else{
      val = (int)((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }         
    return new SLong(val);
  }

  /**
   * Read undefined.
   *
   * @param position the position
   * @return the undefined
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Undefined readUndefined(long position) throws IOException {
    seek(position);
    return readUndefined();
  }
  
  @Override
  public Undefined readUndefined() throws IOException {
    int ch = read();
    if (ch < 0) {
    throw new EOFException();
    }
    return new Undefined(ch);
  }

  /**
   * Read rational.
   *
   * @param position the position
   * @return the rational
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Rational readRational(long position) throws IOException {
    seek(position);
    return readRational();
  }

  @Override
  public Rational readRational() throws IOException {
    int ch1 = read();
    int ch2 = read();
    int ch3 = read();
    int ch4 = read();
    if ((ch1 | ch2 | ch3 | ch4) < 0) {
        throw new EOFException();
    }
    int val;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      val = (int)((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }else{
      val = (int)((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }     
    
    ch1 = read();
    ch2 = read();
    ch3 = read();
    ch4 = read();
    if ((ch1 | ch2 | ch3 | ch4) < 0) {
        throw new EOFException();
    }
    int val2;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      val2 = (int)((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }else{
      val2 = (int)((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }     
    
    return new Rational(val,val2);
  }

  /**
   * Read s rational.
   *
   * @param position the position
   * @return the s rational
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public SRational readSRational(long position) throws IOException {
    seek(position);
    return readSRational();
  }

  @Override
  public SRational readSRational() throws IOException {
    int ch1 = read();
    int ch2 = read();
    int ch3 = read();
    int ch4 = read();
    if ((ch1 | ch2 | ch3 | ch4) < 0) {
        throw new EOFException();
    }
    int val;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      val = (int)((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }else{
      val = (int)((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }     
    
    ch1 = read();
    ch2 = read();
    ch3 = read();
    ch4 = read();
    if ((ch1 | ch2 | ch3 | ch4) < 0) {
        throw new EOFException();
    }
    int val2;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      val2 = (int)((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }else{
      val2 = (int)((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }     
    
    return new SRational(val,val2);
  }

  /**
   * Read float.
   *
   * @param position the position
   * @return the float
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Float readFloat(long position) throws IOException {
    seek(position);
    return readFloat();
  }

  @Override
  public Float readFloat() throws IOException {
    int ch1 = read();
    int ch2 = read();
    int ch3 = read();
    int ch4 = read();
    if ((ch1 | ch2 | ch3 | ch4) < 0) {
        throw new EOFException();
    }
    int val;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      val = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }else{
      val = ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }         
    return new Float( java.lang.Float.intBitsToFloat(val));
  }

  /**
   * Read double.
   *
   * @param position the position
   * @return the double
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public Double readDouble(long position) throws IOException {
    seek(position);
    return readDouble();
  }

  @Override
  public Double readDouble() throws IOException {
    
    int ch1 = read();
    int ch2 = read();
    int ch3 = read();
    int ch4 = read();
    int ch5 = read();
    int ch6 = read();
    int ch7 = read();
    int ch8 = read();
    
    if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8 ) < 0) {
        throw new EOFException();
    }
    
    long val;
    
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      
     val= ((long)ch1 << 56) + ((long)(ch2 & 255) << 48) + ((long)(ch3 & 255) << 40) 
       + ((long)(ch4 & 255) << 32) + ((long)(ch5 & 255) << 24) + ((long)(ch6 & 255) << 16) 
       + ((long)(ch7 & 255) <<  8) + ((long)(ch8 & 255) <<  0);
 
    }else{
      
      val= ((long)ch8 << 56) + ((long)(ch7 & 255) << 48) + ((long)(ch6 & 255) << 40) 
          + ((long)(ch5 & 255) << 32) + ((long)(ch4 & 255) << 24) + ((long)(ch3 & 255) << 16) 
          + ((long)(ch2 & 255) <<  8) + ((long)(ch1 & 255) <<  0);
    }
    
    return new Double(java.lang.Double.longBitsToDouble(val));
  }

  /**
   * File size in bytes.
   *
   * @return the file size.
   */
  public long size() {
    return super.size();
  }
}
