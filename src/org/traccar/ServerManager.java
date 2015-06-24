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

import java.nio.ByteOrder;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.FixedLengthFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LineBasedFrameDecoder;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.traccar.protocol.*;


public class ServerManager {

    private final List<TrackerServer> serverList = new LinkedList<TrackerServer>();

    public void init() throws Exception {


        initGps103Server(new Gps103Protocol());
        initTk103Server(new Tk103Protocol());
        initGl100Server(new Gl100Protocol());
        initGl200Server(new Gl200Protocol());
        initT55Server(new T55Protocol());
        initXexunServer(new XexunProtocol());
        initTotemServer(new TotemProtocol());
        initEnforaServer(new EnforaProtocol());
        initMeiligaoServer(new MeiligaoProtocol());
        initMaxonServer(new MaxonProtocol());
        initSuntechServer(new SuntechProtocol());
        initProgressServer(new ProgressProtocol());
        initH02Server(new H02Protocol());
        initJt600Server(new Jt600Protocol());
        initEv603Server(new Ev603Protocol());
        initV680Server(new V680Protocol());
        initPt502Server(new Pt502Protocol());
        initTr20Server(new Tr20Protocol());
        initNavisServer(new NavisProtocol());
        initMeitrackServer(new MeitrackProtocol());
        initSkypatrolServer(new SkypatrolProtocol());
        initGt02Server(new Gt02Protocol());
        initGt06Server(new Gt06Protocol());
        initMegastekServer(new MegastekProtocol());
        initNavigilServer(new NavigilProtocol());
        initGpsGateServer(new GpsGateProtocol());
        initTeltonikaServer(new TeltonikaProtocol());
        initMta6Server(new Mta6Protocol());
        initMta6CanServer(new Mta6canProtocol());
        initTlt2hServer(new Tlt2hProtocol());
        initSyrusServer(new SyrusProtocol());
        initWondexServer(new WondexProtocol());
        initCellocatorServer(new CellocatorProtocol());
        initGalileoServer(new GalileoProtocol());
        initYwtServer(new YwtProtocol());
        initTk102Server(new Tk102Protocol());
        initIntellitracServer(new IntellitracProtocol());
        initXt7Server(new Xt7Protocol());
        initWialonServer(new WialonProtocol());
        initCarscopServer(new CarscopProtocol());
        initApelServer(new ApelProtocol());
        initManPowerServer(new ManPowerProtocol());
        initGlobalSatServer(new GlobalSatProtocol());
        initAtrackServer(new AtrackProtocol());
        initPt3000Server(new Pt3000Protocol());
        initRuptelaServer(new RuptelaProtocol());
        initTopflytechServer(new TopflytechProtocol());
        initLaipacServer(new LaipacProtocol());
        initAplicomServer(new AplicomProtocol());
        initGotopServer(new GotopProtocol());
        initSanavServer(new SanavProtocol());
        initGatorServer(new GatorProtocol());
        initNoranServer(new NoranProtocol());
        initM2mServer(new M2mProtocol());
        initOsmAndServer(new OsmAndProtocol());
        initEasyTrackServer(new EasyTrackProtocol());
        initTaipServer(new TaipProtocol());
        initKhdServer(new KhdProtocol());
        initPiligrimServer(new PiligrimProtocol());
        initStl060Server(new Stl060Protocol());
        initCarTrackServer(new CarTrackProtocol());
        initMiniFinderServer(new MiniFinderProtocol());
        initHaicomServer(new HaicomProtocol());
        initEelinkServer(new EelinkProtocol());
        initBoxServer(new BoxProtocol());
        initFreedomServer(new FreedomProtocol());
        initTelikServer(new TelikProtocol());
        initTrackboxServer(new TrackboxProtocol());
        initVisiontekServer(new VisiontekProtocol());
        initOrionServer(new OrionProtocol());
        initRitiServer(new RitiProtocol());
        initUlbotechServer(new UlbotechProtocol());
        initTramigoServer(new TramigoProtocol());
        initTr900Server(new Tr900Protocol());
        initArdi01Server(new Ardi01Protocol());
        initXt013Server(new Xt013Protocol());
        initAutoFonServer(new AutoFonProtocol());
        initGoSafeServer(new GoSafeProtocol());
        initAutoFon45Server(new AutoFon45Protocol());
        initBceServer(new BceProtocol());
        initXirgoServer(new XirgoProtocol());
        initCalAmpServer(new CalAmpProtocol());
        initMtxServer(new MtxProtocol());
        initTytanServer(new TytanProtocol());
        initAvl301Server(new Avl301Protocol());
        initCastelServer(new CastelProtocol());
        initMxtServer(new MxtProtocol());

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

    private void initGps103Server(final Gps103Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, "\r\n", "\n", ";"));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gps103ProtocolDecoder(protocol));
                }
            });
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gps103ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTk103Server(final Tk103Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, ')'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tk103ProtocolDecoder(protocol));
                }
            });
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tk103ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initGl100Server(final Gl100Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '\0'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gl100ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initGl200Server(final Gl200Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, "$", "\0"));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gl200ProtocolDecoder(protocol));
                }
            });
            
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gl200ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initT55Server(final T55Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new T55ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initXexunServer(final XexunProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    if (Boolean.valueOf(Context.getProps().getProperty(protocol + ".extended"))) {
                        pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024)); // tracker bug \n\r
                        pipeline.addLast("stringDecoder", new StringDecoder());
                        pipeline.addLast("objectDecoder", new Xexun2ProtocolDecoder(protocol));
                    } else {
                        pipeline.addLast("frameDecoder", new XexunFrameDecoder());
                        pipeline.addLast("stringDecoder", new StringDecoder());
                        pipeline.addLast("objectDecoder", new XexunProtocolDecoder(protocol));
                    }
                }
            });
        }
    }

    private void initTotemServer(final TotemProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new TotemFrameDecoder());
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new TotemProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initEnforaServer(final EnforaProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 0, 2, -2, 2));
                    pipeline.addLast("objectDecoder", new EnforaProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initMeiligaoServer(final MeiligaoProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new MeiligaoFrameDecoder());
                    pipeline.addLast("objectDecoder", new MeiligaoProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initMaxonServer(final MaxonProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new MaxonProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initSuntechServer(final SuntechProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '\r'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new SuntechProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initProgressServer(final ProgressProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 2, 2, 4, 0));
                    pipeline.addLast("objectDecoder", new ProgressProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initH02Server(final H02Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new H02FrameDecoder());
                    pipeline.addLast("objectDecoder", new H02ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initJt600Server(final Jt600Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new Jt600FrameDecoder());
                    pipeline.addLast("objectDecoder", new Jt600ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initEv603Server(final Ev603Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, ';'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Ev603ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initV680Server(final V680Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, "##"));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new V680ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initPt502Server(final Pt502Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new Pt502FrameDecoder());
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Pt502ProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initTr20Server(final Tr20Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tr20ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initNavisServer(final NavisProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(4 * 1024, 12, 2, 2, 0));
                    pipeline.addLast("objectDecoder", new NavisProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initMeitrackServer(final MeitrackProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new MeitrackFrameDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new MeitrackProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initSkypatrolServer(final SkypatrolProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new SkypatrolProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initGt02Server(final Gt02Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(256, 2, 1, 2, 0));
                    pipeline.addLast("objectDecoder", new Gt02ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initGt06Server(final Gt06Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new Gt06FrameDecoder());
                    pipeline.addLast("objectDecoder", new Gt06ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initMegastekServer(final MegastekProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new MegastekProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initNavigilServer(final NavigilProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new NavigilFrameDecoder());
                    pipeline.addLast("objectDecoder", new NavigilProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initGpsGateServer(final GpsGateProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new GpsGateProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTeltonikaServer(final TeltonikaProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new TeltonikaFrameDecoder());
                    pipeline.addLast("objectDecoder", new TeltonikaProtocolDecoder(protocol));
                }
            });
        }
    }
    
    private void initMta6Server(final Mta6Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("httpDecoder", new HttpRequestDecoder());
                    pipeline.addLast("httpEncoder", new HttpResponseEncoder());
                    pipeline.addLast("objectDecoder", new Mta6ProtocolDecoder(protocol, false));
                }
            });
        }
    }

    private void initMta6CanServer(final Mta6canProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("httpDecoder", new HttpRequestDecoder());
                    pipeline.addLast("httpEncoder", new HttpResponseEncoder());
                    pipeline.addLast("objectDecoder", new Mta6ProtocolDecoder(protocol, true));
                }
            });
        }
    }
    
    private void initTlt2hServer(final Tlt2hProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(32 * 1024, "##"));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tlt2hProtocolDecoder(protocol));
                }
            });
        }
    }
    
    private void initSyrusServer(final SyrusProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '<'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new SyrusProtocolDecoder(protocol, true));
                }
            });
        }
    }

    private void initWondexServer(final WondexProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new WondexFrameDecoder());
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new WondexProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initCellocatorServer(final CellocatorProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CellocatorFrameDecoder());
                    pipeline.addLast("objectDecoder", new CellocatorProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initGalileoServer(final GalileoProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new GalileoFrameDecoder());
                    pipeline.addLast("objectDecoder", new GalileoProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initYwtServer(final YwtProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new YwtProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTk102Server(final Tk102Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, ']'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tk102ProtocolDecoder(protocol));
                }
            });
        }
    }
    
    private void initIntellitracServer(final IntellitracProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new IntellitracFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new IntellitracProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initXt7Server(final Xt7Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(256, 20, 1, 5, 0));
                    pipeline.addLast("objectDecoder", new Xt7ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initWialonServer(final WialonProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(4 * 1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new WialonProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initCarscopServer(final CarscopProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '^'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new CarscopProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initApelServer(final ApelProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 2, 2, 4, 0));
                    pipeline.addLast("objectDecoder", new ApelProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initManPowerServer(final ManPowerProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, ';'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new ManPowerProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initGlobalSatServer(final GlobalSatProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '!'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new GlobalSatProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initAtrackServer(final AtrackProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new AtrackFrameDecoder());
                    pipeline.addLast("objectDecoder", new AtrackProtocolDecoder(protocol));
                }
            });
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new AtrackProtocolDecoder(protocol));
                }
            });

        }
    }

    private void initPt3000Server(final Pt3000Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, 'd')); // probably wrong
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Pt3000ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initRuptelaServer(final RuptelaProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 0, 2, 2, 0));
                    pipeline.addLast("objectDecoder", new RuptelaProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTopflytechServer(final TopflytechProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, ')'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new TopflytechProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initLaipacServer(final LaipacProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new LaipacProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initAplicomServer(final AplicomProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new AplicomFrameDecoder());
                    pipeline.addLast("objectDecoder", new AplicomProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initGotopServer(final GotopProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '#'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new GotopProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initSanavServer(final SanavProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '*'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new SanavProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initGatorServer(final GatorProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new GatorProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initNoranServer(final NoranProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ConnectionlessBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new NoranProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initM2mServer(final M2mProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new FixedLengthFrameDecoder(23));
                    pipeline.addLast("objectDecoder", new M2mProtocolDecoder(protocol));
                }
            });
        }
    }
    
    private void initOsmAndServer(final OsmAndProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("httpDecoder", new HttpRequestDecoder());
                    pipeline.addLast("httpEncoder", new HttpResponseEncoder());
                    pipeline.addLast("objectDecoder", new OsmAndProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initEasyTrackServer(final EasyTrackProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '#'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new EasyTrackProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTaipServer(final TaipProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new SyrusProtocolDecoder(protocol, false));
                }
            });
        }
    }

    private void initKhdServer(final KhdProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(256, 3, 2));
                    pipeline.addLast("objectDecoder", new KhdProtocolDecoder(protocol));
                }
            });
        }
    }
    
    private void initPiligrimServer(final PiligrimProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("httpDecoder", new HttpRequestDecoder());
                    pipeline.addLast("httpAggregator", new HttpChunkAggregator(16384));
                    pipeline.addLast("httpEncoder", new HttpResponseEncoder());
                    pipeline.addLast("objectDecoder", new PiligrimProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initStl060Server(final Stl060Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new Stl060FrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Stl060ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initCarTrackServer(final CarTrackProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, "##"));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new CarTrackProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initMiniFinderServer(final MiniFinderProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, ';'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new MiniFinderProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initHaicomServer(final HaicomProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '*'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new HaicomProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initEelinkServer(final EelinkProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 3, 2));
                    pipeline.addLast("objectDecoder", new EelinkProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initBoxServer(final BoxProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '\r'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new BoxProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initFreedomServer(final FreedomProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new FreedomProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTelikServer(final TelikProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '\0'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new TelikProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTrackboxServer(final TrackboxProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new TrackboxProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initVisiontekServer(final VisiontekProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '#'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new VisiontekProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initOrionServer(final OrionProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new OrionFrameDecoder());
                    pipeline.addLast("objectDecoder", new OrionProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initRitiServer(final RitiProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 105, 2, 3, 0));
                    pipeline.addLast("objectDecoder", new RitiProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initUlbotechServer(final UlbotechProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new UlbotechFrameDecoder());
                    pipeline.addLast("objectDecoder", new UlbotechProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTramigoServer(final TramigoProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new TramigoFrameDecoder());
                    pipeline.addLast("objectDecoder", new TramigoProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initTr900Server(final Tr900Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '!'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tr900ProtocolDecoder(protocol));
                }
            });
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tr900ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initArdi01Server(final Ardi01Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Ardi01ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initXt013Server(final Xt013Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Xt013ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initAutoFonServer(final AutoFonProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new AutoFonFrameDecoder());
                    pipeline.addLast("objectDecoder", new AutoFonProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initGoSafeServer(final GoSafeProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '#'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new GoSafeProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initAutoFon45Server(final AutoFon45Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new AutoFon45FrameDecoder());
                    pipeline.addLast("objectDecoder", new AutoFon45ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initBceServer(final BceProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new BceFrameDecoder());
                    pipeline.addLast("objectDecoder", new BceProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }
    
    private void initXirgoServer(final XirgoProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, "##"));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new XirgoProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initCalAmpServer(final CalAmpProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new CalAmpProtocolDecoder(protocol));
                }
            });
        }
    }
    
    private void initMtxServer(final MtxProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new MtxProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTytanServer(final TytanProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new TytanProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initAvl301Server(final Avl301Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(256, 2, 1, -3, 0));
                    pipeline.addLast("objectDecoder", new Avl301ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initCastelServer(final CastelProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 2, 2, -4, 0));
                    pipeline.addLast("objectDecoder", new CastelProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);

            server = new TrackerServer(new ConnectionlessBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new CastelProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initMxtServer(final MxtProtocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol.getName()) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new MxtFrameDecoder());
                    pipeline.addLast("objectDecoder", new MxtProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

}
