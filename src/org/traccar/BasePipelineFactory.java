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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.traccar.helper.Log;

public abstract class BasePipelineFactory implements ChannelPipelineFactory {

    private final TrackerServer server;
    private final int resetDelay;
    private final String keystore_path;
    

    private FilterHandler filterHandler;
    private DistanceHandler distanceHandler;
    private ReverseGeocoderHandler reverseGeocoderHandler;
    private LocationProviderHandler locationProviderHandler;

    private static final class OpenChannelHandler extends SimpleChannelHandler {

        private final TrackerServer server;

        private OpenChannelHandler(TrackerServer server) {
            this.server = server;
        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
            server.getChannelGroup().add(e.getChannel());
        }
    }

    private static class StandardLoggingHandler extends LoggingHandler {

        @Override
        public void log(ChannelEvent e) {
            if (e instanceof MessageEvent) {
                MessageEvent event = (MessageEvent) e;
                StringBuilder msg = new StringBuilder();

                msg.append("[").append(String.format("%08X", e.getChannel().getId())).append(": ");
                msg.append(((InetSocketAddress) e.getChannel().getLocalAddress()).getPort());
                if (e instanceof DownstreamMessageEvent) {
                    msg.append(" > ");
                } else {
                    msg.append(" < ");
                }

                if (event.getRemoteAddress() != null) {
                    msg.append(((InetSocketAddress) event.getRemoteAddress()).getHostString());
                } else {
                    msg.append("null");
                }
                msg.append("]");

                if (event.getMessage() instanceof ChannelBuffer) {
                    msg.append(" HEX: ");
                    msg.append(ChannelBuffers.hexDump((ChannelBuffer) event.getMessage()));
                }

                Log.debug(msg.toString());
            }
        }

    }

    public BasePipelineFactory(TrackerServer server, String protocol) {
        this.server = server;

        resetDelay = Context.getConfig().getInteger(protocol + ".resetDelay", 0);
        
        keystore_path = Context.getConfig().getString("web.keystorePath");

        if (Context.getConfig().getBoolean("filter.enable")) {
            filterHandler = new FilterHandler();
        }

        if (Context.getReverseGeocoder() != null) {
            reverseGeocoderHandler = new ReverseGeocoderHandler(
                    Context.getReverseGeocoder(), Context.getConfig().getBoolean("geocoder.processInvalidPositions"));
        }

        if (Context.getLocationProvider() != null) {
            locationProviderHandler = new LocationProviderHandler(Context.getLocationProvider());
        }

        if (Context.getConfig().getBoolean("distance.enable")) {
            distanceHandler = new DistanceHandler();
        }
    }

    protected abstract void addSpecificHandlers(ChannelPipeline pipeline);

    @Override
    public ChannelPipeline getPipeline() throws KeyStoreException, KeyManagementException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        ChannelPipeline pipeline = Channels.pipeline();
        if (resetDelay != 0) {
            pipeline.addLast("idleHandler", new IdleStateHandler(GlobalTimer.getTimer(), resetDelay, 0, 0));
        }
        pipeline.addLast("openHandler", new OpenChannelHandler(server));
        if (Context.isLoggerEnabled()) {
            pipeline.addLast("logger", new StandardLoggingHandler());
        }

        addSpecificHandlers(pipeline);

        if (distanceHandler != null) {
            pipeline.addLast("distance", distanceHandler);
        }
        if (reverseGeocoderHandler != null) {
            pipeline.addLast("geocoder", reverseGeocoderHandler);
        }
        if (locationProviderHandler != null) {
            pipeline.addLast("location", locationProviderHandler);
        }
        pipeline.addLast("remoteAddress", new RemoteAddressHandler());

        addDynamicHandlers(pipeline);

        if (filterHandler != null) {
            pipeline.addLast("filter", filterHandler);
        }

        if (Context.getDataManager() != null) {
            pipeline.addLast("dataHandler", new DefaultDataHandler());
        }
        if (Context.getConfig().getBoolean("forward.enable")) {
//      Adding ssl support to web application
        
//            if (keystore_path!=null && !keystore_path.isEmpty()){
//                    TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//                    KeyStore tmpKS = null;
//                    tmFactory.init(tmpKS);
//                    KeyStore ks = KeyStore.getInstance("JKS");
//
//                    String keystore_pass = Context.getConfig().getString("web.keystorePassword");
//                    ks.load(new FileInputStream(keystore_path), keystore_pass.toCharArray());
//
//                    // Set up key manager factory to use our key store
//                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//                    kmf.init(ks, keystore_pass.toCharArray());
//
//                    KeyManager[] km = kmf.getKeyManagers();
//                    TrustManager[] tm = tmFactory.getTrustManagers();
//
//                    SSLContext sslContext = SSLContext.getInstance("TLS");
//                    sslContext.init(km, tm, null);
//                    SSLEngine sslEngine = sslContext.createSSLEngine();
//                    sslEngine.setUseClientMode(false);
//                    sslEngine.setEnabledProtocols(sslEngine.getSupportedProtocols());
//                    sslEngine.setEnabledCipherSuites(sslEngine.getSupportedCipherSuites());
//                    sslEngine.setEnableSessionCreation(true);
//                    pipeline.addLast("ssl", new SslHandler(sslEngine));
//            }            
            pipeline.addLast("webHandler", new WebDataHandler(Context.getConfig().getString("forward.url")));
        }
        pipeline.addLast("mainHandler", new MainEventHandler());
        return pipeline;
    }

    private void addDynamicHandlers(ChannelPipeline pipeline) {
        if (Context.getConfig().hasKey("extra.handlers")) {
            String[] handlers = Context.getConfig().getString("extra.handlers").split(",");
            for (int i = 0; i < handlers.length; i++) {
                try {
                    pipeline.addLast("extraHandler." + i, (ChannelHandler) Class.forName(handlers[i]).newInstance());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException error) {
                    Log.warning(error);
                }
            }
        }
    }

}
