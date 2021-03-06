/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */



package thredds.server.opendap.servers;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import opendap.dap.BaseType;
import opendap.dap.DDS;
import opendap.dap.DSequence;
import opendap.dap.NoSuchVariableException;

/**
 * Holds a OPeNDAP Server <code>Sequence</code> value.
 *
 * @author ndp
 * @version $Revision: 15901 $
 * @see BaseType
 */

public abstract class SDSequence extends DSequence implements ServerMethods, RelOps {

  private boolean Synthesized;
  private boolean ReadMe;

  /**
   * Constructs a new <code>SDSequence</code>.
   */
  public SDSequence() {
    super();
    Synthesized = false;
    ReadMe = false;
  }

  /**
   * Constructs a new <code>SDSequence</code> with name <code>n</code>.
   *
   * @param n the name of the variable.
   */
  public SDSequence(String n) {
    super(n);
    Synthesized = false;
    ReadMe = false;
  }


  /**
   * Get the row vector for into which to read a row os data for this sequence.
   * When serving sequence data to clients the prefered method is to read one row
   * of the sequence at a time in to this vector, evaluate the constraint expression
   * clauses on the current data, and then send it to the client if it satisfies
   * the constraint. The NOT recomended way is to read the ENTIRE sequence into memory
   * prior to sending it (that would be most inefficient).
   *
   * @return The base (row 0) row vector for this sequence.
   */
  public List<BaseType> getRowVector() throws NoSuchVariableException {
    if (getRowCount() == 0) {
      ArrayList<BaseType> rv = new ArrayList<>();
      for (int i = 0; i < elementCount(false); i++) {
        rv.add(getVar(i));
      }
      addRow(rv);
    }
    return getRow(0);
  }


  /**
   * Write the variable's declaration in a C-style syntax. This
   * function is used to create textual representation of the Data
   * Descriptor Structure (DDS). See <em>The OPeNDAP User Manual</em> for
   * information about this structure.
   *
   * @param os The <code>PrintWriter</code> on which to print the
   *        declaration.
   * @param space Each line of the declaration will begin with the
   *        characters in this string. Usually used for leading spaces.
   * @param print_semi a boolean value indicating whether to print a
   *        semicolon at the end of the declaration.
   * @param constrained a boolean value indicating whether to print
   *        the declartion dependent on the projection information. <b>This
   *        is only used by Server side code.</b>
   * @see BaseType#printDecl(PrintWriter, String, boolean,boolean)
   */
  public void printDecl(PrintWriter os, String space, boolean print_semi, boolean constrained) {

    // BEWARE! Since printDecl()is (multiple) overloaded in BaseType
    // and all of the different signatures of printDecl() in BaseType
    // lead to one signature, we must be careful to override that
    // SAME signature here. That way all calls to printDecl() for
    // this object lead to this implementation.

    // Also, since printDecl()is (multiple) overloaded in BaseType and
    // all of the different signatures of printDecl() in BaseType lead to
    // the signature we are overriding here, we MUST call the printDecl
    // with the SAME signature THROUGH the super class reference
    // (assuming we want the super class functionality). If we do
    // otherwise, we will create an infinte call loop. OOPS!

    // If we are constrained, make sure some part of this thing is projected
    if (constrained && !isProject())
      return;

    super.printDecl(os, space, print_semi, constrained);


  }


  /**
   * Prints the value of the variable, with its declaration. This
   * function is primarily intended for debugging OPeNDAP applications and
   * text-based clients such as geturl.
   * <p/>
   * <h2>Important Note</h2>
   * This method overrides the BaseType method of the same name and
   * type signature and it significantly changes the behavior for all versions
   * of <code>printVal()</code> for this type:
   * <b><i> All the various versions of printVal() will only
   * print a value, or a value with declaration, if the variable is
   * in the projection.</i></b>
   * <br>
   * <br>
   * In other words, if a call to
   * <code>isProject()</code> for a particular variable returns
   * <code>true</code> then <code>printVal()</code> will print a value
   * (or a declaration and a value).
   * <br>
   * <br>
   * If <code>isProject()</code> for a particular variable returns
   * <code>false</code> then <code>printVal()</code> is basically a No-Op.
   * <br>
   * <br>
   *
   * @param os the <code>PrintWriter</code> on which to print the value.
   * @param space this value is passed to the <code>printDecl</code> method,
   *        and controls the leading spaces of the output.
   * @param print_decl_p a boolean value controlling whether the
   *        variable declaration is printed as well as the value.
   * @see BaseType#printVal(PrintWriter, String, boolean)
   * @see ServerMethods#isProject()
   */
  public void printVal(PrintWriter os, String space, boolean print_decl_p) {
    if (!isProject())
      return;

    if (print_decl_p) {
      printDecl(os, space, false, true);
      os.print(" = ");
    }
    os.print("{ ");

    try {
      boolean firstPass = true;
      for (BaseType bt : getRowVector()) {
        if (((ServerMethods) bt).isProject()) {
          if (!firstPass)
            os.print(", ");
          bt.printVal(os, "", false);
          firstPass = false;
        }

      }
    } catch (NoSuchVariableException e) {
      os.println("Very Bad Things Happened When I Tried To Print " + "A Row Of The Sequence: " + getEncodedName());
    }
    os.print(" }");

    if (print_decl_p)
      os.println(";");
  }

