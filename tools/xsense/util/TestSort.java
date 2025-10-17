/*
 * Created on 21 มี.ค. 2548
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.util;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestSort {

	public static void main(String[] args) {
		BidirBubbleSortAlgorithm bisort =new BidirBubbleSortAlgorithm();
		QSortAlgorithm qsort = new QSortAlgorithm();
		int a[] = {13,2,5,7,15,9,8,7,5,0,1};
		try{
		//bisort.sort(a);
		qsort.sort(a);
		for(int k=0;k<a.length;k++) {
    	  	System.out.println(a[k]);
    	  }
		
		}catch (Exception ex){}
		
		
		
	}
}
