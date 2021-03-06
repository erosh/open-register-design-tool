/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.systemverilog;

import java.util.HashMap;
import java.util.List;

import ordt.extract.Ordt;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.output.FieldProperties;
import ordt.output.RhsReference;
import ordt.output.SignalProperties;
import ordt.output.systemverilog.SystemVerilogDefinedSignals.DefSignalType;
import ordt.output.systemverilog.common.SystemVerilogModule;
import ordt.output.systemverilog.common.SystemVerilogSignal;
import ordt.output.systemverilog.io.SystemVerilogIOSignalList;
import ordt.output.FieldProperties.RhsRefType;
import ordt.output.RegProperties;
import ordt.parameters.ExtParameters;
import ordt.parameters.Utils;

/** derived class for logic module 
 *  note: this class is tightly coupled with builder - uses several builder methods
 *  
 * signal resolution flow:
 *    - RdlModel extractor flags all lhs instance ref assignments w/o a property deref as new signalAssign property
 *          extractor also pre-computes list of user-defined signals and exposes isUserDefinedSignal method
 *    - in generate, when signal encountered, addSignal is called in SVBuilder
 *          at this point signalProperties is updated and signal assignment has been extracted via setAssignExpression in extractProperties
 *          signalProperties is added to module-specific userDefinedSignals using sig_* prefix string name as key 
 *          if an assignment is found, output port strings are created
 *             for each rhs reference, resolveAsSignalOrField is called, addRhsSignal is called
 *                resolveAsSignalOrField - if in userDefinedSignals, converts to name to sig_* form and marks the sig as a rhs reference << FIXME problem if sig assign order is wrong
 *                addRhsSignal - adds signal to list of rhs signals
 *          if no assignment an input port is generated
 *    - after generate, createSignalAssigns is called, which resolves all rhs signals in assign rhs
 *        isRhsReference is called to determine if a signal is unused
 *    - resolveRhsExpression is called by WriteStatements routines << FIXME problem if lhs property reference is prior to signal def

 */
public class SystemVerilogLogicModule extends SystemVerilogModule {
	private HashMap<String, SignalProperties> userDefinedSignals = new HashMap<String, SignalProperties>();  // all user defined signals in current addrmap module / only valid after generate
	private HashMap<String, RhsReferenceInfo> rhsSignals = new HashMap<String, RhsReferenceInfo>();  // all right hand side assignment references in module (used to create usable error messages)

	private FieldProperties fieldProperties;
	private RegProperties regProperties;
	protected SystemVerilogBuilder builder;  // builder creating this module
	
	public SystemVerilogLogicModule(SystemVerilogBuilder builder, int insideLocs, String defaultClkName) {
		super(builder, insideLocs, defaultClkName, builder.getDefaultReset());
		this.builder = builder;  // save reference to calling builder
	}
	
	// -------------- 

	/** set active instances to be used by logic generation methods */
	public void setActiveInstances(RegProperties regProperties, FieldProperties fieldProperties) {
		this.fieldProperties = fieldProperties;
		this.regProperties = regProperties;
	}

	/** add a new scalar IO signal to the hw list based on sigType */
	public void addHwScalar(DefSignalType sigType) {
		this.addHwVector(sigType, 0, 1);
	}

	/** add a new vector IO signal  to the hw list based on sigType */
	public void addHwVector(DefSignalType sigType, int lowIndex, int size) {
		SystemVerilogIOSignalList sigList = ioHash.get(SystemVerilogBuilder.HW);  // get the hw siglist
		if (sigList == null) return;
		sigList.addVector(sigType, lowIndex, size); 
	}
	
	// -------------- field logic generation methods
	
	/** generate verilog statements to write field flops */  
	void genFieldWriteStmts() {
		   // get field-specific verilog signal names
		   String hwToLogicDataName = fieldProperties.getFullSignalName(DefSignalType.H2L_DATA);  // hwBaseName + "_w" 
		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  //"reg_" + hwBaseName;
		   
		   // if sw can write, qualify by sw we
		   if (fieldProperties.swChangesValue()) {  // if sw can write or rclr/rset
			   genFieldRegWriteStmts();    // create statements to define field registers and resets
			   genFieldNextWriteStmts();   // create statements to set value of next based on field settings
		   }
		   
		   // sw cant write so ignore sw we
		   else {
			   // hw only can write, so add write interface  
			   if (fieldProperties.hwChangesValue()) {
				   // if hw uses we or is interrupt/counter we'll need to build next
				   if (fieldProperties.hasHwWriteControl() || fieldProperties.isInterrupt() || fieldProperties.isCounter()) { 
					   genFieldRegWriteStmts();    // create statements to define field registers and resets
					   genFieldNextWriteStmts();   // create statements to set value of next based on field settings
				   }
				   // else no hw we/control sigs, so hw data value is just passed in (no register)
				   else {
					   addVectorReg(fieldRegisterName, 0, fieldProperties.getFieldWidth());  // add field register to define list
					   addHwVector(DefSignalType.H2L_DATA, 0, fieldProperties.getFieldWidth());  // add write data input
					   addCombinAssign(regProperties.getBaseName(), fieldRegisterName + " =  " + hwToLogicDataName + ";");
				   }
			   }
			   // nothing writable so assign to a constant 
			   else {
				   //System.out.println("SystemVerilogBuilder genFieldWriteStmts constant field, id=" + fieldProperties.getPrefixedId() + 
				   //	   ", writeable=" + fieldProperties.isHwWriteable() + ", intr=" + fieldProperties.isInterrupt() + ", changes val=" + fieldProperties.hwChangesValue());
				   if (fieldProperties.getReset() != null ) {
					   addVectorWire(fieldRegisterName, 0, fieldProperties.getFieldWidth());  // add field to wire define list
					   RegNumber resetValue = new RegNumber(fieldProperties.getReset());  // output reset in verilog format
					   resetValue.setNumFormat(RegNumber.NumFormat.Verilog);
					   addWireAssign(fieldRegisterName + " = " + resetValue + ";");
				   }
				   else Ordt.errorMessage("invalid field constant - no reset value for non-writable field " + fieldProperties.getInstancePath());
			   }
		   }
	}

