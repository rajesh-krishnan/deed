package com.cosocket.deed;
import java.util.ArrayList;
//import javax.xml.parsers;

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
    // expression and DTD syntax
    // md2intarray
    // exp2gp
    
    // XSD for record and query syntax, record definition (fields/enums), record instance, query instance
    // need metadata

    public static int[] parseInterest(String s, int m) throws Exception {    
        GP ct;
        
        // faking for now
        ArrayList<GP> x = new ArrayList<GP>();
        for (short i=0; i<64; i++) x.add(GP.Pin(i));
        ct = GP.MultiOr(x);
        
        if(ct.gp().length > m) throw new Exception ("GP too long");
        return ct.gp();
    }
    
    public static int[] parseMetadata(String p, int n) throws Exception {    
        int maxlen = (n - 1)/32 + 1;
        
        // faking for now
        int[] tmp = new int[1];    
        tmp[0] = 1;         

        if (tmp.length == maxlen) return tmp;
        if (tmp.length > maxlen) throw new Exception ("metadata too long");
 
        int[] md = new int[maxlen];
        System.arraycopy(tmp,0,md,0,tmp.length);
        return md;
    }    
}
