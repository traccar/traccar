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
import org.traccar.protocol.ApelProtocolDecoder;
import org.traccar.protocol.AplicomFrameDecoder;
import org.traccar.protocol.AplicomProtocolDecoder;
import org.traccar.protocol.Ardi01ProtocolDecoder;
import org.traccar.protocol.AtrackFrameDecoder;
import org.traccar.protocol.AtrackProtocolDecoder;
import org.traccar.protocol.AutoFon45FrameDecoder;
import org.traccar.protocol.AutoFon45ProtocolDecoder;
import org.traccar.protocol.AutoFonFrameDecoder;
import org.traccar.protocol.AutoFonProtocolDecoder;
import org.traccar.protocol.Avl301ProtocolDecoder;
import org.traccar.protocol.BceFrameDecoder;
import org.traccar.protocol.BceProtocolDecoder;
import org.traccar.protocol.BoxProtocolDecoder;
import org.traccar.protocol.CalAmpProtocolDecoder;
import org.traccar.protocol.CarTrackProtocolDecoder;
import org.traccar.protocol.CarscopProtocolDecoder;
import org.traccar.protocol.CastelProtocolDecoder;
import org.traccar.protocol.CellocatorFrameDecoder;
import org.traccar.protocol.CellocatorProtocolDecoder;
import org.traccar.protocol.EasyTrackProtocolDecoder;
import org.traccar.protocol.EelinkProtocolDecoder;
import org.traccar.protocol.EnforaProtocolDecoder;
import org.traccar.protocol.Ev603ProtocolDecoder;
import org.traccar.protocol.FreedomProtocolDecoder;
import org.traccar.protocol.GalileoFrameDecoder;
import org.traccar.protocol.GalileoProtocolDecoder;
import org.traccar.protocol.GatorProtocolDecoder;
import org.traccar.protocol.Gl100ProtocolDecoder;
import org.traccar.protocol.Gl200ProtocolDecoder;
import org.traccar.protocol.GlobalSatProtocolDecoder;
import org.traccar.protocol.GoSafeProtocolDecoder;
import org.traccar.protocol.GotopProtocolDecoder;
import org.traccar.protocol.Gps103ProtocolDecoder;
import org.traccar.protocol.GpsGateProtocolDecoder;
import org.traccar.protocol.Gt02ProtocolDecoder;
import org.traccar.protocol.Gt06FrameDecoder;
import org.traccar.protocol.Gt06ProtocolDecoder;
import org.traccar.protocol.H02FrameDecoder;
import org.traccar.protocol.H02ProtocolDecoder;
import org.traccar.protocol.HaicomProtocolDecoder;
import org.traccar.protocol.IntellitracFrameDecoder;
import org.traccar.protocol.IntellitracProtocolDecoder;
import org.traccar.protocol.Jt600FrameDecoder;
import org.traccar.protocol.Jt600ProtocolDecoder;
import org.traccar.protocol.KhdProtocolDecoder;
import org.traccar.protocol.LaipacProtocolDecoder;
import org.traccar.protocol.M2mProtocolDecoder;
import org.traccar.protocol.ManPowerProtocolDecoder;
import org.traccar.protocol.MaxonProtocolDecoder;
import org.traccar.protocol.MegastekProtocolDecoder;
import org.traccar.protocol.MeiligaoFrameDecoder;
import org.traccar.protocol.MeiligaoProtocolDecoder;
import org.traccar.protocol.MeitrackFrameDecoder;
import org.traccar.protocol.MeitrackProtocolDecoder;
import org.traccar.protocol.MiniFinderProtocolDecoder;
import org.traccar.protocol.Mta6ProtocolDecoder;
import org.traccar.protocol.MtxProtocolDecoder;
import org.traccar.protocol.NavigilFrameDecoder;
import org.traccar.protocol.NavigilProtocolDecoder;
import org.traccar.protocol.NavisProtocolDecoder;
import org.traccar.protocol.NoranProtocolDecoder;
import org.traccar.protocol.OrionFrameDecoder;
import org.traccar.protocol.OrionProtocolDecoder;
import org.traccar.protocol.OsmAndProtocolDecoder;
import org.traccar.protocol.PiligrimProtocolDecoder;
import org.traccar.protocol.ProgressProtocolDecoder;
import org.traccar.protocol.Pt3000ProtocolDecoder;
import org.traccar.protocol.Pt502FrameDecoder;
import org.traccar.protocol.Pt502ProtocolDecoder;
import org.traccar.protocol.RitiProtocolDecoder;
import org.traccar.protocol.RuptelaProtocolDecoder;
import org.traccar.protocol.SanavProtocolDecoder;
import org.traccar.protocol.SkypatrolProtocolDecoder;
import org.traccar.protocol.Stl060FrameDecoder;
import org.traccar.protocol.Stl060ProtocolDecoder;
import org.traccar.protocol.SuntechProtocolDecoder;
import org.traccar.protocol.SyrusProtocolDecoder;
import org.traccar.protocol.T55ProtocolDecoder;
import org.traccar.protocol.TelikProtocolDecoder;
import org.traccar.protocol.TeltonikaFrameDecoder;
import org.traccar.protocol.TeltonikaProtocolDecoder;
import org.traccar.protocol.Tk102ProtocolDecoder;
import org.traccar.protocol.Tk103ProtocolDecoder;
import org.traccar.protocol.Tlt2hProtocolDecoder;
import org.traccar.protocol.TopflytechProtocolDecoder;
import org.traccar.protocol.TotemFrameDecoder;
import org.traccar.protocol.TotemProtocolDecoder;
import org.traccar.protocol.Tr20ProtocolDecoder;
import org.traccar.protocol.Tr900ProtocolDecoder;
import org.traccar.protocol.TrackboxProtocolDecoder;
import org.traccar.protocol.TramigoFrameDecoder;
import org.traccar.protocol.TramigoProtocolDecoder;
import org.traccar.protocol.TytanProtocolDecoder;
import org.traccar.protocol.UlbotechFrameDecoder;
import org.traccar.protocol.UlbotechProtocolDecoder;
import org.traccar.protocol.V680ProtocolDecoder;
import org.traccar.protocol.VisiontekProtocolDecoder;
import org.traccar.protocol.WialonProtocolDecoder;
import org.traccar.protocol.WondexFrameDecoder;
import org.traccar.protocol.WondexProtocolDecoder;
import org.traccar.protocol.Xexun2ProtocolDecoder;
import org.traccar.protocol.XexunFrameDecoder;
import org.traccar.protocol.XexunProtocolDecoder;
import org.traccar.protocol.XirgoProtocolDecoder;
import org.traccar.protocol.Xt013ProtocolDecoder;
import org.traccar.protocol.Xt7ProtocolDecoder;
import org.traccar.protocol.YwtProtocolDecoder;


