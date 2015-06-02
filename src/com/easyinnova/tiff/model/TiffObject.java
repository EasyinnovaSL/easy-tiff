/**
 * <h1>TiffFile.java</h1>
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
 * @author Víctor Muñoz Solà
 * @version 1.0
 * @since 14/5/2015
 *
 */
package com.easyinnova.tiff.model;

import java.util.ArrayList;


/**
 * The Class TiffFile.
 */
public class TiffObject {

  /** The magic number. */
  private int magicNumber;

  /** The list of Ifd. */
  private ArrayList<IFD> ifds;

  /** The number of ifds. */
  private int nIfds;

  /**
   * Instantiates a new tiff file.
   *
   * @param data the data
   */
  public TiffObject() {
    ifds = new ArrayList<IFD>();
    nIfds = 0;
  }

  /**
   * Adds an IFD to the list.
   *
   * @param ifd the ifd
   */
  public void addIfd(IFD ifd) {
    ifds.add(ifd);
    nIfds++;
  }

  /**
   * Gets the ifd count.
   *
   * @return the ifd count
   */
  public int getIfdCount() {
    return nIfds;
  }

  /**
   * Gets the ifds.
   *
   * @return the ifds
   */
  public ArrayList<IFD> getIfds() {
    return ifds;
  }

  /**
   * Gets the ifd.
   *
   * @param index the index
   * @return the ifd
   */
  public IFD getIfd(int index) {
    return ifds.get(index);
  }

  /**
   * Gets the magic number.
   *
   * @return the magic number
   */
  public int getMagicNumber() {
    return magicNumber;
  }

  /**
   * Sets the magic number.
   *
   * @param magic the new magic number
   */
  public void setMagicNumber(int magic) {
    this.magicNumber = magic;
  }
}