  // --------------- Projection Interface

  /**
   * Set the state of this variable's projection. <code>true</code> means
   * that this variable is part of the current projection as defined by
   * the current constraint expression, otherwise the current projection
   * for this variable should be <code>false</code>.
   *
   * @param state <code>true</code> if the variable is part of the current
   *        projection, <code>false</code> otherwise.
   * @param all If <code>true</code>, set the Project property of all the
   *        members (and their children, and so on).
   * @see CEEvaluator
   */
  @Override
  public void setProject(boolean state, boolean all) {
    setProjected(state);
    if (all)
      for (BaseType bt : varTemplate) {
        ServerMethods sm = (ServerMethods) bt;
        sm.setProject(state);
      }
  }

  // --------------- RelOps Interface

  /**
   * The RelOps interface defines how each type responds to relational
   * operators. Most (all?) types will not have sensible responses to all of
   * the relational operators (e.g. DSequence won't know how to match a regular
   * expression but DString will). For those operators that are nonsensical a
   * class should throw InvalidOperatorException.
   */

  public boolean equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
    throw new InvalidOperatorException("Equals (=) operator does not work with the type SDSequence!");
  }

  public boolean not_equal(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
    throw new InvalidOperatorException("Not Equals (!=) operator does not work with the type SDSequence!");
  }

  public boolean greater(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
    throw new InvalidOperatorException("Greater Than (>)operator does not work with the type SDSequence!");
  }

  public boolean greater_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
    throw new InvalidOperatorException("GreaterThan or equals (<=) operator does not work with the type SDSequence!");
  }

  public boolean less(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
    throw new InvalidOperatorException("LessThan (<) operator does not work with the type SDSequence!");
  }

  public boolean less_eql(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
    throw new InvalidOperatorException("LessThan oe equals (<=) operator does not work with the type SDSequence!");
  }

  public boolean regexp(BaseType bt) throws InvalidOperatorException, RegExpException, SBHException {
    throw new InvalidOperatorException("Regular Expression's don't work with the type SDSequence!");
  }

  // --------------- FileIO Interface

  /**
   * Set the Synthesized property.
   *
   * @param state If <code>true</code> then the variable is considered a
   *        synthetic variable and no part of OPeNDAP will ever try to read it from a
   *        file, otherwise if <code>false</code> the variable is considered a
   *        normal variable whose value should be read using the
   *        <code>read()</code> method. By default this property is false.
   * @see #isSynthesized()
   * @see #read(String, Object)
   */
  public void setSynthesized(boolean state) {
    Synthesized = state;
  }

  /**
   * Get the value of the Synthesized property.
   *
   * @return <code>true</code> if this is a synthetic variable,
   *         <code>false</code> otherwise.
   */
  public boolean isSynthesized() {
    return (Synthesized);
  }

  /**
   * Set the Read property. A normal variable is read using the
   * <code>read()</code> method. Once read the <em>Read</em> property is
   * <code>true</code>. Use this function to manually set the property
   * value. By default this property is false.
   *
   * @param state <code>true</code> if the variable has been read,
   *        <code>false</code> otherwise.
   * @see #isRead()
   * @see #read(String, Object)
   */
  public void setRead(boolean state) {
    ReadMe = state;

    // for (Enumeration e = varTemplate.elements();e.hasMoreElements();) {
    // ServerMethods sm = (ServerMethods)e.nextElement();
    // System.out.println("Setting Read Flag for "+((BaseType)sm).getName()+" to "+state);
    // sm.setRead(state);
    // }


  }

  /**
   * Set the Read property. A normal variable is read using the
   * <code>read()</code> method. Once read the <em>Read</em> property is
   * <code>true</code>. Use this function to manually set the property
   * value. By default this property is false.
   *
   * @param state <code>true</code> if the variable has been read,
   *        <code>false</code> otherwise.
   * @see #isRead()
   * @see #read(String, Object)
   */
  public void setAllReadFlags(boolean state) {
    ReadMe = state;
    for (BaseType bt : varTemplate) {
      ServerMethods sm = (ServerMethods) bt;
      // System.out.println("Setting Read Flag for "+((BaseType)sm).getName()+" to "+state);
      sm.setRead(state);
    }
  }

  /**
   * Get the value of the Read property.
   *
   * @return <code>true</code> if the variable has been read,
   *         <code>false</code> otherwise.
   * @see #read(String, Object)
   * @see #setRead(boolean)
   */
  public boolean isRead() {
    return (ReadMe);
  }

  /**
   * Read a value from the named dataset for this variable.
   *
   * @param datasetName String identifying the file or other data store
   *        from which to read a vaue for this variable.
   * @param specialO This <code>Object</code> is a goody that is used by Server implementations
   *        to deliver important, and as yet unknown, stuff to the read method. If you
   *        don't need it, make it a <code>null</code>.
   * @return <code>true</code> if more data remains to be read, otherwise
   *         <code>false</code>. This is an abtsract method that must be implemented
   *         as part of the installation/localization of a OPeNDAP server.
   * @throws IOException
   * @throws EOFException
   */
  public abstract boolean read(String datasetName, Object specialO)
      throws NoSuchVariableException, IOException, EOFException;


  /**
   * Server-side serialization for OPeNDAP variables (sub-classes of
   * <code>BaseType</code>). This does not send the entire class as the
   * Java <code>Serializable</code> interface does, rather it sends only
   * the binary data values. Other software is responsible for sending
   * variable type information (see <code>DDS</code>).
   * <p>
   * <p/>
   * Writes data to a <code>DataOutputStream</code>. This method is used
   * on the server side of the OPeNDAP client/server connection, and possibly
   * by GUI clients which need to download OPeNDAP data, manipulate it, and
   * then re-save it as a binary file.
   *
   * @param sink a <code>DataOutputStream</code> to write to.
   * @throws IOException thrown on any <code>OutputStream</code> exception.
   * @see BaseType
   * @see DDS
   * @see ServerDDS
   */
  public void serialize(String dataset, DataOutputStream sink, CEEvaluator ce, Object specialO)
      throws NoSuchVariableException, DAP2ServerSideException, IOException {


    boolean moreToRead = true;

    while (moreToRead) {

      if (!isRead()) {
        // ************* Pulled out the getLevel() check in order to support the "new"
        // and "improved" serialization of OPeNDAP sequences. 8/31/01 ndp
        // if(getLevel() != 0 ) // Read only the outermost level
        // return;
        moreToRead = read(dataset, specialO);
      }

      // System.out.println("Evaluating Clauses...");
      if (ce.evalClauses(specialO)) {
        writeMarker(sink, DSequence.START_OF_INSTANCE);
        for (BaseType bt : varTemplate) {
          ServerMethods sm = (ServerMethods) bt;
          if (sm.isProject()) {
            sm.serialize(dataset, sink, ce, specialO);
          }
        }
      }
      if (moreToRead)
        setAllReadFlags(false);
    }
    writeMarker(sink, DSequence.END_OF_SEQUENCE);
  }


  /**
   * Write the variable's declaration in XML. This
   * function is used to create the XML representation of the Data
   * Descriptor Structure (DDS). See <em>The OPeNDAP User Manual</em> for
   * information about this structure.
   *
   * @param pw The <code>PrintWriter</code> on which to print the
   *        declaration.
   * @param pad Each line of the declaration will begin with the
   *        characters in this string. Usually used for leading spaces.
   * @param constrained a boolean value indicating whether to print
   *        the declartion dependent on the projection information. <b>This
   *        is only used by Server side code.</b>
   * @see DDS
   */
  public void printXML(PrintWriter pw, String pad, boolean constrained) {

    // BEWARE! Since printXML()is (multiple) overloaded in BaseType
    // and all of the different signatures of printXML() in BaseType
    // lead to one signature, we must be careful to override that
    // SAME signature here. That way all calls to printDecl() for
    // this object lead to this implementation.

    // Also, since printXML()is (multiple) overloaded in BaseType
    // and all of the different signatures of printXML() in BaseType
    // lead to the signature we are overriding here, we MUST call
    // the printXML with the SAME signature THROUGH the super class
    // reference (assuming we want the super class functionality). If
    // we do otherwise, we will create an infinte call loop. OOPS!

    // System.out.println("SDSequence.printXML(pw,pad,"+constrained+") Project:"+Project);

    if (constrained && !isProject())
      return;

    super.printXML(pw, pad, constrained);
  }


}


