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
  TiffStreamIO data;

  /** The number of ifds. */
  int nIfds;

  /**
   * The duplicate tag tolerance.<br>
   * 0: No tolerance. 10: Full tolerance (keep first appearance only)
   */
  int duplicateTagTolerance = 10;

  /**
   * The next ifd tolerance. 0: No tolerance. 10: Full tolerance (assume 0)
   */
  int nextIFDTolerance = 0;

  /**
   * Instantiates a new tiff structure object.
   */
  public IfdStructure(TiffStreamIO data) {
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
    try {
      int offset0 = (int) data.getInt(4);
      IFD ifd0 = readIFD(offset0);
      if (ifd0 == null) {
        validation.addError("Parsing error in first IFD");
      } else {
        addIfd(ifd0);
        nIfds++;
        IFD current_ifd = ifd0;

        // Read next IFDs
        while (current_ifd.hasNextIFD()) {
          int offset = current_ifd.nextIFDOffset();
          if (usedOffset(offset)) {
            // Circular reference
            validation.addError("IFD offset already used");
            break;
          } else {
            current_ifd = readIFD(offset);
            if (current_ifd == null) {
              validation.addError("Error in IFD " + nIfds, "" + offset);
              break;
            } else {
              addIfd(current_ifd);
              nIfds++;
            }
          }
        }
      }
    } catch (Exception ex) {
      validation.addError("IFD parsing error");
    }
  }

  /**
   * Reads a ifd.
   *
   * @param IFD_idx Position in file
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

        // Reads the tags
        for (int i = 0; i < directoryEntries; i++) {
          int tagid = data.getUshort(index);
          int tagType = data.getShort(index + 2);
          int tagN = data.getInt(index + 4);
          IfdEntry tag = new IfdEntry(tagid, tagType, tagN, data);
          if (ifd.tags.containsTagId(tagid)) {
            if (duplicateTagTolerance > 0)
              validation.addWarning("Duplicate tag", tagid);
            else
              validation.addError("Duplicate tag", tagid);
          } else {
            ifd.tags.addTag(tag);
            try {
              tag.getValueOrOffset(index + 8);
            } catch (Exception ex) {
              validation.addError("Parse error in tag " + tagid + " value");
            }
          }

          index += 12;
        }

        // Reads the position of the next IFD
        try {
          ifd.nextIFD = (int) data.getInt(index);
        } catch (Exception ex) {
          ifd.nextIFD = 0;
          if (nextIFDTolerance > 0)
            validation.addWarning("Unreadable next IFD offset");
          else
            validation.addError("Unreadable next IFD offset");
        }
        if (ifd.hasNextIFD())
          if (ifd.nextIFD < 7)
            validation.addError("Invalid next IFD offset", ifd.nextIFD);

        // Validate ifd entries
        ifd.validate();
        validation.add(ifd.validation);
      }
    } catch (IndexOutOfBoundsException ex) {
      ifd = null;
    }
    return ifd;
  }
}

