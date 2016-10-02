/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.location;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.traccar.Config;
import org.traccar.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CellInfo {

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class Cell {

        private int mcc;
        private int mnc;
        private int lac;
        private int cid;
        private int signal;

        public Cell() {
        }

        public Cell(int mcc, int mnc, int lac, int cid, int signal) {
            this.mcc = mcc;
            this.mnc = mnc;
            this.lac = lac;
            this.cid = cid;
            this.signal = signal;
        }

        public int getMcc() {
            return mcc;
        }

        public void setMcc(int mcc) {
            this.mcc = mcc;
        }

        public int getMnc() {
            return mnc;
        }

        public void setMnc(int mnc) {
            this.mnc = mnc;
        }

        public int getLac() {
            return lac;
        }

        public void setLac(int lac) {
            this.lac = lac;
        }

        public int getCid() {
            return cid;
        }

        public void setCid(int cid) {
            this.cid = cid;
        }

        public int getSignal() {
            return signal;
        }

        public void setSignal(int signal) {
            this.signal = signal;
        }
    }

    public CellInfo() {
    }

    public CellInfo(Collection<Cell> cells) {
        this.cells.addAll(cells);
    }

    private List<Cell> cells = new ArrayList<>();

    public List<Cell> getCells() {
        return cells;
    }

    public void addCell(int lac, int cid) {
        Config config = Context.getConfig();
        if (config.hasKey("location.mcc") && config.hasKey("location.mnc")) {
            int mcc = config.getInteger("location.mcc");
            int mnc = config.getInteger("location.mnc");
            cells.add(new Cell(mcc, mnc, lac, cid, 0));
        }
    }

    public void addCell(int mcc, int mnc, int lac, int cid) {
        cells.add(new Cell(mcc, mnc, lac, cid, 0));
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(cells);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static CellInfo fromString(String json) {
        try {
            return new CellInfo(Arrays.asList(new ObjectMapper().readValue(json, Cell[].class)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
