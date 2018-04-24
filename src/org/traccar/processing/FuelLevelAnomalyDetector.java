package org.traccar.processing;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.algorithms.*;
import org.numenta.nupic.network.Network;
import org.numenta.nupic.network.PublisherSupplier;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.util.Tuple;

import java.util.HashMap;
import java.util.Map;

import static org.numenta.nupic.Parameters.KEY;

public class FuelLevelAnomalyDetector {

    public void setupNetwork() {
        PublisherSupplier manual = PublisherSupplier.builder()
                                                    .addHeader("timestamp,consumption")
                                                    .addHeader("datetime,double")
                                                    .addHeader("T,B") //see SensorFlags.java for more info
                                                    .build();

        Parameters p = getNetworkDemoTestEncoderParams();


        Network network = Network.create("Network API Demo", p)         // Name the Network whatever you wish...
                                 .add(Network.createRegion("Region 1")                       // Name the Region whatever you wish...
                                             .add(Network.createLayer("Layer 2/3", p)                // Name the Layer whatever you wish...
                                                         .alterParameter(Parameters.KEY.AUTO_CLASSIFY, Boolean.TRUE)    // (Optional) Add a CLAClassifier
                                                         .add(Anomaly.create())                              // (Optional) Add an Anomaly detector
                                                         .add(new TemporalMemory())                          // Core Component but also it's "optional"
                                                         .add(new SpatialPooler())                           // Core Component, but also "optional"
                                                         .add(Sensor.create(ObservableSensor::create, SensorParams.create(
                                                                 SensorParams.Keys::obs, "", manual)))));

        network.start();
    }

    public static Parameters getNetworkDemoTestEncoderParams() {
        Map<String, Map<String, Object>> fieldEncodings = getNetworkDemoFieldEncodingMap();

        Parameters p = Parameters.getEncoderDefaultParameters();
        p.set(KEY.GLOBAL_INHIBITION, true);
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        p.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 40.0);
        p.set(KEY.POTENTIAL_PCT, 0.8);
        p.set(KEY.SYN_PERM_CONNECTED,0.1);
        p.set(KEY.SYN_PERM_ACTIVE_INC, 0.0001);
        p.set(KEY.SYN_PERM_INACTIVE_DEC, 0.0005);
        p.set(KEY.MAX_BOOST, 1.0);
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", SDRClassifier.class));

        p.set(KEY.MAX_NEW_SYNAPSE_COUNT, 20);
        p.set(KEY.INITIAL_PERMANENCE, 0.21);
        p.set(KEY.PERMANENCE_INCREMENT, 0.1);
        p.set(KEY.PERMANENCE_DECREMENT, 0.1);
        p.set(KEY.MIN_THRESHOLD, 9);
        p.set(KEY.ACTIVATION_THRESHOLD, 12);

        p.set(KEY.CLIP_INPUT, true);
        p.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);

        return p;
    }

    public static Map<String, Class<? extends Classifier>> getInferredFieldsMap(
            String field, Class<? extends Classifier> classifier) {
        Map<String, Class<? extends Classifier>> inferredFieldsMap = new HashMap<>();
        inferredFieldsMap.put(field, classifier);
        return inferredFieldsMap;
    }

    public static Map<String, Map<String, Object>> getNetworkDemoFieldEncodingMap() {
        Map<String, Map<String, Object>> fieldEncodings = setupMap(
                null,
                0, // n
                0, // w
                0, 0, 0, 0, null, null, null,
                "timestamp", "datetime", "DateEncoder");
        fieldEncodings = setupMap(
                fieldEncodings,
                50,
                21,
                0, 100, 0, 0.1, null, Boolean.TRUE, null,
                "consumption", "float", "ScalarEncoder");

        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_TOFD.getFieldName(), new Tuple(21,9.5)); // Time of day
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_PATTERN.getFieldName(), "MM/dd/YY HH:mm");

        return fieldEncodings;
    }

    public static Map<String, Map<String, Object>> setupMap(
            Map<String, Map<String, Object>> map,
            int n, int w, double min, double max, double radius, double resolution, Boolean periodic,
            Boolean clip, Boolean forced, String fieldName, String fieldType, String encoderType) {

        if(map == null) {
            map = new HashMap<String, Map<String, Object>>();
        }
        Map<String, Object> inner = null;
        if((inner = map.get(fieldName)) == null) {
            map.put(fieldName, inner = new HashMap<String, Object>());
        }

        inner.put("n", n);
        inner.put("w", w);
        inner.put("minVal", min);
        inner.put("maxVal", max);
        inner.put("radius", radius);
        inner.put("resolution", resolution);

        if(periodic != null) inner.put("periodic", periodic);
        if(clip != null) inner.put("clipInput", clip);
        if(forced != null) inner.put("forced", forced);
        if(fieldName != null) inner.put("fieldName", fieldName);
        if(fieldType != null) inner.put("fieldType", fieldType);
        if(encoderType != null) inner.put("encoderType", encoderType);

        return map;
    }
}
