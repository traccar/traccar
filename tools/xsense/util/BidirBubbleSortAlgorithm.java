/*
 * Created on 21 มี.ค. 2548
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.util;

import com.xsense.message.pack.ExtendPosition;
import com.xsense.message.pack.PositionReport;;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class BidirBubbleSortAlgorithm  {
    public  void sort(int a[]) throws Exception {
	int j;
	int limit = a.length;
	int st = -1;
	while (st < limit) {
	    st++;
	    limit--;
	    boolean swapped = false;
	    for (j = st; j < limit; j++) {
		/*if (stopRequested) {
		    return;
		}*/
		if (a[j] > a[j + 1]) {
		    int T = a[j];
		    a[j] = a[j + 1];
		    a[j + 1] = T;
		    swapped = true;
		}
		//pause(st, limit);
		//System.out.println("st:"+st +",Limit:"+ limit);
	    }
	    if (!swapped) {	
		  return ;
	    }
	    else
		swapped = false;
	    for (j = limit; --j >= st;) {
		/*if (stopRequested) {
		    return;
		}*/
		if (a[j] > a[j + 1]) {
		    int T = a[j];
		    a[j] = a[j + 1];
		    a[j + 1] = T;
		    swapped = true;
		}
		//pause(st, limit);
		//System.out.println("st:"+st +",Limit:"+ limit);
	    }
	    if (!swapped) {	 
		  return ;
	    }
	    
	}
	//return a;
	
    }
    public   void sortExt(ExtendPosition mx[]) throws Exception {
    	int j;
    	int limit = mx.length;
    	int st = -1;
    	while (st < limit) {
    	    st++;
    	    limit--;
    	    boolean swapped = false;
    	    for (j = st; j < limit; j++) {
    		/*if (stopRequested) {
    		    return;
    		}*/
    		if (mx[j].getDateTimeInt() > mx[j + 1].getDateTimeInt()) {
    			ExtendPosition T = mx[j];
    		    mx[j] = mx[j + 1];
    		    mx[j + 1] = T;
    		    swapped = true;
    		}
    		//pause(st, limit);
    		//System.out.println("st:"+st +",Limit:"+ limit);
    	    }
    	    if (!swapped) {	
    		  return ;
    	    }
    	    else
    		swapped = false;
    	    for (j = limit; --j >= st;) {
    		/*if (stopRequested) {
    		    return;
    		}*/
    		if (mx[j].getDateTimeInt() > mx[j + 1].getDateTimeInt()) {
    			ExtendPosition T = mx[j];
    		    mx[j] = mx[j + 1];
    		    mx[j + 1] = T;
    		    swapped = true;
    		}
    		//pause(st, limit);
    		//System.out.println("st:"+st +",Limit:"+ limit);
    	    }
    	    if (!swapped) {	 
    		  return ;
    	    }
    	    
    	}
    	//return a;
    	
        }
    
    public   void sortExt(PositionReport mx[]) throws Exception {
    	int j;
    	int limit = mx.length;
    	int st = -1;
    	while (st < limit) {
    	    st++;
    	    limit--;
    	    boolean swapped = false;
    	    for (j = st; j < limit; j++) {
    		/*if (stopRequested) {
    		    return;
    		}*/
    		if (mx[j].getDateTimeInt() > mx[j + 1].getDateTimeInt()) {
    			PositionReport T = mx[j];
    		    mx[j] = mx[j + 1];
    		    mx[j + 1] = T;
    		    swapped = true;
    		}
    		//pause(st, limit);
    		//System.out.println("st:"+st +",Limit:"+ limit);
    	    }
    	    if (!swapped) {	
    		  return ;
    	    }
    	    else
    		swapped = false;
    	    for (j = limit; --j >= st;) {
    		/*if (stopRequested) {
    		    return;
    		}*/
    		if (mx[j].getDateTimeInt() > mx[j + 1].getDateTimeInt()) {
    			PositionReport T = mx[j];
    		    mx[j] = mx[j + 1];
    		    mx[j + 1] = T;
    		    swapped = true;
    		}
    		//pause(st, limit);
    		//System.out.println("st:"+st +",Limit:"+ limit);
    	    }
    	    if (!swapped) {	 
    		  return ;
    	    }
    	    
    	}
    	//return a;
    	
        }
}

