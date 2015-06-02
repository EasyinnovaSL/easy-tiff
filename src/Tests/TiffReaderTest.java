/**
 * <h1>TiffReaderTest.java</h1> 
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
 * @since 2/6/2015
 *
 */
package Tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.easyinnova.tiff.model.IFD;
import com.easyinnova.tiff.model.TiffObject;
import com.easyinnova.tiff.model.types.TagValue;
import com.easyinnova.tiff.reader.TiffReader;

/**
 * The Class TiffReaderTest.
 */
public class TiffReaderTest {

  /**
   * Test.
   */
  @Test
  public void test() {
    TiffReader tr;
    TiffObject to;
    TagValue tv;
    IFD ifd;
    int result;

    tr = new TiffReader();

    result = tr.readFile("tests\\Small\\Grey_stripped.tif");
    assertEquals(0, result);
    assertEquals(true, tr.validation.correct);
    to = tr.getModel();
    assertEquals(1, to.getIfdCount());
    ifd = to.getIfd(0);

    tv = ifd.getMetadata("ImageWidth");
    assertEquals(1, tv.getCardinality());
    assertEquals(999, tv.getFirstNumericValue());

    tv = ifd.getMetadata("ImageLength");
    assertEquals(1, tv.getCardinality());
    assertEquals(662, tv.getFirstNumericValue());

    tv = ifd.getMetadata("BitsPerSample");
    assertEquals(1, tv.getCardinality());
    assertEquals(8, tv.getFirstNumericValue());

    tv = ifd.getMetadata("PhotometricInterpretation");
    assertEquals(1, tv.getCardinality());
    assertEquals(1, tv.getFirstNumericValue());

    tv = ifd.getMetadata("PlanarConfiguration");
    assertEquals(1, tv.getCardinality());
    assertEquals(1, tv.getFirstNumericValue());


    result = tr.readFile("tests\\Organization\\Planar tile.tif");
    assertEquals(0, result);
    assertEquals(true, tr.validation.correct);
    to = tr.getModel();
    assertEquals(1, to.getIfdCount());
    ifd = to.getIfd(0);

    tv = ifd.getMetadata("ImageWidth");
    assertEquals(1, tv.getCardinality());
    assertEquals(2000, tv.getFirstNumericValue());

    tv = ifd.getMetadata("ImageLength");
    assertEquals(1, tv.getCardinality());
    assertEquals(1500, tv.getFirstNumericValue());

    tv = ifd.getMetadata("BitsPerSample");
    assertEquals(3, tv.getCardinality());
    assertEquals(8, tv.getFirstNumericValue());

    tv = ifd.getMetadata("SamplesPerPixel");
    assertEquals(1, tv.getCardinality());
    assertEquals(3, tv.getFirstNumericValue());

    tv = ifd.getMetadata("Compression");
    assertEquals(1, tv.getCardinality());
    assertEquals(1, tv.getFirstNumericValue());

    tv = ifd.getMetadata("PhotometricInterpretation");
    assertEquals(1, tv.getCardinality());
    assertEquals(2, tv.getFirstNumericValue());

    tv = ifd.getMetadata("PlanarConfiguration");
    assertEquals(1, tv.getCardinality());
    assertEquals(2, tv.getFirstNumericValue());
  }
}

