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

* Neither the name of the Cosocket LLC nor the names of its
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
    protected static final String DEEDSCHEMA_XSD = "xml/deedschema.xsd";
    protected static final String METADATA_XML   = "xml/mdrecord-example.xml";
    protected static final String INTEREST_XML   = "xml/interest-example.xml";
    protected static final String METADATA2_XML  = "xml/mdrecord-example-2.xml";
    protected static final String INTEREST2_XML  = "xml/interest-example-2.xml";

    protected static void fullExample(String sch, String pxml, String sxml, int n, int m, boolean flip, boolean detail) throws Exception {
        long tBB, t0,t1;
        byte[] sharedKey  = new byte[]{1,-1,2,-2,3,-3,4,-4,5,-5,6,-6,7,-7,8,-8,9,-9,10,-10};

        // Init at Pub
        t0 = System.currentTimeMillis();
        Parse up = new Parse(sch);
        SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
        prng.setSeed(sharedKey);
        byte[] prseq = new byte[4*m*n];
        byte[] cpgp = new byte[2*m*n];
        t1 = System.currentTimeMillis(); if(detail) System.out.println("init at pub   : " + (t1 - t0) + " ms");

        // Init at Sub
        t0 = System.currentTimeMillis();
        Parse us = new Parse(sch);
        SecureRandom srng = SecureRandom.getInstance("SHA1PRNG");
        srng.setSeed(sharedKey);
        byte[] srseq = new byte[4*m*n];
        byte[] csgp = new byte[2*m*n+1];
        t1 = System.currentTimeMillis(); if(detail) System.out.println("init at sub   : " + (t1 - t0) + " ms");

        tBB = System.currentTimeMillis();

        // Process at Pub
        t0 = System.currentTimeMillis();
        int[]  md = up.parseMetadataXML(pxml, n);
        byte[] pgp = Evalprep.mdelements(md,n,m,flip);
        t1 = System.currentTimeMillis(); if(detail) System.out.println("prepare at pub: " + (t1 - t0) + " ms");

        t0 = System.currentTimeMillis();
        S5.randseq(prng, prseq);
        t1 = System.currentTimeMillis(); if(detail) System.out.println("randseq at pub: " + (t1 - t0) + " ms");

        t0 = System.currentTimeMillis();
        Evalprep.pblind(prseq,pgp,cpgp);
        t1 = System.currentTimeMillis(); if(detail) System.out.println("blind at pub  : " + (t1 - t0) + " ms");

        // Process at Sub
        t0 = System.currentTimeMillis();
        int[] si = us.parseInterestXML(sxml, n, m);
        byte[] sgp = Evalprep.selectorize(si,n,m,flip);
        t1 = System.currentTimeMillis(); if(detail) System.out.println("prepare at sub: " + (t1 - t0) + " ms");

        t0 = System.currentTimeMillis();
        S5.randseq(srng, srseq);
        t1 = System.currentTimeMillis(); if(detail) System.out.println("randseq at sub: " + (t1 - t0) + " ms");

        t0 = System.currentTimeMillis();
        Evalprep.sblind(srseq,sgp,csgp);
        t1 = System.currentTimeMillis(); if(detail) System.out.println("blind at sub  : " + (t1 - t0) + " ms");

        // Process at Broker
        t0 = System.currentTimeMillis();
        boolean rslt = Evalprep.valideval(cpgp,csgp);
        t1 = System.currentTimeMillis(); if(detail) System.out.println("eval at broker: " + (t1 - t0) + " ms");

        if (detail) System.out.println("end-to-end run: " + (t1 - tBB) + " ms");
        System.out.println("pub-sub match?: " + (rslt ? "yes" : "no"));
    }

    protected static void helper(int wid, int[] md, ArrayList<GP> x, int xval, ArrayList<GP> y, int yval) {
        x.clear();
        y.clear();
        for (int i = 0; i < wid; i++) {
            if(((xval >> i) & 1) == 1) Evalprep.setbit(md, i); else Evalprep.clearbit(md, i);
            x.add(GP.Pin(i));
            y.add(GP.Const(((yval >> i) & 1) == 1));
        }
    }

    protected static void testGP() throws Exception {
        int n = 8;
        int m = 16384;
        boolean flip = true;
        int[] md = new int[(n - 1)/32 + 1];
        GP ct = null;
        ArrayList<GP> x = new ArrayList<GP>();
        ArrayList<GP> y = new ArrayList<GP>();

        helper(n, md, x, 3, y, 5);
        ct = GP.LesserOrEqual(x,y);
        //ct = GP.Equal(x,GP.ShiftRight(y,1));
        //ct = GP.Equal(GP.BitOpBlock(x,x,"Or"),x);
        //ct = GP.Equal(GP.IfThenElseBlock(GP.Const(true),x,y),y);

        byte[]  pgp  = Evalprep.mdelements(md,n,m,flip);
        byte[]  sgp  = Evalprep.selectorize(ct.gp(),n,m,flip);
        boolean rslt = Evalprep.valideval(pgp,sgp);
        System.out.println("TestGP eval: " + rslt);
    }

    public static void main(String[] args) throws Exception {
        testGP();
        System.out.println("\nExample 1: INTELRECORD");
        fullExample(DEEDSCHEMA_XSD, METADATA_XML,  INTEREST_XML,   32,   1024, true, false);
        System.out.println("\nExample 2: STOCKRECORD");
        fullExample(DEEDSCHEMA_XSD, METADATA2_XML, INTEREST2_XML, 128, 131072, false, true);
    }
}
