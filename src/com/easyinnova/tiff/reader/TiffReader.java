/**
 * <h1>TiffReader.java</h1>
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
 * @since 28/5/2015
 *
 */
package com.easyinnova.tiff.reader;

import com.easyinnova.tiff.io.TiffStreamIO;
import com.easyinnova.tiff.model.Tag;
import com.easyinnova.tiff.model.TagValue;
import com.easyinnova.tiff.model.TiffDocument;
import com.easyinnova.tiff.model.TiffTags;
import com.easyinnova.tiff.model.ValidationResult;
import com.easyinnova.tiff.model.types.Ascii;
import com.easyinnova.tiff.model.types.Byte;
import com.easyinnova.tiff.model.types.Double;
import com.easyinnova.tiff.model.types.Float;
import com.easyinnova.tiff.model.types.IFD;
import com.easyinnova.tiff.model.types.Long;
import com.easyinnova.tiff.model.types.Rational;
import com.easyinnova.tiff.model.types.SByte;
import com.easyinnova.tiff.model.types.SLong;
import com.easyinnova.tiff.model.types.SRational;
import com.easyinnova.tiff.model.types.SShort;
import com.easyinnova.tiff.model.types.Short;
import com.easyinnova.tiff.model.types.Undefined;
import com.easyinnova.tiff.model.types.abstractTiffType;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

/**
 * Reads and parses a Tiff file, storing it in an internal model.
 */
public class TiffReader {

  /** The model containing the Tiff data. */
  TiffDocument tiffModel;

  /** The filename. */
  String filename;

  /** The stream to get data from the file. */
  TiffStreamIO data;

  /** The size in bytes of the tags' values field (=4 for Tiff, =8 for BigTiff). */
  int tagValueSize = 4;

  /** The result of the validation. */
  ValidationResult validation;

  /**
   * Tolerance to duplicate tags.<br>
   * 0: No tolerance. 1: Tolerate duplicates keeping first appearance only
   */
  int duplicateTagTolerance = 10;

  /**
   * Tolerance to errors in the next IFD pointer.<br>
   * 0: No tolerance. 1: Tolerate errors assuming there is no next Ifd (offset = 0)
   */
  int nextIFDTolerance = 0;

  /**
   * Tolerance to errors in the byte ordering.<br>
   * 0: No tolerance. 1: Lower case tolerance. 2: Full tolerance, assuming little endian.
   * */
  int byteOrderErrorTolerance = 0;

  /**
   * Default constructor.<br>
   * Instantiates a new tiff reader.
   */
  public TiffReader() {
    tiffModel = null;
    filename = null;
  }

  /**
   * Gets the internal model of the Tiff file.
   *
   * @return the model
   */
  public TiffDocument getModel() {
    return tiffModel;
  }

  /**
   * Gets the filename.
   *
   * @return the filename
   */
  public String getFilename() {
    return filename;
  }

  /**
   * Gets the result of the validation.
   *
   * @return the validation result
   */
  public ValidationResult getValidation() {
    return validation;
  }

  /**
   * Reads a Tiff File.
   *
   * @param filename the Tiff filename
   * @return Error code (0: successful, -1: file not found, -2: IO exception, -4: File header error,
   *         -5: Magic number mismatch)
   */
  public int readFile(String filename) {
    int result = 0;
    this.filename = filename;

    try {
      if (Files.exists(Paths.get(filename))) {
        data = new TiffStreamIO(null);

        try {
          tiffModel = new TiffDocument();
          validation = new ValidationResult();
          data.load(filename);
          readHeader();
          if (tiffModel.getMagicNumber() != 42) {
            // Incorrect tiff magic number
            result = -5;
          }
        } catch (Exception ex) {
          // Header error
          result = -4;
        }

        if (result == 0)
          readIFDs();

        data.close();
      } else {
        // File not found
        result = -1;
      }
    } catch (Exception ex) {
      // IO exception
      result = -2;
    }

    return result;
  }

  /**
   * Reads the Tiff header.
   *
   * @throws Exception parsing error
   */
  private void readHeader() throws Exception {
    boolean correct = true;
    ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    try {
      // read the first two bytes, in order to know the byte ordering
      if (data.get(0) == 'I' && data.get(1) == 'I')
        byteOrder = ByteOrder.LITTLE_ENDIAN;
      else if (data.get(0) == 'M' && data.get(1) == 'M')
        byteOrder = ByteOrder.BIG_ENDIAN;
      else if (byteOrderErrorTolerance > 0 && data.get(0) == 'i' && data.get(1) == 'i') {
        validation.addWarning("Byte Order in lower case");
        byteOrder = ByteOrder.LITTLE_ENDIAN;
      } else if (byteOrderErrorTolerance > 0 && data.get(0) == 'm' && data.get(1) == 'm') {
        validation.addWarning("Byte Order in lower case");
        byteOrder = ByteOrder.BIG_ENDIAN;
      } else if (byteOrderErrorTolerance > 1) {
        validation.addWarning("Non-sense Byte Order. Trying Little Endian.");
        byteOrder = ByteOrder.LITTLE_ENDIAN;
      } else {
        correct = false;
        throw new Exception("Invalid Byte Order " + data.get(0) + data.get(1));
      }
    } catch (Exception ex) {
      correct = false;
      throw new Exception("Header format error");
    }

    if (correct) {
      // set byte ordering to the stream
      data.order(byteOrder);

      try {
        // read magic number
        tiffModel.setMagicNumber(data.getShort(2));
      } catch (IndexOutOfBoundsException ex) {
        throw new Exception("Magic number parsing error");
      }
    }
  }

