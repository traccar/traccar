package org.siemens;

public class Gateway_test_function {
	
	public static void main(String[] args) throws Exception {
		String base_station = "3E38393636303130383036303733343330333637000000000032000000000000";
		String fbase_station = (Gateway_HexConvert.hex2string(base_station)).trim();
		System.out.println(base_station);
		System.out.println(fbase_station);
	}
}
