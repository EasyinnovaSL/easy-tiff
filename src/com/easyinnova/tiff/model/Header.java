/**
 * <h1>Header.java</h1> 
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
 * @author Víctor Muñoz Solà
 * @version 1.0
 * @since 20/5/2015
 *
 */
package com.easyinnova.tiff.model;

import com.easyinnova.tiff.io.TiffStreamIO;

import java.nio.ByteOrder;

/**
 * The Tiff Header.
 */
public class Header {

  /** The Byte order. */
  public ByteOrder byteOrder;

  /** The Magic number. */
  public int magicNumber;

  /** The Validation. */
  public ValidationResult validation;

  /** The data. */
  private TiffStreamIO data;
  
  /**
   * The byte order error tolerance.<br>
   * 0: No tolerance. 1: Lower case tolerance. 10: Full tolerance (assume little endian).
   * */
  private int byteOrderErrorTolerance = 0;

  /**
   * The magic number error tolerance.<br>
   * 0: No tolerance. 10: Full tolerance (assume 42).
   * */
  private int magicNumberTolerance = 0;

  /**
   * Instantiates a new header.
   *
   * @param data the data
   */
  public Header(TiffStreamIO data) {
    validation = new ValidationResult();
    this.data = data;
  }

  /**
   * Reads the header.
   *
   * @param data the data
   * @return true, if successful
   */
  public void read() {
    try {
      // read the first two bytes to know the byte ordering
      if (data.get(0) == 'I' && data.get(1) == 'I')
        byteOrder = ByteOrder.LITTLE_ENDIAN;
      else if (data.get(0) == 'M' && data.get(1) == 'M')
        byteOrder = ByteOrder.BIG_ENDIAN;
      else if (byteOrderErrorTolerance > 0 && data.get(0) == 'i' && data.get(1) == 'i') {
        validation.addWarning("Byte Order in lower case");
        byteOrder = ByteOrder.LITTLE_ENDIAN;
      } else if (byteOrderErrorTolerance > 0 && data.get(0) == 'm' && data.get(1) == 'm') {
        validation.addWarning("Byte Order in lower case");
        byteOrder = ByteOrder.BIG_ENDIAN;
      } else if (byteOrderErrorTolerance > 1) {
        validation.addWarning("Non-sense Byte Order. Trying Little Endian.");
        byteOrder = ByteOrder.LITTLE_ENDIAN;
      } else {
        validation.addError("Invalid Byte Order", "" + data.get(0) + data.get(1));
      }
    } catch (Exception ex) {
      validation.addError("Header format error");
    }

    if (validation.correct) {
      // set byte ordering
      data.order(byteOrder);

      try {
        // read magic number
        magicNumber = data.getShort(2);
        if (magicNumber != 42) {
          if (magicNumberTolerance > 0)
            validation.addWarning("Invalid magic number. Assuming 42.", magicNumber);
          else
            validation.addError("Magic number != 42", magicNumber);
        }
      } catch (IndexOutOfBoundsException ex) {
        validation.addError("Magic number format error");
      }
    }
  }

  /**
   * Writes the header.
   *
   * @param odata the odata
   */
  public void write(TiffStreamIO odata) {
    if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
      odata.put((byte) 'I');
      odata.put((byte) 'I');
    } else if (byteOrder == ByteOrder.BIG_ENDIAN) {
      odata.put((byte) 'M');
      odata.put((byte) 'M');
    }
    odata.order(byteOrder);

    odata.putShort((short) 42);

    odata.putInt(8);
  }
}