  /**
   * Read the IFDs contained in the Tiff file.
   */
  public void readIFDs() {
    try {
      // The pointer to the first IFD is located in bytes 4-7
      int offset0 = (int) data.getInt(4);

      IfdReader ifd0 = readIFD(offset0, true);
      HashSet<Integer> usedOffsets = new HashSet<Integer>();
      usedOffsets.add(offset0);
      if (ifd0.getIfd() == null) {
        validation.addError("Parsing error in first IFD");
      } else {
        tiffModel.addIfd0(ifd0.getIfd());

        IfdReader current_ifd = ifd0;

        // Read next IFDs
        while (current_ifd.getNextIfdOffset() > 0) {
          if (usedOffsets.contains(current_ifd.getNextIfdOffset())) {
            // Circular reference
            validation.addError("IFD offset already used");
            break;
          } else {
            usedOffsets.add(current_ifd.getNextIfdOffset());
            IfdReader next_ifd = readIFD(current_ifd.getNextIfdOffset(), true);
            current_ifd.getIfd().setNextIFD(next_ifd.getIfd());
            current_ifd = next_ifd;
          }
        }
      }
    } catch (Exception ex) {
      validation.addError("IFD parsing error");
    }
  }

  /**
   * Parses the image file descriptor data.
   *
   * @param offset the file offset (in bytes) pointing to the IFD
   * @param isImage the is image
   * @return the ifd reading result
   */
  IfdReader readIFD(int offset, boolean isImage) {
    IFD ifd = new IFD(isImage);
    int nextIfdOffset = 0;
    try {
      int index = offset;
      int directoryEntries = data.getShort(offset);
      if (directoryEntries < 0) {
        validation.addError("Incorrect number of IFD entries",
            directoryEntries);
      } else {
        index += 2;

        // Reads the tags
        for (int i = 0; i < directoryEntries; i++) {
          int tagid = data.getUshort(index);
          int tagType = data.getShort(index + 2);
          int tagN = data.getInt(index + 4);
          TagValue tv = getValue(tagType, tagN, tagid, index + 8, ifd);
          if (ifd.containsTagId(tagid)) {
            if (duplicateTagTolerance > 0)
              validation.addWarning("Duplicate tag", tagid);
            else
              validation.addError("Duplicate tag", tagid);
          } else {
            ifd.addTag(tv);
          }
          try {
          } catch (Exception ex) {
            validation.addError("Parse error in tag " + tagid + " value");
          }

          index += 12;
        }

        // Reads the position of the next IFD
        nextIfdOffset = 0;
        try {
          nextIfdOffset = (int) data.getInt(index);
        } catch (Exception ex) {
          nextIfdOffset = 0;
          if (nextIFDTolerance > 0)
            validation.addWarning("Unreadable next IFD offset");
          else
            validation.addError("Unreadable next IFD offset");
        }
        if (nextIfdOffset > 0 && nextIfdOffset < 7) {
          validation.addError("Invalid next IFD offset", nextIfdOffset);
          nextIfdOffset = 0;
        }

        // Validate ifd entries
        if (isImage) {
          BaselineProfile bp = new BaselineProfile();
          bp.validateIfd(ifd);
          validation.add(bp.getValidation());
        }
      }
    } catch (Exception ex) {
      ifd = null;
    }
    IfdReader ir = new IfdReader();
    ir.setIfd(ifd);
    ir.setNextIfdOffset(nextIfdOffset);
    ir.readImage();
    return ir;
  }

