/**
 * <h1>TiffFile.java</h1> 
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
 * NB: for the � statement, include Easy Innova SL or other company/Person contributing the code.
 * </p>
 * <p>
 * � 2015 Easy Innova, SL
 * </p>
 *
 * @author V�ctor Mu�oz Sol�
 * @version 1.0
 * @since 14/5/2015
 *
 */
package com.easyinnova.main;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The Class TiffFile.
 */
public class TiffFile {

  /** The filename. */
  String Filename;

  /** The file data (buffered). */
  MappedByteBuffer data;

  /** Structure of the Tiff file. */
  TiffStructure IfdStructure;

  /** The result of the validation. */
  ValidationResult validation_result;

  /** Indicate if the Tiff file is readable. */
  public boolean Correct;

  /**
   * Instantiates a new tiff file.
   *
   * @param filename File name
   */
  public TiffFile(String filename) {
    this.Filename = filename;
    validation_result = new ValidationResult();
    IfdStructure = new TiffStructure();
    Correct = true;
  }

  /**
   * Reads a Tiff File.
   *
   * @return Error code (0 if successful)
   */
  public int Read() {
    Path path = Paths.get(Filename);
    int error = 0;

    try {
      if (Files.exists(path)) {
        RandomAccessFile aFile = new RandomAccessFile(Filename, "rw");
        FileChannel channel = aFile.getChannel();
        data = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());
        data.load();

        // Read the Tiff Head
        Correct = ReadHeader();

        // Read Tiff IFDs
        if (Correct)
          ReadIFDs();

        channel.close();
        aFile.close();
      } else {
        // File not found
        error = -1;
      }
    } catch (IOException ex) {
      // Exception
      error = -2;
    }

    return error;
  }

  /**
   * Read Image Directories.
   */
  void ReadIFDs() {
    int n = 0;

    // Read first IFD
    int offset0 = (int) data.getInt(4);
    IFD ifd0 = ReadIFD(offset0);
    if (ifd0 == null) {
      Correct = false;
      validation_result.addError("IFD 0 does not exist");
    }
    else {
      ifd0.Validate(validation_result, data);
      IfdStructure.AddIfd(ifd0);
      n++;
      IFD current_ifd = ifd0;
      // Read next IFDs
      while (current_ifd.hasNextIFD()) {
        int offset = current_ifd.nextIFDOffset();
        if (!IfdStructure.UsedOffset(offset)) {
        current_ifd = ReadIFD(offset);
        if (current_ifd == null) {
          validation_result.addError("Next IFD does not exist", "" + offset);
          break;
        } else {
            current_ifd.Validate(validation_result, data);
          IfdStructure.AddIfd(current_ifd);
        }
        n++;
        } else {
          validation_result.addError("IFD offset already used");
          break;
        }
      }
    }
    validation_result.nifds = n;
  }

  /**
   * Reads a ifd.
   *
   * @param IFD_idx Offset
   * @return the ifd
   */
  IFD ReadIFD(int offset) {
    IFD ifd = new IFD(offset);
    try {
      int index = offset;
      int directoryEntries = data.getShort(offset);
      if (directoryEntries < 0) {
        validation_result.addError("Incorrect number of IFD entries", directoryEntries);
        ifd.Correct = false;
      } else {
        index += 2;
        for (int i = 0; i < directoryEntries; i++) {
          // Read a tag
          int tagid = data.getShort(index);
          int tagType = data.getShort(index + 2);
          int tagN = data.getInt(index + 4);
          IfdEntry tag = new IfdEntry(tagid, tagType, tagN);
          tag.getValue(data, index + 8, tagType);
          ifd.AddTag(tag);

          index += 12;
        }
        // Reads the position of the next IFD
        ifd.NextIFD = (int) data.getInt(index);
      }
    } catch (IndexOutOfBoundsException ex) {
      ifd = null;
    }
    return ifd;
  }

  /**
   * Reads the header.
   * 
   * @return true, if successful
   */
  public boolean ReadHeader() {
    boolean ok = true;
    ByteOrder order = ByteOrder.BIG_ENDIAN;

    // read the first two bytes to know the byte ordering
    if (data.get(0) == 'I' && data.get(1) == 'I')
      order = ByteOrder.LITTLE_ENDIAN;
    else if (data.get(0) == 'M' && data.get(1) == 'M')
      order = ByteOrder.BIG_ENDIAN;
    else {
      validation_result.addError("Invalid Byte Order", "" + data.get(0) + data.get(1));
      ok = false;
    }

    // set byte ordering
    data.order(order);

    if (ok) {
      try{
        int magic = data.getShort(2);
        if (magic != 42) {
          ok = false;
          validation_result.addError("Magic number != 42", magic);
        }
      } catch (IndexOutOfBoundsException ex) {
        validation_result.addError("Magic number format error");
        ok = false;
      }
    }

    return ok;
  }

  /**
   * Reads a short. (deprecated)
   *
   * @param index Offset
   * @param order the order
   * @return the short
   * @throws IOException Signals that an I/O exception has occurred.
   */
  int ReadShort(int index, ByteOrder order) throws IOException {
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
   * @return the long
   * @throws IOException Signals that an I/O exception has occurred.
   */
  long ReadLong(int index, ByteOrder order) throws IOException {
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

 
  /**
   * Gets the validation.
   *
   * @param output_file the output_file
   * @return true, if successful
   */
  public boolean GetValidation(String output_file) {
    return validation_result.Errors.size() == 0;
  }

  /**
   * Prints the errors.
   */
  public void PrintErrors() {
    for (ValidationError ve : validation_result.Errors) {
      ve.Print();
    }
  }

  /**
   * Prints the warnings.
   */
  public void PrintWarnings() {
    for (ValidationError ve : validation_result.Warnings) {
      ve.PrintWarning();
    }
  }
}