public class ServerManager {

    private final List<TrackerServer> serverList = new LinkedList<TrackerServer>();

    public void init() throws Exception {

        initGps103Server("gps103");
        initTk103Server("tk103");
        initGl100Server("gl100");
        initGl200Server("gl200");
        initT55Server("t55");
        initXexunServer("xexun");
        initTotemServer("totem");
        initEnforaServer("enfora");
        initMeiligaoServer("meiligao");
        initMaxonServer("maxon");
        initSuntechServer("suntech");
        initProgressServer("progress");
        initH02Server("h02");
        initJt600Server("jt600");
        initEv603Server("ev603");
        initV680Server("v680");
        initPt502Server("pt502");
        initTr20Server("tr20");
        initNavisServer("navis");
        initMeitrackServer("meitrack");
        initSkypatrolServer("skypatrol");
        initGt02Server("gt02");
        initGt06Server("gt06");
        initMegastekServer("megastek");
        initNavigilServer("navigil");
        initGpsGateServer("gpsgate");
        initTeltonikaServer("teltonika");
        initMta6Server("mta6");
        initMta6CanServer("mta6can");
        initTlt2hServer("tlt2h");
        initSyrusServer("syrus");
        initWondexServer("wondex");
        initCellocatorServer("cellocator");
        initGalileoServer("galileo");
        initYwtServer("ywt");
        initTk102Server("tk102");
        initIntellitracServer("intellitrac");
        initXt7Server("xt7");
        initWialonServer("wialon");
        initCarscopServer("carscop");
        initApelServer("apel");
        initManPowerServer("manpower");
        initGlobalSatServer("globalsat");
        initAtrackServer("atrack");
        initPt3000Server("pt3000");
        initRuptelaServer("ruptela");
        initTopflytechServer("topflytech");
        initLaipacServer("laipac");
        initAplicomServer("aplicom");
        initGotopServer("gotop");
        initSanavServer("sanav");
        initGatorServer("gator");
        initNoranServer("noran");
        initM2mServer("m2m");
        initOsmAndServer("osmand");
        initEasyTrackServer("easytrack");
        initTaipServer("taip");
        initKhdServer("khd");
        initPiligrimServer("piligrim");
        initStl060Server("stl060");
        initCarTrackServer("cartrack");
        initMiniFinderServer("minifinder");
        initHaicomServer("haicom");
        initEelinkServer("eelink");
        initBoxServer("box");
        initFreedomServer("freedom");
        initTelikServer("telik");
        initTrackboxServer("trackbox");
        initVisiontekServer("visiontek");
        initOrionServer("orion");
        initRitiServer("riti");
        initUlbotechServer("ulbotech");
        initTramigoServer("tramigo");
        initTr900Server("tr900");
        initArdi01Server("ardi01");
        initXt013Server("xt013");
        initAutoFonServer("autofon");
        initGoSafeServer("gosafe");
        initAutoFon45Server("autofon45");
        initBceServer("bce");
        initXirgoServer("xirgo");
        initCalAmpServer("calamp");
        initMtxServer("mtx");
        initTytanServer("tytan");
        initAvl301Server("avl301");
        initCastelServer("castel");

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
        String enabled = Context.getProps().getProperty(protocol + ".enable");
        if (enabled != null) {
            return Boolean.valueOf(enabled);
        }
        return false;
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

    private void initGps103Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, "\r\n", "\n", ";"));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gps103ProtocolDecoder(protocol));
                }
            });
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gps103ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTk103Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, ')'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tk103ProtocolDecoder(protocol));
                }
            });
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tk103ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initGl100Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initGl200Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, "$", "\0"));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gl200ProtocolDecoder(protocol));
                }
            });
            
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gl200ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initT55Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initXexunServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initTotemServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new TotemFrameDecoder());
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new TotemProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initEnforaServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 0, 2, -2, 2));
                    pipeline.addLast("objectDecoder", new EnforaProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initMeiligaoServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new MeiligaoFrameDecoder());
                    pipeline.addLast("objectDecoder", new MeiligaoProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initMaxonServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initSuntechServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '\r'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new SuntechProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initProgressServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initH02Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new H02FrameDecoder());
                    pipeline.addLast("objectDecoder", new H02ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initJt600Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new Jt600FrameDecoder());
                    pipeline.addLast("objectDecoder", new Jt600ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initEv603Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, ';'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Ev603ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initV680Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, "##"));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new V680ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initPt502Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initTr20Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initNavisServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initMeitrackServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initSkypatrolServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new SkypatrolProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initGt02Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(256, 2, 1, 2, 0));
                    pipeline.addLast("objectDecoder", new Gt02ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initGt06Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new Gt06FrameDecoder());
                    pipeline.addLast("objectDecoder", new Gt06ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initMegastekServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initNavigilServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initGpsGateServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initTeltonikaServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new TeltonikaFrameDecoder());
                    pipeline.addLast("objectDecoder", new TeltonikaProtocolDecoder(protocol));
                }
            });
        }
    }
    
    private void initMta6Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("httpDecoder", new HttpRequestDecoder());
                    pipeline.addLast("httpEncoder", new HttpResponseEncoder());
                    pipeline.addLast("objectDecoder", new Mta6ProtocolDecoder(protocol, false));
                }
            });
        }
    }

    private void initMta6CanServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("httpDecoder", new HttpRequestDecoder());
                    pipeline.addLast("httpEncoder", new HttpResponseEncoder());
                    pipeline.addLast("objectDecoder", new Mta6ProtocolDecoder(protocol, true));
                }
            });
        }
    }
    
    private void initTlt2hServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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
    
    private void initSyrusServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initWondexServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new WondexFrameDecoder());
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new WondexProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initCellocatorServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initGalileoServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initYwtServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initTk102Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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
    
    private void initIntellitracServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initXt7Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(256, 20, 1, 5, 0));
                    pipeline.addLast("objectDecoder", new Xt7ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initWialonServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initCarscopServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initApelServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initManPowerServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initGlobalSatServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initAtrackServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new AtrackFrameDecoder());
                    pipeline.addLast("objectDecoder", new AtrackProtocolDecoder(protocol));
                }
            });
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new AtrackProtocolDecoder(protocol));
                }
            });

        }
    }

    private void initPt3000Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initRuptelaServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 0, 2, 2, 0));
                    pipeline.addLast("objectDecoder", new RuptelaProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTopflytechServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, ')'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new TopflytechProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initLaipacServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initAplicomServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new AplicomFrameDecoder());
                    pipeline.addLast("objectDecoder", new AplicomProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initGotopServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '#'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new GotopProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initSanavServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '*'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new SanavProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initGatorServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new GatorProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initNoranServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            TrackerServer server = new TrackerServer(new ConnectionlessBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new NoranProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initM2mServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new FixedLengthFrameDecoder(23));
                    pipeline.addLast("objectDecoder", new M2mProtocolDecoder(protocol));
                }
            });
        }
    }
    
    private void initOsmAndServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("httpDecoder", new HttpRequestDecoder());
                    pipeline.addLast("httpEncoder", new HttpResponseEncoder());
                    pipeline.addLast("objectDecoder", new OsmAndProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initEasyTrackServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '#'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new EasyTrackProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTaipServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new SyrusProtocolDecoder(protocol, false));
                }
            });
        }
    }

    private void initKhdServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(256, 3, 2));
                    pipeline.addLast("objectDecoder", new KhdProtocolDecoder(protocol));
                }
            });
        }
    }
    
    private void initPiligrimServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initStl060Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new Stl060FrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Stl060ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initCarTrackServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, "##"));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new CarTrackProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initMiniFinderServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, ';'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new MiniFinderProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initHaicomServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '*'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new HaicomProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initEelinkServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 3, 2));
                    pipeline.addLast("objectDecoder", new EelinkProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initBoxServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '\r'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new BoxProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initFreedomServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new FreedomProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTelikServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '\0'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new TelikProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTrackboxServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initVisiontekServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '#'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new VisiontekProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initOrionServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initRitiServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initUlbotechServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new UlbotechFrameDecoder());
                    pipeline.addLast("objectDecoder", new UlbotechProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initTramigoServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initTr900Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '!'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tr900ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initArdi01Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Ardi01ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initXt013Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(1024));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Xt013ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initAutoFonServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new AutoFonFrameDecoder());
                    pipeline.addLast("objectDecoder", new AutoFonProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initGoSafeServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, '#'));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new GoSafeProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initAutoFon45Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new AutoFon45FrameDecoder());
                    pipeline.addLast("objectDecoder", new AutoFon45ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initBceServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol) {
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
    
    private void initXirgoServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initCalAmpServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new CalAmpProtocolDecoder(protocol));
                }
            });
        }
    }
    
    private void initMtxServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
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

    private void initTytanServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new TytanProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initAvl301Server(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(256, 2, 1, -3, 0));
                    pipeline.addLast("objectDecoder", new Avl301ProtocolDecoder(protocol));
                }
            });
        }
    }

    private void initCastelServer(final String protocol) throws SQLException {
        if (isProtocolEnabled(protocol)) {
            TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 2, 2, -2, 0));
                    pipeline.addLast("objectDecoder", new CastelProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);

            server = new TrackerServer(new ConnectionlessBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new Gps103ProtocolDecoder(protocol));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

}
