use crate::session::{ConnectionKey, DeviceSession, SharedConnectionManager};
use crate::{ProtocolDefinition, ProtocolError, Transport};
use bytes::BytesMut;
use futures::StreamExt;
use std::net::SocketAddr;
use std::sync::Arc;
use tokio::net::{TcpListener, UdpSocket};
use tokio_util::codec::Framed;
use tracing::{error, info, warn};

/// A tracker server that listens for device connections on a specific port
/// and decodes position data using a given protocol.
pub struct TrackerServer {
    protocol: Arc<ProtocolDefinition>,
    port: u16,
    transport: Transport,
    connection_manager: SharedConnectionManager,
}

impl TrackerServer {
    /// Create a new tracker server for a given protocol.
    pub fn new(
        protocol: Arc<ProtocolDefinition>,
        port: u16,
        transport: Transport,
        connection_manager: SharedConnectionManager,
    ) -> Self {
        Self {
            protocol,
            port,
            transport,
            connection_manager,
        }
    }

    /// Start the server, spawning TCP and/or UDP listeners as configured.
    pub async fn start(self: Arc<Self>) -> Result<(), ProtocolError> {
        match self.transport {
            Transport::Tcp => {
                self.start_tcp().await?;
            }
            Transport::Udp => {
                self.start_udp().await?;
            }
            Transport::Both => {
                let tcp_self = self.clone();
                let udp_self = self.clone();
                tokio::spawn(async move {
                    if let Err(e) = tcp_self.start_tcp().await {
                        error!(protocol = %tcp_self.protocol.name, "TCP server error: {}", e);
                    }
                });
                udp_self.start_udp().await?;
            }
        }
        Ok(())
    }

    /// Start the TCP listener.
    async fn start_tcp(&self) -> Result<(), ProtocolError> {
        let addr = SocketAddr::from(([0, 0, 0, 0], self.port));
        let listener = TcpListener::bind(addr).await?;

        info!(
            protocol = %self.protocol.name,
            port = self.port,
            "TCP server listening"
        );

        loop {
            match listener.accept().await {
                Ok((stream, remote_addr)) => {
                    info!(
                        protocol = %self.protocol.name,
                        remote = %remote_addr,
                        "New TCP connection"
                    );

                    let protocol = self.protocol.clone();
                    let connection_manager = self.connection_manager.clone();

                    tokio::spawn(async move {
                        if let Err(e) =
                            handle_tcp_connection(stream, remote_addr, protocol, connection_manager)
                                .await
                        {
                            warn!(
                                remote = %remote_addr,
                                "TCP connection error: {}", e
                            );
                        }
                    });
                }
                Err(e) => {
                    error!(protocol = %self.protocol.name, "Accept error: {}", e);
                }
            }
        }
    }

    /// Start the UDP socket listener.
    async fn start_udp(&self) -> Result<(), ProtocolError> {
        let addr = SocketAddr::from(([0, 0, 0, 0], self.port));
        let socket = Arc::new(UdpSocket::bind(addr).await?);

        info!(
            protocol = %self.protocol.name,
            port = self.port,
            "UDP server listening"
        );

        let mut recv_buf = vec![0u8; 4096];

        loop {
            match socket.recv_from(&mut recv_buf).await {
                Ok((len, remote_addr)) => {
                    let data = BytesMut::from(&recv_buf[..len]);
                    let protocol = self.protocol.clone();
                    let connection_manager = self.connection_manager.clone();

                    tokio::spawn(async move {
                        if let Err(e) =
                            handle_udp_datagram(data, remote_addr, protocol, connection_manager)
                                .await
                        {
                            warn!(
                                remote = %remote_addr,
                                "UDP datagram error: {}", e
                            );
                        }
                    });
                }
                Err(e) => {
                    error!(protocol = %self.protocol.name, "UDP recv error: {}", e);
                }
            }
        }
    }
}

