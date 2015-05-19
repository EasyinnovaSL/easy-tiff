/**
 * <h1>IFD.java</h1>
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
package com.easyinnova.main;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The Class IFD.
 */
public class IFD {

  /** Tag list. */
  public ArrayList<IfdEntry> Tags;

  /** The Hash tags id. */
  public HashMap<Integer, IfdEntry> HashTagsId;

  /** The Hash tags name. */
  public HashMap<String, IfdEntry> HashTagsName;

  /** The Next ifd. */
  public int NextIFD = 0;

  /**
   * Instantiates a new ifd.
   */
  public IFD() {
    Tags = new ArrayList<IfdEntry>();
    HashTagsId = new HashMap<Integer, IfdEntry>();
    HashTagsName = new HashMap<String, IfdEntry>();
  }

  /**
   * Adds a tag.
   *
   * @param tag the tag
   */
  public void AddTag(IfdEntry tag) {
    Tags.add(tag);
    HashTagsId.put(tag.id, tag);
    Tag t = TiffTags.getTag(tag.id);
    if (t != null)
      HashTagsName.put(t.name, tag);
  }

  /**
   * Checks for next ifd.
   *
   * @return true, if next IFD exists
   */
  public boolean hasNextIFD() {
    return NextIFD > 0;
  }

  /**
   * Get Next IFD.
   *
   * @return the next ifd
   */
  public int nextIFDOffset() {
    return NextIFD;
  }

  /**
   * Validates the IFD.
   *
   * @param validation_result the validation_result
   */
  public void Validate(ValidationResult validation_result) {
    for (IfdEntry ie : Tags) {
      ie.Validate(validation_result);
    }
  }

  /**
   * Gets the tag value.
   *
   * @param string the string
   * @return the tag value
   */
  public int getTagValue(String tagname) {
    int val = 0;
    if (HashTagsName.containsKey(tagname))
      val = HashTagsName.get(tagname).value;
    return val;
  }
}
