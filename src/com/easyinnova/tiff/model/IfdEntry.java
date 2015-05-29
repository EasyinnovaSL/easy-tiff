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
import com.easyinnova.tiff.model.types.IccProfile;
import com.easyinnova.tiff.model.types.abstractTiffType;

import java.util.ArrayList;

/**
 * The Class Tag.
 */
public class IfdEntry {

  /** The id. */
  int id;

  /** The type. */
  int type;

  /** The value. */
  abstractTiffType value;

  /** The Validation. */
  ValidationResult validation;

  /** The data. */
  TiffStreamIO data;

  /**
   * Instantiates a new tag.
   *
   * @param id Tag identifier
   * @param type Tag type
   * @param count Number of values
   * @param data the data
   */
  public IfdEntry(int id, int type, TiffStreamIO data) {
    this.id = id;
    this.type = type;
    this.data = data;
    validation = new ValidationResult();
  }

  /**
   * Gets the value if it fits in the tag field.<br>
   * Otherwise, sets the offset flag to true, indicating that the actual value of the tag is
   * contained in another position of the file.
   *
   * @param offset the position of the tag
   * @param n the cardinality
   * @return the value or offset
   */
  public void getValueOrOffset(int offset, int n) {
    switch (type) {
      case 1:
        value = new com.easyinnova.tiff.model.types.Byte((byte) data.get(offset));
        break;
      case 2:
        value = new com.easyinnova.tiff.model.types.Byte((byte) data.get(offset));
        break;
      case 6:
        value = new com.easyinnova.tiff.model.types.SByte((byte) data.get(offset));
        break;
      case 7:
        value = new com.easyinnova.tiff.model.types.Undefined((byte) data.get(offset));
        break;
      case 3:
        value = new com.easyinnova.tiff.model.types.Short((char) data.getShort(offset));
        break;
      case 8:
        value = new com.easyinnova.tiff.model.types.SShort((short) data.getShort(offset));
        break;
      case 4:
        value = new com.easyinnova.tiff.model.types.Long((long) data.getInt(offset));
        break;
      case 9:
        value = new com.easyinnova.tiff.model.types.SLong((long) data.getInt(offset));
        break;
      case 5:
        value =
            new com.easyinnova.tiff.model.types.Rational(data.getInt(offset),
                data.getInt(offset + 4));
        break;
      case 10:
        value =
            new com.easyinnova.tiff.model.types.SRational(data.getInt(offset),
                data.getInt(offset + 4));
        break;
      case 11:
        value = new com.easyinnova.tiff.model.types.Float(data.getInt(offset));
        break;
      case 12:
        value = new com.easyinnova.tiff.model.types.Double(data.getInt(offset));
        break;
    }
    if (id == 34675) {
      value = new IccProfile(getNumericValue(), n, data);
      validation.add(((IccProfile) value).validation);
    }

    value.setN(n);
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
        if (card != value.getN())
          validation.addError("Cardinality for tag " + id + " must be " + card, value.getN());
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
    if (!value.isOffset())
      s += value.toString();
    else if (value.isOffset()) {
      if (id == 34675) {
        s += value.toString();
      } else if (id == 700 || id == 33723 || id == 34377) {
        // XMP, IPTC, Photoshop
      } else {
        if (value.getN() > 1)
          s += "[";
        for (int i = 0; i < value.getN(); i++) {
          if (i > 0 && type != 2)
            s += ", ";
          int intval = (int) ((com.easyinnova.tiff.model.types.Long) value).getValue();
          int offset = intval + i * value.getTypeSize();

          switch (value.getTypeSize()) {
            case 1:
              if (type == 2) {
                if (data.get(offset) != 0)
                  s += (char) (data.get(offset));
              } else
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
        if (value.getN() > 1)
          s += "]";
      }
    }
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
      intval = (int) ((com.easyinnova.tiff.model.types.Long) value).getValue();
    } catch (Exception ex) {
      try {
        intval = (int) ((com.easyinnova.tiff.model.types.Short) value).getValue();
      } catch (Exception ex2) {
        try {
          intval = (int) ((com.easyinnova.tiff.model.types.Byte) value).getValue();
        } catch (Exception ex3) {
          throw ex3;
        }
      }
    }
    return intval;
  }

  /**
   * Gets the int array.
   *
   * @return the int array or null if error
   */
  public ArrayList<Integer> getIntArray() {
    ArrayList<Integer> l = new ArrayList<Integer>();
    try {
      for (int i = 0; i < value.getN(); i++) {
        switch (value.getTypeSize()) {
          case 1:
            l.add(data.get(getNumericValue() + i * 2));
            break;
          case 2:
            l.add(data.getShort(getNumericValue() + i * 2));
            break;
          case 4:
            l.add(data.getInt(getNumericValue() + i * 2));
            break;
          case 8:
            l.add((int) data.getLong(getNumericValue() + i * 2));
            break;
        }
      }
    } catch (Exception ex) {
      l = null;
    }
    return l;
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
    odata.putInt((int) value.getN());
    odata.putInt(getNumericValue());
  }

  /**
   * Write content.
   *
   * @param odata the odata
   * @return the int
   */
  public int writeContent(TiffStreamIO odata) {
    int totalSize = value.getTotalByteSize();
    for (int i = 0; i < totalSize; i++) {
      int v = data.get(getNumericValue() + i);
      odata.put((byte) v);
    }
    return totalSize;
  }

  /**
   * Sets the int value.
   *
   * @param offset the new int value
   */
  public void setIntValue(int offset) {
    ((com.easyinnova.tiff.model.types.Long) value).setValue(offset);
  }
}
