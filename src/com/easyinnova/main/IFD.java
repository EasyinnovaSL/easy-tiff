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
      tags.validate(validation);

      // Validate image
      checkImage();
    }
  }

  /**
   * Check image.
   */
  public boolean checkImage() {
    boolean ok = true;

    ok &= tags.checkField(256, validation);
    ok &= tags.checkField(257, validation);
    ok &= tags.checkField(262, validation);
    
    if (!tags.containsTagId(258)) {
      type = ImageType.BILEVEL;
    } else {
      if (tags.containsTagId(320)) {
        type = ImageType.PALETTE;
        ok &= tags.checkField(258, validation);
      } else if (tags.containsTagId(277)) {
        type = ImageType.RGB;
        ok &= tags.checkField(258, validation);
        long val = tags.get(257).value.getLongValue();
        if (val < 3)
          validation.addError("Samples per Pixel < 3", (int) val);
        if (!tags.get(258).value.isOffset)
          validation.addError("Incorrect Bits Per Sample tag type");
        else {
          TagValue tag = tags.get(258).value;
          int short1 = data.getShort((int) tag.value);
          int short2 = data.getShort((int) tag.value + 2);
          int short3 = data.getShort((int) tag.value + 4);
          if (short1 < 8 || short2 < 8 || short3 < 8) {
            validation.addError("Bits Per Sample != 8", short3);
          }
        }
      } else {
        type = ImageType.GRAYSCALE;
        long val = tags.get(258).value.getLongValue();
        if (tags.get(258).value.isOffset)
          validation.addError("Incorrect Bits Per Sample tag type");
        else if (val != 4 && val != 8)
          validation.addError("Incorrect Bits Per Sample tag", (int) val);
      }
    }

    return ok;
  }
}
