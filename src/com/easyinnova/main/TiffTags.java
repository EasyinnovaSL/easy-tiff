/**
 * <h1>TiffTags.java</h1>
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
 * @author Xavier Tarrés Bonet
 * @version 1.0
 * @since 18/5/2015
 *
 */

package com.easyinnova.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map.Entry;

import com.google.gson.Gson; 
import com.google.gson.GsonBuilder;


/**
 * The Class TiffTags.
 */
public class TiffTags {

  /** The singleton instance. */
  private static TiffTags instance = null;

  /** The tag map. */
  protected static HashMap<Integer, Tag> tagMap = new HashMap<Integer, Tag>();

  /**
   * Instantiates a new tiff tags.
   */
  protected TiffTags() {
   
    File folder = new File("./config/tifftags/");
    Gson gson = new GsonBuilder().setDateFormat("dd/MM/yyyy").create();
    
    for (final File fileEntry : folder.listFiles()) {
      try {
        BufferedReader br = new BufferedReader(  
            new FileReader(fileEntry.toPath().toString()));
        
        Tag tag = gson.fromJson(br, Tag.class); 
        
        tagMap.put(tag.getId(), tag);
      } catch (FileNotFoundException e) {
        
        e.printStackTrace();
      }  
   } 
    
  }
  
  /**
   * Gets the tiff tags.
   *
   * @return the singleton instance
   */
  public static synchronized TiffTags getTiffTags() {

    if (instance == null) {
      instance = new TiffTags();
    }
    return instance;
  }

}
