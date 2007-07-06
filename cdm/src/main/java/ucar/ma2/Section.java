/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.ma2;

import ucar.nc2.Variable;

import java.util.*;

/**
 * A section of multidimensional array indices.
 * Represented as List<Range>.
 * Immutable if finish() was called.
 *
 * @author caron
 */

public class Section {
  private List<Range> list;
  private boolean immutable = false;

  /**
   * Create Section from a shape array, assumes 0 origin.
   *
   * @param shape array of lengths for each Range
   * @throws InvalidRangeException if shape[i] < 1.
   */
  public Section(int[] shape) throws InvalidRangeException {
    list = new ArrayList<Range>();
    for (int i = 0; i < shape.length; i++) {
      list.add(shape[i] > 0 ? new Range(0, shape[i] - 1) : Range.EMPTY);
    }
  }

  /**
   * Create Section from a shape and origin arrays.
   *
   * @param origin array of start for each Range
   * @param shape  array of lengths for each Range
   * @throws InvalidRangeException if origin < 0, shape < 1.
   */
  public Section(int[] origin, int[] shape) throws InvalidRangeException {
    list = new ArrayList<Range>();
    for (int i = 0; i < shape.length; i++) {
      list.add(shape[i] > 0 ? new Range(origin[i], origin[i] + shape[i] - 1) : Range.EMPTY);
    }
  }

  /**
   * Create Section from a List<Range>.
   * @param from the list of Range
   */
  public Section(List<Range> from) {
    list = new ArrayList<Range>(from);
  }

  /**
   * Create Section from a List<Range>.
   * @param from the list of Range
   * @param shape use this as default shape if any of the ranges are null.
   * @throws InvalidRangeException if shape and range list done match
   */
  public Section(List<Range> from, int[] shape) throws InvalidRangeException {
    list = new ArrayList<Range>(from);
    setDefaults(shape);
  }

  /**
   * Return a Section guarenteed to be non null, with no null Ranges, and within the bounds set by shape.
   * A section with no nulls is called "filled".
   * If s is already filled, return it, otherwise return a new Section, filled from the shape.
   *
   * @param s the original Section, may be null or not filled
   * @param shape use this as default shape if any of the ranges are null.
   * @throws InvalidRangeException if shape and s and shape rank dont match, or if s has invalid range compared to shape
   * @return a filled Section
   */
  static public Section fill(Section s, int[] shape) throws InvalidRangeException {
    // want all
    if (s == null)
      return new Section(shape);

    // if s is already filled, use it
    boolean ok = true;
    for (int i = 0; i < shape.length; i++) {
      Range r = s.getRange(i);
      ok &= (r != null);
    }
    if (ok) {
      String errs = s.checkInRange(shape);
      if (errs != null) throw new InvalidRangeException(errs);
      return s;
    }

    // fill in any nulls
    Section result = new Section(s.getRanges(), shape);
    String errs = result.checkInRange(shape);
    if (errs != null) throw new InvalidRangeException(errs);
    return result;
  }

