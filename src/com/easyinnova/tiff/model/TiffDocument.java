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
import com.easyinnova.tiff.model.types.abstractTiffType;

import java.util.ArrayList;
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
  private Metadata metadata;

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
    if (metadata == null)
      return 0;
    if (metadata.contains("IFD")) {
      return getMetadataList("IFD").size();
    } else
      return 0;
  }

  /**
   * Gets the images count.
   *
   * @return the ifd count
   */
  public int getIfdImagesCount() {
    if (metadata.contains("IFD")) {
      List<TiffObject> l = getMetadataList("IFD");
      int n = 0;
      for (TiffObject to : l) {
        IFD ifd = (IFD) to;
        if (ifd.isImage())
          n++;
      }
      return n;
    }
    else
      return 0;
  }

  /**
   * Gets the Subifd count.
   *
   * @return the Subifd count
   */
  public int getSubIfdCount() {
    if (metadata == null)
      return 0;
    if (metadata.contains("SubIFDs"))
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
   * Returns a list of ifds including exifds.
   *
   * @return the ifds list
   */
  public List<TiffObject> getIfds() {
    if (metadata.contains("IFD"))
      return getMetadataList("IFD");
    else
      return new ArrayList<TiffObject>();
  }

  /**
   * Returns a list of ifds representing Images.
   *
   * @return the ifds list
   */
  public List<TiffObject> getImageIfds() {
    if (metadata.contains("IFD")) {
      List<TiffObject> l = new ArrayList<TiffObject>();
      for (TiffObject ifd : getMetadataList("IFD")) {
        if (((IFD) ifd).isImage())
          l.add(ifd);
      }
      return l;
    }
    else
      return new ArrayList<TiffObject>();
  }

  /**
   * Returns a list of subifds.
   *
   * @return the subifds list
   */
  public List<TiffObject> getSubIfds() {
    if (metadata.contains("SubIFDs"))
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
    List<TiffObject> l = getImageIfds();
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
    if (metadata.contains(name))
      return metadata.getFirst(name).toString();
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
    if (metadata == null)
      createMetadataDictionary();
    if (metadata.contains(name))
      return metadata.getList(name);
    else
      return new ArrayList<TiffObject>();
  }

  /**
   * Creates the metadata dictionary.
   */
  public void createMetadataDictionary() {
    metadata = new Metadata();
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
    metadata.add(key, ifd);
    for (TagValue tag : ifd.getMetadata().getTags()) {
      if (tag.getCardinality() == 1) {
        abstractTiffType t = tag.getValue().get(0);
        if (t.isIFD()) {
          addMetadataFromIFD((IFD) t, key);
        } else if (t.containsMetadata()) {
          Metadata meta = t.createMetadata();
          metadata.addMetadata(meta);
        } else {
          metadata.add(tag.getName(), t);
        }
      } else {
        metadata.add(tag.getName(), tag);
      }
    }
    if (ifd.hasNextIFD()) {
      addMetadataFromIFD(ifd.getNextIFD(), key);
    }
  }

  /**
   * Prints the metadata.
   */
  public void printMetadata() {
    if (metadata == null)
      createMetadataDictionary();
    System.out.println("METADATA");
    for (String name : metadata.keySet()) {
      String mult = "";
      if (getMetadataList(name).size() > 1)
        mult = "(x" + getMetadataList(name).size() + ")";
      System.out.println(name + mult + ": "
          + getMetadataSingleString(name));
    }
  }

  /**
   * Gets the metadata.
   *
   * @return the metadata
   */
  public Metadata getMetadata() {
    return metadata;
  }
}
