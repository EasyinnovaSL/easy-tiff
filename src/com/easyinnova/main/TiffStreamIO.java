/**
 * <h1>TiffStreamReader.java</h1> 
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
 * @since 22/5/2015
 *
 */
package com.easyinnova.main;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

// TODO: Auto-generated Javadoc
/**
 * The Class TiffStreamReader.
 */
public class TiffStreamIO {

  /** The data. */
  MappedByteBuffer data;

  /** The a file. */
  RandomAccessFile aFile;

  /** The channel. */
  FileChannel channel;

  /** The filename. */
  String filename;

  /**
   * Instantiates a new tiff stream reader.
   *
   * @param filename the path
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public TiffStreamIO(String filename) {
    this.filename = filename;
  }

  /**
   * Read.
   */
  public void read() throws IOException {
    aFile = new RandomAccessFile(filename, "rw");
    channel = aFile.getChannel();
    data = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());

    // loads the file in memory
    data.load();
  }

  /**
   * Write.
   */
  public void write() throws IOException {
    aFile = new RandomAccessFile(filename, "rw");
    channel = aFile.getChannel();
    data = channel.map(FileChannel.MapMode.READ_WRITE, 0, 10000000);
  }

  /**
   * Close.
   */
  public void close() {
    try {
      channel.close();
      aFile.close();
    } catch (Exception ex) {
    }
  }

  /**
   * Gets a byte.
   *
   * @param offset the file position offset
   * @return the int
   */
  public int get(int offset) {
    return data.get(offset);
  }

  /**
   * Puts a byte.
   *
   * @param offset the file position offset
   * @return the int
   */
  public void put(byte val) {
    data.put(val);
  }

  /**
   * Puts a short (2 bytes).
   *
   * @param offset the file position offset
   * @return the int
   */
  public void putShort(short val) {
    data.putShort(val);
  }

  /**
   * Puts a int (4 bytes).
   *
   * @param offset the file position offset
   * @return the int
   */
  public void putInt(int val) {
    data.putInt(val);
  }

  /**
   * Define Byte Order.
   *
   * @param byteOrder the byte order
   */
  public void order(ByteOrder byteOrder) {
    data.order(byteOrder);
  }

  /**
   * Gets a short (2 bytes).
   *
   * @param offset the file position offset
   * @return the short
   */
  public int getShort(int offset) {
    return data.getShort(offset);
  }

  /**
   * Gets a int (4 bytes).
   *
   * @param offset the file position offset
   * @return the int
   */
  public int getInt(int offset) {
    return data.getInt(offset);
  }

  /**
   * Gets a long (8 bytes).
   *
   * @param offset the file position offset
   * @return the long
   */
  public long getLong(int offset) {
    return data.getLong(offset);
  }

  /**
   * Gets a ushort (8 bytes).
   *
   * @param offset the file position offset
   * @return the ushort
   */
  public int getUshort(int offset) {
    return getShort(offset) & 0xFFFF;
  }

  /**
   * Position.
   *
   * @return the int
   */
  public int position() {
    return data.position();
  }
}

