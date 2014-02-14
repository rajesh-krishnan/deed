package com.cosocket.deed;
import java.io.File;
import java.util.ArrayList;
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
    protected static class BitFld {String name; int ordinal; int cardinality; int bits;}
	
	private static <T> T unmarshal(Class<T> rtClass, String schFile, String xmlFile) throws Exception {
		Schema mySchema;
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		mySchema = sf.newSchema(new File(schFile));

		String packageName = rtClass.getPackage().getName();
		JAXBContext jc = JAXBContext.newInstance(packageName);
		Unmarshaller u = jc.createUnmarshaller();
		u.setSchema(mySchema);
		
		@SuppressWarnings("unchecked")
		JAXBElement<T> root = (JAXBElement<T>)u.unmarshal(new File(xmlFile));
		return root.getValue();
	}
	
    private static final int[] packbits(ArrayList<BitFld> bfArr, int n) throws Exception {         
        int[] result = new int[(n - 1) / 32 + 1];      
        int index = 0;
        for (BitFld bf : bfArr)
        	for(int j=0; j < bf.bits; j++) {            		
        		if(((bf.ordinal >> j) & 1) == 1) Evalprep.setbit(result, index);
        		index++;
        	}         	      
        // for (int k = 0; k < n; k++) System.out.print(Evalprep.getbit(result, k) ? 1 : 0);
        // System.out.println();        
        return result;
    }
    
	public static int[] parseMetadataXML(String schFile, String xmlFile, int n) throws Exception { 
		Object x = unmarshal(IntelRecord.class, schFile, xmlFile);
		
        if (x instanceof IntelRecord) { // OR other metadata types as they are defined
            ArrayList<BitFld> bfArr = new ArrayList<BitFld>();
            Class<?> cls = Class.forName(IntelRecord.class.getPackage().getName() + "." + ((IntelRecord) x).getFIELDSENUM());
            
            for (Object ec : cls.getEnumConstants()) {
                Object y = IntelRecord.class.getMethod("get" + ec.toString()).invoke(x);
            	if(y.getClass().isEnum()) {
        	        Enum<?> z      = (Enum<?>) y;
        	        BitFld bf      = new Parse.BitFld();
        	        bf.name        = z.toString();
        	        bf.ordinal     = z.ordinal();
        	        bf.cardinality = z.getClass().getEnumConstants().length;
        	        bf.bits        = Evalprep.bitsNeeded(bf.cardinality);
        	        bfArr.add(bf);            	    
        	    }
        	    // handle xsd:int and xsd:binary here 
            }
            
            // for (BitFld bf : bfArr) System.out.println(bf.name + ", " + bf.ordinal + ", " + bf.cardinality + ", " + bf.bits);
            int totalbits = 0;
            for (BitFld bf : bfArr) totalbits += bf.bits;
            if(totalbits > n) throw new Exception("Too many bits needed");
            return packbits(bfArr, n);
        }
        return null;
    }

	public static int[] parseInterestXML(String schFile, String xmlFile, int n, int m) throws Exception { 
		Object x = unmarshal(Expr.class, schFile, xmlFile);

	    if (x instanceof Expr) {
        // parse interest into GP
	    }
	    
		return null;
    }	 
	
    public static void main(String[] args) throws Exception {    	
    	String METADATA_XML   = "xml/mdrecord-example.xml";
    	String DEEDSCHEMA_XSD = "xml/deedschema.xsd";
        int n = 64;
        int m = 16000;
    	int[] x = Parse.parseInterestXML(DEEDSCHEMA_XSD, METADATA_XML, n, m); 
    	//for (int i : x) System.out.println(i + " ");
    	if(x != null) System.out.println();
    }	
}
