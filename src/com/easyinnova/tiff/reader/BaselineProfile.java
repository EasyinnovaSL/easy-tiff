/**
 * <h1>BaselineProfile.java</h1>
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
 * @since 4/6/2015
 *
 */
package com.easyinnova.tiff.reader;

import com.easyinnova.tiff.model.IfdTags;
import com.easyinnova.tiff.model.Tag;
import com.easyinnova.tiff.model.TagValue;
import com.easyinnova.tiff.model.TiffTags;
import com.easyinnova.tiff.model.ValidationResult;
import com.easyinnova.tiff.model.types.IFD;
import com.easyinnova.tiff.model.types.Rational;

/**
 * Checks if the Tiff file complies with the Baseline 6.0.
 */
public class BaselineProfile {

  /**
   * Types of images.
   */
  public enum ImageType {
    /** Bilevel (black and white). */
    BILEVEL,
    /** Crayscale. */
    GRAYSCALE,
    /** Palette-color. */
    PALETTE,
    /** RGB. */
    RGB,
    /** Undefined. */
    UNDEFINED
  };

  /** The result of the check. */
  private ValidationResult validation;

  /** The image yype. */
  private ImageType type;

  /** The photometric interpretation. */
  private int photometric;

  /** Is image in strips. */
  private boolean strips = false;

  /** Is image in tiles. */
  private boolean tiles = false;

  /**
   * The tag order tolerance.<br>
   * 0: No tolerance. 10: Full tolerance (no matter if tags are not in ascending order)
   * */
  private int tagOrderTolerance = 10;

  /**
   * Instantiates a new baseline profile.
   */
  public BaselineProfile() {
    validation = new ValidationResult();
    type = ImageType.UNDEFINED;
  }

  /**
   * Validates an IFD.
   *
   * @param ifd the image file descriptor
   */
  public void validateIfd(IFD ifd) {
    IfdTags metadata = ifd.getMetadata();

    // Validate tags
    validation.add(validateMetadata(metadata));

    // Validate image
    checkImage(metadata);

    // Checks the color data
    CheckColorProfile();
  }

  /**
   * Validates the ifd entries.
   *
   * @param metadata the metadata
   * @return the validation result
   */
  public ValidationResult validateMetadata(IfdTags metadata) {
    ValidationResult validation = new ValidationResult();
    int prevTagId = 0;
    TiffTags.getTiffTags();
    for (TagValue ie : metadata.getTags()) {
      if (!TiffTags.tagMap.containsKey(ie.getId()))
        validation.addError("Undefined tag id " + ie.getId());
      else if (!TiffTags.tagTypes.containsKey(ie.getType()))
        validation.addWarning("Unknown tag type " + ie.getType());
      else {
        Tag t = TiffTags.getTag(ie.getId());
        String stype = TiffTags.tagTypes.get(ie.getType());
        if (!t.validType(stype)) {
          String stypes = "";
          for (String tt : t.getType()) {
            if (stypes.length() > 0)
              stypes += ",";
            stypes += tt;
          }
          validation.addError("Invalid type for tag " + ie.getId() + "[" + stypes + "]", stype);
        }
        try {
          int card = Integer.parseInt(t.getCardinality());
          if (card != ie.getCardinality())
            validation.addError("Cardinality for tag " + ie.getId() + " must be " + card,
                ie.getCardinality());
        } catch (Exception e) {
          // TODO: Deal with formulas?
        }
      }

      if (ie.getId() < prevTagId) {
        if (tagOrderTolerance > 0)
          validation.addWarning("Tags are not in ascending order");
        else
          validation.addError("Tags are not in ascending order");
      }
      prevTagId = ie.getId();
    }
    return validation;
  }

  /**
   * Checks the color profile.
   */
  private void CheckColorProfile() {
    // TODO: Everything
  }

