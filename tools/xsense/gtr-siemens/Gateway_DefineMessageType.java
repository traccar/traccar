package org.siemens;

public class Gateway_DefineMessageType {
	 /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\
	  * |-----------------------------------------------------------------| *
	  * | Type | Seq | Size | BoxID | Message+Data |;Extend | CRC16/CCITT | *
	  * |  1   |  1  |	1   |  2    |       N      |    M   |    2        | *
	  * |-----------------------------------------------------------------| *
	 \* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	// ----------------- declare Type ---------------- //
	public  static final byte system_log=97;
	public static final byte update_time_result=100;
	public static final byte ping_reply=102;
	public static final byte ping_reply_enh_io=109;
	public static final byte new_position_gps32_report=110;
	public static final byte tini_batch_online_enh_io=114;
	public static final byte tini_batch_offline_enh_io=115;
	public static final byte location_base_report=116;
	public static final byte gps_report=117;
}