package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class H02ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new H02ProtocolDecoder(null));

        verifyPosition(decoder, buffer(
                "*HQ,5905101893,V1,105759,A,37573392,S,145037022,E,000.00,173,280122,FF7FFBFF,,,9059e2c,8232,4#"));

        verifyPosition(decoder, buffer(
                "*HQ,4970105243,V1,104000,A,2235.1777,N,11357.8913,E,000.27,235,130721,FFFFFBFF,460,11,d18e105,7752,6#"));

        verifyAttribute(decoder, buffer(
                "*HQ,135790246811220,HTBT,100#"),
                Position.KEY_BATTERY_LEVEL, 100);

        verifyPosition(decoder, buffer(
                "*HQ,9180271064,V5,091233,V,2348.8912,N,09021.3302,E,000.00,000,051219,FFFFBBFF,470,01,21019,2033,2921283#"));

        verifyPosition(decoder, binary(
                "2491802711800850240512192350143206090249758e000001ffffbbff00bdf0900000000001d60161cc4b9a35"));

        verifyNull(decoder, buffer(
                "*HQ,135790246811220,HTBT#"));

        verifyPosition(decoder, buffer(
                "*HQ,865205035331981,V1,132926,A,1935.3933,N,07920.4134,E,  3.34,342,280519,FFFFFFFF#"));

        verifyPosition(decoder, binary(
                "24702802061601234020031910125482600612695044000000ffffbbff000000000000000001760d04e2c9934d"));

        verifyNotNull(decoder, buffer(
                "*hq,356327081001239,VP1,V,470,002,92,3565,0Y92,19433,30Y92,1340,29#"));

        verifyPosition(decoder, binary(
                "2435248308419329301047591808172627335900074412294E024138FEFFFFFFFF01120064BA73005ECC"));

        verifyPosition(decoder, buffer(
                "*HQ,4210209006,V1,054048,A,2828.2297,N,07733.4332,E,000.5,047,4,080918,EEE7FBDF,4261193,0#"));

        verifyPosition(decoder, buffer(
                "*HQ,353505221264507,V2,100220,0,5238.26259,N,00507.33983,E,0.25,0,280917,FFFFFFFF,cc,28,  db,d75b#"));

        verifyPosition(decoder, buffer(
                "*HQ,,V1,173212,A,2225.78879,S,02829.19021,E,0.00,0,290418,FFFFFBFF#"));

        verifyPosition(decoder, buffer(
                "*HQ,353505221264507,VI1,075146,0,5238.25900,N,00507.33429,E,0.54,0,250917,FFFFFFFF,cc,28,  db,d75b#"));

        verifyNull(decoder, buffer(
                "*HQ,353505510948929,V1,,V,,N,,E,0.00,0,,FFFFF7FF,f0,a,11a0,c0c6#"));

        verifyPosition(decoder, buffer(
                "*hq,356327080425330,VP1,A,2702.7245,S,15251.9311,E,0.48,0.0000,080917#"));

        verifyPosition(decoder, buffer(
                "*HQ,4209951296,V19,214452,A,5201.0178,N,01830.5029,E,000.00,000,200417,,195.63.13.195,89480610500392633029,BFFFFBFF#"));

        verifyPosition(decoder, buffer(
                "*hq,356327080425330,VP1,A,2702.7215,S,15251.9309,E,0.62,0.0000,050917#"));

        verifyNull(decoder, buffer(
                "*HQ,356327080425330,XT,1,100#"));

        verifyAttributes(decoder, buffer(
                "*HQ,353111080001055,V3,044855,28403,01,001450,011473,158,-62,0292,0,X,030817,FFFFFBFF#"));

        verifyPosition(decoder, binary(
                "2442091341332059572807175137358006000183640e000000fffffbfdff001f080000000000ea1e0000000021"));

        verifyNull(decoder, buffer(
                "*HQ,4109198974,#"));

        verifyAttributes(decoder, buffer(
                "*HQ,1700086468,LINK,180902,15,0,84,0,0,240517,FFFFFBFF#"));

        verifyAttributes(decoder, buffer(
                "*HQ,355488020882405,V3,095426,74001,01,010278,045142,128,-92,02DE,0,X,090517,FFFFFBFF#"));

        verifyAttributes(decoder, buffer(
                "*HQ,355488020882405,V3,095426,74001,04,010278,045142,128,-92,010278,026311,125,,010278,026582,125,,010278,028322,119,,02DD,0,X,090517,FFFFFBFF#"));

        verifyPosition(decoder, buffer(
                "*HQ,4109179024,V19,181519,V,3853.2587,S,06205.9175,W,000.00,000,090217,,5492932630888,8954315265044716555?,FFFFFBFF#"));

        verifyPosition(decoder, buffer(
                "*HQ,8161289587,V1,181933,A,5444.3994,N,02516.3844,E,000.05,000,090317,FFFFBBFF,246,03,00002,41565#"));

        verifyPosition(decoder, binary(
                "2421305109380127171003170520046500100286297e003085ffffdfffff03440069129344006400001151415a20"));

        verifyPosition(decoder, buffer(
                "*HQ,2130510938,V1,012632,A,0520.0663,N,10028.6324,E,0.286,023,100317,FFFFDFFF,69129336,0,100.0,18,5141,5A20#"));

        verifyPosition(decoder, buffer(
                "*HQ,4210209006,V1,201812,A,2608.9437,N,08016.2521,W,000.80,000,150317,EFE7F9FF,310,260,0,0,6#"));

        verifyPosition(decoder, buffer(
                "*HQ,4210209006,V1,201844,A,2608.9437,N,08016.2521,W,000.80,000,150317,FFFFF9FF,310,260,0,0,6#"));

        verifyPosition(decoder, buffer(
                "*HQ,4109179024,V19,103732,V,3853.2770,S,06205.8678,W,000.00,000,100217,,5492932630888,8954314165044716555?,FFFFFBFF#"));

        verifyAttributes(decoder, buffer(
                "*HQ,4109179024,NBR,103732,722,310,0,6,8106,32010,23,8101,22007,25,8106,12010,23,8106,22105,22,8101,22012,16,8106,42010,5,100217,FFFFFBFF,5#"));

        verifyAttributes(decoder, buffer(
                "*HQ,355488020930796,V3,002339,62160,06,024852,035421,148,0,024852,035425,143,,022251,036482,137,,024852,000335,133,,024852,031751,133,,024852,035423,133,,02A1,0,X,010104,EFE7FBFF#"));

        verifyPosition(decoder, buffer(
                "*HQ,4106012736,V1,224434,A,1827.3855,N,06705.7577,W,000.00,000,100117,FFFFFBFF,310,260,49101,1753,5#"));

        verifyAttributes(decoder, buffer(
                "*HQ,4208150188,NBR,210249,260,6,0,7,1014,50675,37,1014,50633,27,1014,17933,18,1014,17231,15,1014,50632,12,1014,13211,11,1014,17031,10,281216,FFFFFBFF,2#"));
        
        verifyAttributes(decoder, buffer(
                "*HQ,7401004662,NBR,160453,234,10,0,2,21580,27766,-67,21580,27766,-67,291019,FFFFFBFF,6#"));

        verifyAttributes(decoder, buffer(
                "*HQ,1600068812,NBR,141335,262,02,255,6,431,17003,26,431,11101,13,431,6353,13,431,22172,13,431,11093,13,431,60861,10,151216,FFFFFBFF#"));

        verifyPosition(decoder, buffer(
                "*HQ,353588020068342,V1,084436,A,3257.01525,N,00655.03865,W,57.78,40,011216,FFFBFFFF,25c,a, 154,b04c#"));

        verifyNull(decoder, buffer(
                "*HQ,356803210091319,BS,,2d4,a,1b63,1969,26,1b63,10b2,31,0,0,25,,ffffffff,60#"));

        verifyAttributes(decoder, buffer(
                "*HQ,1400046168,NBR,160169,460,0,1,4,9338,3692,150,9338,3691,145,9338,3690,140,9338,3692,139,180813,FFFFFBFF#"));

        verifyAttributes(decoder, buffer(
                "*HQ,1600068860,NBR,120156,262,03,255,6,802,54702,46,802,5032,37,802,54782,30,802,5052,28,802,54712,12,802,5042,12,081116,FFFFFBFF#"));

        verifyAttributes(decoder, buffer(
                "*HQ,1600068860,NBR,110326,262,03,255,6,802,23152,23,812,49449,14,802,35382,13,802,35402,11,812,56622,09,802,23132,04,081116,FFFFFBFF#"));

        verifyAttributes(decoder, buffer(
                "*HQ,1600068860,LINK,112137,20,8,67,0,0,081116,FFFFFBFF#"));

        verifyAttributes(decoder, buffer(
                "*HQ,355488020533263,V3,121536,65501,04,000152,014001,156,-64,000161,010642,138,,000152,014003,129,,000152,013973,126,,02E4,0,X,071116,FFFFFBFF#"));

        verifyPosition(decoder, buffer(
                "*HQ,4209917484,V19,093043,V,5052.9749,N,00426.4322,E,000.00,000,130916,,0032475874141,8944538530000543700F,FFFFFBFF#"));

        verifyPosition(decoder, buffer(
                "*HQ,353505220873067,V1,,V,4605.75732,N,01430.73863,E,0.00,0,,FFFFFFEF,125,194,  64,d3#"));

        verifyPosition(decoder, buffer(
                "*HQ,4210051415,V1,164549,A,0956.3869,N,08406.7068,W,000.00,000,221215,FFFFFBFF,712,01,0,0,6#"),
                position("2015-12-22 16:45:49.000", true, 9.93978, -84.11178));

        verifyAttributes(decoder, buffer(
                "*HQ,1451316451,NBR,112315,724,10,2,2,215,2135,123,215,2131,121,011215,FFFFFFFF#"));

        verifyPosition(decoder, buffer(
                "*HQ,1451316485,V1,121557,A,-23-3.3408,S,-48-2.8926,W,0.1,158,241115,FFFFFFFF#"));

        verifyPosition(decoder, buffer(
                "*HQ,1451316485,V1,121557,A,-23-35.3408,S,-48-2.8926,W,0.1,158,241115,FFFFFFFF#"));
        
        verifyPosition(decoder, buffer(
                "*HQ,355488020119695,V1,050418,,2827.61232,N,07703.84822,E,0.00,0,031015,FFFEFBFF#"),
                position("2015-10-03 05:04:18.000", false, 28.46021, 77.06414));

        verifyPosition(decoder, buffer(
                "*HQ,1451316409,V1,030149,A,-23-29.0095,S,-46-51.5852,W,2.4,065,070315,FFFFFFFF#"),
                position("2015-03-07 03:01:49.000", true, -23.48349, -46.85975));

        verifyNull(decoder, buffer(
                "*HQ,353588020068342,V1,000000,V,0.0000,0,0.0000,0,0.00,0.00,000000,ffffffff,000106,000002,000203,004c87,16#"));

        verifyPosition(decoder, buffer(
                "*HQ,3800008786,V1,062507,V,3048.2437,N,03058.5617,E,000.00,000,250413,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*HQ,4300256455,V1,111817,A,1935.5128,N,04656.3243,E,0.00,100,170913,FFE7FBFF#"));

        verifyPosition(decoder, buffer(
                "*HQ,123456789012345,V1,155850,A,5214.5346,N,2117.4683,E,0.00,270.90,131012,ffffffff,000000,000000,000000,000000#"));
        
        verifyPosition(decoder, buffer(
                "*HQ,353588010001689,V1,221116,A,1548.8220,S,4753.1679,W,0.00,0.00,300413,ffffffff,0002d4,000004,0001cd,000047#"));

        verifyPosition(decoder, buffer(
                "*HQ,354188045498669,V1,195200,A,701.8915,S,3450.3399,W,0.00,205.70,050213,ffffffff,000243,000000,000000#"));
        
        verifyPosition(decoder, buffer(
                "*HQ,2705171109,V1,213324,A,5002.5849,N,01433.7822,E,0.00,000,140613,FFFFFFFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V1,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#\r\n"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,S17,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,S14,100,10,1,3,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,S20,ERROR,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,S20,DONE,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,F7FFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,R8,ERROR,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,S23,165.165.33.250:8800,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,S24,thit.gd,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,S1,OK,pass_word,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFD#"));
        
        verifyPosition(decoder, buffer(
                "*HQ,353588020068342,V1,062840,A,5241.1249,N,954.9490,E,0.00,0.00,231013,ffffffff,000106,000002,000203,004c87,24#"));

        verifyPosition(decoder, buffer(
                "*HQ,353505220903211,V1,075228,A,5227.5039,N,01032.8443,E,0.00,0,231013,FFFBFFFF,106,14, 201,2173#"));

        verifyPosition(decoder, buffer(
                "*HQ,353505220903211,V1,140817,A,5239.3538,N,01003.5292,E,21.03,312,221013,FFFBFFFF,106,14, 203,1cd#"));
        
        verifyPosition(decoder, buffer(
                "*HQ,356823035368767,V1,083618,A,0955.6392,N,07809.0796,E,0.00,0,070414,FFFBFFFF,194,3b5,  71,c9a9#"));

        verifyNull(decoder, buffer(
                "*HQ,8401016597,BASE,152609,0,0,0,0,211014,FFFFFFFF#"));

        verifyPosition(decoder, binary(
                "2441060116601245431311165035313006004318210e000000fffffbffff0024"));

        verifyPosition(decoder, binary(
                "24410600082621532131081504419390060740418306000000fffffbfdff0015060000002c02dc0c000000001f"),
                position("2015-08-31 21:53:21.000", true, 4.69898, -74.06971));

        verifyPosition(decoder, binary(
                "2427051711092133391406135002584900014337822e000000ffffffffff0000"));

        verifyPosition(decoder, binary(
                "2427051711092134091406135002584900014337822e000000ffffffffff0000"));

        verifyPosition(decoder, binary(
                "2410307310010503162209022212874500113466574C014028fffffbffff0000"));

        verifyPosition(decoder, binary(
                "2441090013450831250401145047888000008554650e000000fffff9ffff001006000000000106020299109c01"));

        verifyPosition(decoder, binary(
                "24270517030820321418041423307879000463213792000056fffff9ffff0000"));

        verifyPosition(decoder, binary(
                "2441091144271222470112142233983006114026520E000000FFFFFBFFFF0014060000000001CC00262B0F170A"));
        
        verifyPosition(decoder, binary(
                "24971305007205201916101533335008000073206976000000effffbffff000252776566060000000000000000000049"));

    }

    @Test
    public void testDecodeStatus() throws Exception {

        var decoder = inject(new H02ProtocolDecoder(null));

        verifyAttribute(decoder, buffer(
                "*HQ,2705171109,V1,213324,A,5002.5849,N,01433.7822,E,0.00,000,140613,FFFFFFFF#"),
                Position.KEY_STATUS, 0xFFFFFFFFL);

        verifyAttribute(decoder, binary(
                "2441091144271222470112142233983006114026520E000000FFFFFBFFFF0014060000000001CC00262B0F170A"),
                Position.KEY_STATUS, 0xFFFFFBFFL);

        verifyAttribute(decoder, buffer(
                "*HQ,4210051415,V1,164549,A,0956.3869,N,08406.7068,W,000.00,000,221215,FFFFFBFF,712,01,0,0,6#"),
                Position.KEY_STATUS, 0xFFFFFBFFL);

    }

}
