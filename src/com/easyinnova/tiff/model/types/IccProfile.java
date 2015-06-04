/**
 * <h1>IccProfile.java</h1> 
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
 * @since 25/5/2015
 *
 */
package com.easyinnova.tiff.model.types;

import com.easyinnova.tiff.io.TiffStreamIO;
import com.easyinnova.tiff.model.ValidationResult;

/**
 * The Class IccProfile.
 */
public class IccProfile extends abstractTiffType {

  /** The tags. */
  public IccTags tags;

  /** The data. */
  TiffStreamIO data;

  /** The validation result. */
  public ValidationResult validation;

  /**
   * Instantiates a new icc profile.
   *
   * @param offset the offset in bytes to the beginning of the ICC Profile
   * @param size the size in bytes of the embedded ICC Profile
   * @param data the data
   */
  public IccProfile(int offset, int size, TiffStreamIO data) {
    this.data = data;
    validation = new ValidationResult();
    setTypeSize(size);
    tags = new IccTags();
    readIccProfile(offset, size);
  }

  /**
   * Read icc profile.
   *
   * @param offset the offset
   * @param size the size
   */
  private void readIccProfile(int offset, int size) {
    int iccsize = data.getInt(offset);
    if (iccsize != size)
      validation.addError("ICC Profile size does not match");

    int index = offset + 128;
    int tagCount = data.getInt(index);

    for (int i = 0; i < tagCount; i++) {
      int signature = data.getInt(index);
      int tagOffset = data.getInt(index + 4);
      int tagSize = data.getInt(index + 8);
      IccTag tag = new IccTag(signature, tagOffset, tagSize);
      tags.addTag(tag);
      index += 12;
    }
  }

  @Override
  public String toString() {
    String s = "";
    for (IccTag tag : tags.tags) {
      s += "[" + tag.signature + "->" + tag.offset + "]";
    }
    return s;
  }
}

