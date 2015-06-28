/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.traccar.protocol.*;

public class ServerManager {

    private final List<TrackerServer> serverList = new LinkedList<TrackerServer>();

    public void init() throws Exception {

        initProtocolServer(new Gps103Protocol());
        initProtocolServer(new Tk103Protocol());
        initProtocolServer(new Gl100Protocol());
        initProtocolServer(new Gl200Protocol());
        initProtocolServer(new T55Protocol());
        initProtocolServer(new XexunProtocol());
        initProtocolServer(new TotemProtocol());
        initProtocolServer(new EnforaProtocol());
        initProtocolServer(new MeiligaoProtocol());
        initProtocolServer(new MaxonProtocol());
        initProtocolServer(new SuntechProtocol());
        initProtocolServer(new ProgressProtocol());
        initProtocolServer(new H02Protocol());
        initProtocolServer(new Jt600Protocol());
        initProtocolServer(new Ev603Protocol());
        initProtocolServer(new V680Protocol());
        initProtocolServer(new Pt502Protocol());
        initProtocolServer(new Tr20Protocol());
        initProtocolServer(new NavisProtocol());
        initProtocolServer(new MeitrackProtocol());
        initProtocolServer(new SkypatrolProtocol());
        initProtocolServer(new Gt02Protocol());
        initProtocolServer(new Gt06Protocol());
        initProtocolServer(new MegastekProtocol());
        initProtocolServer(new NavigilProtocol());
        initProtocolServer(new GpsGateProtocol());
        initProtocolServer(new TeltonikaProtocol());
        initProtocolServer(new Mta6Protocol());
        initProtocolServer(new Tlt2hProtocol());
        initProtocolServer(new SyrusProtocol());
        initProtocolServer(new WondexProtocol());
        initProtocolServer(new CellocatorProtocol());
        initProtocolServer(new GalileoProtocol());
        initProtocolServer(new YwtProtocol());
        initProtocolServer(new Tk102Protocol());
        initProtocolServer(new IntellitracProtocol());
        initProtocolServer(new Xt7Protocol());
        initProtocolServer(new WialonProtocol());
        initProtocolServer(new CarscopProtocol());
        initProtocolServer(new ApelProtocol());
        initProtocolServer(new ManPowerProtocol());
        initProtocolServer(new GlobalSatProtocol());
        initProtocolServer(new AtrackProtocol());
        initProtocolServer(new Pt3000Protocol());
        initProtocolServer(new RuptelaProtocol());
        initProtocolServer(new TopflytechProtocol());
        initProtocolServer(new LaipacProtocol());
        initProtocolServer(new AplicomProtocol());
        initProtocolServer(new GotopProtocol());
        initProtocolServer(new SanavProtocol());
        initProtocolServer(new GatorProtocol());
        initProtocolServer(new NoranProtocol());
        initProtocolServer(new M2mProtocol());
        initProtocolServer(new OsmAndProtocol());
        initProtocolServer(new EasyTrackProtocol());
        initProtocolServer(new TaipProtocol());
        initProtocolServer(new KhdProtocol());
        initProtocolServer(new PiligrimProtocol());
        initProtocolServer(new Stl060Protocol());
        initProtocolServer(new CarTrackProtocol());
        initProtocolServer(new MiniFinderProtocol());
        initProtocolServer(new HaicomProtocol());
        initProtocolServer(new EelinkProtocol());
        initProtocolServer(new BoxProtocol());
        initProtocolServer(new FreedomProtocol());
        initProtocolServer(new TelikProtocol());
        initProtocolServer(new TrackboxProtocol());
        initProtocolServer(new VisiontekProtocol());
        initProtocolServer(new OrionProtocol());
        initProtocolServer(new RitiProtocol());
        initProtocolServer(new UlbotechProtocol());
        initProtocolServer(new TramigoProtocol());
        initProtocolServer(new Tr900Protocol());
        initProtocolServer(new Ardi01Protocol());
        initProtocolServer(new Xt013Protocol());
        initProtocolServer(new AutoFonProtocol());
        initProtocolServer(new GoSafeProtocol());
        initProtocolServer(new AutoFon45Protocol());
        initProtocolServer(new BceProtocol());
        initProtocolServer(new XirgoProtocol());
        initProtocolServer(new CalAmpProtocol());
        initProtocolServer(new MtxProtocol());
        initProtocolServer(new TytanProtocol());
        initProtocolServer(new Avl301Protocol());
        initProtocolServer(new CastelProtocol());
        initProtocolServer(new MxtProtocol());

        initProtocolDetector();
    }

    public void start() {
        for (Object server: serverList) {
            ((TrackerServer) server).start();
        }
    }

    public void stop() {
        for (Object server: serverList) {
            ((TrackerServer) server).stop();
        }

        // Release resources
        GlobalChannelFactory.release();
        GlobalTimer.release();
    }

    private boolean isProtocolEnabled(String protocol) {
        return Context.getProps().containsKey(protocol + ".port");
    }

    private void initProtocolDetector() throws SQLException {
        String protocol = "detector";
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("detectorHandler", new DetectorHandler(serverList));
                }
            });
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("detectorHandler", new DetectorHandler(serverList));
                }
            });
        }
    }

    private void initProtocolServer(final Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            protocol.initTrackerServers(serverList);
        }
    }

}
