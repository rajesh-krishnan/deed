package com.cosocket.deed;
import java.security.SecureRandom;
import java.util.ArrayList;
import com.cosocket.deed.Evalprep;
import com.cosocket.deed.Parse;

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

public class Test {
	public static final String METADATA_XML   = "xml/mdrecord-example.xml";
	public static final String DEEDSCHEMA_XSD = "xml/deedschema.xsd";
	
    protected static int[] testInterest1(int n, int m) {    
        GP ct;        
        ArrayList<GP> x = new ArrayList<GP>();
        ArrayList<GP> y = new ArrayList<GP>();
        for (short i=0; i<4; i++) x.add(GP.Pin(i));
        y.add(GP.Const(true));
        y.add(GP.Const(true));
        y.add(GP.Const(false));
        y.add(GP.Const(false));
        ct = GP.MultiEqual(x,y);
        return ct.gp();
    }
    
    protected static int[] testMetadata1(int n) {    
        int[] tmp = new int[(n - 1)/32 + 1];    
        Evalprep.setbit(tmp, 0);
        Evalprep.setbit(tmp, 1);
        return tmp;
    }
    
    protected static int[] testMetadata2(int n) throws Exception {  
    	return Parse.parseMetadataXML(DEEDSCHEMA_XSD, METADATA_XML, n); 
    }
    
    protected static int[] testMetadata2(int n, int m) throws Exception {  
    	return Parse.parseMetadataXML(DEEDSCHEMA_XSD, METADATA_XML, n); 
    }
      
    public static void main(String[] args) throws Exception {            
        long t0,t1;
        t0 = System.currentTimeMillis();
        int n = 64;
        int m = 16000;
                
        // At publisher
        SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
        prng.setSeed(new byte[]{1,-1,2,-2,3,-3,4,-4,5,-5,6,-6,7,-7,8,-8,9,-9,10,-10});        
        int[] md = testMetadata1(n);
        // int[] md = testMetadata2(n);
        
        for (int k = 0; k < n; k++) System.out.print(Evalprep.getbit(md, k) ? 1 : 0);
        System.out.println();  
       
        byte[] pgp = Evalprep.mdelements(md,n,m);
        byte[] prseq = new byte[4*m*n];
        Evalprep.randseq(prng, prseq);
        byte[] cpgp = new byte[2*m*n];
        Evalprep.pblind(prseq,pgp,cpgp);

        // At subscriber
        SecureRandom srng = SecureRandom.getInstance("SHA1PRNG");
        srng.setSeed(new byte[]{1,-1,2,-2,3,-3,4,-4,5,-5,6,-6,7,-7,8,-8,9,-9,10,-10});
        int[] si = testInterest1(n, m);
        // int[] si = testInterest2(n, m);
        byte[] sgp = Evalprep.selectorize(si,n,m);
        byte[] srseq = new byte[4*m*n];
        Evalprep.randseq(srng, srseq);        
        byte[] csgp = new byte[2*m*n+1];
        Evalprep.sblind(srseq,sgp,csgp);

        // At broker                
        boolean rslt = Evalprep.eval(cpgp,csgp);
        
        t1 = System.currentTimeMillis();
        System.out.println("eval: " + rslt + " time: " + (t1 - t0) + " ms");
    }
}
