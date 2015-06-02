/**
 * <h1>TiffWriter.java</h1> 
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
 * @since 28/5/2015
 *
 */
package com.easyinnova.tiff.writer;

import com.easyinnova.tiff.io.TiffStreamIO;
import com.easyinnova.tiff.model.IFD;
import com.easyinnova.tiff.model.IfdEntry;
import com.easyinnova.tiff.model.IfdTags;
import com.easyinnova.tiff.model.TiffObject;
import com.easyinnova.tiff.model.types.TagValue;

import java.util.ArrayList;

/**
 * The Class TiffWriter.
 */
public class TiffWriter {

  /** The model. */
  TiffObject model;

  /** The odata. */
  TiffStreamIO data;

  /**
   * Instantiates a new tiff writer.
   *
   * @param data the data
   */
  public TiffWriter() {
    model = new TiffObject();
  }

  /**
   * Sets the model.
   *
   * @param model the model
   */
  public void SetModel(TiffObject model) {
    this.model = model;
  }

  /**
   * Write.
   *
   * @param filename the filename
   * @throws Exception
   */
  public void write(String filename) throws Exception {
    data = new TiffStreamIO(null);
    try {
      data.write(filename);
      data.writeHeader();
      writeTiff();
      data.close();
    } catch (Exception ex) {
      throw ex;
    }
  }

  /**
   * Write.
   *
   * @param odata the odata
   */
  public void writeTiff() {
    for (int i = model.getIfds().size() - 1; i >= 0; i--) {
      IFD ifd = model.getIfds().get(i);
      writeIFD(ifd);
    }
  }

  /**
   * Write IFD data.
   *
   * @param ifd the ifd
   * @return the int
   */
  public int writeIFD(IFD ifd) {
    int offset2 = writeMetadata(ifd.metadata);
    // data.putInt(ifd.offset);
    return offset2;
  }

  /**
   * Write.
   *
   * @param metadata the metadata
   * @return the int
   */
  public int writeMetadata(IfdTags metadata) {
    ArrayList<IfdEntry> tags = metadata.tags;
    int offset = data.position();
    for (IfdEntry tag : tags) {
      if (tag.id == 273) {
        writeStripData(metadata);
      } else if (tag.id == 279) {
        // Nothing to do here, writeStripData does everything
      } else {
        int size = writeTag(tag);
        tag.setIntValue(offset);
        offset += size;
      }
    }
    data.putShort((short) tags.size());
    for (IfdEntry tag : tags) {
      tag.write(data);
    }
    return data.position();
  }

  /**
   * Write strip data.
   *
   * @param odata the odata
   */
  private void writeStripData(IfdTags metadata) {
    TagValue stripOffsets = metadata.hashTagsId.get(273).value;
    TagValue stripSizes = metadata.hashTagsId.get(279).value;
    ArrayList<Integer> stripOffsets2 = new ArrayList<Integer>();
    ArrayList<Integer> stripSizes2 = new ArrayList<Integer>();
    for (int i = 0; i < stripOffsets.getCardinality(); i++) {
      stripOffsets2.add(data.position());
      stripSizes2.add((int) stripSizes.getValue().get(i).getNumericValue());
      for (int j = 0; j < stripSizes.getValue().get(i).getNumericValue(); j++) {
        int v = data.get((int) stripOffsets.getValue().get(i).getNumericValue());
        data.put((byte) v);
      }
    }
    int offsetStripOffsets = data.position();
    for (int i = 0; i < stripOffsets2.size(); i++) {
      data.putInt(stripOffsets2.get(i));
    }
    int offsetStripSizes = data.position();
    for (int i = 0; i < stripSizes2.size(); i++) {
      data.putInt(stripOffsets2.get(i));
    }
    metadata.hashTagsId.get(273).setIntValue(offsetStripOffsets);
    metadata.hashTagsId.get(279).setIntValue(offsetStripSizes);
  }

  /**
   * Write content.
   *
   * @param tag the tag
   * @return the int
   */
  public int writeTag(IfdEntry tag) {
    int totalSize = 0;
    for (int i = 0; i < totalSize; i++) {
      int v = data.get(tag.getNumericValue() + i);
      data.put((byte) v);
    }
    return totalSize;
  }
}

