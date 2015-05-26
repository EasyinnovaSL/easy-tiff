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

  /** The photometric. */
  int photometric;

  /** Image in strips. */
  boolean strips = false;

  /** Image in tiles. */
  boolean tiles = false;

  /** The validation. */
  ValidationResult validation;

  /** The data. */
  TiffStreamIO data;

  /**
   * Instantiates a new ifd.
   */
  public IFD(int offset, TiffStreamIO data) {
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
      validation.add(tags.validation);

      // Validate image
      checkImage();

      // Checks the color data
      CheckColorProfile();
    }
  }

  /**
   * Checks the color profile.
   */
  private void CheckColorProfile() {
    // TODO: Everything
  }

  /**
   * Check image.
   */
  public void checkImage() {
    if (!tags.containsTagId(262)) {
      validation.addError("Missing Photometric Interpretation");
    } else if (tags.get(262).n != 1) {
      validation.addError("Invalid Photometric Interpretation");
    } else {
      photometric = (int) tags.get(262).getIntValue();;
      switch (photometric) {
        case 0:
        case 1:
          if (!tags.containsTagId(258)) {
            type = ImageType.BILEVEL;
            CheckBilevelImage();
          } else {
            type = ImageType.GRAYSCALE;
            CheckGrayscaleImage();
          }
          break;
        case 2:
          type = ImageType.RGB;
          CheckRGBImage();
          break;
        case 3:
          type = ImageType.PALETTE;
          CheckPalleteImage();
          break;
        default:
          validation.addError("Invalid Photometric Interpretation", photometric);
          break;
      }
    }
  }

  /**
   * Check Bilevel Image.
   */
  private void CheckBilevelImage() {
    CheckCommonFields();

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

    if (!tags.containsTagId(320)) {
      validation.addError("Missing Color Map");
    } else {
      int colormap = tags.get(320).getIntValue();
      if (colormap <= 0)
        validation.addError("Color Map", colormap);
    }

    // Bits per Sample
    int bps = tags.get(258).getIntValue();
    if (bps != 4 && bps != 8)
      validation.addError("Invalid Bits per Sample", bps);

    // Compression
    int comp = tags.get(259).getIntValue();
    if (comp != 1 && comp != 32773)
      validation.addError("Invalid Compression", comp);
  }

  /**
   * Check RGB Image.
   */
  private void CheckRGBImage() {
    CheckCommonFields();

    // Samples per Pixel
    int samples = tags.get(277).getIntValue();
    if (samples < 3)
      validation.addError("Invalid Samples per Pixel", samples);

    // Compression
    int comp = tags.get(259).getIntValue();
    if (comp != 1 && comp != 32773)
      validation.addError("Invalid Compression", comp);
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
    } else {
      int val = tags.get(id).getIntValue();
      if (val != 1 && val != 2 && val != 3)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // XResolution
    id = 282;
    if (!tags.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      float val = tags.get(id).getRationalValue();
      if (val <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // YResolution
    id = 283;
    if (!tags.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      float val = tags.get(id).getRationalValue();
      if (val <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // Planar Configuration
    id = 284;
    if (!tags.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      int val = tags.get(id).getIntValue();
      if (val != 1 && val != 2)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // Orientation
    id = 274;
    if (!tags.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      int val = tags.get(id).getIntValue();
      if (val <= 0 || val > 8)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // Check whether tiles or strips
    strips = false;
    tiles = false;
    if (tags.containsTagId(273) && tags.containsTagId(279))
      strips = true;
    if (tags.containsTagId(325) && tags.containsTagId(324))
      tiles = true;
    if (!strips && !tiles)
      validation.addError("Missing image organization tags");
    else if (strips && tiles)
      validation.addError("Image in both strips and tiles");
    else if (strips) {
      CheckStrips();
    } else if (tiles) {
      CheckTiles();
    }

    // Check pixel samples bits
    if (tags.containsTagId(258) && tags.containsTagId(277)) {
      int spp = tags.get(277).getIntValue();
      int bps = tags.get(258).n;
      if (spp != bps) {
        validation.addError("Sampes per Pixel and Bits per Sample count do not match");
        if (bps == 1) {
          // TODO: Tolerate and proceed as if the BitsPerSample tag had a count equal to the
          // SamplesPerPixel tag value, and with all values equal to the single value actually given
        }
      }

      if (tags.containsTagId(338)) {
        int ext = tags.get(338).n;
        if (ext + 3 != bps) {
          validation.addError("Incorrect Extra Samples Count", ext);
        } else if (ext > 0 && bps <= 3) {
          validation.addError("Unnecessary Extra Samples", ext);
        }
      }

      if (bps > 1) {
        ArrayList<Integer> lbps = tags.get(258).getIntArray();
        if (lbps == null) {
          validation.addError("Invalid Bits per Sample");
        } else {
          boolean distinct_bps_samples = false;
          for (int i = 1; i < lbps.size(); i++) {
            if (lbps.get(i) != lbps.get(i - 1))
              distinct_bps_samples = true;
          }
          if (distinct_bps_samples)
            validation.addError("Distinct Bits per Sample values");
        }
      }
    }
  }

  /**
   * Check strips.
   */
  private void CheckStrips() {
    int id, offset;

    // Strip offsets
    id = 273;
    offset = (int) tags.get(id).value;
    if (offset <= 0)
      validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);

    // Strip Byte Counts
    id = 279;
    offset = (int) tags.get(id).value;
    if (offset <= 0)
      validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);

    // Rows per Strip
    id = 278;
    if (!tags.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    }
    else {
      offset = (int) tags.get(id).value;
      if (offset <= 0 || tags.get(id).isOffset)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);
    }
  }

  /**
   * Check tiles.
   */
  private void CheckTiles() {
    int id, offset;

    // Tile Offsets
    id = 324;
    offset = (int) tags.get(id).value;
    if (offset <= 0)
      validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);

    // Tile Byte Counts
    id = 325;
    offset = (int) tags.get(id).value;
    if (offset <= 0)
      validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);

    // Tile Width
    id = 322;
    if (!tags.containsTagId(id))
      validation.addError("Missing required field for tiles " + TiffTags.getTag(id).name, offset);
    else {
      offset = (int) tags.get(id).value;
      if (offset <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);
    }

    // Tile Length
    id = 323;
    if (!tags.containsTagId(id))
      validation.addError("Missing required field for tiles " + TiffTags.getTag(id).name, offset);
    else {
      offset = (int) tags.get(id).value;
      if (offset <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);
    }
  }

  /**
   * Prints the tags.
   */
  public void printTags() {
    for (IfdEntry ie : tags.tags) {
      try {
        String name = TiffTags.getTag(ie.id).name;
        String val = ie.toString();
        String off = "";
        String type = TiffTags.tagTypes.get(ie.type);
        if (ie.isOffset)
          off = "*";
        System.out.println(name + "(" + ie.n + "x" + type + off + "): " + val);
      } catch (Exception ex) {
        System.out.println("Tag error");
      }
    }
  }

  /**
   * Write.
   *
   * @param odata the odata
   * @param offset the offset
   * @return the int
   */
  public int write(TiffStreamIO odata, int offset) {
    int offset2 = offset;
    offset2 += tags.write(odata, offset2);
    offset2 += 4;
    odata.putInt(offset2);
    return offset2;
  }
}
