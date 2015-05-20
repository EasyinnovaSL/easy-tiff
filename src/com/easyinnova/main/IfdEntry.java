/**
 * <h1>Tag.java</h1> 
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
 * @since 14/5/2015
 *
 */
package com.easyinnova.main;

import java.nio.MappedByteBuffer;

/**
 * The Class Tag.
 */
public class IfdEntry {

  /** The id. */
  int id;

  /** The type. */
  int type;

  /** Number of values. */
  int n;

  /** The value. */
  public TagValue value;

  /**
   * Instantiates a new tag.
   *
   * @param id Tag identifier
   * @param type Tag type
   * @param n Number of values
   */
  public IfdEntry(int id, int type, int n) {
    this.id = id;
    this.type = type;
    this.n = n;
  }

  /**
   * Gets the value.
   *
   * @param data Tiff File
   * @param offset the position of the tag
   * @param tagType the tag type
   */
  public void getValue(MappedByteBuffer data, int offset, int tagType) {
    int size = 0;
    switch (tagType) {
      case 1:
      case 2:
      case 6:
      case 7:
        size = n;
        break;
      case 3:
      case 8:
        size = 2 * n;
        break;
      case 4:
      case 9:
        size = 4 * n;
        break;
      case 5:
      case 10:
        size = 8 * n;
        break;
      case 11:
        size = 4 * n;
        break;
      case 12:
        size = 8 * n;
        break;
    }
    if (size == 1)
      value = new TagValue(data.get(offset), false);
    else if (size == 2)
      value = new TagValue(data.getShort(offset), false);
    else if (size == 4)
      value = new TagValue(data.getInt(offset), false);
    else if (size > 4)
      value = new TagValue(data.getInt(offset), true);
  }

  /**
   * Validates the tag.
   *
   */
  public void validate(ValidationResult validation_result) {
    TiffTags.getTiffTags();
    if (!TiffTags.tagMap.containsKey(id))
      validation_result.addError("Undefined tag id " + id);
    else if (!TiffTags.tagTypes.containsKey(type))
      validation_result.addWarning("Unknown tag type " + type);
    else {
      Tag t = TiffTags.getTag(id);
      String stype = TiffTags.tagTypes.get(type);
      if (!t.type.contains(stype))
        validation_result.addError("Invalid type for tag " + id, stype);
      try {
        int card = Integer.parseInt(t.cardinality);
        if (card != n)
          validation_result.addError("Cardinality for tag " + id + " must be " + card, n);
      } catch (Exception e) {
        e.toString();
      }
    }
  }
}
