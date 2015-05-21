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

import java.nio.MappedByteBuffer;
import java.util.ArrayList;

/**
 * The Class IFD.
 */
public class IFD {
  
  /**
   * The Enum ImageType.
   */
  public enum ImageType {

    /** The bilevel. */
    BILEVEL,
    /** The grayscale. */
    GRAYSCALE,
    /** The palette. */
    PALETTE,
    /** The rgb. */
    RGB,
    /** The undefined. */
    UNDEFINED
  };

  /** The tags. */
  public IfdTags tags;

  /** The Next ifd. */
  public int nextIFD = 0;

  /** The Offset. */
  public int offset = 0;

  /** The Correct. */
  public boolean correct;
  
  /** The Type. */
  public ImageType type;

  /** The validation. */
  ValidationResult validation;

  /** The data. */
  MappedByteBuffer data;

  /**
   * Instantiates a new ifd.
   */
  public IFD(int offset, MappedByteBuffer data) {
    this.offset = offset;
    tags = new IfdTags();
    correct = true;
    type = ImageType.UNDEFINED;
    validation = new ValidationResult();
    this.data = data;
  }

  /**
   * Checks for next ifd.
   *
   * @return true, if next IFD exists
   */
  public boolean hasNextIFD() {
    return nextIFD > 0;
  }

  /**
   * Get Next IFD.
   *
   * @return the next ifd
   */
  public int nextIFDOffset() {
    return nextIFD;
  }

  /**
   * Validates the IFD.
   *
   * @param validation_result the validation_result
   */
  public void validate() {
    if (correct) {
      // Validate tags
      tags.validate();

      // Validate image
      checkImage();

      validation.add(tags.validation);
    }
  }

  /**
   * Check image.
   */
  public void checkImage() {
    if (!tags.containsTagId(258)) {
      type = ImageType.BILEVEL;
      CheckBilevelImage();
    } else {
      int photo = -1;
      if (tags.containsTagId(262))
        photo = (int) tags.get(262).getIntValue();

      if (tags.containsTagId(320) && photo == 3) {
        type = ImageType.PALETTE;
        CheckPalleteImage();
      } else if (photo == 2) {
        type = ImageType.RGB;
        CheckRGBImage();
      } else {
        type = ImageType.GRAYSCALE;
        CheckGrayscaleImage();
      }
    }
  }

  /**
   * Check common fields.
   */
  private void CheckCommonFields() {
    int id;

    // Width
    id = 256;
    if (!tags.containsTagId(id))
      validation.addError("Missing required field", TiffTags.getTag(id).name);
    else {
      int val = tags.get(id).getIntValue();
      if (val <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // Height
    id = 257;
    if (!tags.containsTagId(id))
      validation.addError("Missing required field", TiffTags.getTag(id).name);
    else {
      int val = tags.get(id).getIntValue();
      if (val <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // Resolution Unit
    id = 296;
    if (!tags.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    }
    else {
      int val = tags.get(id).getIntValue();
      if (val != 1 && val != 2 && val != 3)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // XResolution
    id = 282;
    if (!tags.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    }
    else {
      float val = tags.get(id).getRationalValue();
      if (val <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // YResolution
    id = 283;
    if (!tags.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    }
    else {
      float val = tags.get(id).getRationalValue();
      if (val <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // Strip offsets
    id = 273;
    if (!tags.containsTagId(id))
      validation.addError("Missing required field", TiffTags.getTag(id).name);
    else {
      int offset = (int) tags.get(id).value;
      if (offset <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);
    }

    // Rows per Strip
    id = 278;
    if (!tags.containsTagId(id))
      validation.addError("Missing required field", TiffTags.getTag(id).name);
    else {
      int offset = (int) tags.get(id).value;
      if (offset <= 0 || tags.get(id).isOffset)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);
    }

    // Strip Byte Counts
    id = 279;
    if (!tags.containsTagId(id))
      validation.addError("Missing required field", TiffTags.getTag(id).name);
    else {
      int offset = (int) tags.get(id).value;
      if (offset <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);
    }
  }

  /**
   * Check Bilevel Image.
   */
  private void CheckBilevelImage() {
    CheckCommonFields();

    // Photometric interpretation
    int photo = tags.get(262).getIntValue();
    if (photo != 0 && photo != 1)
      validation.addError("Invalid Photometric Interpretation", photo);

    // Compression
    int comp = tags.get(259).getIntValue();
    if (comp != 1 && comp != 2 && comp != 32773)
      validation.addError("Invalid Compression", comp);
  }

  /**
   * Check Grayscale Image.
   */
  private void CheckGrayscaleImage() {
    CheckCommonFields();

    // Bits per Sample
    int bps = tags.get(258).getIntValue();
    if (bps != 4 && bps != 8)
      validation.addError("Invalid Bits per Sample", bps);

    // Photometric interpretation
    int photo = tags.get(262).getIntValue();
    if (photo != 0 && photo != 1)
      validation.addError("Invalid Photometric Interpretation", photo);

    // Compression
    int comp = tags.get(259).getIntValue();
    if (comp != 1 && comp != 32773)
      validation.addError("Invalid Compression", comp);
  }

  /**
   * Check Pallete Color Image.
   */
  private void CheckPalleteImage() {
    CheckCommonFields();

    // Bits per Sample
    int bps = tags.get(258).getIntValue();
    if (bps != 4 && bps != 8)
      validation.addError("Invalid Bits per Sample", bps);

    // Photometric interpretation
    int photo = tags.get(262).getIntValue();
    if (photo != 3)
      validation.addError("Invalid Photometric Interpretation", photo);

    // Compression
    int comp = tags.get(259).getIntValue();
    if (comp != 1 && comp != 32773)
      validation.addError("Invalid Compression", comp);

    // Color Map
    int colormap = tags.get(320).getIntValue();
    if (colormap <= 0)
      validation.addError("Color Map", colormap);
  }

  /**
   * Check RGB Image.
   */
  private void CheckRGBImage() {
    CheckCommonFields();

    // Bits per Sample
    ArrayList<Integer> bps = tags.get(258).getIntArray();
    if (bps == null) {
      validation.addError("Invalid Bits per Sample");
    } else if (bps.get(0) != 8 || bps.get(1) != 8 || bps.get(2) != 8) {
      // validation.addError("Invalid Bits per Sample value");
    }

    // Photometric interpretation
    int photo = tags.get(262).getIntValue();
    if (photo != 2)
      validation.addError("Invalid Photometric Interpretation", photo);

    // Samples per Pixel
    int samples = tags.get(277).getIntValue();
    if (samples < 3)
      validation.addError("Invalid Samples per Pixel", photo);

    // Compression
    int comp = tags.get(259).getIntValue();
    if (comp != 1 && comp != 32773)
      validation.addError("Invalid Compression", comp);
  }

  /**
   * Prints the tags.
   */
  public void printTags() {
    for (IfdEntry ie : tags.tags) {
      String name = TiffTags.getTag(ie.id).name;
      String val = ie.toString();
      String off = "";
      if (ie.isOffset)
        off = "*";
      System.out.println(name + "(" + ie.n + "x" + TiffTags.tagTypes.get(ie.type) + off + "): "
          + val);
    }
  }
}
