/**
 * <h1>ValidationResult.java</h1> 
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
 * @since 18/5/2015
 *
 */
package com.easyinnova.main;

import java.util.ArrayList;

/**
 * The Class ValidationResult.
 */
public class ValidationResult {

  /** Errors List. */
  public ArrayList<ValidationError> errors;

  /** Warnings List. */
  public ArrayList<ValidationError> warnings;

  /** The Correct. */
  public boolean correct;

  /**
   * Instantiates a new validation result object.
   */
  public ValidationResult() {
    errors = new ArrayList<ValidationError>();
    warnings = new ArrayList<ValidationError>();
    correct = true;
  }

  /**
   * Adds the error.
   *
   * @param desc description
   * @param value the value
   */
  private void iaddError(String desc, String value) {
    ValidationError ve = new ValidationError(desc, value);
    errors.add(ve);
    correct = false;
  }

  /**
   * Adds the warning.
   *
   * @param desc description
   * @param value the value
   */
  private void iaddWarning(String desc, String value) {
    ValidationError ve = new ValidationError(desc, value);
    warnings.add(ve);
  }

  /**
   * Adds an error.
   *
   * @param desc Error description
   * @param value Value
   */
  public void addError(String desc, int value) {
    iaddError(desc, "" + value);
  }

  /**
   * Adds an error.
   *
   * @param desc Error description
   * @param value Value
   */
  public void addError(String desc, String value) {
    iaddError(desc, value);
  }

  /**
   * Adds an error.
   *
   * @param desc Error description
   */
  public void addError(String desc) {
    iaddError(desc, null);
  }

  /**
   * Adds an warning.
   *
   * @param desc Warning description
   */
  public void addWarning(String desc) {
    iaddWarning(desc, null);
  }

  /**
   * Adds a validation result to this.
   *
   * @param validation the validation to add
   */
  public void add(ValidationResult validation) {
    correct &= validation.correct;
    errors.addAll(validation.errors);
    warnings.addAll(validation.warnings);
  }

  /**
   * Prints the errors.
   */
  public void printErrors() {
    for (ValidationError ve : errors) {
      ve.print();
    }
  }

  /**
   * Prints the warnings.
   */
  public void printWarnings() {
    for (ValidationError ve : warnings) {
      ve.printWarning();
    }
  }
}