  /**
   * Gets the value of the given tag field.
   *
   * @param type the tag type
   * @param n the cardinality
   * @param id the tag id
   * @param beginOffset the offset position of the tag value
   * @param parentIFD the parent ifd
   * @return the tag value object
   */
  public TagValue getValue(int type, int n, int id, int beginOffset, IFD parentIFD) {
    // Create TagValue object
    TagValue tv = new TagValue(id, type);

    // Defined tags
    int offset = beginOffset;

    // Get type Size
    int typeSize = 1;
    switch (type) {
      case 3:
      case 8:
        typeSize = 2;
        break;
      case 4:
      case 9:
      case 11:
      case 13:
        typeSize = 4;
        break;
      case 5:
      case 10:
      case 12:
        typeSize = 8;
        break;
      default:
        typeSize = 1;
        break;
    }

    // Check if the tag value fits in the directory entry value field, and get offset if not
    if (typeSize * n > tagValueSize) {
      offset = data.getInt(offset);
    }

    for (int i = 0; i < n; i++) {
      // Get N tag values
      switch (type) {
        case 1:
          tv.add(new Byte((byte) data.get(offset)));
          break;
        case 2:
          tv.add(new Ascii((byte) data.get(offset)));
          break;
        case 6:
          tv.add(new SByte((byte) data.get(offset)));
          break;
        case 7:
          tv.add(new Undefined((byte) data.get(offset)));
          break;
        case 3:
          tv.add(new Short((char) data.getUshort(offset)));
          break;
        case 8:
          tv.add(new SShort((short) data.getShort(offset)));
          break;
        case 4:
          tv.add(new Long(data.getInt(offset)));
          break;
        case 9:
          tv.add(new SLong(data.getInt(offset)));
          break;
        case 5:
          int num = new Long(data.getInt(offset)).toInt();
          int den = new Long(data.getInt(offset + 4)).toInt();
          tv.add(new Rational(num, den));
          break;
        case 10:
          int snum = new SLong(data.getInt(offset)).toInt();
          int sden = new SLong(data.getInt(offset + 4)).toInt();
          tv.add(new SRational(snum, sden));
          break;
        case 11:
          tv.add(new Float(data.getFloat(offset)));
          break;
        case 12:
          tv.add(new Double(data.getDouble(offset)));
          break;
        case 13:
          int ifdOffset = data.getInt(offset);
          IfdReader ifd = readIFD(ifdOffset, true);
          IFD subIfd = ifd.getIfd();
          subIfd.setParent(parentIFD);
          tv.add(subIfd);
          break;
      }
      offset += typeSize;
    }

    if (TiffTags.hasTag(id)) {
      Tag t = TiffTags.getTag(id);
      if (t.hasTypedef()) {
        String tagtype = t.getTypedef();

        try {
          abstractTiffType instanceOfMyClass =
              (abstractTiffType) Class.forName("com.easyinnova.tiff.model.types." + tagtype)
                  .getConstructor().newInstance();
          if (instanceOfMyClass.isIFD()) {
            int ifdOffset = tv.getFirstNumericValue();
            IfdReader ifd = readIFD(ifdOffset, false);
            IFD exifIfd = ifd.getIfd();
            tv.clear();
            tv.add(exifIfd);
          } else {
            instanceOfMyClass.read(tv);
          }
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
        } catch (NoSuchMethodException | SecurityException e) {
          e.printStackTrace();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
            | InvocationTargetException e) {
          e.printStackTrace();
        }

      }
    }

    return tv;
  }

  /**
   * Reads a short. (deprecated)
   *
   * @param index Offset
   * @param order the order
   * @param data the data
   * @return the short
   * @throws IOException Signals that an I/O exception has occurred.
   * @deprecated use {data.getShort} instead.
   */
  @Deprecated
  int readShort(int index, ByteOrder order, TiffStreamIO data) throws IOException {
    int result = 0;

    try {
      if (order == ByteOrder.BIG_ENDIAN) {
        result = data.get(index) & 0xff;
        result <<= 8;
        result += data.get(index + 1) & 0xff;
      } else {
        result = data.get(index + 1) & 0xff;
        result <<= 8;
        result += data.get(index) & 0xff;
      }
    } catch (Exception ex) {
      throw new IOException();
    }
    return result;
  }

  /**
   * Reads a long. (deprecated)
   *
   * @param index Offset
   * @param order the order
   * @param data the data
   * @return the long
   * @throws IOException Signals that an I/O exception has occurred.
   * @deprecated use {data.getInt} instead.
   */
  @Deprecated
  long readLong(int index, ByteOrder order, TiffStreamIO data) throws IOException {
    long result = 0;

    try {
      if (order == ByteOrder.BIG_ENDIAN) {
        result = data.get(index) & 0xff;
        result <<= 8;
        result += data.get(index + 1) & 0xff;
        result <<= 8;
        result += data.get(index + 2) & 0xff;
        result <<= 8;
        result += data.get(index + 3) & 0xff;
      } else {
        result = data.get(index + 3) & 0xff;
        result <<= 8;
        result = data.get(index + 2) & 0xff;
        result <<= 8;
        result = data.get(index + 1) & 0xff;
        result <<= 8;
        result += data.get(index) & 0xff;
      }
    } catch (Exception ex) {
      throw new IOException();
    }
    return result;
  }
}

