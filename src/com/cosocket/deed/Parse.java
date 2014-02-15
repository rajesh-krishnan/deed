package com.cosocket.deed;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import com.cosocket.deed.xjcgen.*;

/*
Copyright (c) 2014, Cosocket LLC
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this
  list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

* Neither the name of the {organization} nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

public class Parse {
  	private Unmarshaller u;
	private Hashtable<String,String[]> classFields;
  	private Hashtable<String,String>   fieldTypeTbl;
	private Hashtable<String,Integer>  fieldStartTbl;
	private Hashtable<String,Integer>  fieldSizeTbl;
	private Hashtable<String,Integer>  ordinalValueTbl;
	private String[] getFields(String className)                    {return classFields.get(className);}
	private String   fieldType(String className, String fieldName)  {return fieldTypeTbl.get(className + ":" + fieldName);}
	private int      fieldStart(String className, String fieldName) {return fieldStartTbl.get(className + ":" + fieldName);}
	private int      fieldSize(String fieldType)                    {return fieldSizeTbl.get(fieldType);}
	private int      ordinalValue(String fieldType, String value)   {return ordinalValueTbl.get(fieldType + ":" + value);}

	// Assumes RECORDTYPE and RECORDTYPECODE exists in JAXB generated code
	public Parse(String schFile) throws Exception  {
		String pkgName  = RECORDTYPECODE.class.getPackage().getName();
		u               = Parse.createUnmarshaller(pkgName, schFile);
		classFields     = new Hashtable<String,String[]>();
		fieldTypeTbl    = new Hashtable<String,String>();
		fieldStartTbl   = new Hashtable<String,Integer>();
		fieldSizeTbl    = new Hashtable<String,Integer>();
		ordinalValueTbl = new Hashtable<String,Integer>();
		for(RECORDTYPECODE r : RECORDTYPECODE.class.getEnumConstants()) 
		   	determineSizes(Class.forName(pkgName + "." + r.toString()));
	}
	
    private static Unmarshaller createUnmarshaller(String packageName, String schFile) throws Exception {
	    Schema mySchema;
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		mySchema = sf.newSchema(new File(schFile));		
		JAXBContext jc = JAXBContext.newInstance(packageName);
		Unmarshaller u = jc.createUnmarshaller();
		u.setSchema(mySchema);		
		return u;
	}
	
    private void determineSizes(Class<?> cls) throws Exception {		
    	ArrayList<String> flds = new ArrayList<String>();
    	String className = cls.getSimpleName();
    	for (Method m : cls.getDeclaredMethods()) {
    		String str = m.getName();
    		if (str.matches("get[A-Z](.*)")) {
    			String fieldName = str.substring(3);
       	    	if(!fieldName.equals("RECORDTYPE")) flds.add(fieldName);
    			Class<?> fld = (Class<?>) m.getGenericReturnType();
    			String fieldType = fld.getSimpleName();
    			if(fld.isEnum()) {
        	        int fieldSize = Evalprep.bitsNeeded(fld.getEnumConstants().length);
        	    	fieldTypeTbl.put(className + ":" + fieldName, fieldType);
        	    	fieldSizeTbl.put(fieldType,fieldSize);
    				for (Object x : fld.getEnumConstants())
            	    	ordinalValueTbl.put(fieldType + ":" + x.toString(), ((Enum<?>) x).ordinal());
        	    } // handle xsd:int and xsd:binary here? 
    		}
    	}
	  	int index=0; // use alphabetical order as reflection does not guarantee declaration order
	  	Collections.sort(flds);	  	
	  	flds.add(0,"RECORDTYPE");
	  	classFields.put(className, flds.toArray(new String[flds.size()]));
    	for (String f : flds) {
    	    fieldStartTbl.put(className + ":" + f, index);
    	    index += fieldSize(fieldType(className,f));
    	}
    }
		
	public int[] parseMetadataXML(String xmlFile, int n) throws Exception {	
		JAXBElement<?> root = (JAXBElement<?>)u.unmarshal(new File(xmlFile));
		Object cls = root.getValue();		
		String className = cls.getClass().getSimpleName();
		int[] result = new int[(n - 1) / 32 + 1];    
    	for (String fieldName : getFields(className)) {
    	    String fieldType = this.fieldType(className, fieldName);
    	    Object y = cls.getClass().getMethod("get" + fieldName).invoke(cls);  			
            if(y.getClass().isEnum()) {            		
        	    String value = y.toString();
        	    int index = this.fieldStart(className, fieldName);
        	    int szbits = this.fieldSize(fieldType);
        	    int ordval = this.ordinalValue(fieldType, value); 
        	    for(int j=0; j < szbits; j++)            		
                	if(((ordval >> j) & 1) == 1) Evalprep.setbit(result, index+j);
        	} // handle xsd:int and xsd:binary here? 
    	}		
		return result;
    }
	
	public int[] parseInterestXML(String xmlFile, int n, int m) throws Exception { 
		JAXBElement<?> root = (JAXBElement<?>)u.unmarshal(new File(xmlFile));
		Object x = root.getValue();
	    if (!(x instanceof EXPR)) throw new Exception("Expecting EXPR");
	    int[] result = recurse((EXPR) x).gp();
	    if(result.length <= 2*m+1) return result; 
	    else throw new Exception("GP too big");
    }
	
	private GP recurse(EXPR y) throws Exception {
		List<EXPR> exps = y.getE();
    	ArrayList<GP> args = new ArrayList<GP>();
    	
		switch(y.getOP()) {
        case NOT:
        	if (exps.size() != 1) throw new Exception("NOT takes one argument");
        	return GP.Not(recurse(exps.get(0)));
		case AND: 
        	for(EXPR e : exps) args.add(recurse(e));
        	return GP.MultiAnd(args);
		case OR:
        	for(EXPR e : exps) args.add(recurse(e));
        	return GP.MultiOr(args);
		case EQUAL:  
			if (exps.size() != 2) throw new Exception("EQUAL takes two arguments");
			return GP.MultiEqual(multibit(exps.get(0)),multibit(exps.get(1)));		
        default: 
        	throw new Exception("Unsupported");
        }
    } 	
	
	private ArrayList<GP> multibit(EXPR y) throws Exception {
    	String mdT, mdV;
		ArrayList<GP> result = new ArrayList<GP>();
		int x, sz;
		
    	switch(y.getOP()) {
	    case CODE: 
    	    mdT = y.getTYP();
    	    mdV = y.getVAL();
	    	x   = ordinalValue(mdT, mdV); 
	    	sz  = fieldSize(mdT);
	    	for(int j = 0; j < sz; j++) result.add(GP.Const(((x >> j) & 1) == 1));	    	
    	    return result;
        case FIELD:
    	    mdT = y.getTYP();
    	    mdV = y.getVAL();
	    	x   = fieldStart(mdT, mdV); 
	    	sz  = fieldSize(fieldType(mdT,mdV));
    	    for(int j = 0; j < sz; j++) result.add(GP.Pin(x + j)); 
        	return result;
        default: 
    	    throw new Exception("Unsupported");
        } 	
	}	
}
