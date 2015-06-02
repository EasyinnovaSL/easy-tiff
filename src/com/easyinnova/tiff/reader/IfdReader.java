/**
 * <h1>IfdReader.java</h1> 
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
 * NB: for the � statement, include Easy Innova SL or other company/Person contributing the code.
 * </p>
 * <p>
 * � 2015 Easy Innova, SL
 * </p>
 *
 * @author V�ctor Mu�oz Sol�
 * @version 1.0
 * @since 2/6/2015
 *
 */
package com.easyinnova.tiff.reader;

import com.easyinnova.tiff.model.IFD;

/**
 * The Class IfdReader.
 */
public class IfdReader {

  /** The ifd. */
  IFD ifd;

  /** The next ifd offset. */
  int nextIfdOffset;

  /**
   * Gets the next ifd offset.
   *
   * @return the next ifd offset
   */
  public int getNextIfdOffset() {
    return nextIfdOffset;
  }

  /**
   * Gets the ifd.
   *
   * @return the ifd
   */
  public IFD getIfd() {
    return ifd;
  }

  /**
   * Sets the ifd.
   *
   * @param ifd the new ifd
   */
  public void setIfd(IFD ifd) {
    this.ifd = ifd;
  }

  /**
   * Sets the next ifd offset.
   *
   * @param offset the new next ifd offset
   */
  public void setNextIfdOffset(int offset) {
    nextIfdOffset = offset;
  }
}

