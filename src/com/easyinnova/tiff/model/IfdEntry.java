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
package com.easyinnova.tiff.model;

import com.easyinnova.tiff.io.TiffStreamIO;
import com.easyinnova.tiff.model.types.TagValue;

/**
 * The Class Tag.
 */
public class IfdEntry {

  /** The id. */
  public int id;

  /** The type. */
  public int type;

  /** The value. */
  public TagValue value;

  /** The Validation. */
  public ValidationResult validation;

  /**
   * Instantiates a new tag.
   *
   * @param id Tag identifier
   * @param type Tag type
   * @param count Number of values
   * @param data the data
   */
  public IfdEntry(int id, int type) {
    this.id = id;
    this.type = type;
    validation = new ValidationResult();
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
      if (!t.validType(stype)) {
        String stypes = "";
        for (String tt : t.type) {
          if (stypes.length() > 0)
            stypes += ",";
          stypes += tt;
        }
        validation.addError("Invalid type for tag " + id + "[" + stypes + "]", stype);
      }
      try {
        int card = Integer.parseInt(t.cardinality);
        if (card != value.getValue().size())
          validation.addError("Cardinality for tag " + id + " must be " + card, value.getValue()
              .size());
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
    int n = value.getValue().size();
    if (n > 1)
      s += "[";
    for (int i = 0; i < n; i++) {
      s += value.getValue().get(i).toString();
      if (n > 1 && i + 1 < n && type != 2)
        s += ",";
    }
    if (n > 1)
      s += "]";
    return s;
  }

  /**
   * Gets the int value.
   *
   * @return the int value
   */
  public int getNumericValue() {
    int intval = 0;
    try {
      intval = (int) ((com.easyinnova.tiff.model.types.Long) value.getValue().get(0)).getValue();
    } catch (Exception ex) {
      try {
        intval = (int) ((com.easyinnova.tiff.model.types.Short) value.getValue().get(0)).getValue();
      } catch (Exception ex2) {
        try {
          intval =
              (int) ((com.easyinnova.tiff.model.types.Byte) value.getValue().get(0)).getValue();
        } catch (Exception ex3) {
          throw ex3;
        }
      }
    }
    return intval;
  }

  /**
   * Write.
   *
   * @param odata the odata
   * @param offset the offset
   */
  public void write(TiffStreamIO odata) {
    odata.putShort((short) id);
    odata.putShort((short) type);
    odata.putInt((int) value.getValue().size());
    odata.putInt(getNumericValue());
  }

  /**
   * Sets the int value.
   *
   * @param offset the new int value
   */
  public void setIntValue(int offset) {
    // TODO: All
  }

  /**
   * @param tv
   */
  public void setValue(TagValue tv) {
    value = tv;
  }
}
