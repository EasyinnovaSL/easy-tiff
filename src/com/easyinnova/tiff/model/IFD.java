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
package com.easyinnova.tiff.model;

/**
 * The Class IFD.
 */
public class IFD {

  /** The tags. */
  private IfdTags metadata;

  /** The next ifd. */
  private IFD nextIFD;

  /**
   * Instantiates a new ifd.
   *
   * @param id the id
   */
  public IFD() {
    metadata = new IfdTags();
    nextIFD = null;
  }

  /**
   * Checks for next ifd.
   *
   * @return true, if next IFD exists
   */
  public boolean hasNextIFD() {
    return nextIFD != null;
  }

  /**
   * Gets the metadata.
   *
   * @param name the name
   * @return the metadata
   */
  public TagValue getTag(String name) {
    int id = TiffTags.getTagId(name);
    return metadata.get(id);
  }

  /**
   * Gets the metadata.
   *
   * @return the metadata
   */
  public IfdTags getMetadata() {
    return metadata;
  }

  /**
   * Adds the metadata.
   *
   * @param tv the tv
   */
  public void addMetadata(TagValue tv) {
    metadata.addTag(tv);
  }

  /**
   * Contains tag id.
   *
   * @param tagid the tagid
   * @return true, if successful
   */
  public boolean containsTagId(int tagid) {
    return metadata.containsTagId(tagid);
  }

  /**
   * Prints the tags.
   */
  public void printTags() {
    for (TagValue ie : metadata.getTags()) {
      try {
        String name = TiffTags.getTag(ie.getId()).name;
        String val = ie.toString();
        String type = TiffTags.tagTypes.get(ie.getType());
        System.out.println(name + "(" + ie.getType() + "->" + type + "): " + val);
      } catch (Exception ex) {
        System.out.println("Tag error");
      }
    }
  }
}
