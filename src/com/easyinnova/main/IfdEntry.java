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
 * NB: for the � statement, include Easy Innova SL or other company/Person contributing the code.
 * </p>
 * <p>
 * � 2015 Easy Innova, SL
 * </p>
 *
 * @author V�ctor Mu�oz Sol�
 * @version 1.0
 * @since 14/5/2015
 *
 */
package com.easyinnova.main;

import java.nio.MappedByteBuffer;
import java.util.ArrayList;

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

  /** The tagsize. */
  int tagsize;

  /** The Value. */
  long value;

  /** The is offset. */
  boolean isOffset;

  /** The Validation. */
  ValidationResult validation;

  /** The data. */
  MappedByteBuffer data;

  /**
   * Instantiates a new tag.
   *
   * @param id Tag identifier
   * @param type Tag type
   * @param n Number of values
   */
  public IfdEntry(int id, int type, int n, MappedByteBuffer data) {
    this.id = id;
    this.type = type;
    this.n = n;
    this.data = data;
    isOffset = false;
    validation = new ValidationResult();
  }

  /**
   * Gets the value if it fits in the tag field.<br>
   * Otherwise, sets the offset flag to true, indicating that the actual value of the tag is
   * contained in another position of the file.
   *
   * @param data Tiff File
   * @param offset the position of the tag
   * @param tagType the tag type
   */
  public void getValueOrOffset(int offset) {
    switch (type) {
      case 1:
      case 2:
      case 6:
      case 7:
        tagsize = 1;
        break;
      case 3:
      case 8:
        tagsize = 2;
        break;
      case 4:
      case 9:
        tagsize = 4;
        break;
      case 5:
      case 10:
        tagsize = 8;
        break;
      case 11:
        tagsize = 4;
        break;
      case 12:
        tagsize = 8;
        break;
    }
    int totalsize = tagsize * n;
    if (totalsize == 1) {
      value = data.get(offset);
    } else if (totalsize == 2) {
      value = data.getShort(offset);
    } else if (totalsize == 4) {
      value = data.getInt(offset);
    } else if (totalsize > 4) {
      value = data.getInt(offset);
      isOffset = true;
    }
  }

  /**
   * Validates that the tag type and cardinality have correct values.
   *
   */
  public void validate() {
    TiffTags.getTiffTags();
    if (!TiffTags.tagMap.containsKey(id))
      validation.addError("Undefined tag id " + id);
    else if (!TiffTags.tagTypes.containsKey(type))
      validation.addWarning("Unknown tag type " + type);
    else {
      Tag t = TiffTags.getTag(id);
      String stype = TiffTags.tagTypes.get(type);
      if (!t.type.contains(stype))
        validation.addError("Invalid type for tag " + id, stype);
      try {
        int card = Integer.parseInt(t.cardinality);
        if (card != n)
          validation.addError("Cardinality for tag " + id + " must be " + card, n);
      } catch (Exception e) {
        // TODO: Deal with formulas?
      }
    }
  }

  /**
   * Gets the value.
   *
   * @return the value
   */
  public String toString() {
    String s = "";
    if (!isOffset)
      s += value;
    else if (isOffset) {
      if (n > 1)
        s += "[";
      for (int i = 0; i < n; i++) {
        if (i > 0 && type != 2)
          s += ", ";
        int offset = (int) value + i * tagsize;

        switch (tagsize) {
          case 1:
            if (type == 2)
              s += (char) (data.get(offset));
            else
              s += data.get(offset);
            break;
          case 2:
            s += data.getShort(offset);
            break;
          case 4:
            s += data.getInt(offset);
            break;
          case 8:
            if (type == 5 || type == 10)
              s += data.getInt(offset) + "/" + data.getInt(offset + 4);
            else
              s += data.getLong(offset);
            break;
        }
      }
      if (n > 1)
        s += "]";
    }
    return s;
  }

  /**
   * Gets the int value.
   *
   * @return the int value
   */
  public int getIntValue() {
    int val = 0;
    if (!isOffset)
      val = (int) value;
    return val;
  }

  /**
   * Gets the rational value.
   *
   * @return the rational value
   */
  public float getRationalValue() {
    float val = 0;
    if (isOffset) {
      int numerator = data.getInt((int) value);
      int denominator = data.getInt((int) value + 4);
      val = (float) numerator / denominator;
    }
    return val;
  }

  /**
   * Gets the int array.
   *
   * @return the int array
   */
  public ArrayList<Integer> getIntArray() {
    ArrayList<Integer> l = new ArrayList<Integer>();
    for (int i=0;i<n;i++) {
      l.add(data.getInt((int) value + i * 2));
    }
    return l;
  }
}
