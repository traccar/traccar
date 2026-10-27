/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

/**
 * A vehicle or driver of a tenant's vendor as OptiDeli reports it. It waits here until a device (registration number)
 * or a driver (licence number) with the same key exists, and is applied to it as soon as one does.
 */
@StorageName("tc_optinexus_vendor_records")
public class OptinexusVendorRecord extends BaseModel {

    public static final String VEHICLE = "VEHICLE";
    public static final String DRIVER = "DRIVER";

    private String tenantId;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    private String kind;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    private String recordKey;

    public String getRecordKey() {
        return recordKey;
    }

    public void setRecordKey(String recordKey) {
        this.recordKey = recordKey;
    }

    private long version;

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    private String payload;

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    private Date updatedAt = new Date();

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

}
