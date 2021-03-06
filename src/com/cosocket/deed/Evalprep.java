package com.cosocket.deed;
import  com.cosocket.deed.S5;

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

public class Evalprep {
    public static final boolean valideval(byte[] p, byte[] s) throws Exception {
        if(s.length != p.length + 1) throw new Exception("Incorrect GP lengths");
        byte a = eval(p,s);
        if(!(a == S5.A || a == S5.I)) throw new Exception("Not alpha-computing");
        return a == S5.A;
    }

    protected static final byte eval(byte[] p, byte[] s) {
        byte a = S5.I;
        for (int i = 0; i < p.length; i++) a = S5.gmul[S5.gmul[a][s[i]]][p[i]];
        return S5.gmul[a][s[p.length]];
    }

    protected static final boolean getbit(int[] d, int j) {return (((d[(j / 32)] >>> (j % 32)) & 1) != 0);}
    protected static final void setbit(int[] d, int j) {d[(j / 32)] |= (1 << (j % 32));}
    protected static final void clearbit(int[] d, int j) {d[(j / 32)] &= ~(1 << (j % 32));}
    protected static final int bitsNeeded(int a) {return a == 0 ? 0 : 32 - Integer.numberOfLeadingZeros(a - 1);}

    public static final byte[] mdelements(int[] md, int n, int m, boolean flip) {
        assert(md.length == ((n - 1) / 32) + 1);
        byte[] pgp = new byte[2*m*n];
        byte[] tmp = new byte[2*n];
        for(int i = 0; i < n; i++) if(getbit(md,i)) {tmp[2*i]=S5.A; tmp[2*i+1] = flip ? S5.AI : S5.A;}
        for (int j = 0; j < pgp.length; j+=tmp.length) System.arraycopy(tmp, 0, pgp, j, tmp.length);
        return pgp;
    }

    /**
     * Pad and selectorize the input GP
     * Given input gp, a byte array containing a canonical GP of length <= 2*m + 1
     * and an output sgp, a byte array of length 2 * m * n + 1
     * where n is the known maximum length of metadata in
     * and the known maximum number of elements in canonical group program is 2*m + 1
     *
     * Initialize an array of elements of length 2*m*n + 1 to I (which is 0)
     * Replace elements of array at index: j*n with Sj for j in 0,2,� in canonical GP
     * To select input bit Pj (Pj in 0..n-1) at j in 1,3,... in canonical GP
     *   pre-multiply element at index: (j-1)*n+2*Pj+1 with B, (j-1)*n+2*Pj+2 with BI
     *   pre- and post-multiply each selector block with G2AL and G2AR respectively
     *   if flip==false, publisher makes [A,A] instead of [A,AI], so wrap A2AIL and A2AIR constants
     *
     */
    public static final byte[] selectorize(int[] gp, int n, int m, boolean flip) throws Exception {
        if((gp.length % 2 != 1) || (gp.length > 2*m + 1)) throw new Exception("GP too long");
        byte[] sgp = new byte[2*m*n + 1];
        if(!flip) for (int j = 1; j < sgp.length; j+=2) {sgp[j]=S5.A2AIL; sgp[j+1]=S5.A2AIR;}
        for (int j = 0; j < gp.length - 1; j+=2) {
            int p    = j*n;
            int q    = p+2*n;
            sgp[p]   = S5.gmul[sgp[p]][gp[j]];
            int k    = p + 1 + 2 * gp[j+1];
            sgp[k]   = S5.gmul[S5.B][sgp[k]];
            sgp[k+1] = S5.gmul[sgp[k+1]][S5.BI];
            sgp[p]   = S5.gmul[sgp[p]][S5.G2AL];
            sgp[q]   = S5.gmul[sgp[q]][S5.G2AR];
         }
         sgp[gp.length*n] = S5.gmul[sgp[gp.length*n]][gp[gp.length-1]];
         return sgp;
    }

    public static final void pblind(byte[] r, byte[] d, byte[] o) {
        assert(o.length == d.length);
        assert(r.length >= 2 * d.length - 1);
        for (int i = 0; i < d.length; i++)
            o[i] = S5.gmul[S5.gmul[r[2*i]][d[i]]][S5.ginv[r[2*i+1]]];
    }

    public static final void sblind(byte[] r, byte[] d, byte[] o) {
        assert(o.length == d.length);
        assert(r.length >= 2 * d.length - 2);
        o[0]=S5.gmul[d[0]][S5.ginv[r[0]]];
        for (int i = 1; i < d.length - 1; i++)
            o[i] = S5.gmul[S5.gmul[r[2*i-1]][d[i]]][S5.ginv[r[2*i]]];
        o[d.length-1]=S5.gmul[r[2*d.length - 3]][d[d.length-1]];
    }
}