	/** create statements to define field registers and resets */
	private void genFieldRegWriteStmts() {
		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  //"reg_" + hwBaseName;
		   String fieldRegisterNextName = fieldProperties.getFullSignalName(DefSignalType.FIELD_NEXT);  //"reg_" + hwBaseName + "_next";
		   addVectorReg(fieldRegisterName, 0, fieldProperties.getFieldWidth());  // add field registers to define list
		   addVectorReg(fieldRegisterNextName, 0, fieldProperties.getFieldWidth());  // we'll be using next value since complex assign
		   // generate flop reset stmts
		   if (fieldProperties.getReset() != null ) {
			   String resetSignalName = builder.getDefaultReset();
			   boolean resetSignalActiveLow = builder.getDefaultResetActiveLow();
			   if (builder.getLogicReset() != null) {
				   resetSignalName = builder.getLogicReset();
				   resetSignalActiveLow = builder.getLogicResetActiveLow();
			   }
			   else if (fieldProperties.hasRef(RhsRefType.RESET_SIGNAL)) {
				   resetSignalActiveLow = false;  // user defined resets are active high 
				   resetSignalName = resolveRhsExpression(RhsRefType.RESET_SIGNAL);
			   }
			   addReset(resetSignalName, resetSignalActiveLow);
			   RegNumber resetValue = new RegNumber(fieldProperties.getReset());  // output reset in verilog format
			   resetValue.setNumFormat(RegNumber.NumFormat.Verilog);
			   addResetAssign(regProperties.getBaseName(), resetSignalName, fieldRegisterName + " <= #1 " + resetValue + ";");  // ff reset assigns			   
		   }
		   else if (!ExtParameters.sysVerSuppressNoResetWarnings()) Ordt.warnMessage("field " + fieldProperties.getInstancePath() + " has no reset defined");
		   
		   addRegAssign(regProperties.getBaseName(),  fieldRegisterName + " <= #1  " + fieldRegisterNextName + ";");  // assign next to flop
	}

	/** create statements to set value of next based on field settings */ 
	private  void genFieldNextWriteStmts() {
		   // get field-specific verilog signal names
		   String hwToLogicDataName = fieldProperties.getFullSignalName(DefSignalType.H2L_DATA);  // hwBaseName + "_w" 
		   
		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  //"reg_" + hwBaseName;  
		   String fieldRegisterNextName = fieldProperties.getFullSignalName(DefSignalType.FIELD_NEXT);  //"reg_" + hwBaseName + "_next";
		   
		   // if hw is writable add the write data input
		   if (fieldProperties.isHwWriteable()) addHwVector(DefSignalType.H2L_DATA, 0, fieldProperties.getFieldWidth());

		   // first set default next value, if hw write w/o enable use hw data
		   if (fieldProperties.isHwWriteable()  && !fieldProperties.hasHwWriteControl() && !fieldProperties.isCounter()) 
			   addCombinAssign(regProperties.getBaseName(), fieldRegisterNextName + " = " + hwToLogicDataName + ";");
		   // otherwise if a singlepulse field then default to zero value
		   else if (fieldProperties.isSinglePulse())
			   addCombinAssign(regProperties.getBaseName(), fieldRegisterNextName + " = 0;");
		   // otherwise hold current registered data
		   else 
			   addCombinAssign(regProperties.getBaseName(), fieldRegisterNextName + " = " + fieldRegisterName + ";");
		   	   
		   // set field precedence
		   boolean hwPrecedence = fieldProperties.hasHwPrecedence();
		   boolean swPrecedence = !(fieldProperties.hasHwPrecedence());
		   
		   // if a counter (special case, hw has precedence in counters by default)  
		   if (fieldProperties.isCounter()) {
			   genCounterWriteStmts(hwPrecedence);
		   }
		   
		   // if hw uses interrupt  
		   else if (fieldProperties.isInterrupt()) {  
			   genInterruptWriteStmts(hwPrecedence);
		   }
		   
		   // if an explicit next assignment 
		   if (fieldProperties.hasRef(RhsRefType.NEXT)) {
			   String refName = resolveRhsExpression(RhsRefType.NEXT);
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, fieldRegisterNextName + " = " + refName + ";");	
		   }
		   
