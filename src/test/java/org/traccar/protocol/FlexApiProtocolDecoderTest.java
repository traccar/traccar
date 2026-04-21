package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class FlexApiProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new FlexApiProtocolDecoder(null));

        verifyAttributes(decoder, text(
                "${\"topic\":\"v1/VF3102021113001/motion/info\",\"payload\":{\"motion.ts\":1641885877,\"motion.ax\":0.006344,\"motion.ay\":0.289384,\"motion.az\":-0.939156,\"motion.gx\":0.420000,\"motion.gy\":0.420000,\"motion.gz\":-0.280000}}xx"));

        verifyAttributes(decoder, text(
                "${\"topic\":\"v1/VF3102021113001/gnss/info\",\"payload\":{\"gnss.ts\":1639713510,\"gnss.latitude\":30.587509,\"gnss.longitude\":104.053650,\"gnss.altitude\":391,\"gnss.speed\":0,\"gnss.heading\":0,\"gnss.hdop\":1.100000,\"gnss.fix\":4,\"gnss.num_sv\":10}}xx"));

        verifyAttributes(decoder, text(
                "${\"topic\":\"v1/VF3102021113001/cellular1/info\",\"payload\":{\"modem1.ts\":1639713510,\"modem1.imei\":\"863674047326655\",\"modem1.imsi\":\"\",\"modem1.iccid\":\"\",\"modem1.phone_num\":\"\",\"modem1.signal_lvl\":0,\"modem1.reg_status\":0,\"modem1.operator\":\"\",\"modem1.network\":0,\"modem1.lac\":\"\",\"modem1.cell_id\":\"\",\"modem1.rssi\":0,\"modem1.rsrp\":0,\"modem1.rsrq\":0,\"cellular1.status\":2,\"cellular1.ip\":\"0.0.0.0\",\"cellular1.netmask\":\"255.255.255.255\",\"cellular1.gateway\":\"0.0.0.0\",\"cellular1.dns1\":\"0.0.0.0\",\"cellular1.up_at\":602}}xx"));

        verifyAttributes(decoder, text(
                "${\"topic\":\"v1/VF3102029000003/obd/info\",\"payload\":{\"obd.ts\":1639037377,\"obd.speed\":211,\"obd.f_lvl\":50.196079,\"obd.mil\":0,\"obd.dtcs\":0,\"obd.rpm\":14531.250000,\"obd.e_load\":50.980392,\"obd.c_temp\":118,\"obd.o_temp\":56,\"obd.a_temp\":-40,\"obd.f_press\":48,\"obd.t_pos\":51.764706,\"obd.b_volt\":13.782000,\"obd.up_time\":2265,\"obd.m_dist\":4643,\"obd.m_time\":257,\"obd.d_dist\":200,\"obd.d_time\":771,\"obd.vin\":\"LFV3B28R8A3025310\",\"obd.f_rate\":10,\"obd.t_dist\":4843,\"obd.b_press\":101,\"obd.f_r_press\":48,\"obd.i_temp\":37,\"obd.i_press\":32,\"obd.r_torque\":4128,\"obd.a_torque\":2,\"obd.mf_mon\":1,\"obd.f_s_mon\":0,\"obd.c_c_mon\":0,\"obd.c_mon\":1,\"obd.e_s_mon\":1,\"obd.e_v_s_mon\":1,\"obd.o_s_mon\":1,\"obd.o_s_h_mon\":1}}xx"));

        verifyAttributes(decoder, text(
                "${\"topic\":\"v1/VF3102021111601/obd/info\",\"payload\":{\"obd.ts\":1637225390,\"obd.speed\":0,\"obd.f_lvl\":null,\"obd.odo\":0,\"obd.e_hours\":0,\"obd.ab_level\":null,\"obd.mil\":0,\"obd.dtcs\":null,\"obd.rpm\":0,\"obd.e_load\":null,\"obd.c_temp\":-40,\"obd.o_temp\":-273,\"obd.a_temp\":null,\"obd.f_press\":null,\"obd.t_pos\":0,\"obd.b_volt\":null,\"obd.up_time\":null,\"obd.m_dist\":null,\"obd.m_time\":null,\"obd.d_dist\":null,\"obd.d_time\":null,\"obd.vin\":\"NLVIN123456789ABC\",\"obd.brake\":0,\"obd.parking\":0,\"obd.s_w_angle\":null,\"obd.f_rate\":0,\"obd.f_econ\":0,\"obd.a_pos\":null,\"obd.t_dist\":0,\"obd.b_press\":null,\"obd.f_r_press\":null,\"obd.i_temp\":null,\"obd.i_press\":null,\"obd.r_torque\":null,\"obd.f_torque\":null,\"obd.max_avl_torque\":null,\"obd.a_torque\":-125,\"obd.d_e_f_vol\":null,\"obd.mf_mon\":null,\"obd.f_s_mon\":null,\"obd.c_c_mon\":null,\"obd.c_mon\":null,\"obd.h_c_mon\":null,\"obd.e_s_mon\":null,\"obd.s_a_s_mon\":null,\"obd.a_s_r_mon\":null,\"obd.e_g_s_mon\":null,\"obd.e_g_s_h_mon\":null,\"obd.e_v_s_mon\":null,\"obd.c_s_a_s_mon\":null,\"obd.b_p_c_s_mon\":null,\"obd.dpf_mon\":null,\"obd.n_c_mon\":null,\"obd.nmhc_mon\":null,\"obd.o_s_mon\":null,\"obd.o_s_h_mon\":null,\"obd.pf_mon\":null}}xx"));

        verifyPosition(decoder, text(
                "${\"topic\":\"v1/VF3102021111601/gnss/info\",\"payload\":{\"time\":1637225390,\"lat\":30.587942,\"log\":104.053543,\"gnss.altitude\":480.399994,\"gnss.speed\":0,\"gnss.heading\":0,\"gnss.hdop\":0.900000,\"gnss.fix\":4,\"gnss.num_sv\":11}}xx"));

        verifyAttributes(decoder, text(
                "${\"topic\":\"v1/VF3102021111601/motion/info\",\"payload\":{\"motion.ts\":1637225450,\"motion.ax\":0.009272,\"motion.ay\":0.278404,\"motion.az\":-0.941596,\"motion.gx\":0.420000,\"motion.gy\":-0.490000,\"motion.gz\":0.140000}}xx"));

        verifyAttributes(decoder, text(
                "${\"topic\":\"v1/VF3102021111601/sysinfo/info\",\"payload\":{\"sysinfo.ts\":1637224740,\"sysinfo.model_name\":\"310\",\"sysinfo.oem_name\":\"inhand\",\"sysinfo.serial_number\":\"VF3102021111601\",\"sysinfo.firmware_version\":\"VT3_V1.1.32\",\"sysinfo.product_number\":\"FQ58\",\"sysinfo.description\":\"www.inhand.com.cn\"}}xx"));

        verifyAttributes(decoder, text(
                "${\"topic\":\"v1/VF3102021111601/io/info\",\"payload\":{\"io.ts\":1637227722,\"io.AI1\":0,\"io.DI1\":1,\"io.DI2\":0,\"io.DI3\":0,\"io.DI4\":0,\"io.DI1_pullup\":0,\"io.DI2_pullup\":0,\"io.DI3_pullup\":0,\"io.DI4_pullup\":0,\"io.DO1\":0,\"io.DO2\":0,\"io.DO3\":0,\"io.IGT\":1}}xx"));

        verifyAttributes(decoder, text(
                "${\"topic\":\"v1/VF3102021111601/cellular1/info\",\"payload\":{\"modem1.ts\":1637225330,\"modem1.imei\":\"863674047324999\",\"modem1.imsi\":\"460111150414721\",\"modem1.iccid\":\"89860319482086580401\",\"modem1.phone_num\":\"\",\"modem1.signal_lvl\":25,\"modem1.reg_status\":1,\"modem1.operator\":\"46011\",\"modem1.network\":3,\"modem1.lac\":\"EA00\",\"modem1.cell_id\":\"E779B81\",\"modem1.rssi\":0,\"modem1.rsrp\":0,\"modem1.rsrq\":0,\"cellular1.status\":3,\"cellular1.ip\":\"10.136.143.193\",\"cellular1.netmask\":\"255.255.255.255\",\"cellular1.gateway\":\"10.64.64.64\",\"cellular1.dns1\":\"223.5.5.5\",\"cellular1.up_at\":450}}xx"));

    }

}
