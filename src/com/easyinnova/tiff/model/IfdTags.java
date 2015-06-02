/**
 * <h1>IfdTags.java</h1> 
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
 * @since 20/5/2015
 *
 */
package com.easyinnova.tiff.model;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The Class IfdTags.
 */
public class IfdTags {
  /** Tag list. */
  public ArrayList<IfdEntry> tags;

  /** The Hash tags id. */
  public HashMap<Integer, IfdEntry> hashTagsId;

  /** The Hash tags name. */
  public HashMap<String, IfdEntry> hashTagsName;

  /** The validation. */
  ValidationResult validation;

  /**
   * The tag order tolerance.<br>
   * 0: No tolerance. 10: Full tolerance (no matter if tags are not in ascending order)
   * */
  private int tagOrderTolerance = 10;

  /**
   * Instantiates a new ifd tags.
   */
  public IfdTags() {
    tags = new ArrayList<IfdEntry>();
    hashTagsId = new HashMap<Integer, IfdEntry>();
    hashTagsName = new HashMap<String, IfdEntry>();
    validation = new ValidationResult();
  }

  /**
   * Adds a tag.
   *
   * @param tag the tag
   */
  public void addTag(IfdEntry tag) {
    tags.add(tag);
    hashTagsId.put(tag.id, tag);
    Tag t = TiffTags.getTag(tag.id);
    if (t != null)
      hashTagsName.put(t.name, tag);
  }

  /**
   * Validates the ifd entries.
   *
   * @param validation the validation
   */
  public void validate() {
    int prevTagId = 0;
    for (IfdEntry ie : tags) {
      ie.validate();
      validation.add(ie.validation);
      if (ie.id < prevTagId) {
        if (tagOrderTolerance > 0)
          validation.addWarning("Tags are not in ascending order");
        else
          validation.addError("Tags are not in ascending order");
      }
      prevTagId = ie.id;
    }
  }

  /**
   * Contains tag id.
   *
   * @param id the id
   * @return true, if successful
   */
  public boolean containsTagId(int id) {
    return hashTagsId.containsKey(id);
  }

  /**
   * Gets the Tag.
   *
   * @param id the id
   * @return the IfdEntry
   */
  public IfdEntry get(int id) {
    return hashTagsId.get(id);
  }
}

