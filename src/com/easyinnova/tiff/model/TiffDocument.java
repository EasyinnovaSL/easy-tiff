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
  private HashMap<String, List<TiffObject>> metadata;

  /**
   * Instantiates a new tiff file.
   */
  public TiffDocument() {
    firstIFD = null;
    metadata = null;
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
    if (metadataContains("IFD"))
      return getMetadataList("IFD").size();
    else
      return 0;
  }

  /**
   * Gets the Subifd count.
   *
   * @return the Subifd count
   */
  public int getSubIfdCount() {
    if (metadataContains("SubIFDs"))
      return getMetadataList("SubIFDs").size();
    else
      return 0;
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
    if (metadataContains("IFD"))
      return getMetadataList("IFD");
    else
      return new ArrayList<TiffObject>();
  }

  /**
   * Returns a list of subifds.
   *
   * @return the subifds list
   */
  public List<TiffObject> getSubIfds() {
    if (metadataContains("SubIFDs"))
      return getMetadataList("SubIFDs");
    else
      return new ArrayList<TiffObject>();
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
    List<TiffObject> l = getIfds();
    if (l.size() > 0)
      return (IFD) l.get(0);
    else
      return null;
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
    if (metadata == null)
      createMetadataDictionary();
    if (metadata.containsKey(name))
      return metadata.get(name).get(0).toString();
    else
      return "";
  }

  /**
   * Metadata contains.
   *
   * @param name the name
   * @return true, if successful
   */
  public boolean metadataContains(String name) {
    if (metadata == null)
      createMetadataDictionary();
    return metadata.containsKey(name);
  }

  /**
   * Gets the metadata ok a given class name.
   *
   * @param name the class name
   * @return the list of metadata that matches with the class name
   */
  public List<TiffObject> getMetadataList(String name) {
    if (metadata == null)
      createMetadataDictionary();
    if (metadata.containsKey(name))
      return metadata.get(name);
    else
      return new ArrayList<TiffObject>();
  }

  /**
   * Creates the metadata dictionary.
   */
  private void createMetadataDictionary() {
    metadata = new HashMap<String, List<TiffObject>>();
    if (firstIFD != null) {
      addMetadataFromIFD(firstIFD, "IFD");
    }
  }

  /**
   * Adds the metadata from ifd.
   *
   * @param ifd the ifd
   * @param key the key
   */
  private void addMetadataFromIFD(IFD ifd, String key) {
    addMetadata(key, ifd);
    for (TagValue tag : ifd.getMetadata().getTags()) {
      for (int i = 0; i < tag.getCardinality(); i++) {
        if (tag.getValue().get(i).isIFD()) {
          addMetadataFromIFD((IFD) tag.getValue().get(i), key);
        } else {
          addMetadata(tag.getName(), tag);
        }
      }
    }
    if (ifd.hasNextIFD()) {
      addMetadataFromIFD(ifd.getNextIFD(), key);
    }
  }

  /**
   * Adds metadata to the tiff model.
   *
   * @param key the classname
   * @param data the data
   */
  private void addMetadata(String key, TiffObject data) {
    if (!metadata.containsKey(key))
      metadata.put(key, new ArrayList<TiffObject>());
    metadata.get(key).add(data);
  }

  /**
   * Prints the metadata.
   */
  public void printMetadata() {
    if (metadata == null)
      createMetadataDictionary();
    System.out.println("METADATA");
    for (String name : metadata.keySet()) {
      System.out.println(name + ": " + getMetadataSingleString(name));
    }
  }
}
