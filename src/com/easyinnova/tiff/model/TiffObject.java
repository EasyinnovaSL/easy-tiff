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
package com.easyinnova.tiff.model;

import com.easyinnova.tiff.io.TiffStreamIO;

/**
 * The Class TiffFile.
 */
public class TiffObject {

  /** The file data. */
  TiffStreamIO data;

  /** Structure of the Tiff file. */
  public IfdStructure ifdStructure;

  /** The result of the validation. */
  public ValidationResult validation;

  /**
   * Instantiates a new tiff file.
   *
   * @param data the data
   */
  public TiffObject(TiffStreamIO data) {
    this.data = data;
    validation = new ValidationResult();
  }

  /**
   * Gets the stream.
   *
   * @return the stream
   */
  public TiffStreamIO getStream() {
    return data;
  }

  /**
   * Parses the data.
   */
  public void readTiff() {
    // Read the IFDs
    ifdStructure = new IfdStructure(data);
    ifdStructure.read();
    validation.add(ifdStructure.validation);
  }
}
