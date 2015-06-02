/**
 * <h1>abstractTiffTag.java</h1> 
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
package com.easyinnova.tiff.model.types;

/**
 * The Class abstractTiffTag.
 */
public class abstractTiffType implements tiffType{

  /** The tag size. */
  private int typeSize;

  /** Number of values. */
  private int count;

  /**
   * Instantiates a new abstract tiff type.
   */
  public abstractTiffType() {
  }

  /**
   * Sets the n.
   *
   * @param n the new n
   */
  public void setN(int n) {
    this.count = n;
  }

  /**
   * Gets the n.
   *
   * @return the n
   */
  public int getN() {
    return count;
  }

  /**
   * Sets the type size.
   *
   * @param size the new type size
   */
  public void setTypeSize(int size) {
    typeSize = size;
  }

  /**
   * Gets the type size.
   *
   * @return the type size
   */
  public int getTypeSize() {
    return typeSize;
  }

  /**
   * Gets the total byte size.
   *
   * @return the total byte size
   */
  public int getTotalByteSize() {
    return count * typeSize;
  }

  /**
   * Checks if is offset.
   *
   * @return true, if is offset
   */
  public boolean isOffset() {
    return getTotalByteSize() > 4;
  }

  /**
   * Gets the int value.
   *
   * @return the int value
   */
  public int getNumericValue() {
    int intval = 0;
    try {
      intval = (int) ((com.easyinnova.tiff.model.types.Long) this).getValue();
    } catch (Exception ex) {
      try {
        intval = (int) ((com.easyinnova.tiff.model.types.Short) this).getValue();
      } catch (Exception ex2) {
        try {
          intval = (int) ((com.easyinnova.tiff.model.types.Byte) this).getValue();
        } catch (Exception ex3) {
          try {
            intval = (int) ((com.easyinnova.tiff.model.types.SByte) this).getIntValue();
          } catch (Exception ex4) {
            try {
              intval = (int) ((com.easyinnova.tiff.model.types.SLong) this).getIntValue();
            } catch (Exception ex5) {
              throw ex5;
            }
          }
        }
      }
    }
    return intval;
  }
}
