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
package com.easyinnova.tiff.model;

import com.easyinnova.tiff.model.types.Rational;
import com.easyinnova.tiff.model.types.TagValue;


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
  public IfdTags metadata;

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
  public ValidationResult validation;

  /**
   * Instantiates a new ifd.
   *
   * @param offset the offset
   * @param data the data
   */
  public IFD(int offset) {
    this.offset = offset;
    metadata = new IfdTags();
    correct = true;
    type = ImageType.UNDEFINED;
    validation = new ValidationResult();
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
      metadata.validate();
      validation.add(metadata.validation);

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
    if (!metadata.containsTagId(262)) {
      validation.addError("Missing Photometric Interpretation");
    } else if (metadata.get(262).value.getValue().size() != 1) {
      validation.addError("Invalid Photometric Interpretation");
    } else {
      photometric = (int) metadata.get(262).getNumericValue();
      switch (photometric) {
        case 0:
        case 1:
          if (!metadata.containsTagId(258) || metadata.get(258).getNumericValue() == 1) {
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
    int comp = metadata.get(259).getNumericValue();
    if (comp != 1 && comp != 2 && comp != 32773)
      validation.addError("Invalid Compression", comp);
  }

  /**
   * Check Grayscale Image.
   */
  private void CheckGrayscaleImage() {
    CheckCommonFields();

    // Bits per Sample
    int bps = metadata.get(258).value.getFirstNumericValue();
    if (bps != 4 && bps != 8)
      validation.addError("Invalid Bits per Sample", bps);

    // Compression
    int comp = metadata.get(259).getNumericValue();
    if (comp != 1 && comp != 32773)
      validation.addError("Invalid Compression", comp);
  }

  /**
   * Check Pallete Color Image.
   */
  private void CheckPalleteImage() {
    CheckCommonFields();

    if (!metadata.containsTagId(320)) {
      validation.addError("Missing Color Map");
    } else {
      int colormap = metadata.get(320).getNumericValue();
      if (colormap <= 0)
        validation.addError("Color Map", colormap);
    }

    // Bits per Sample
    int bps = metadata.get(258).getNumericValue();
    if (bps != 4 && bps != 8)
      validation.addError("Invalid Bits per Sample", bps);

    // Compression
    int comp = metadata.get(259).getNumericValue();
    if (comp != 1 && comp != 32773)
      validation.addError("Invalid Compression", comp);
  }

  /**
   * Check RGB Image.
   */
  private void CheckRGBImage() {
    CheckCommonFields();

    // Samples per Pixel
    int samples = metadata.get(277).getNumericValue();
    if (samples < 3)
      validation.addError("Invalid Samples per Pixel", samples);

    // Compression
    int comp = metadata.get(259).getNumericValue();
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
    if (!metadata.containsTagId(id))
      validation.addError("Missing required field", TiffTags.getTag(id).name);
    else {
      int val = metadata.get(id).getNumericValue();
      if (val <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // Height
    id = 257;
    if (!metadata.containsTagId(id))
      validation.addError("Missing required field", TiffTags.getTag(id).name);
    else {
      int val = metadata.get(id).getNumericValue();
      if (val <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // Resolution Unit
    id = 296;
    if (!metadata.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      int val = metadata.get(id).getNumericValue();
      if (val != 1 && val != 2 && val != 3)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // XResolution
    id = 282;
    if (!metadata.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      float val = ((Rational) metadata.get(id).value.getValue().get(0)).getFloatValue();
      if (val <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // YResolution
    id = 283;
    if (!metadata.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      float val = ((Rational) metadata.get(id).value.getValue().get(0)).getFloatValue();
      if (val <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // Planar Configuration
    id = 284;
    if (!metadata.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      int val = metadata.get(id).getNumericValue();
      if (val != 1 && val != 2)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // Orientation
    id = 274;
    if (!metadata.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      int val = metadata.get(id).getNumericValue();
      if (val <= 0 || val > 8)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, val);
    }

    // Check whether tiles or strips
    strips = false;
    tiles = false;
    if (metadata.containsTagId(273) && metadata.containsTagId(279))
      strips = true;
    if (metadata.containsTagId(325) && metadata.containsTagId(324))
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
    if (metadata.containsTagId(258) && metadata.containsTagId(277)) {
      int spp = metadata.get(277).getNumericValue();
      int bps = metadata.get(258).value.getValue().size();
      if (spp != bps) {
        validation.addError("Sampes per Pixel and Bits per Sample count do not match");
        if (bps == 1) {
          // TODO: Tolerate and proceed as if the BitsPerSample tag had a count equal to the
          // SamplesPerPixel tag value, and with all values equal to the single value actually given
        }
      }

      if (metadata.containsTagId(338)) {
        int ext = metadata.get(338).value.getValue().size();
        if (ext + 3 != bps) {
          validation.addError("Incorrect Extra Samples Count", ext);
        } else if (ext > 0 && bps <= 3) {
          validation.addError("Unnecessary Extra Samples", ext);
        }
      }

      if (bps > 1) {
        TagValue lbps = metadata.get(258).value;
        if (lbps == null || lbps.getValue() == null) {
          validation.addError("Invalid Bits per Sample");
        } else {
          boolean distinct_bps_samples = false;
          for (int i = 1; i < lbps.getCardinality(); i++) {
            if (lbps.getValue().get(i).getNumericValue() != lbps.getValue().get(i - 1)
                .getNumericValue())
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
    offset = metadata.get(id).getNumericValue();
    if (offset <= 0)
      validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);

    // Strip Byte Counts
    id = 279;
    offset = metadata.get(id).getNumericValue();
    if (offset <= 0)
      validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);

    // Rows per Strip
    id = 278;
    if (!metadata.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    }
    else {
      offset = metadata.get(id).getNumericValue();
      if (offset <= 0)
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
    offset = metadata.get(id).getNumericValue();
    if (offset <= 0)
      validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);

    // Tile Byte Counts
    id = 325;
    offset = metadata.get(id).getNumericValue();
    if (offset <= 0)
      validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);

    // Tile Width
    id = 322;
    if (!metadata.containsTagId(id))
      validation.addError("Missing required field for tiles " + TiffTags.getTag(id).name, offset);
    else {
      offset = metadata.get(id).getNumericValue();
      if (offset <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);
    }

    // Tile Length
    id = 323;
    if (!metadata.containsTagId(id))
      validation.addError("Missing required field for tiles " + TiffTags.getTag(id).name, offset);
    else {
      offset = metadata.get(id).getNumericValue();
      if (offset <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).name, offset);
    }
  }

  /**
   * Prints the tags.
   */
  public void printTags() {
    for (IfdEntry ie : metadata.tags) {
      try {
        String name = TiffTags.getTag(ie.id).name;
        String val = ie.toString();
        String off = "";
        String type = TiffTags.tagTypes.get(ie.type);
        System.out
            .println(name + "(" + ie.value.getValue().get(0) + "x" + type + off + "): " + val);
      } catch (Exception ex) {
        System.out.println("Tag error");
      }
    }
  }

  /**
   * Gets the metadata.
   *
   * @param name the name
   * @return the metadata
   */
  public TagValue getMetadata(String name) {
    int id = TiffTags.getTagId(name);
    return metadata.get(id).value;
  }
}
