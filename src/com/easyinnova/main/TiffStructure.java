<<<<<<< HEAD:src/com/easyinnova/main/Main.java
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

import java.io.File;

import javafx.stage.Stage;


/**
 * The Class Main.
 */
public class Main {

  /**
   * The main method.
   *
   * @param args asdf
   * @throws Exception asd
   */
  public static void main(final String[] args) throws Exception {
    
    //System.out.println(new java.io.File("").getAbsolutePath());
    //System.out.println(Main.class.getClassLoader().getResource("").getPath());
   
   
    TiffTags a= TiffTags.getTiffTags();
    
   
    }
  
 
}

=======
/**
 * <h1>TiffStructure.java</h1> 
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
 * @since 18/5/2015
 *
 */
package com.easyinnova.main;

import java.util.ArrayList;

/**
 * The Class TiffStructure.
 */
public class TiffStructure {

  /** The list of Ifd. */
  ArrayList<IFD> IFDs;

  /**
   * Instantiates a new tiff structure object.
   */
  public TiffStructure() {
    IFDs = new ArrayList<IFD>();
  }

  /**
   * Adds an IFD to the list.
   *
   * @param ifd the ifd
   */
  public void AddIfd(IFD ifd) {
    IFDs.add(ifd);
  }
}

>>>>>>> 696870206a23088be30762534e1fd8b32eda89e5:src/com/easyinnova/main/TiffStructure.java
