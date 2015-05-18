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
import java.nio.file.Path;
import java.nio.file.Paths;

public class TiffFile {
  String filename;
  TiffHeader header;
  byte[] data;
  IFD IFD0;

  public TiffFile(String filename) {
    this.filename = filename;
  }

  public int Read() {
    Path path = Paths.get(filename);
    int error = 0;

    try {
      if (Files.exists(path)) {
        this.data = Files.readAllBytes(path);
        boolean result = ReadHeader();
        if (!result)
          error = -3;
        else {
          ReadIFD0();
        }
      } else {
        error = -1;
      }
    } catch (IOException ex) {
      error = -2;
    }

    return error;
  }

  void ReadIFD0() {
    try {
      long IFD0_idx = ReadLong(4);
      IFD0 = ReadIFD(IFD0_idx);
    } catch (IOException ex) {
      IFD0 = null;
    }
  }

  IFD ReadIFD(long IFD_idx) {
    IFD ifd = new IFD();
    int index = (int) IFD_idx;
    try {
      int directoryEntries = ReadShort(index);
      index += 2;
      for (int i = 0; i < directoryEntries; i++) {
        int tagid = ReadShort(index);
        int tagType = ReadShort(index + 2);
        long tagN = ReadLong(index + 4);
        long tagValue = ReadLong(index + 8);

        Tag tag = new Tag(tagid, tagType, tagN, tagValue);
        ifd.AddTag(tag);
      }
    } catch (IOException ex) {
      ifd = null;
    }
    return ifd;
  }

  public boolean ReadHeader() {
    boolean ok = true;
    header = new TiffHeader();

    // read the first two bytes to know the byte ordering
    if (data[0] == 'I' && data[1] == 'I')
      header.Order = ByteOrder.LITTLE_ENDIAN;
    else if (data[0] == 'M' && data[1] == 'M')
      header.Order = ByteOrder.BIG_ENDIAN;
    else
      ok = false;

    if (ok) {
      try{
        int magic = ReadShort(2);
        if (magic != 42)
          ok = false;
      }
 catch (IOException ex) {
        ok = false;
      }
    }

    return ok;
  }

  int ReadShort(int index) throws IOException {
    int result = 0;

    if (index + 1 >= data.length) {
      throw new IOException();
    }

    if (header.Order == ByteOrder.BIG_ENDIAN) {
      result = data[index] & 0xff;
      result <<= 8;
      result += data[index + 1] & 0xff;
    } else {
      result = data[index + 1] & 0xff;
      result <<= 8;
      result += data[index] & 0xff;
    }
    return result;
  }

  long ReadLong(int index) throws IOException {
    long result = 0;

    if (index + 3 >= data.length) {
      throw new IOException();
    }

    if (header.Order == ByteOrder.BIG_ENDIAN) {
      result = data[index] & 0xff;
      result <<= 8;
      result += data[index + 1] & 0xff;
      result <<= 8;
      result += data[index + 2] & 0xff;
      result <<= 8;
      result += data[index + 3] & 0xff;
    } else {
      result = data[index + 3] & 0xff;
      result <<= 8;
      result = data[index + 2] & 0xff;
      result <<= 8;
      result = data[index + 1] & 0xff;
      result <<= 8;
      result += data[index] & 0xff;
    }
    return result;
  }
}
