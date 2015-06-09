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
 * @since 18/5/2015
 *
 */
package com.easyinnova.main;

import com.easyinnova.tiff.model.TiffDocument;
import com.easyinnova.tiff.model.TiffObject;
import com.easyinnova.tiff.model.types.IFD;
import com.easyinnova.tiff.reader.BaselineProfile;
import com.easyinnova.tiff.reader.TiffReader;
import com.easyinnova.tiff.writer.TiffWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The Main Class. <br>
 * Reads the files specified in the args, processes them and displays the results
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

    // Reads the parameters
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
        displayHelp();
        break;
      } else if (arg.startsWith("-")) {
        // unknown option
        args_error = true;
        break;
      } else {
        // File or directory to process
        File f = new File(arg);
        if (f.isFile())
          files.add(arg);
        else if (f.isDirectory()) {
          File[] listOfFiles = f.listFiles();
          for (int j = 0; j < listOfFiles.length; j++) {
            if (listOfFiles[j].isFile()) {
              files.add(listOfFiles[j].getPath());
            }
          }
        }
      }
    }
    if (args_error) {
      // Shows the program usage
      displayHelp();
    } else {
      // Process files
      for (final String filename : files) {
        TiffReader tr = new TiffReader();
        int result = tr.readFile(filename);
        reportResults(tr, result, output_file);

        TiffWriter tw = new TiffWriter();
        tw.SetModel(tr.getModel());
        try {
          // tw.write("out.tif");
        } catch (Exception ex) {
          System.out.println("Error writing TIFF");
        }
      }
    }
  }

  /**
   * Report the results of the reading process to the console.
   *
   * @param tiffReader the tiff reader
   * @param result the result
   * @param output_file the output_file
   */
  private static void reportResults(TiffReader tiffReader, int result, String output_file) {
    String filename = tiffReader.getFilename();
    TiffDocument to = tiffReader.getModel();
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
          System.out.println("File '" + filename + "' is not a TIFF");
          break;
        case -4:
          System.out.println("Header parsing exception in file '" + filename + "'");
          break;
        case -5:
          System.out.println("Incorrect magic number");
          break;
        case 0:
          if (tiffReader.getValidation().correct) {
            // The file is correct
            System.out.println("Everything ok in file '" + filename + "'");
            System.out.println("IFDs: " + to.getIfdCount());
            System.out.println("SubIFDs: " + to.getSubIfdCount());
            
            int index = 0;
            to.printMetadata();
            for (TiffObject o : to.getIfds()) {
              IFD ifd = (IFD) o;
              if (ifd != null) {
                BaselineProfile bp = new BaselineProfile();
                bp.validateIfd(ifd);
                String t = bp.getType().toString();
                // System.out.println("IFD " + index++ + " (" + t + ")");
                // ifd.printTags();
              }
            }
          } else {
            // The file is not correct
            System.out.println("Errors in file '" + filename + "'");
            if (to != null) {
              System.out.println("IFDs: " + to.getIfdCount());
              System.out.println("SubIFDs: " + to.getSubIfdCount());
              
              int index = 0;
              to.printMetadata();
              for (TiffObject o : to.getIfds()) {
                IFD ifd = (IFD) o;
                BaselineProfile bp = new BaselineProfile();
                bp.validateIfd(ifd);
                // System.out.println("IFD " + index++ + " (" + bp.getType().toString() + ")");
                // ifd.printTags();
              }
            }
            tiffReader.getValidation().printErrors();
          }
          tiffReader.getValidation().printWarnings();
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
  static void displayHelp() {
    System.out.println("Usage: TiffReader [options] <file1> <file2> ... <fileN>");
    System.out.println("Options: -help displays help");
  }
}