  /**
   * Check image.
   *
   * @param metadata the metadata
   */
  public void checkImage(IfdTags metadata) {
    if (!metadata.containsTagId(262)) {
      validation.addError("Missing Photometric Interpretation");
    } else if (metadata.get(262).getValue().size() != 1) {
      validation.addError("Invalid Photometric Interpretation");
    } else {
      photometric = (int) metadata.get(262).getFirstNumericValue();
      switch (photometric) {
        case 0:
        case 1:
          if (!metadata.containsTagId(258) || metadata.get(258).getFirstNumericValue() == 1) {
            type = ImageType.BILEVEL;
            CheckBilevelImage(metadata);
          } else {
            type = ImageType.GRAYSCALE;
            CheckGrayscaleImage(metadata);
          }
          break;
        case 2:
          type = ImageType.RGB;
          CheckRGBImage(metadata);
          break;
        case 3:
          type = ImageType.PALETTE;
          CheckPalleteImage(metadata);
          break;
        default:
          validation.addError("Invalid Photometric Interpretation", photometric);
          break;
      }
    }
  }

  /**
   * Check Bilevel Image.
   *
   * @param metadata the metadata
   */
  private void CheckBilevelImage(IfdTags metadata) {
    CheckCommonFields(metadata);

    // Compression
    int comp = metadata.get(259).getFirstNumericValue();
    if (comp != 1 && comp != 2 && comp != 32773)
      validation.addError("Invalid Compression", comp);
  }

  /**
   * Check Grayscale Image.
   *
   * @param metadata the metadata
   */
  private void CheckGrayscaleImage(IfdTags metadata) {
    CheckCommonFields(metadata);

    // Bits per Sample
    int bps = metadata.get(258).getFirstNumericValue();
    if (bps != 4 && bps != 8)
      validation.addError("Invalid Bits per Sample", bps);

    // Compression
    int comp = metadata.get(259).getFirstNumericValue();
    if (comp != 1 && comp != 32773)
      validation.addError("Invalid Compression", comp);
  }

  /**
   * Check Pallete Color Image.
   *
   * @param metadata the metadata
   */
  private void CheckPalleteImage(IfdTags metadata) {
    CheckCommonFields(metadata);

    if (!metadata.containsTagId(320)) {
      validation.addError("Missing Color Map");
    } else {
      int colormap = metadata.get(320).getFirstNumericValue();
      if (colormap <= 0)
        validation.addError("Color Map", colormap);
    }

    // Bits per Sample
    int bps = metadata.get(258).getFirstNumericValue();
    if (bps != 4 && bps != 8)
      validation.addError("Invalid Bits per Sample", bps);

    // Compression
    int comp = metadata.get(259).getFirstNumericValue();
    if (comp != 1 && comp != 32773)
      validation.addError("Invalid Compression", comp);
  }

  /**
   * Check RGB Image.
   *
   * @param metadata the metadata
   */
  private void CheckRGBImage(IfdTags metadata) {
    CheckCommonFields(metadata);

    // Samples per Pixel
    int samples = metadata.get(277).getFirstNumericValue();
    if (samples < 3)
      validation.addError("Invalid Samples per Pixel", samples);

    // Compression
    int comp = metadata.get(259).getFirstNumericValue();
    if (comp != 1 && comp != 32773)
      validation.addError("Invalid Compression", comp);
  }

  /**
   * Check common fields.
   *
   * @param metadata the metadata
   */
  private void CheckCommonFields(IfdTags metadata) {
    int id;

    // Width
    id = 256;
    if (!metadata.containsTagId(id))
      validation.addError("Missing required field", TiffTags.getTag(id).getName());
    else {
      int val = metadata.get(id).getFirstNumericValue();
      if (val <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).getName(), val);
    }

