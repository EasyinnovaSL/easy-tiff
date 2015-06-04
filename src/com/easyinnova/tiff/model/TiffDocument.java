/**
 * <h1>TiffFile.java</h1>
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
package com.easyinnova.tiff.model;

import com.easyinnova.tiff.model.types.IFD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Modeling of the TIFF file with methods to access its IFDs and metadata.
 */
public class TiffDocument {

  /** The magic number. */
  private int magicNumber;

  /** The list of Ifd. */
  private IFD firstIFD;

  /** The metadata. */
  private HashMap<String, List<TiffObject>> dictionary;

  /**
   * Instantiates a new tiff file.
   */
  public TiffDocument() {
    firstIFD = null;
    dictionary = null;
  }

  /**
   * Adds an IFD to the model.
   *
   * @param ifd the ifd
   */
  public void addIfd0(IFD ifd) {
    firstIFD = ifd;
  }

  /**
   * Gets the ifd count.
   *
   * @return the ifd count
   */
  public int getIfdCount() {
    return getMetadataList("IFD").size();
  }

  /**
   * Gets the Subifd count.
   *
   * @return the Subifd count
   */
  public int getSubIfdCount() {
    return getMetadataList("SubIFDs").size();
  }

  /**
   * Gets the count of IFDs and SubIFDs.
   *
   * @return the ifd count
   */
  public int getIfdAndSubIfdCount() {
    return getMetadataList("IFD").size() + getMetadataList("SubIFDs").size();
  }

  /**
   * Returns a list of ifds.
   *
   * @return the ifds list
   */
  public List<TiffObject> getIfds() {
    return getMetadataList("IFD");
  }

  /**
   * Returns a list of subifds.
   *
   * @return the subifds list
   */
  public List<TiffObject> getSubIfds() {
    return getMetadataList("SubIFDs");
  }

  /**
   * Returns a list of subifds.
   *
   * @return the subifds list
   */
  public List<TiffObject> getIfdsAndSubIfds() {
    List<TiffObject> all = new ArrayList<TiffObject>();
    all.addAll(getMetadataList("IFD"));
    all.addAll(getMetadataList("SubIFDs"));
    return all;
  }

  /**
   * Returns the first image of the file.
   *
   * @return image file d
   */
  public IFD getFirstIFD() {
    return (IFD) getIfds().get(0);
  }

  /**
   * Gets the magic number of the Tiff file.
   *
   * @return the magic number
   */
  public int getMagicNumber() {
    return magicNumber;
  }

  /**
   * Sets the magic number of the Tiff file.
   *
   * @param magic the new magic number
   */
  public void setMagicNumber(int magic) {
    this.magicNumber = magic;
  }

  /**
   * Gets an string with the value of the first tag matching the given tag name.<br>
   *
   * @param name the tag name
   * @return the metadata single string
   */
  public String getMetadataSingleString(String name) {
    if (dictionary == null)
      createMetadataDictionary();
    if (dictionary.containsKey(name))
      return dictionary.get(name).get(0).toString();
    else
      return "";
  }

  /**
   * Gets the metadata ok a given class name.
   *
   * @param name the class name
   * @return the list of metadata that matches with the class name
   */
  public List<TiffObject> getMetadataList(String name) {
    if (dictionary == null)
      createMetadataDictionary();
    if (dictionary.containsKey(name))
      return dictionary.get(name);
    else
      return new ArrayList<TiffObject>();
  }

  /**
   * Creates the metadata dictionary.
   */
  private void createMetadataDictionary() {
    dictionary = new HashMap<String, List<TiffObject>>();
    String key = "IFD";
    if (firstIFD != null) {
      addMetadata(key, firstIFD);
      for (TagValue tag : firstIFD.getMetadata().getTags()) {
        addMetadata(tag.getName(), tag);
      }
      IFD currentIFD = firstIFD;
      while (currentIFD.hasNextIFD()) {
        addMetadata(key, currentIFD);
        for (TagValue tag : currentIFD.getMetadata().getTags()) {
          addMetadata(tag.getName(), tag);
        }
        currentIFD = currentIFD.getNextIFD();
      }
    }
  }

  /**
   * Adds metadata to the tiff model.
   *
   * @param key the classname
   * @param data the data
   */
  private void addMetadata(String key, TiffObject data) {
    if (!dictionary.containsKey(key))
      dictionary.put(key, new ArrayList<TiffObject>());
    dictionary.get(key).add(data);
  }
}
