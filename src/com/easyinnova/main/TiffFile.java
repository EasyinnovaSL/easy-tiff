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

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The Class TiffFile.
 */
public class TiffFile {

  /** The filename. */
  String filename;

  /** The file data. */
  TiffStreamIO data;

  /** The header. */
  Header header;

  /** Structure of the Tiff file. */
  IfdStructure ifdStructure;

  /** The result of the validation. */
  public ValidationResult validation;

  /**
   * Instantiates a new tiff file.
   *
   * @param filename File name
   */
  public TiffFile(String filename) {
    this.filename = filename;
    validation = new ValidationResult();
  }

  /**
   * Reads a Tiff File.
   *
   * @return Error code (0: successful, -1: file not found, -2: IO exception, -3: other exception)
   */
  public int read() {
    int error = 0;

    try {
      if (Files.exists(Paths.get(filename))) {
        data = new TiffStreamIO(filename);
        data.read();

        validation.correct = true;

        try {
          // parses the file
          readTiff();
        } catch (Exception ex) {
          // Internal parsing exception
          error = -3;
        }

        data.close();
      } else {
        // File not found
        error = -1;
      }
    } catch (IOException ex) {
      // IO exception
      error = -2;
    }

    return error;
  }

  /**
   * Parses the data.
   */
  private void readTiff() {
    // Read the Head
    header = new Header(data);
    header.read();
    validation.add(header.validation);

    // Read the IFDs
    if (header.validation.correct) {
      ifdStructure = new IfdStructure(data);
      ifdStructure.read();
      validation.add(ifdStructure.validation);
    }
    // else if the header is not correct we can't read anything more
  }

  /**
   * Reads a short. (deprecated)
   *
   * @param index Offset
   * @param order the order
   * @return the short
   * @throws IOException Signals that an I/O exception has occurred.
   * @deprecated use {data.getShort} instead.
   */
  @Deprecated
  int readShort(int index, ByteOrder order) throws IOException {
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
   * @deprecated use {data.getInt} instead.
   */
  @Deprecated
  long readLong(int index, ByteOrder order) throws IOException {
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
   * Write.
   */
  public void write(TiffFile data) {
    TiffStreamIO odata = new TiffStreamIO("out.tif");
    try {
      odata.write();
      data.header.write(odata);
      data.ifdStructure.write(odata);
      odata.close();
    } catch (Exception ex) {
      ex.toString();
    }
  }
}
