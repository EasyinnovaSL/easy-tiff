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
import java.util.HashMap;

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

  /** Tag list. */
  public ArrayList<IfdEntry> Tags;

  /** The Hash tags id. */
  public HashMap<Integer, IfdEntry> HashTagsId;

  /** The Hash tags name. */
  public HashMap<String, IfdEntry> HashTagsName;

  /** The Next ifd. */
  public int NextIFD = 0;

  /** The Offset. */
  public int Offset = 0;

  /** The Correct. */
  public boolean Correct;
  
  /** The Type. */
  public ImageType Type;

  /**
   * Instantiates a new ifd.
   */
  public IFD(int offset) {
    Offset = offset;
    Tags = new ArrayList<IfdEntry>();
    HashTagsId = new HashMap<Integer, IfdEntry>();
    HashTagsName = new HashMap<String, IfdEntry>();
    Correct = true;
    Type = ImageType.UNDEFINED;
  }

  /**
   * Adds a tag.
   *
   * @param tag the tag
   */
  public void AddTag(IfdEntry tag) {
    Tags.add(tag);
    HashTagsId.put(tag.id, tag);
    Tag t = TiffTags.getTag(tag.id);
    if (t != null)
      HashTagsName.put(t.name, tag);
  }

  /**
   * Checks for next ifd.
   *
   * @return true, if next IFD exists
   */
  public boolean hasNextIFD() {
    return NextIFD > 0;
  }

  /**
   * Get Next IFD.
   *
   * @return the next ifd
   */
  public int nextIFDOffset() {
    return NextIFD;
  }

  /**
   * Validates the IFD.
   *
   * @param validation_result the validation_result
   */
  public void Validate(ValidationResult validation_result, MappedByteBuffer data) {
    if (Correct) {
      // Validate tags
      for (IfdEntry ie : Tags) {
        ie.Validate(validation_result);
      }

      // Validate image
      CheckImage(validation_result, data);
    }
  }

  /**
   * Check image.
   */
  public boolean CheckImage(ValidationResult validation_result, MappedByteBuffer data) {
    boolean ok = true;
    if (!HashTagsId.containsKey(256)) {
      ok = false;
      validation_result.addError("Missing image width tag");
    } else if (HashTagsId.get(256).value.isOffset) {
      ok = false;
      validation_result.addError("Incorrect image width tag");
    }

    if (!HashTagsId.containsKey(257)) {
      ok = false;
      validation_result.addError("Missing image height tag");
    } else if (HashTagsId.get(257).value.isOffset) {
      ok = false;
      validation_result.addError("Incorrect image height tag");
    }

    if (!HashTagsId.containsKey(262)) {
      ok = false;
      validation_result.addError("Missing Photometric Interpretation tag");
    } else if (HashTagsId.get(262).value.isOffset) {
      long val = HashTagsId.get(262).value.Value;
      ok = val == 0 || val == 1;
      if (!ok)
        validation_result.addError("Incorrect Photometric Interpretation tag");
    }
    
    if (!HashTagsId.containsKey(258)) {
      Type = ImageType.BILEVEL;
    } else {
      if (HashTagsId.containsKey(320)) {
        Type = ImageType.PALETTE;
        if (HashTagsId.get(258).value.isOffset)
          validation_result.addError("Incorrect Bits Per Sample tag type");
        else if (HashTagsId.get(258).value.getLongValue() != 4
            && HashTagsId.get(258).value.getLongValue() != 8)
          validation_result.addError("Bits Per Sample tag != 8",
              (int) HashTagsId.get(258).value.getLongValue());
      } else if (HashTagsId.containsKey(277)) {
        Type = ImageType.RGB;
        if (HashTagsId.get(257).value.getLongValue() < 3) {
          validation_result.addError("Samples per Pixel < 3",
              (int) HashTagsId.get(257).value.getLongValue());
        }
        if (!HashTagsId.get(258).value.isOffset)
          validation_result.addError("Incorrect Bits Per Sample tag type");
        else {
          int short1 = data.getShort((int) HashTagsId.get(258).value.Value);
          int short2 = data.getShort((int) HashTagsId.get(258).value.Value + 2);
          int short3 = data.getShort((int) HashTagsId.get(258).value.Value + 4);
          if (short1 < 8 || short2 < 8 || short3 < 8) {
            validation_result.addError("Bits Per Sample != 8", short3);
          }
        }
      } else {
        Type = ImageType.GRAYSCALE;
        if (HashTagsId.get(258).value.isOffset)
          validation_result.addError("Incorrect Bits Per Sample tag type");
        else if (HashTagsId.get(258).value.getLongValue() != 4
            && HashTagsId.get(258).value.getLongValue() != 8)
          validation_result.addError("Incorrect Bits Per Sample tag");
      }
    }

    return ok;
  }

  /**
   * Gets the tag value.
   *
   * @param string the string
   * @return the tag value
   */
  public String getTagValue(String tagname) {
    String val = "";
    if (HashTagsName.containsKey(tagname))
      val = HashTagsName.get(tagname).value.getValue();
    return val;
  }
}
