/**
 * <h1>TiffStructure.java</h1> 
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
 * @since 18/5/2015
 *
 */
package com.easyinnova.main;

import java.nio.MappedByteBuffer;
import java.util.ArrayList;

/**
 * The Class TiffStructure.
 */
public class IfdStructure {

  /** The list of Ifd. */
  ArrayList<IFD> ifds;

  /** The validation. */
  ValidationResult validation;

  /** The data. */
  MappedByteBuffer data;

  /** The number of ifds. */
  int nIfds;

  /**
   * Instantiates a new tiff structure object.
   */
  public IfdStructure(MappedByteBuffer data) {
    ifds = new ArrayList<IFD>();
    validation = new ValidationResult();
    this.data = data;
    nIfds = 0;
  }

  /**
   * Adds an IFD to the list.
   *
   * @param ifd the ifd
   */
  public void addIfd(IFD ifd) {
    ifds.add(ifd);
  }

  /**
   * Circular offset.
   *
   * @param offset the offset
   * @return true, if successful
   */
  public boolean usedOffset(int offset) {
    boolean used = false;
    for (IFD ifd : ifds) {
      if (ifd.offset == offset) {
        used = true;
        break;
      }
    }
    return used;
  }

  /**
   * Read Image Directories.
   *
   * @param data the data
   */
  public void read() {
    // Read first IFD
    int offset0 = (int) data.getInt(4);
    IFD ifd0 = readIFD(offset0);
    if (ifd0 == null) {
      validation.addError("IFD 0 does not exist");
    } else {
      ifd0.validate();
      validation.add(ifd0.validation);
      addIfd(ifd0);
      nIfds++;
      IFD current_ifd = ifd0;

      // Read next IFDs
      while (current_ifd.hasNextIFD()) {
        int offset = current_ifd.nextIFDOffset();
        if (usedOffset(offset)) {
          // Circular reference check
          validation.addError("IFD offset already used");
          break;
        } else {
          current_ifd = readIFD(offset);
          if (current_ifd == null) {
            validation.addError("Next IFD does not exist", "" + offset);
            break;
          } else {
            current_ifd.validate();
            validation.add(current_ifd.validation);
            addIfd(current_ifd);
          }
          nIfds++;
        }
      }
    }
  }

  /**
   * Reads a ifd.
   *
   * @param IFD_idx Offset
   * @return the ifd
   */
  IFD readIFD(int offset) {
    IFD ifd = new IFD(offset, data);
    try {
      int index = offset;
      int directoryEntries = data.getShort(offset);
      if (directoryEntries < 0) {
        validation.addError("Incorrect number of IFD entries", directoryEntries);
        ifd.correct = false;
      } else {
        index += 2;
        for (int i = 0; i < directoryEntries; i++) {
          // Read a tag
          int tagid = data.getShort(index);
          int tagType = data.getShort(index + 2);
          int tagN = data.getInt(index + 4);
          IfdEntry tag = new IfdEntry(tagid, tagType, tagN);
          tag.getValue(data, index + 8, tagType);
          ifd.tags.addTag(tag);

          index += 12;
        }
        // Reads the position of the next IFD
        ifd.nextIFD = (int) data.getInt(index);
      }
    } catch (IndexOutOfBoundsException ex) {
      ifd = null;
    }
    return ifd;
  }
}

