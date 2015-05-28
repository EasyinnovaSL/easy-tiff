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
package com.easyinnova.tiff.reader;

import com.easyinnova.tiff.io.TiffStreamIO;
import com.easyinnova.tiff.model.TiffObject;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The Class TiffReader.
 */
public class TiffReader {

  /** The tiff file. */
  TiffObject tiffFile;

  /** The filename. */
  String filename;

  /**
   * Instantiates a new tiff reader.
   */
  public TiffReader() {
    tiffFile = null;
    filename = null;
  }

  /**
   * Reads a Tiff File.
   *
   * @param filename the Tiff filename
   * @return Error code (0: successful, -1: file not found, -2: IO exception, -3: other exception)
   */
  public int readFile(String filename) {
    int result = 0;
    this.filename = filename;

    try {
      if (Files.exists(Paths.get(filename))) {
        TiffStreamIO data = new TiffStreamIO(null);
        data.load(filename);
        try {
          tiffFile = new TiffObject(data);
          tiffFile.readTiff();
        } catch (Exception ex) {
          // Internal parsing exception
          result = -3;
        }

        data.close();
      } else {
        // File not found
        result = -1;
      }
    } catch (IOException ex) {
      // Exception
      result = -2;
    }

    return result;
  }

  /**
   * Gets the model.
   *
   * @return the model
   */
  public TiffObject getModel() {
    return tiffFile;
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
   * Gets the stream.
   *
   * @return the stream
   */
  public TiffStreamIO getStream() {
    return tiffFile.getStream();
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

