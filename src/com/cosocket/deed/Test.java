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
    protected static void fullExample(String sch, String pxml, String sxml, int n, int m, boolean time) throws Exception {       
        long tBB, t0,t1;

        // Warm up RNG and JVM 
        t0 = System.currentTimeMillis();  
        (new Parse(sch)).parseMetadataXML(pxml, 64);
        SecureRandom.getInstance("SHA1PRNG").nextBytes(new byte[1000000]);
        t1 = System.currentTimeMillis(); if(time) System.out.println("system warmup time: " + (t1 - t0) + " ms");
        
        // Init at publisher
        t0 = System.currentTimeMillis();        
    	Parse up = new Parse(sch);
        SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
        prng.setSeed(new byte[]{1,-1,2,-2,3,-3,4,-4,5,-5,6,-6,7,-7,8,-8,9,-9,10,-10});
        t1 = System.currentTimeMillis(); if(time) System.out.println("init at pub: " + (t1 - t0) + " ms");
        
        // Init at subscriber
        t0 = System.currentTimeMillis();
    	Parse us = new Parse(sch);
        SecureRandom srng = SecureRandom.getInstance("SHA1PRNG");
        srng.setSeed(new byte[]{1,-1,2,-2,3,-3,4,-4,5,-5,6,-6,7,-7,8,-8,9,-9,10,-10});
        t1 = System.currentTimeMillis(); if(time) System.out.println("init at sub: " + (t1 - t0) + " ms");
                
        tBB = System.currentTimeMillis();

        // At publisher
        t0 = System.currentTimeMillis();
        int[] md = up.parseMetadataXML(pxml, n);

        byte[] pgp = Evalprep.mdelements(md,n,m);
        byte[] prseq = new byte[4*m*n];
        byte[] cpgp = new byte[2*m*n];        
        t1 = System.currentTimeMillis(); if(time) System.out.println("preparation at pub: " + (t1 - t0) + " ms");
        
        t0 = System.currentTimeMillis();
        
        Evalprep.randseq(prng, prseq);
        t1 = System.currentTimeMillis(); if(time) System.out.println("randseq at pub: " + (t1 - t0) + " ms");
        
        t0 = System.currentTimeMillis();
        Evalprep.pblind(prseq,pgp,cpgp);
        t1 = System.currentTimeMillis(); if(time) System.out.println("blinding at pub: " + (t1 - t0) + " ms");

        // At subscriber
        t0 = System.currentTimeMillis();        
        int[] si = us.parseInterestXML(sxml, n, m);
        byte[] sgp = Evalprep.selectorize(si,n,m);
        byte[] srseq = new byte[4*m*n];
        byte[] csgp = new byte[2*m*n+1];
        t1 = System.currentTimeMillis(); if(time) System.out.println("preparation at sub: " + (t1 - t0) + " ms");
        
        t0 = System.currentTimeMillis();
        Evalprep.randseq(srng, srseq);
        t1 = System.currentTimeMillis(); if(time) System.out.println("randseq at sub: " + (t1 - t0) + " ms");
        
        t0 = System.currentTimeMillis();
        Evalprep.sblind(srseq,sgp,csgp);
        t1 = System.currentTimeMillis(); if(time) System.out.println("blinding at sub: " + (t1 - t0) + " ms");
    
        // At broker
        t0 = System.currentTimeMillis();
        boolean rslt = Evalprep.eval(cpgp,csgp);
        t1 = System.currentTimeMillis(); if(time) System.out.println("eval at broker: " + (t1 - t0) + " ms");
        
        if(time) System.out.println("end-to-end: " + (t1 - tBB) + " ms");
        System.out.println("metadata matches interest: " + (rslt ? "yes" : "no"));
        // Random number generation dominates; use of RDRAND should be of benefit
    }
    
    protected static void testGP() throws Exception {     	    	
    	int n = 16;
        int m = 16384;       
        int[] md = new int[(n - 1)/32 + 1];
        
        Evalprep.setbit(md, 0);
        Evalprep.setbit(md, 1);
              
        ArrayList<GP> x = new ArrayList<GP>();
        ArrayList<GP> y = new ArrayList<GP>();
        for (int i=0; i<4; i++) x.add(GP.Pin(i));
        y.add(GP.Const(false));
        y.add(GP.Const(true));
        y.add(GP.Const(true));
        y.add(GP.Const(false));
        //GP ct = GP.Equal(x,GP.ShiftRight(y,1));
        //GP ct = GP.BitOpBlock(x,y,"And").get(1);
        //GP ct = GP.IfThenElseBlock(GP.Const(true),x,y).get(0);
        //GP ct = GP.Equal(GP.IfThenElseBlock(GP.Const(true),x,y),y);
        GP ct = GP.Equal(GP.BitOpBlock(x,x,"Or"),x);

        // for(int i = 0; i < 16; i++) System.out.print(Evalprep.getbit(md, i)?1:0); System.out.println();
        byte[]  pgp  = Evalprep.mdelements(md,n,m);
        byte[]  sgp  = Evalprep.selectorize(ct.gp(),n,m);
        boolean rslt = Evalprep.eval(pgp,sgp);

        System.out.println("eval: " + rslt);
    }

    public static void main(String[] args) throws Exception {
    	String METADATA_XML   = "xml/mdrecord-example.xml";
    	String INTEREST_XML   = "xml/interest-example.xml";
    	String DEEDSCHEMA_XSD = "xml/deedschema.xsd";   	
    	fullExample(DEEDSCHEMA_XSD, METADATA_XML, INTEREST_XML, 64, 16384, false);
    	
    	testGP();
    }
}