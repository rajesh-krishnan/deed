package com.cosocket.deed;
import java.lang.reflect.Method;
import java.util.ArrayList;
import com.cosocket.deed.S5;

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

public class GP {
    private static final byte GPLT0 = S5.G2AL;
    private static final byte GPLT1 = S5.A2BL;        
    private static final byte GPLT2 = S5.gmul[S5.A2BR][S5.A2AIL];
    private static final byte GPLT3 = S5.gmul[S5.A2AIR][S5.A2BIL];
    private static final byte GPLT4 = S5.gmul[S5.A2BIR][S5.G2AR];

    private int[] gp; 
    public  int[] gp()    {return gp;};
    private GP(boolean x) {gp = new int[]{x ? S5.A : S5.I};}
    private GP(int x)     {gp = new int[]{S5.I,x,S5.I};}
    private GP(int[] x)   {gp = x;}
    
    private static int[] invert(int[] x) {
        int[] gp = x.clone();
        int e     = gp.length - 1;
        gp[0]     = S5.gmul[S5.INVL][gp[0]];
        gp[e]     = S5.gmul[gp[e]][S5.INVR];
        return gp;
    }
    
    private static int[] conjunct(int[] lt, int[] rt) {              
        int[] gp = new int[2 * (lt.length + rt.length) - 3];
        int x     = 0;            
        gp[x]     = S5.gmul[GPLT0][lt[0]];
        System.arraycopy(lt,1,gp,x+1,lt.length-1);            
        x         = x + lt.length - 1;            
        gp[x]     = S5.gmul[S5.gmul[gp[x]][GPLT1]][rt[0]];
        System.arraycopy(rt,1,gp,x+1,rt.length-1);
        x         = x + rt.length - 1;
        gp[x]     = S5.gmul[S5.gmul[gp[x]][GPLT2]][lt[0]];
        System.arraycopy(lt,1,gp,x+1,lt.length-1);
        x         = x + lt.length - 1;
        gp[x]     = S5.gmul[S5.gmul[gp[x]][GPLT3]][rt[0]];
        System.arraycopy(rt,1,gp,x+1,rt.length-1);
        x         = x + rt.length - 1;            
        gp[x]     = S5.gmul[gp[x]][GPLT4];
        return gp;
    }
    
    public static final GP Const(boolean a)             {return new GP(a);}    
    public static final GP Pin(int a)                   {return new GP(a);}
    public static final GP Not(GP a)                    {return new GP(invert(a.gp()));}
    public static final GP And(GP a, GP b)              {return new GP(conjunct(a.gp(), b.gp()));}
    public static final GP Nand(GP a, GP b)             {return Not(And(a, b));}
    public static final GP Nor(GP a, GP b)              {return And(Not(a), Not(b));}
    public static final GP Or(GP a, GP b)               {return Not(Nor(a, b));}    
    public static final GP Xor(GP a, GP b)              {return Or(And(Not(a),b), And(a,Not(b)));}
    public static final GP Xnor(GP a, GP b)             {return Or(And(a,b), And(Not(a),Not(b)));}
    public static final GP Select(GP x, GP a, GP b)     {return Or(And(Not(x), a), And(x,b));}
    public static final GP IfThenElse(GP x, GP a, GP b) {return Or(And(x, a), And(Not(x),b));}
    public static final GP MultiNand(ArrayList<GP> x)   {return Not(MultiAnd(x));}
    public static final GP MultiNor(ArrayList<GP> x)    {return Not(MultiOr(x));}
    public static final GP AddSum(GP a, GP b, GP c)     {return Xor(Xor(a,b),c);}
    public static final GP AddCarry(GP a, GP b, GP c)   {return Or(And(a,b), And(Or(a,b),c));}
    public static final GP SubDiff(GP a, GP b, GP c)    {return AddSum(a,b,c);}
    public static final GP SubBorrow(GP a, GP b, GP c)  {return Or(And(a, And(b,c)), And(Not(a), Or(b,c)));}
    
    public static final GP MultiAnd(ArrayList<GP> x) {
        while(x.size() > 1) {
            ArrayList<GP> y = new ArrayList<GP>();
            while(x.size() > 1) y.add(And(x.remove(0),x.remove(0)));
            if(x.size()==1)     y.add(x.remove(0));
            x = y;
        }
        return x.remove(0);
    }
    
    public static final GP MultiOr(ArrayList<GP> x) {
        ArrayList<GP> y = new ArrayList<GP>();
        for (GP c : x)  y.add(Not(c));
        return Not(MultiAnd(y));
    }
    
    public static final GP Equal(ArrayList<GP> x, ArrayList<GP> y) {
        assert(x.size() == y.size());
        ArrayList<GP> z = new ArrayList<GP>();
        for(int i = 0; i < x.size(); i++) z.add(Xnor(x.get(i), y.get(i)));
        return MultiAnd(z);
    }

    // make a block of 2-input Boolean gates
    public static final ArrayList<GP> BitOpBlock(ArrayList<GP> x, ArrayList<GP> y, String boolMethod) throws Exception {
        assert(x.size() == y.size());
        ArrayList<GP> z = new ArrayList<GP>();
        Method M = GP.class.getMethod(boolMethod, GP.class, GP.class);
        for(int i = 0; i < x.size(); i++) z.add((GP)M.invoke(GP.class, x.get(i), y.get(i)));
        return z;
    }
    
    public static final ArrayList<GP> IfThenElseBlock(GP cond, ArrayList<GP> x, ArrayList<GP> y) {
        assert(x.size() == y.size());
        ArrayList<GP> z = new ArrayList<GP>();
        for(int i = 0; i < x.size(); i++) z.add(IfThenElse(cond, x.get(i), y.get(i)));
        return z;
    }
    
    public static final ArrayList<GP> ShiftRight(ArrayList<GP> x, int num) {
        assert(x.size() >= num);
        for(int i = 0; i < num; i++) {x.remove(0); x.add(GP.Const(false));}
        return x;
    }
    
    public static final ArrayList<GP> ShiftLeft(ArrayList<GP> x, int num) {
        assert(x.size() >= num);
        for(int i = 0; i < num; i++) {x.remove(x.size()-1); x.add(0,GP.Const(false));}
        return x;
    }
       
    /*
     * TODO: greater-than, add, subtract, parity, k-of-n threshold, majority, ...
       public static final GP Greater(ArrayList<GP> x, ArrayList<GP> y) {
       }
    */
}