  /**
   * Parse an index section String specification, return equivilent Section.
   * A null Range means "all" (i.e.":") indices in that dimension.
   * <p/>
   * The sectionSpec string uses fortran90 array section syntax, namely:
   * <pre>
   *   sectionSpec := dims
   *   dims := dim | dim, dims
   *   dim := ':' | slice | start ':' end | start ':' end ':' stride
   *   slice := INTEGER
   *   start := INTEGER
   *   stride := INTEGER
   *   end := INTEGER
   * <p/>
   * where nonterminals are in lower case, terminals are in upper case, literals are in single quotes.
   * <p/>
   * Meaning of index selector :
   *  ':' = all
   *  slice = hold index to that value
   *  start:end = all indices from start to end inclusive
   *  start:end:stride = all indices from start to end inclusive with given stride
   * <p/>
   * </pre>
   *
   * @param sectionSpec the token to parse, eg "(1:20,:,3,10:20:2)", parenthesis optional
   * @throws InvalidRangeException    when the Range is illegal
   * @throws IllegalArgumentException when sectionSpec is misformed
   */
  public Section(String sectionSpec) throws InvalidRangeException {

    list = new ArrayList<Range>();
    Range range;

    StringTokenizer stoke = new StringTokenizer(sectionSpec, "(),");
    while (stoke.hasMoreTokens()) {
      String s = stoke.nextToken().trim();
      if (s.equals(":"))
        range = null; // all

      else if (s.indexOf(':') < 0) { // just a number : slice
        try {
          int index = Integer.parseInt(s);
          range = new Range(index, index);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(" illegal selector: " + s + " part of <" + sectionSpec + ">");
        }

      } else {  // gotta be "start : end" or "start : end : stride"
        StringTokenizer stoke2 = new StringTokenizer(s, ":");
        String s1 = stoke2.nextToken();
        String s2 = stoke2.nextToken();
        String s3 = stoke2.hasMoreTokens() ? stoke2.nextToken() : null;
        try {
          int index1 = Integer.parseInt(s1);
          int index2 = Integer.parseInt(s2);
          int stride = (s3 != null) ? Integer.parseInt(s3) : 1;
          range = new Range(index1, index2, stride);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(" illegal selector: " + s + " part of <" + sectionSpec + ">");
        }
      }

      list.add(range);
    }

  }

  /**
   * Create a new Section by composing with a Section that is reletive to this Section.
   * @param want Section reletive to this one. If null, return this. If individual ranges are null, use corresponding Range in this.
   * @return new Section, composed
   * @throws InvalidRangeException if want.getRank() not equal to this.getRank(), or invalid component Range
   */
  public Section compose(Section want) throws InvalidRangeException {
    // all nulls
    if (want == null) return this; // LOOK maybe a copy ??

    if (want.getRank() != getRank())
        throw new InvalidRangeException("Invalid Section rank");

    // check individual nulls
    List<Range> results = new ArrayList<Range>(getRank());
    for (int j = 0; j < list.size(); j++) {
      Range base = list.get(j);
      Range r =  want.getRange(j);

      if (r == null)
        results.add( base);
      else
        results.add( base.compose(r));
    }

    return new Section( results);
  }

  /**
   * Convert List of Ranges to String Spec.
   * Inverse of new Section(String sectionSpec)
   *
   * @return index section String specification
   */
  public String toString() {
    StringBuffer sbuff = new StringBuffer();
    for (int i = 0; i < list.size(); i++) {
      Range r = list.get(i);
      if (i > 0) sbuff.append(",");
      if (r == null)
        sbuff.append(":");
      else
        sbuff.append(r.toString());
    }
    return sbuff.toString();
  }

  /**
   * No-arg Constructor
   */
  public Section() {
    list = new ArrayList<Range>();
  }

  // these make it mutable
  /**
   * Append a null Range to the Section - meaning "all"
   *
   * @return this
   */
  public Section appendRange() {
    if (immutable) throw new IllegalStateException("Cant modify");
    list.add(null);
    return this;
  }

  /**
   * Append a new Range(0,size-1) to the Section
   *
   * @param size add this Range
   * @return this
   * @throws InvalidRangeException if size < 1
   */
  public Section appendRange(int size) throws InvalidRangeException {
    if (immutable) throw new IllegalStateException("Cant modify");
    list.add(size > 1 ? new Range(0, size - 1) : Range.EMPTY);
    return this;
  }

  /**
   * Append a new Range(first, last) to the Section
   *
   * @param first starting index
   * @param last  last index, inclusive
   * @return this
   * @throws InvalidRangeException if last < first
   */
  public Section appendRange(int first, int last) throws InvalidRangeException {
    if (immutable) throw new IllegalStateException("Cant modify");
    list.add(new Range(first, last));
    return this;
  }

  /**
   * Append a new Range(first,last,stride) to the Section
   *
   * @param first  starting index
   * @param last   last index, inclusive
   * @param stride stride
   * @return this
   * @throws InvalidRangeException if last < first
   */
  public Section appendRange(int first, int last, int stride) throws InvalidRangeException {
    if (immutable) throw new IllegalStateException("Cant modify");
    list.add(new Range(first, last, stride));
    return this;
  }

