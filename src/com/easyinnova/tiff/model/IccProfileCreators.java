/**
 * <h1>IccProfileCreators.java</h1> 
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
 * @since 8/6/2015
 *
 */
package com.easyinnova.tiff.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

/**
 * The Class IccProfileCreators.
 */
public class IccProfileCreators {

  /** The singleton instance. */
  private static IccProfileCreators instance = null;

  /** The tag map. */
  public static HashMap<Integer, IccProfileCreator> creatorsMap =
      new HashMap<Integer, IccProfileCreator>();

  /**
   * Instantiates a new tiff tags.
   */
  protected IccProfileCreators() {
    try {
      FileReader fr = new FileReader("./config/iccprofile/creators.txt");
      BufferedReader br = new BufferedReader(fr);
      String line = br.readLine();
      while (line != null) {
        String[] fields = line.split("\t");
        if (fields.length == 3) {
          int signature = Integer.parseInt(fields[1], 16);
          IccProfileCreator icp = new IccProfileCreator(signature, fields[0], fields[2]);
          creatorsMap.put(icp.getSignature(), icp);
        }
        line = br.readLine();
      }
      br.close();
      fr.close();
    } catch (Exception ex) {

    }
  }

  /**
   * Gets the tiff tags.
   *
   * @return the singleton instance
   */
  public static synchronized IccProfileCreators getIccProfileCreators() {
    if (instance == null) {
      instance = new IccProfileCreators();
    }
    return instance;
  }

  /**
   * Gets tag information.
   *
   * @param identifier Tag id
   * @return the tag or null if the identifier does not exist
   */
  public static IccProfileCreator getIccProfile(int identifier) {
    IccProfileCreator icc = null;
    if (instance == null)
      getIccProfileCreators();
    if (creatorsMap.containsKey(identifier))
      icc = creatorsMap.get(identifier);
    return icc;
  }

  /**
   * Checks for tag.
   *
   * @param id the id
   * @return true, if successful
   */
  public static boolean hasIccCreator(int id) {
    return creatorsMap.containsKey(id);
  }
}