/// Handle a single TCP connection: frame data, decode messages, and process positions.
async fn handle_tcp_connection(
    stream: tokio::net::TcpStream,
    remote_addr: SocketAddr,
    protocol: Arc<ProtocolDefinition>,
    connection_manager: SharedConnectionManager,
) -> Result<(), ProtocolError> {
    let frame_decoder = (protocol.frame_decoder_factory)();
    let framed = Framed::new(stream, FrameDecoderWrapper(frame_decoder));
    let mut framed = framed;

    let conn_key = ConnectionKey::new(remote_addr.to_string(), protocol.name.clone());
    let mut session = DeviceSession::unauthenticated(&protocol.name);

    let decoder = (protocol.decoder_factory)();

    while let Some(frame_result) = framed.next().await {
        match frame_result {
            Ok(mut frame) => {
                connection_manager.touch_session(&conn_key);
                session.touch();

                match decoder.decode(&mut frame, &mut session).await {
                    Ok(Some(positions)) => {
                        // If session just got authenticated, register it
                        if session.is_authenticated() {
                            connection_manager
                                .register_session(conn_key.clone(), session.clone());
                        }

                        for position in &positions {
                            tracing::debug!(
                                protocol = %protocol.name,
                                device_id = position.device_id,
                                lat = position.latitude,
                                lon = position.longitude,
                                "Position decoded"
                            );
                        }

                        // TODO: Pass positions to handler pipeline
                    }
                    Ok(None) => {
                        // Non-position message (login, heartbeat, etc.)
                        if session.is_authenticated() {
                            connection_manager
                                .register_session(conn_key.clone(), session.clone());
                        }
                    }
                    Err(e) => {
                        warn!(
                            protocol = %protocol.name,
                            remote = %remote_addr,
                            "Decode error: {}", e
                        );
                    }
                }
            }
            Err(e) => {
                warn!(
                    protocol = %protocol.name,
                    remote = %remote_addr,
                    "Frame error: {}", e
                );
                break;
            }
        }
    }

    // Cleanup on disconnect
    connection_manager.remove_session(&conn_key);
    info!(
        protocol = %protocol.name,
        remote = %remote_addr,
        "TCP connection closed"
    );

    Ok(())
}

/// Handle a single UDP datagram: frame, decode, and process.
async fn handle_udp_datagram(
    mut data: BytesMut,
    remote_addr: SocketAddr,
    protocol: Arc<ProtocolDefinition>,
    connection_manager: SharedConnectionManager,
) -> Result<(), ProtocolError> {
    let conn_key = ConnectionKey::new(remote_addr.to_string(), protocol.name.clone());

    // Get or create session for this remote address
    let mut session = connection_manager
        .get_session(&conn_key)
        .unwrap_or_else(|| DeviceSession::unauthenticated(&protocol.name));

    let decoder = (protocol.decoder_factory)();

    match decoder.decode(&mut data, &mut session).await {
        Ok(Some(positions)) => {
            if session.is_authenticated() {
                connection_manager.register_session(conn_key, session);
            }

            for position in &positions {
                tracing::debug!(
                    protocol = %protocol.name,
                    device_id = position.device_id,
                    lat = position.latitude,
                    lon = position.longitude,
                    "UDP position decoded"
                );
            }

            // TODO: Pass positions to handler pipeline
        }
        Ok(None) => {
            if session.is_authenticated() {
                connection_manager.register_session(conn_key, session);
            }
        }
        Err(e) => {
            warn!(
                protocol = %protocol.name,
                remote = %remote_addr,
                "UDP decode error: {}", e
            );
        }
    }

    Ok(())
}

/// Wrapper to make a boxed Decoder work with Framed.
struct FrameDecoderWrapper(
    Box<dyn tokio_util::codec::Decoder<Item = BytesMut, Error = std::io::Error> + Send + Sync>,
);

impl tokio_util::codec::Decoder for FrameDecoderWrapper {
    type Item = BytesMut;
    type Error = std::io::Error;

    fn decode(&mut self, src: &mut BytesMut) -> Result<Option<BytesMut>, std::io::Error> {
        self.0.decode(src)
    }
}