    // Height
    id = 257;
    if (!metadata.containsTagId(id))
      validation.addError("Missing required field", TiffTags.getTag(id).getName());
    else {
      int val = metadata.get(id).getFirstNumericValue();
      if (val <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).getName(), val);
    }

    // Resolution Unit
    id = 296;
    if (!metadata.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      int val = metadata.get(id).getFirstNumericValue();
      if (val != 1 && val != 2 && val != 3)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).getName(), val);
    }

    // XResolution
    id = 282;
    if (!metadata.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      float val = ((Rational) metadata.get(id).getValue().get(0)).getFloatValue();
      if (val <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).getName(), val);
    }

    // YResolution
    id = 283;
    if (!metadata.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      float val = ((Rational) metadata.get(id).getValue().get(0)).getFloatValue();
      if (val <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).getName(), val);
    }

    // Planar Configuration
    id = 284;
    if (!metadata.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      int val = metadata.get(id).getFirstNumericValue();
      if (val != 1 && val != 2)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).getName(), val);
    }

    // Orientation
    id = 274;
    if (!metadata.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      int val = metadata.get(id).getFirstNumericValue();
      if (val <= 0 || val > 8)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).getName(), val);
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
      CheckStrips(metadata);
    } else if (tiles) {
      CheckTiles(metadata);
    }

    // Check pixel samples bits
    if (metadata.containsTagId(258) && metadata.containsTagId(277)) {
      int spp = metadata.get(277).getFirstNumericValue();
      int bps = metadata.get(258).getValue().size();
      if (spp != bps) {
        validation.addError("Sampes per Pixel and Bits per Sample count do not match");
        if (bps == 1) {
          // TODO: Tolerate and proceed as if the BitsPerSample tag had a count equal to the
          // SamplesPerPixel tag value, and with all values equal to the single value actually given
        }
      }

      if (metadata.containsTagId(338)) {
        int ext = metadata.get(338).getValue().size();
        if (ext + 3 != bps) {
          validation.addError("Incorrect Extra Samples Count", ext);
        } else if (ext > 0 && bps <= 3) {
          validation.addError("Unnecessary Extra Samples", ext);
        }
      }

      if (bps > 1) {
        TagValue lbps = metadata.get(258);
        if (lbps == null || lbps.getValue() == null) {
          validation.addError("Invalid Bits per Sample");
        } else {
          boolean distinct_bps_samples = false;
          for (int i = 1; i < lbps.getCardinality(); i++) {
            if (lbps.getValue().get(i).toInt() != lbps.getValue().get(i - 1).toInt())
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
   *
   * @param metadata the metadata
   */
  private void CheckStrips(IfdTags metadata) {
    int id, offset;

    // Strip offsets
    id = 273;
    offset = metadata.get(id).getFirstNumericValue();
    if (offset <= 0)
      validation.addError("Invalid value for field " + TiffTags.getTag(id).getName(), offset);

    // Strip Byte Counts
    id = 279;
    offset = metadata.get(id).getFirstNumericValue();
    if (offset <= 0)
      validation.addError("Invalid value for field " + TiffTags.getTag(id).getName(), offset);

    // Rows per Strip
    id = 278;
    if (!metadata.containsTagId(id)) {
      // validation.addError("Missing required field", TiffTags.getTag(id).name);
    } else {
      offset = metadata.get(id).getFirstNumericValue();
      if (offset <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).getName(), offset);
    }
  }

  /**
   * Check tiles.
   *
   * @param metadata the metadata
   */
  private void CheckTiles(IfdTags metadata) {
    int id, offset;

    // Tile Offsets
    id = 324;
    offset = metadata.get(id).getFirstNumericValue();
    if (offset <= 0)
      validation.addError("Invalid value for field " + TiffTags.getTag(id).getName(), offset);

    // Tile Byte Counts
    id = 325;
    offset = metadata.get(id).getFirstNumericValue();
    if (offset <= 0)
      validation.addError("Invalid value for field " + TiffTags.getTag(id).getName(), offset);

    // Tile Width
    id = 322;
    if (!metadata.containsTagId(id))
      validation.addError("Missing required field for tiles " + TiffTags.getTag(id).getName(),
          offset);
    else {
      offset = metadata.get(id).getFirstNumericValue();
      if (offset <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).getName(), offset);
    }

    // Tile Length
    id = 323;
    if (!metadata.containsTagId(id))
      validation.addError("Missing required field for tiles " + TiffTags.getTag(id).getName(),
          offset);
    else {
      offset = metadata.get(id).getFirstNumericValue();
      if (offset <= 0)
        validation.addError("Invalid value for field " + TiffTags.getTag(id).getName(), offset);
    }
  }

  /**
   * Gets the validation.
   *
   * @return the validation
   */
  public ValidationResult getValidation() {
    return validation;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public ImageType getType() {
    return type;
  }
}