		   // if hw uses we
		   if (fieldProperties.hasWriteEnableH()) { 
			   String hwToLogicWeName = generateInputOrAssign(DefSignalType.H2L_WE, RhsRefType.WE, 1, false, hwPrecedence); 
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicWeName + ") " + fieldRegisterNextName + " = " + hwToLogicDataName + ";");				   
		   }
		   // if hw uses wel
		   else if (fieldProperties.hasWriteEnableL()) {  
			   String hwToLogicWelName = generateInputOrAssign(DefSignalType.H2L_WEL, RhsRefType.WE, 1, false, hwPrecedence); 
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (~" + hwToLogicWelName + ") " + fieldRegisterNextName + " = " + hwToLogicDataName + ";");				   
		   }
		   
		   // if hw has hw set
		   if (fieldProperties.hasHwSet()) { 
			   String hwToLogicHwSetName = generateInputOrAssign(DefSignalType.H2L_HWSET, RhsRefType.HW_SET, 1, false, hwPrecedence);
			   RegNumber constVal = new RegNumber(1);
			   constVal.setVectorLen(fieldProperties.getFieldWidth());
			   constVal.lshift(fieldProperties.getFieldWidth());
			   constVal.subtract(1);
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicHwSetName + ") " + 
			      fieldRegisterNextName + " = " + constVal.toFormat(NumBase.Hex, NumFormat.Verilog) + ";");				   
		   }
		   
		   // if hw has hw clr
		   if (fieldProperties.hasHwClr()) { 
			   String hwToLogicHwClrName = generateInputOrAssign(DefSignalType.H2L_HWCLR, RhsRefType.HW_CLR, 1, false, hwPrecedence);
			   RegNumber constVal = new RegNumber(0);
			   constVal.setVectorLen(fieldProperties.getFieldWidth());
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicHwClrName + ") " + 
			      fieldRegisterNextName + " = " + constVal.toFormat(NumBase.Hex, NumFormat.Verilog) + ";");				   
		   }

		   // add sw statements
		   String nameOverride = fieldProperties.isCounter() ? fieldProperties.getFullSignalName(DefSignalType.CNTR_NEXT) : null;
		   genSwFieldNextWriteStmts(nameOverride, swPrecedence);    // create statements to set value of next based on sw field settings
	}

	/** if a fieldProperty signal of specified type has a rhs assignment, generate appropriate defines/assign stmts for 
	 *   an internal signal, else create an input signal.  return the resolved assign/input name or the resolved
	 *   rhs expression if createDefaultSignal is false
	 * 
	 * @param sigType - type of signal being assigned or made an input
	 * @param rType - rhs reference type
	 * @param sigWidth - width of the input/internal signal created
	 * @param createDefaultSignal - if false, do not create internal signal/assigns, just output the resolved rhs expression
	 * @param hiPrecedence - if true, combi assign of default signal will be given high priority (ignored if createDefaultSignal is false)
	 */
	private String generateInputOrAssign(DefSignalType sigType, RhsRefType rType, int sigWidth, boolean createDefaultSignal, boolean hiPrecedence) {
		String defaultName = fieldProperties.getFullSignalName(sigType);
		   // if an assigned reference signal use it rather than default 
		   if (fieldProperties.hasRef(rType)) { 
			   String refName = resolveRhsExpression(rType);
			   if (createDefaultSignal) {
				   addVectorReg(defaultName, 0, sigWidth); 
				   addPrecCombinAssign(regProperties.getBaseName(), hiPrecedence, defaultName + " = " + refName + ";");
			   }
			   else return refName;  // otherwise use new name in subsequent logic
		   }
		   // otherwise create an input
		   else 
			   addHwVector(sigType, 0, sigWidth); 
		return defaultName;
	}

	/** resolve rhs rtl expression, save in rhs signal list, and mark for post-gen name resolution.
	 *  (this method is for resolving refs defined in fieldProperties, not signalProperties) */
	private String resolveRhsExpression(RhsRefType rType) {
		String retExpression = fieldProperties.getRefRtlExpression(rType, false);   // create signal name from rhs
		retExpression = resolveAsSignalOrField(retExpression);
		addRhsSignal(retExpression, builder.getInstancePath(), fieldProperties.getRef(rType).getRawReference() );
		return retExpression;
	}

	/** create statements to set value of next based on field settings for sw interface.
	 *  save sw write statements in alternate list so they can be moved depending on hw/sw precedence of field
	 * @param nextNameOverride - if non null, this will be the signal modified by sw write stmts
	 *  */ 
	void genSwFieldNextWriteStmts(String nextNameOverride, boolean swPrecedence) {  
		   // get base register and field names
		   String regBaseName = regProperties.getBaseName();
		   String fieldRegisterNextName = fieldProperties.getFullSignalName(DefSignalType.FIELD_NEXT);  //"reg_" + hwBaseName + "_next";
		   String fieldArrayString = fieldProperties.getFieldArrayString();  
		   
		   // override names if an aliased register
		   if (regProperties.isAlias()) {
			   regBaseName = regProperties.getAliasBaseName();
			   fieldRegisterNextName = FieldProperties.getFieldRegisterNextName(regBaseName + "_" + fieldProperties.getPrefixedId(), true);  
		   }

		   // override the assigned name if specified
		   if (nextNameOverride != null) fieldRegisterNextName = nextNameOverride;
		   
		   String decodeToLogicDataName = regProperties.getFullSignalName(DefSignalType.D2L_DATA);  // write data from decoder
		   String decodeToLogicEnableName = regProperties.getFullSignalName(DefSignalType.D2L_ENABLE);  // write enable from decoder
		   String decodeToLogicWeName = regProperties.getFullSignalName(DefSignalType.D2L_WE);  // write enable from decoder
		   String decodeToLogicReName = regProperties.getFullSignalName(DefSignalType.D2L_RE);  // read enable from decoder
		   
		   // build an enable expression if swwe/swwel are used
		   String swWeStr = "";
		   if (fieldProperties.hasSwWriteEnableH()) { 
			   String hwToLogicSwWeName = generateInputOrAssign(DefSignalType.H2L_SWWE, RhsRefType.SW_WE, 1, false, swPrecedence); 
			   swWeStr = " & " + hwToLogicSwWeName;				   
		   }
		   else if (fieldProperties.hasSwWriteEnableL()) {  
			   String hwToLogicSwWelName = generateInputOrAssign(DefSignalType.H2L_SWWEL, RhsRefType.SW_WE, 1, false, swPrecedence); 
			   swWeStr = " & ~" + hwToLogicSwWelName;				   
		   }
		   
		   // build write data string qualified by enables
		   String woEnabledDataString = (!SystemVerilogBuilder.hasWriteEnables())? decodeToLogicDataName + fieldArrayString :
			   "(" + decodeToLogicDataName + fieldArrayString + " & " + decodeToLogicEnableName + fieldArrayString + ")";
		   
		   // if a sw write one to clr/set
		   if (fieldProperties.isWoset()) {
			   addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicWeName + swWeStr + ") " + fieldRegisterNextName + " = (" + 
					   fieldRegisterNextName + " | " + woEnabledDataString + ");");				   
		   }
		   else if (fieldProperties.isWoclr()) {
			   addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicWeName + swWeStr + ") " + fieldRegisterNextName + " = (" + 
					   fieldRegisterNextName + " & ~" + woEnabledDataString + ");");				   
		   }
		   // if a sw write is alowed 
		   else if (fieldProperties.isSwWriteable()) {		   
			   // build write data string qualified by enables
			   String fullEnabledDataString = (!SystemVerilogBuilder.hasWriteEnables())? decodeToLogicDataName + fieldArrayString :
				   "(" + woEnabledDataString + " | (" + fieldRegisterNextName + " & ~" + decodeToLogicEnableName + fieldArrayString + "))";
			   addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicWeName + swWeStr + ") " + fieldRegisterNextName + " = " + fullEnabledDataString + ";");				   
		   }
			   			   
		   // if a sw read set
		   if (fieldProperties.isRset()) {
			   addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicReName + swWeStr + ") " + fieldRegisterNextName + " = " + 
		           fieldProperties.getFieldWidth() + "'b" + Utils.repeat('1', fieldProperties.getFieldWidth()) + ";");
		   }
		   // if sw rclr 
		   else if (fieldProperties.isRclr()) {
			   addPrecCombinAssign(regBaseName, swPrecedence, "if (" + decodeToLogicReName + swWeStr + ") " + fieldRegisterNextName + " = " + 
		           fieldProperties.getFieldWidth() + "'b0;");
		   }
		   
		   // if has sw access output
		   if (fieldProperties.hasSwAcc()) {
			   String logicToHwSwAccName = fieldProperties.getFullSignalName(DefSignalType.L2H_SWACC);
			   addHwScalar(DefSignalType.L2H_SWACC);   // add sw access output
			   addScalarReg(logicToHwSwAccName);  
			   addPrecCombinAssign(regBaseName, swPrecedence, logicToHwSwAccName + 
					   " = " + decodeToLogicReName + " | " + decodeToLogicWeName + ";");
		   }
		   // if has sw modify output
		   if (fieldProperties.hasSwMod()) {
			   String logicToHwSwModName = fieldProperties.getFullSignalName(DefSignalType.L2H_SWMOD);
			   addHwScalar(DefSignalType.L2H_SWMOD);   // add sw access output
			   addScalarReg(logicToHwSwModName); 
			   String readMod = (fieldProperties.isRclr() || fieldProperties.isRset())? "(" + decodeToLogicReName + " | " + decodeToLogicWeName + ")" : decodeToLogicWeName;
			   addPrecCombinAssign(regBaseName, swPrecedence, logicToHwSwModName + " = " + readMod + swWeStr + ";");
		   }
	}

	/** write interrupt field verilog 
	 * @param hwPrecedence */   
	private void genInterruptWriteStmts(boolean hwPrecedence) {
		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  //"reg_" + hwBaseName;  
		   String fieldRegisterNextName = fieldProperties.getFullSignalName(DefSignalType.FIELD_NEXT);  //"reg_" + hwBaseName + "_next";
		   int fieldWidth = fieldProperties.getFieldWidth();
		   
		   // if register is not already interrupt, then create signal assigns and mark for output creation in finishRegister
		   String intrOutput = regProperties.getFullSignalName(DefSignalType.L2H_INTR);
		   String intrClear = regProperties.getFullSignalName(DefSignalType.INTR_CLEAR);  // interrupt clear detect signal
		   if (!regProperties.hasInterruptOutputDefined()) {
			   regProperties.setHasInterruptOutputDefined(true);
			   addScalarReg(intrOutput);
		       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, intrOutput + " = 1'b0;");  // default to intr off
		       // if pulse on clear is set then add clear detect signal
		       if (ExtParameters.sysVerPulseIntrOnClear()) {
				   addScalarReg(intrClear);
			       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, intrClear + " = 1'b0;");  // default to clear off
		       }
		   }
		   
		   // if a halt field and register is not already halt, then create signal assigns and mark for output creation in finishRegister
		   String haltOutput = regProperties.getFullSignalName(DefSignalType.L2H_HALT);
		   if (fieldProperties.isHalt() && !regProperties.hasHaltOutputDefined()) {
			   regProperties.setHasHaltOutputDefined(true);
			   addScalarReg(haltOutput);
		       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, haltOutput + " = 1'b0;");  // default to halt off
		   }

		   // if a input intr reference then assign else create an input with width of field  
		   String hwToLogicIntrName = fieldProperties.getFullSignalName(DefSignalType.H2L_INTR);  // hwBaseName + "_intr" 	   
		   if (fieldProperties.hasRef(RhsRefType.INTR)) {  //  intr assign not allowed by rdl1.0 spec, but allow for addl options vs next
			   //System.out.println("SystemVerilogBuilder genInterruptWriteStmts: " + fieldProperties.getInstancePath() + " has an intr reference, raw=" + fieldProperties.getIntrRef().getRawReference());
			   addVectorReg(hwToLogicIntrName, 0, fieldProperties.getFieldWidth());
			   String refName = resolveRhsExpression(RhsRefType.INTR);
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, hwToLogicIntrName  +  " = " + refName + ";");
		   }
		   // otherwise, if next property isn't set then add an intr input
		   else if (!fieldProperties.hasRef(RhsRefType.NEXT)) {
			   addHwVector(DefSignalType.H2L_INTR, 0, fieldProperties.getFieldWidth());   // add hw interrupt input
			   addVectorWire(hwToLogicIntrName, 0, fieldProperties.getFieldWidth());			   
		   }

		   // if next is assigned then skip all the intr-specific next generation
		   String intrOutputModifier = "";
		   if (!fieldProperties.hasRef(RhsRefType.NEXT)) {
				   
			   // create mask/enable output and bit modifier if specified
			   String intrBitModifier = "";
			   if (fieldProperties.hasRef(RhsRefType.INTR_ENABLE)) {
				   String refName = resolveRhsExpression(RhsRefType.INTR_ENABLE);
				   if (fieldProperties.isMaskIntrBits()) intrBitModifier = " & " + refName;
				   else intrOutputModifier = " & " + refName;
			   }
			   else if (fieldProperties.hasRef(RhsRefType.INTR_MASK)) {
				   String refName = resolveRhsExpression(RhsRefType.INTR_MASK);
				   if (fieldProperties.isMaskIntrBits()) intrBitModifier = " & ~" + refName;
				   else intrOutputModifier = " & ~" + refName;
			   }
			   
			   // create intr detect based on intrType (level, posedge, negedge, bothedge)
			   String detectStr = hwToLogicIntrName;  // default to LEVEL
			   String prevIntrName = fieldProperties.getFullSignalName(DefSignalType.PREVINTR);  // hwBaseName + "_previntr" 	   
			   // if not LEVEL, need to store previous intr value
			   if (fieldProperties.getIntrType() != FieldProperties.IntrType.LEVEL) {
				   addVectorReg(prevIntrName, 0, fieldProperties.getFieldWidth());
				   addRegAssign(regProperties.getBaseName(), prevIntrName  +  " <= #1 " + hwToLogicIntrName + ";");
				   // if posedge detect
				   if (fieldProperties.getIntrType() == FieldProperties.IntrType.POSEDGE) 
					   detectStr = "(" + hwToLogicIntrName + " & ~" + prevIntrName + ")";
				   else if (fieldProperties.getIntrType() == FieldProperties.IntrType.NEGEDGE) 
					   detectStr = "(" + prevIntrName + " & ~" + hwToLogicIntrName + ")";
				   else // BOTHEDGE detect  
					   detectStr = "(" + hwToLogicIntrName + " ^ " + prevIntrName + ")";
		   }
		   
		   // assign field based on detect and intrStickyype (nonsticky, sticky, stickybit)  
		   if (fieldProperties.getIntrStickyType() == FieldProperties.IntrStickyType.NONSTICKY) 
		      addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, fieldRegisterNextName + " = " + detectStr + intrBitModifier + ";");
		   else if (fieldProperties.getIntrStickyType() == FieldProperties.IntrStickyType.STICKY) 
			  addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + detectStr + " != " + fieldWidth + "'b0) " +
		                         fieldRegisterNextName +  " = " + detectStr + intrBitModifier + ";");	
		   else // STICKYBIT default 
			  addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, fieldRegisterNextName + " = (" + detectStr + " | " +
		                         fieldRegisterName + ")" + intrBitModifier + ";");
		   }

		   // if an enable/mask then gate interrupt output with this signal
		   String orStr = " | (";  String endStr = ");";
		   if (fieldWidth > 1) {
			   orStr = " | ( | (";  endStr = "));";  // use or reduction
		   }
	       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, intrOutput + " = " + intrOutput  + orStr + fieldRegisterName + intrOutputModifier + endStr);
	       // if pulse on clear is set then create delayed intr value and add to clear detect signal
	       if (ExtParameters.sysVerPulseIntrOnClear()) {
			   String intrDlyName = fieldProperties.getFullSignalName(DefSignalType.INTR_DLY); 	   
			   addVectorReg(intrDlyName, 0, fieldProperties.getFieldWidth());
			   addRegAssign(regProperties.getBaseName(), intrDlyName  +  " <= #1 " + fieldRegisterName + ";");
		       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, intrClear + " = " + intrClear  + orStr + intrDlyName + " & ~" + fieldRegisterName + endStr);  // negedge detect
	       }

		   // if an enable/mask then gate halt output with this signal
		   if (fieldProperties.hasRef(RhsRefType.HALT_ENABLE)) {
			   String refName = resolveRhsExpression(RhsRefType.HALT_ENABLE);
		       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, haltOutput + " = " + haltOutput  + orStr + fieldRegisterName + " & " + refName + endStr);
		   }
		   else if (fieldProperties.hasRef(RhsRefType.HALT_MASK)) {
			   String refName = resolveRhsExpression(RhsRefType.HALT_MASK);
		       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, haltOutput + " = " + haltOutput  + orStr + fieldRegisterName + " & ~" + refName + endStr);
		   }
		   else if (fieldProperties.isHalt())
		       addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, haltOutput + " = " + haltOutput + orStr + fieldRegisterName + endStr);
	}
	
	/** write counter field verilog 
	 * @param hwPrecedence */   
	private void genCounterWriteStmts(boolean hwPrecedence) {
		   // get field-specific verilog signal names
		   String hwToLogicIncrName = fieldProperties.getFullSignalName(DefSignalType.H2L_INCR);  // hwBaseName + "_incr" 
		   String logicToHwOverflowName = fieldProperties.getFullSignalName(DefSignalType.L2H_OVERFLOW);  // hwBaseName + "_overflow" 
		   
		   String hwToLogicDecrName = fieldProperties.getFullSignalName(DefSignalType.H2L_DECR);  // hwBaseName + "_decr" 
		   String logicToHwUnderflowName = fieldProperties.getFullSignalName(DefSignalType.L2H_UNDERFLOW);  // hwBaseName + "_underflow" 
		   String nextCountName = fieldProperties.getFullSignalName(DefSignalType.CNTR_NEXT);   
		   
		   String logicToHwIncrSatName = fieldProperties.getFullSignalName(DefSignalType.L2H_INCRSAT);  // hwBaseName + "_incrsat_o" 
		   String logicToHwIncrTholdName = fieldProperties.getFullSignalName(DefSignalType.L2H_INCRTHOLD);  // hwBaseName + "_incrthold_o" 
		   String logicToHwDecrSatName = fieldProperties.getFullSignalName(DefSignalType.L2H_DECRSAT);  // hwBaseName + "_decrsat_o" 
		   String logicToHwDecrTholdName = fieldProperties.getFullSignalName(DefSignalType.L2H_DECRTHOLD);  // hwBaseName + "_decrthold_o" 

		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  //"reg_" + hwBaseName;  
		   String fieldRegisterNextName = fieldProperties.getFullSignalName(DefSignalType.FIELD_NEXT);  //"reg_" + hwBaseName + "_next";
		   
		   int fieldWidth = fieldProperties.getFieldWidth();
		   int countWidth = fieldWidth + 1;  // add a bit for over/underflow
		   
		   // create the next count value
		   addVectorReg(nextCountName, 0, countWidth);  
		   addCombinAssign(regProperties.getBaseName(), nextCountName + " = { 1'b0, " + fieldRegisterName + "};");  // no precedence - this stmt goes first  
		   
		   // if an incr is specified
		   if (fieldProperties.isIncrCounter()) {
			   
			   // add overflow output
			   if (fieldProperties.hasOverflow()) {
				   addHwScalar(DefSignalType.L2H_OVERFLOW);   // add hw overflow output
				   addScalarReg(logicToHwOverflowName);  
				   addRegAssign(regProperties.getBaseName(), logicToHwOverflowName +
						   " <= #1 " + nextCountName + "[" + fieldWidth + "] & ~" +  logicToHwOverflowName + ";");  // only active for one cycle  
			   }

			   // if a ref is being used for increment assign it, else add an input
			   //System.out.println("SystemVerilogBuilder genCounterWriteStmts: " + fieldProperties.getInstancePath() + " is an incr counter, hasIncrRef=" + fieldProperties.hasIncrRef());
			   generateInputOrAssign(DefSignalType.H2L_INCR, RhsRefType.INCR, 1, true, hwPrecedence); 

			   // create incr value from reference, constant, or input
			   String incrValueString =getCountIncrValueString(countWidth);
			   
			   // increment the count
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicIncrName + ")");
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "   " + nextCountName + " = "  + nextCountName + " + " + incrValueString + ";");
		   }
		   
		   // if a decr is specified
		   if (fieldProperties.isDecrCounter()) {
			   
			   // add underflow output
			   if (fieldProperties.hasUnderflow()) {
				   addHwScalar(DefSignalType.L2H_UNDERFLOW);   // add hw underflow output
				   addScalarReg(logicToHwUnderflowName);  
				   addRegAssign(regProperties.getBaseName(), logicToHwUnderflowName +
						   " <= #1 " + nextCountName + "[" + fieldWidth + "] & ~" +  logicToHwUnderflowName + ";");  // only active for one cycle  
			   }

			   // if a ref is being used for decrement assign it, else add an input
			   generateInputOrAssign(DefSignalType.H2L_DECR, RhsRefType.DECR, 1, true, hwPrecedence); 

			   // create decr value from reference, constant, or input
			   String decrValueString =getCountDecrValueString(countWidth);
			   
			   // decrement the count
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + hwToLogicDecrName + ")");
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "   " + nextCountName + " = "  + nextCountName + " - " + decrValueString + ";");
		   }
		   
		   // if a incr saturating counter add checks
		   if (fieldProperties.isIncrSatCounter()) {
			   String incrSatValueString = "0";
			   
			   // set saturate value from constant or ref
			   if (fieldProperties.hasRef(RhsRefType.INCR_SAT_VALUE)) {  // if a reference is specified
				   incrSatValueString = resolveRhsExpression(RhsRefType.INCR_SAT_VALUE);
			   }
			   else {  // otherwise a constant
				   RegNumber regNum = fieldProperties.getIncrSatValue();
				   regNum.setVectorLen(countWidth);
				   incrSatValueString = regNum.toString();
			   }
			   // limit next count to value of saturate
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + nextCountName + " > " + incrSatValueString + ")");
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "   " + nextCountName + " = "  + incrSatValueString + ";");
			   // add incrsat output
			   if (fieldProperties.hasSaturateOutputs()) addHwScalar(DefSignalType.L2H_INCRSAT);   // add hw incrsaturate output
			   addScalarReg(logicToHwIncrSatName);  
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, logicToHwIncrSatName + " = ( {1'b0, " + fieldRegisterName + "} == " + incrSatValueString + ");");
		   }
		   
		   // check for incrthreshold
		   if (fieldProperties.isIncrTholdCounter()) {
			   String incrTholdValueString = "0";
			   
			   // set saturate value from constant or ref
			   if (fieldProperties.hasRef(RhsRefType.INCR_THOLD_VALUE)) {  // if a reference is specified
				   incrTholdValueString = resolveRhsExpression(RhsRefType.INCR_THOLD_VALUE);
			   }
			   else {  // otherwise a constant
				   RegNumber regNum = fieldProperties.getIncrTholdValue();
				   regNum.setVectorLen(countWidth);
				   if (countWidth > 7) regNum.setNumBase(RegNumber.NumBase.Hex);
				   incrTholdValueString = regNum.toString();
			   }
			   // add incrthold output
			   addHwScalar(DefSignalType.L2H_INCRTHOLD);   // add hw incrthreshold output
			   addScalarReg(logicToHwIncrTholdName);  
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, logicToHwIncrTholdName + " = ( {1'b0, " + fieldRegisterName + "} == " + incrTholdValueString + ");");
		   }
		   
		   // if a decr saturating counter add checks
		   if (fieldProperties.isDecrSatCounter()) {
			   String decrSatValueString = "0";
			   
			   // set saturate value from constant or ref
			   if (fieldProperties.hasRef(RhsRefType.DECR_SAT_VALUE) ) {  // if a reference is specified
				   decrSatValueString = resolveRhsExpression(RhsRefType.DECR_SAT_VALUE);
			   }
			   else {  // otherwise a constant
				   RegNumber regNum = fieldProperties.getDecrSatValue();
				   regNum.setVectorLen(countWidth);
				   if (countWidth > 7) regNum.setNumBase(RegNumber.NumBase.Hex);
				   decrSatValueString = regNum.toString();
			   }
			   // limit next count to value of saturate
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "if (" + nextCountName + " < " + decrSatValueString + ")");
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, "   " + nextCountName + " = "  + decrSatValueString + ";");
			   // add decrsat output
			   if (fieldProperties.hasSaturateOutputs()) addHwScalar(DefSignalType.L2H_DECRSAT);   // add hw decrsaturate output
			   addScalarReg(logicToHwDecrSatName);  
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, logicToHwDecrSatName + " = ( {1'b0, " + fieldRegisterName + "} == " + decrSatValueString + ");");
		   }
		   
		   // check for decrthreshold
		   if (fieldProperties.isDecrTholdCounter()) {
			   String decrTholdValueString = "0";
			   
			   // set saturate value from constant or ref
			   if (fieldProperties.hasRef(RhsRefType.DECR_THOLD_VALUE)) {  // if a reference is specified
				   decrTholdValueString = resolveRhsExpression(RhsRefType.DECR_THOLD_VALUE);
			   }
			   else {  // otherwise a constant
				   RegNumber regNum = fieldProperties.getDecrTholdValue();
				   regNum.setVectorLen(countWidth);
				   if (countWidth > 7) regNum.setNumBase(RegNumber.NumBase.Hex);
				   decrTholdValueString = regNum.toString();
			   }
			   // add decrthold output
			   addHwScalar(DefSignalType.L2H_DECRTHOLD);   // add hw decrthreshold output
			   addScalarReg(logicToHwDecrTholdName);  
			   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, logicToHwDecrTholdName + " = ( {1'b0, " + fieldRegisterName + "} == " + decrTholdValueString + ");");
		   }
		   
		   // now assign the next count value to the reg
		   addPrecCombinAssign(regProperties.getBaseName(), hwPrecedence, fieldRegisterNextName + " = " + nextCountName + SystemVerilogSignal.genDefArrayString(0, fieldWidth) + ";");			
		
	}

	/** create count increment string */
	private String getCountIncrValueString(int countWidth) {
		String incrValueString = "0";
		String hwToLogicIncrValueName = fieldProperties.getFullSignalName(DefSignalType.H2L_INCRVALUE);  // hwBaseName + "_incrvalue" 
		Integer incrWidth = fieldProperties.getIncrWidth();
		if (incrWidth != null) {  // if an external input is specified
			addHwVector(DefSignalType.H2L_INCRVALUE, 0, incrWidth);   // add hw incr value input
			incrValueString = "{" + (countWidth - incrWidth) + "'b0, " + hwToLogicIncrValueName + "}";
		}
		else if (fieldProperties.hasRef(RhsRefType.INCR_VALUE)) {  // if a reference is specified
			incrValueString = resolveRhsExpression(RhsRefType.INCR_VALUE);
		}
		else {  // otherwise a constant
			RegNumber regNum = fieldProperties.getIncrValue();
			regNum.setVectorLen(countWidth);
			if (countWidth > 7) regNum.setNumBase(RegNumber.NumBase.Hex);
			incrValueString = regNum.toString();
		}
		return incrValueString;
	}
	
	/** create count decrement string */
	private String getCountDecrValueString(int countWidth) {
		String decrValueString = "0";
		String hwToLogicDecrValueName = fieldProperties.getFullSignalName(DefSignalType.H2L_DECRVALUE);  // hwBaseName + "_decrvalue" 
		Integer decrWidth = fieldProperties.getDecrWidth();
		if (decrWidth != null) {  // if an external input is specified
			addHwVector(DefSignalType.H2L_DECRVALUE, 0, decrWidth);   // add hw decr value input
			decrValueString = "{" + (countWidth - decrWidth) + "'b0, " + hwToLogicDecrValueName + "}";
		}
		else if (fieldProperties.hasRef(RhsRefType.DECR_VALUE)) {  // if a reference is specified
			decrValueString = resolveRhsExpression(RhsRefType.DECR_VALUE);
		}
		else {  // otherwise a constant
			RegNumber regNum = fieldProperties.getDecrValue();
			regNum.setVectorLen(countWidth);
			if (countWidth > 7) regNum.setNumBase(RegNumber.NumBase.Hex);
			decrValueString = regNum.toString();
		}
		return decrValueString;
	}

	/** generate field read statements */  
	void genFieldReadStmts() {
		   // create field-specific verilog signal names
		   String logicToHwDataName = fieldProperties.getFullSignalName(DefSignalType.L2H_DATA);  // hwBaseName + "_r" ;
		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  //"ff_" + hwBaseName;

		   // if sw readable, set output (else return 0s)
		   genSwFieldReadStmts();
		   
		   // if hw readable, add the interface and set output
		   if (fieldProperties.isHwReadable()) {
			   // add read signals from logic to hw
			   addHwVector(DefSignalType.L2H_DATA, 0, fieldProperties.getFieldWidth());    // logic to hw list 
			   
			   // assign hw read data outputs
			   addVectorReg(logicToHwDataName, 0, fieldProperties.getFieldWidth());  // add outputs to define list since we'll use block assign
			   addCombinAssign(regProperties.getBaseName(), logicToHwDataName + " = " + fieldRegisterName + ";");  
		   }
		   
		   // if anded/ored/xored outputs specified
		   genBitwiseOutputStmts();
	}
	
	/** create bitwise outputs for this field */
	private void genBitwiseOutputStmts() {
		   // create field-specific verilog signal names
		   String logicToHwAndedName = fieldProperties.getFullSignalName(DefSignalType.L2H_ANDED);  
		   String logicToHwOredName = fieldProperties.getFullSignalName(DefSignalType.L2H_ORED);  
		   String logicToHwXoredName = fieldProperties.getFullSignalName(DefSignalType.L2H_XORED);  
		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  
		   
		   // anded output
		   if (fieldProperties.isAnded()) {
			   addHwScalar(DefSignalType.L2H_ANDED);    // logic to hw list 
			   addScalarReg(logicToHwAndedName);  // add outputs to define list since we'll use block assign
			   addCombinAssign(regProperties.getBaseName(), logicToHwAndedName + " = & " + fieldRegisterName + ";");  			   
		   }
		   // ored output
		   if (fieldProperties.isOred()) {
			   addHwScalar(DefSignalType.L2H_ORED);    // logic to hw list 
			   addScalarReg(logicToHwOredName);  // add outputs to define list since we'll use block assign
			   addCombinAssign(regProperties.getBaseName(), logicToHwOredName + " = | " + fieldRegisterName + ";");  			   
		   }
		   // xored output
		   if (fieldProperties.isXored()) {
			   addHwScalar(DefSignalType.L2H_XORED);    // logic to hw list 
			   addScalarReg(logicToHwXoredName);  // add outputs to define list since we'll use block assign
			   addCombinAssign(regProperties.getBaseName(), logicToHwXoredName + " = ^ " + fieldRegisterName + ";");  			   
		   }
		
	}

	/** generate alias register field read statements */  
	void genSwFieldReadStmts() {
		   // create field-specific verilog signal names
		   String fieldRegisterName = fieldProperties.getFullSignalName(DefSignalType.FIELD);  //"rg_" + hwBaseName;
		   String fieldArrayString = fieldProperties.getFieldArrayString(); 
		   // if an aliased register override the field name
		   if (regProperties.isAlias()) {
			   String aliasBaseName = regProperties.getAliasBaseName();
			   fieldRegisterName = FieldProperties.getFieldRegisterName(aliasBaseName + "_" + fieldProperties.getPrefixedId(), true);  //"rg_" + AliasBaseName;
		   }
		   // if sw readable, set output (else return 0s)
		   if (fieldProperties.isSwReadable()) {
			   builder.addToTempAssignList(regProperties.getFullSignalName(DefSignalType.L2D_DATA) + fieldArrayString + " = " + fieldRegisterName + ";"); // need to set unused bits to 0 after all fields added		
		   }
		   
	}

	// -------------- user defined signal methods

	/** save user defined signal info */
	public void addUserDefinedSignal(String rtlName, SignalProperties signalProperties) {
		//System.out.println("SystemVerilogLogicModule addUserDefinedSignal: ref=" + rtlName + ", key=" + signalProperties.getFullSignalName(DefSignalType.USR_SIGNAL));
		String sigName = signalProperties.getFullSignalName(DefSignalType.USR_SIGNAL);
		boolean setAsRhsReference = userDefinedSignals.containsKey(sigName) && (userDefinedSignals.get(sigName) == null);
		if (setAsRhsReference) signalProperties.setRhsReference(true);
		userDefinedSignals.put(sigName, signalProperties); 
	}
	
	/** determine if a rhs reference is a signal or a field and return modified name if a signal.
	 *  if a signal, it is tagged as a rhsReference in userDefinedSignals list. 
	 * this method should only be called after entire signal list is created at addrmap exit */
	public String resolveAsSignalOrField(String ref) {
		String sigNameStr = ref.replaceFirst("rg_", "sig_");  // speculatively convert to signal prefix for lookup
		//if (ref.contains("int_detected_cas_tx_afifo2_mem_0")) System.out.println("SystemVerilogLogicModule resolveAsSignalOrField: ref=" + ref + ", isUserDefinedSignal(sigNameStr)=" + builder.isUserDefinedSignal(sigNameStr));
		if ((ref.startsWith("rg_") && builder.isUserDefinedSignal(sigNameStr))) {  // check that signal is in pre-computed set  
			// the local list may not have been populated, but can load with null to indicate that it's been seen on rhs of an assign
			if (!userDefinedSignals.containsKey(sigNameStr)) {
				//System.out.println("SystemVerilogLogicModule resolveAsSignalOrField: " + sigNameStr + " was found in master list, but not in module-specific list");
				userDefinedSignals.put(sigNameStr, null);
			}
			else {
				//if (userDefinedSignals.get(sigNameStr)==null) System.out.println("SystemVerilogLogicModule resolveAsSignalOrField: marking null signal " + sigNameStr + " as rhsReference");
				if (userDefinedSignals.get(sigNameStr)!=null) userDefinedSignals.get(sigNameStr).setRhsReference(true);  // indicate that this signal is used internally as rhs  
			}
			return sigNameStr;  // return signal form if found
		}	
		return ref;  // no name change by default
	}
	
	/** loop through user defined signals and add assign statements to set these signals - call after build when userDefinedSignals is valid */  
	public void createSignalAssigns() {
		// first loop through signals, detect any signals on rhs, and verify each sig in rhs exists
		for (String key: userDefinedSignals.keySet()) {
			SignalProperties sig = userDefinedSignals.get(key);
			//if (sig == null) System.out.println("SystemVerilogLogicModule createSignalAssigns: null sig key=" + key);
			//else System.out.println("SystemVerilogLogicModule createSignalAssigns: sig key=" + key + ", sig id=" + sig.getId());
			// if signal in current module is assigned internally and has simple rhs, check for valid vlog define and resolve sig vs reg
			if ((sig != null) && sig.hasAssignExpr() ) {  
				// loop thru refs here... check each for well formed
				List<RhsReference> rhsRefList = sig.getAssignExpr().getRefList(); 
				for (RhsReference ref: rhsRefList) {
					//System.out.println("SystemVerilogLogicModule createSignalAssigns: sig key=" + key + ": ref=" + ref.getRawReference() + ", depth=" + ref.getDepth() + ", inst=" + ref.getInstancePath());
					if (ref.isWellFormedSignalName()) {
						String refName = ref.getReferenceName(sig, false); 
						//System.out.println("SystemVerilogLogicModule createSignalAssigns: sig prop inst=" + sig.getInstancePath() + ", refName=" + refName);
						refName = resolveAsSignalOrField(refName);  // resolve and tag any signals as rhsReference
						// check for a valid signal
						if (!this.hasDefinedSignal(refName) && (rhsSignals.containsKey(refName))) {  
							RhsReferenceInfo rInfo = rhsSignals.get(refName);
							//System.out.println("SystemVerilogLogicModule createSignalAssigns: refName=" + refName + ", hasDefinedSignal=" + this.hasDefinedSignal(refName) + ", rhsSignals.containsKey=" + rhsSignals.containsKey(refName));
							Ordt.errorMessage("unable to resolve " + rInfo.getRhsRefString() + " referenced in rhs dynamic property assignment for " + rInfo.getLhsInstance()); 
						}						
					}
				}
			}
		}
		// now that rhs references have been detected, create assigns and detect unused signals
		for (String key: userDefinedSignals.keySet()) {
			SignalProperties sig = userDefinedSignals.get(key);
			//System.out.println("SystemVerilogLogicModule createSignalAssigns: signal key=" + key + ", isRhs=" + sig.isRhsReference());
			// if signal is assigned internally add an assign else an input
			if (sig != null) {
				if (sig.hasAssignExpr()) {
					//System.out.println("SystemVerilogLogicModule createSignalAssigns: raw expr=" + sig.getAssignExpr().getRawExpression() + ", res expr=" + sig.getAssignExpr().getResolvedExpression(sig, userDefinedSignals));
					String rhsSigExpression = sig.getAssignExpr().getResolvedExpression(sig, userDefinedSignals); 
					this.addCombinAssign("user defined signal assigns", sig.getFullSignalName(DefSignalType.USR_SIGNAL) + " = " + rhsSigExpression + ";");
				}
				// if not assigned a ref, must be an input, so verify use in an assign
				else {
					// if not used internally, issue an error
					if (!sig.isRhsReference())
						Ordt.errorMessage("user defined signal " + sig.getFullSignalName(DefSignalType.USR_SIGNAL) + " is not used");		
				}
			}
		}
	}
	
	// -------------- rhs assign signal methods

	/** add a signal to the list of rhs references */
	public void addRhsSignal(String refName, String instancePath, String rawReference) {
		rhsSignals.put(refName, new RhsReferenceInfo(instancePath, rawReference ));
	}
	
	/** check that a resolved signal is in valid list of logic module signals and issue an
	 *  error message if not. 
	 * this method should only be called after entire signal list is created at addrmap exit 
	 * @param preResolveName - name of signal before resolution (used to lookup error msg info)
	 * @param postResolveName - name of signal after resolution
	 * */
	public void checkSignalName(String preResolveName, String postResolveName) {
		// issue an error if resolved name is not in the defined signal list
		if (!this.hasDefinedSignal(postResolveName)) {
			if (rhsSignals.containsKey(preResolveName)) {
				RhsReferenceInfo rInfo = rhsSignals.get(preResolveName);
				//System.out.println("SystemVerilogLogicModule checkSignalName: preResolveName=" + preResolveName + " found in rhsSignals, but postResolveName=" + postResolveName + " not found in definedSignals");
				Ordt.errorMessage("unable to resolve " + rInfo.getRhsRefString() + " referenced in rhs dynamic property assignment for " + rInfo.getLhsInstance()); 
			}
			else
				Ordt.errorMessage("unable to resolve signal " + postResolveName + " inferred in rhs dynamic property assignment" ); 
		}
	}
	
	// -------------- coverpoints
	
	/** add coverpoint associated with this field */
	public void addFieldCoverPoints(FieldProperties fieldProperties) {
		// if an interrupt field, cover input signal
		if ((fieldProperties.generateRtlCoverage() || ExtParameters.sysVerIncludeDefaultCoverage()) && fieldProperties.isInterrupt()) {
			// add coverage on input intr signal (if it exists)
			if (!fieldProperties.hasRef(RhsRefType.INTR) && !fieldProperties.hasRef(RhsRefType.NEXT)) {
				String intrName = fieldProperties.getFullSignalName(DefSignalType.H2L_INTR);
				this.addCoverPoint("interrupt_cg", intrName, intrName, null);
			}			
		}
		// if a counter field, cover incr/decr signals, rollover/saturate, threshold
		else if ((fieldProperties.generateRtlCoverage() || ExtParameters.sysVerIncludeDefaultCoverage()) && fieldProperties.isCounter()) {
			// add coverage on input incr signal (if it exists)
			if (fieldProperties.isIncrCounter() && !fieldProperties.hasRef(RhsRefType.INCR)) {
				String incrName = fieldProperties.getFullSignalName(DefSignalType.H2L_INCR);
				this.addCoverPoint("counter_cg", incrName, incrName, null);
			}			
			// add coverage on input decr signal (if it exists)
			if (fieldProperties.isDecrCounter() && !fieldProperties.hasRef(RhsRefType.DECR)) {
				String decrName = fieldProperties.getFullSignalName(DefSignalType.H2L_DECR);
				this.addCoverPoint("counter_cg", decrName, decrName, null);
			}			
			// TODO - add rollover / saturate test?
		}
		// otherwise if rtl_coverage is explicitly specified
		else if (fieldProperties.generateRtlCoverage()) {
			String fldReg = fieldProperties.getFullSignalName(DefSignalType.FIELD);
			this.addCoverPoint("field_cg", fldReg, fldReg, null);
		}
		
	}
	
	//---------------------------- inner classes ----------------------------------------
	
	/** class to hold rhs assignment info for performing sig checks once all instances have been added to builder */
	private class RhsReferenceInfo {
		String lhsInstance;
		String rhsRefString;
		
		public RhsReferenceInfo(String lhsInstance, String rhsRefString) {
			this.lhsInstance = lhsInstance;
			this.rhsRefString = rhsRefString;
		}

		/** get lhsInstance
		 *  @return the lhsInstance
		 */
		public String getLhsInstance() {
			return lhsInstance;
		}

		/** get rhsRefString
		 *  @return the rhsRefString
		 */
		public String getRhsRefString() {
			return rhsRefString;
		}
	}

}
