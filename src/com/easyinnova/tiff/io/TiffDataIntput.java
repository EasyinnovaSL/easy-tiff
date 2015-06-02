/**
 * <h1>TiffDataIntput.java</h1> 
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
 * along with this program. If not, see <a href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a> and at
 * <a href="http://mozilla.org/MPL/2.0">http://mozilla.org/MPL/2.0</a> .
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
import com.easyinnova.tiff.model.types.Rational;
import com.easyinnova.tiff.model.types.SByte;
import com.easyinnova.tiff.model.types.SLong;
import com.easyinnova.tiff.model.types.SRational;
import com.easyinnova.tiff.model.types.SShort;
import com.easyinnova.tiff.model.types.Undefined;

import java.io.IOException;

/**
 * The Interface TiffDataIntput.
 */
public interface TiffDataIntput {
  
  
  /**
   * Skip bytes.
   *
   * @param n the n
   * @return the int
   * @throws IOException Signals that an I/O exception has occurred.
   */
  int skipBytes(int n) throws IOException;
  
  /**
   * Read byte.
   *
   * @return the byte
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Byte readByte() throws IOException;
  
  /**
   * Read ascii.
   *
   * @return the ascii
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Ascii readAscii() throws IOException;
  
  /**
   * Read ascii.
   *
   * @param n the n
   * @return the ascii
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Ascii readAscii(int n) throws IOException;

  /**
   * Read short.
   *
   * @return the short
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Short readShort() throws IOException;

  /**
   * Read long.
   *
   * @return the long
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Long readLong() throws IOException;

  /**
   * Read rational.
   *
   * @return the rational
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Rational readRational() throws IOException;

  /**
   * Read s byte.
   *
   * @return the s byte
   * @throws IOException Signals that an I/O exception has occurred.
   */
  SByte readSByte() throws IOException;

  /**
   * Read undefined.
   *
   * @return the undefined
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Undefined readUndefined() throws IOException;

  /**
   * Read s short.
   *
   * @return the s short
   * @throws IOException Signals that an I/O exception has occurred.
   */
  SShort readSShort() throws IOException;
  
  /**
   * Read s long.
   *
   * @return the s long
   * @throws IOException Signals that an I/O exception has occurred.
   */
  SLong readSLong() throws IOException;

  /**
   * Read s rational.
   *
   * @return the s rational
   * @throws IOException Signals that an I/O exception has occurred.
   */
  SRational readSRational() throws IOException;

  /**
   * Read float.
   *
   * @return the float
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Float readFloat() throws IOException;

  /**
   * Read double.
   *
   * @return the double
   * @throws IOException Signals that an I/O exception has occurred.
   */
  Double readDouble() throws IOException;

}
/*
1 = BYTE 8-bit unsigned integer.
2 = ASCII 8-bit byte that contains a 7-bit ASCII code; the last byte
must be NUL (binary zero).
3 = SHORT 16-bit (2-byte) unsigned integer.
4 = LONG 32-bit (4-byte) unsigned integer.
5 = RATIONAL Two LONGs: the first represents the numerator of a
fraction; the second, the denominator.
6 = SBYTE An 8-bit signed (twos-complement) integer.
7 = UNDEFINED An 8-bit byte that may contain anything, depending on
the definition of the field.
8 = SSHORT A 16-bit (2-byte) signed (twos-complement) integer.
9 = SLONG A 32-bit (4-byte) signed (twos-complement) integer.
10 = SRATIONAL Two SLONG’s: the first represents the numerator of a
fraction, the second the denominator.
11 = FLOAT Single precision (4-byte) IEEE format.
12 = DOUBLE Double precision (8-byte) IEEE format.
*/