/**
 * <h1>Test1.java</h1> 
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
 * @since 21/5/2015
 *
 */
package Tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.easyinnova.main.TiffFile;

/**
 * The Class Tests.
 */
public class Tests {

  /**
   * Valid examples set.
   */
  @Test
  public void ValidExamples() {
    TiffFile tf;
    int result;

    tf = new TiffFile("tests\\Header\\Classic Intel.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(true, tf.validation.correct);

    tf = new TiffFile("tests\\Header\\Classic Motorola.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(true, tf.validation.correct);

    tf = new TiffFile("tests\\Colorspace\\F32.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(true, tf.validation.correct);

    tf = new TiffFile("tests\\IFD tree\\Recommended list.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(true, tf.validation.correct);

    tf = new TiffFile("tests\\IFD tree\\Old school E.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(true, tf.validation.correct);

    tf = new TiffFile("tests\\Organization\\Chunky multistrip.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(true, tf.validation.correct);

    tf = new TiffFile("tests\\Organization\\Chunky singlestrip.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(true, tf.validation.correct);

    tf = new TiffFile("tests\\Organization\\Chunky tile.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(true, tf.validation.correct);

    tf = new TiffFile("tests\\Organization\\Planar multistrip.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(true, tf.validation.correct);

    tf = new TiffFile("tests\\Organization\\Planar singlestrip.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(true, tf.validation.correct);

    tf = new TiffFile("tests\\Organization\\Planar tile.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(true, tf.validation.correct);
  }

  /**
   * Invalid examples set.
   */
  @Test
  public void InvalidExamples() {
    TiffFile tf;
    int result;

    tf = new TiffFile("tests\\Header\\Nonsense byteorder E.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(false, tf.validation.correct);

    tf = new TiffFile("tests\\Header\\Incorrect version E.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(false, tf.validation.correct);

    tf = new TiffFile("tests\\IFD Struct\\Insane tag count E.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(false, tf.validation.correct);

    tf = new TiffFile("tests\\IFD Struct\\Circular E.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(false, tf.validation.correct);

    tf = new TiffFile("tests\\IFD Struct\\Circular Short E.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(false, tf.validation.correct);

    tf = new TiffFile("tests\\IFD Struct\\Beyond EOF E.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(false, tf.validation.correct);

    tf = new TiffFile("tests\\IFD Struct\\Premature EOF E.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(false, tf.validation.correct);

    tf = new TiffFile("tests\\Colorspace\\I8 bad BitsPerSample count E.TIF");
    result = tf.read();
    assertEquals(0, result);
    assertEquals(false, tf.validation.correct);
  }
}