  /**
   * Insert a range at the specified index in the list.
   *
   * @param index insert here in the list, existing ranges at or after this index get shifted by one
   * @param r  insert this Range
   * @return this
   * @throws IndexOutOfBoundsException if bad index
   */
  public Section insertRange(int index, Range r) {
    if (immutable) throw new IllegalStateException("Cant modify");
    list.add(index, r);
    return this;
  }

  /**
   * Replace a range at the specified index in the list.
   *
   * @param index replace here in the list.
   * @param r  use this Range
   * @return this
   * @throws IndexOutOfBoundsException if bad index
   */
  public Section replaceRange(int index, Range r) {
    if (immutable) throw new IllegalStateException("Cant modify");
    list.set(index, r);
    return this;
  }

  /**
   * If any of the ranges are null, which means "all", set the Range from the
   * corresponding length in shape[].
   *
   * @param shape default length for each Range; must have matching rank.
   * @throws InvalidRangeException if rank is wrong, or shape[i] < 1
   */
  public void setDefaults(int[] shape) throws InvalidRangeException {
    if (immutable) throw new IllegalStateException("Cant modify");
    if (shape.length != list.size())
      throw new InvalidRangeException(" shape[] must have same rank as Section");

    // check that any individual Range is null
    for (int i = 0; i < shape.length; i++) {
      Range r = list.get(i);
      if (r == null) {
        list.set(i, new Range(0, shape[i] - 1));
      }
    }
  }

  /**
   * Makes the object immutable, so can be safely shared
   *
   * @return this Section
   */
  public Section finish() {
    immutable = true;
    list = Collections.unmodifiableList(list);
    return this;
  }

  // end mutable methods

  public boolean isImmutable() { return immutable; }

  /**
   * Get shape array using the Range.length() values.
   *
   * @return int[] shape
   */
  public int[] getShape() {
    int[] result = new int[list.size()];
    for (int i = 0; i < list.size(); i++) {
      result[i] = list.get(i).length();
    }
    return result;
  }

  /**
   * Get origin array using the Range.first() values.
   *
   * @return int[] origin
   */
  public int[] getOrigin() {
    int[] result = new int[list.size()];
    for (int i = 0; i < list.size(); i++) {
      result[i] = list.get(i).first();
    }
    return result;
  }

  /**
   * Get origin of the ith Range
   *
   * @param i index of Range
   * @return origin of ith Range
   */
  public int getOrigin(int i) {
    return list.get(i).first();
  }

  /**
   * Get length of the ith Range
   *
   * @param i index of Range
   * @return length of ith Range
   */
  public int getShape(int i) {
    return list.get(i).length();
  }

  /**
   * Get stride of the ith Range
   *
   * @param i index of Range
   * @return stride of ith Range
   */
  public int getStride(int i) {
    return list.get(i).stride();
  }

  /**
   * Get rank - number of Ranges.
   *
   * @return rank
   */
  public int getRank() {
    return list.size();
  }

  /**
   * Compute total number of elements represented by the section.
   *
   * @return total number of elements
   */
  public long computeSize() {
    return Index.computeSize(getShape());
  }

  /**
   * Get the list of Ranges.
   *
   * @return the List<Range>
   */
  public List<Range> getRanges() {
    return immutable ? list : new ArrayList<Range>(list);
  }

  /**
   * Get the ith Range
   *
   * @param i index into the list of Ranges
   * @return ith Range
   */
  public Range getRange(int i) {
    return list.get(i);
  }

  /**
   * Check if this Section is legal for the given shape.
   *
   * @param shape range must fit within this shape, rank must match.
   * @return error message if illegal, null if all ok
   */
  public String checkInRange(int shape[]) {
    if (list.size() != shape.length)
      return "Number of ranges in section (" + list.size() + ") must be = " + shape.length;

    for (int i = 0; i < list.size(); i++) {
      Range r = list.get(i);
      if (r == null) continue;
      if (r.last() >= shape[i])
        return "Illegal Range for dimension " + i + ": last requested " + r.last() + " > max " + (shape[i] - 1);
    }

    return null;
  }

}