/**
 * <h1>TiffReaderWriter.java</h1> 
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

import java.io.IOException;
import java.util.ArrayList;

/**
 * The Class EasyTiff.
 */
public class TiffReaderWriter {

  /**
   * The main method.
   *
   * @param args the arguments
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static void main(final String[] args) throws IOException {
    ArrayList<String> files = new ArrayList<String>();
    String output_file = null;
    boolean args_error = false;
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg == "-o") {
        if (i + 1 < args.length)
          output_file = args[++i];
        else {
          args_error = true;
          break;
        }
      } else if (arg == "-help") {
        DisplayHelp();
      } else if (arg.startsWith("-")) {
        args_error = true;
        break;
      } else {
        files.add(arg);
      }
    }
    if (args_error) {
      DisplayHelp();
    } else {
      for (final String filename : files) {
        TiffFile tiffFile = new TiffFile(filename);
        int result = tiffFile.Read();
        ReportResults(tiffFile, result, output_file);
      }
    }
  }

  /**
   * @param tiffFile
   * @param result
   */
  private static void ReportResults(TiffFile tiffFile, int result, String output_file) {
    String filename = tiffFile.Filename;
    if (output_file != null) {
      // TODO: Create xml file with report
    } else {
      // Display results human readable
      switch (result) {
        case -1:
          System.out.println("File '" + filename + "' does not exist");
          break;
        case -2:
          System.out.println("IO Exception in file '" + filename + "'");
          break;
        case -3:
          System.out.println("Internal exception in file '" + filename + "'");
          break;
        case 0:
          if (tiffFile.GetValidation(output_file)) {
            System.out.println("Everything ok in file '" + filename + "'");
            System.out.println("IFDs: " + tiffFile.validation_result.nifds);
          } else {
            System.out.println("Errors in file '" + filename + "'");
            tiffFile.PrintErrors();
          }
          break;
        default:
          System.out.println("Unknown result (" + result + ") in file '" + filename + "'");
          break;
      }
    }
  }

  /**
   * Shows program usage.
   */
  static void DisplayHelp() {
    System.out.println("Usage: TiffReader [options] <file1> <file2> ... <fileN>");
    System.out.println("Options: -help displays help");
  }
}
