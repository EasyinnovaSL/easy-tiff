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
  int value;

  /**
   * Instantiates a new tag.
   *
   * @param id Tag identifier
   * @param type Tag type
   * @param n Number of values
   * @param val Tag value
   */
  public IfdEntry(int id, int type, int n, int val) {
    this.id = id;
    this.type = type;
    this.n = n;
    this.value = val;
  }

  /**
   * Validates the tag.
   *
   */
  public void Validate(ValidationResult validation_result) {
    TiffTags.getTiffTags();
    if (!TiffTags.tagMap.containsKey(id))
      validation_result.addError("Undefined tag id " + id);
    else if (!TiffTags.tagTypes.containsKey(type))
      validation_result.addError("Unknown tag type " + type);
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
