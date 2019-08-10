package org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors;

import org.apache.commons.lang.StringUtils;
import org.traccar.model.Device;
import org.traccar.model.Position;

public class ProcessingInfo {

    private static final String FUEL_PROCESS_TYPE_ATTR = "processFuel";
    public static final String FINAL_CALIB_FUEL_FIELD_NAME_ATTR = "finalCalib";

    public static final String AVG_FUEL_PROCESS_TYPE = "avg";
    public static final String SUM_FUEL_PROCESS_TYPE = "sum";
    public static final String ISOLATE_FUEL_PROCESS_TYPE = "isolate";
    public static final String DEFAULT_FUEL_PROCESS_TYPE = ISOLATE_FUEL_PROCESS_TYPE;

    private String processingType;
    private String finalCalibFieldName;

    public ProcessingInfo() {
        processingType = DEFAULT_FUEL_PROCESS_TYPE;
        finalCalibFieldName = Position.KEY_CALIBRATED_FUEL_LEVEL;
    }

    public ProcessingInfo(Device device) {
        this();

        if (StringUtils.isNotBlank(device.getString(FUEL_PROCESS_TYPE_ATTR))) {
            this.processingType =  device.getString(FUEL_PROCESS_TYPE_ATTR);
        }

        if (StringUtils.isNotBlank(device.getString(FINAL_CALIB_FUEL_FIELD_NAME_ATTR))) {
            this.finalCalibFieldName = device.getString(FINAL_CALIB_FUEL_FIELD_NAME_ATTR);
        }
    }

    public String getProcessingType() {
        return processingType;
    }

    public String getFinalCalibFieldName() {
        return finalCalibFieldName;
    }
}
