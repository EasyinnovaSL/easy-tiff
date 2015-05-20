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
package com.easyinnova.main;

import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;

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
  private MappedByteBuffer data;

  /**
   * Instantiates a new header.
   */
  public Header(MappedByteBuffer data) {
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
    // read the first two bytes to know the byte ordering
    if (data.get(0) == 'I' && data.get(1) == 'I')
      byteOrder = ByteOrder.LITTLE_ENDIAN;
    else if (data.get(0) == 'M' && data.get(1) == 'M')
      byteOrder = ByteOrder.BIG_ENDIAN;
    else {
      validation.addError("Invalid Byte Order", "" + data.get(0) + data.get(1));
    }

    // set byte ordering
    data.order(byteOrder);

    if (validation.correct) {
      try {
        magicNumber = data.getShort(2);
        if (magicNumber != 42) {
          validation.addError("Magic number != 42", magicNumber);
        }
      } catch (IndexOutOfBoundsException ex) {
        validation.addError("Magic number format error");
      }
    }
  }

}

