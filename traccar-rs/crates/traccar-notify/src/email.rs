use async_trait::async_trait;
use traccar_model::{Event, Position, User};

use crate::{Notificator, NotifyError};

/// Sends email notifications using SMTP via the `lettre` crate.
pub struct EmailNotificator {
    pub smtp_host: String,
    pub smtp_port: u16,
    pub smtp_user: Option<String>,
    pub smtp_password: Option<String>,
    pub from_address: String,
}

impl EmailNotificator {
    pub fn new(host: &str, port: u16, from: &str) -> Self {
        Self {
            smtp_host: host.to_string(),
            smtp_port: port,
            smtp_user: None,
            smtp_password: None,
            from_address: from.to_string(),
        }
    }
}

#[async_trait]
impl Notificator for EmailNotificator {
    fn name(&self) -> &str {
        "mail"
    }

    async fn send_notification(
        &self,
        event: &Event,
        _position: &Position,
        user: &User,
    ) -> Result<(), NotifyError> {
        let to = &user.email;
        let subject = format!("Traccar: {}", event.event_type);
        let body = format!(
            "Event {} occurred on device {}",
            event.event_type, event.device_id
        );

        tracing::info!(to = %to, subject = %subject, "Sending email notification");

        use lettre::message::Mailbox;
        use lettre::transport::smtp::authentication::Credentials;
        use lettre::{AsyncSmtpTransport, AsyncTransport, Message, Tokio1Executor};

        let from: Mailbox = self
            .from_address
            .parse()
            .map_err(|e: lettre::address::AddressError| NotifyError::Config(e.to_string()))?;
        let to_mailbox: Mailbox = to
            .parse()
            .map_err(|e: lettre::address::AddressError| NotifyError::Config(e.to_string()))?;

        let email = Message::builder()
            .from(from)
            .to(to_mailbox)
            .subject(subject)
            .body(body)
            .map_err(|e| NotifyError::Send(e.to_string()))?;

        let mut transport_builder =
            AsyncSmtpTransport::<Tokio1Executor>::relay(&self.smtp_host)
                .map_err(|e| NotifyError::Send(e.to_string()))?
                .port(self.smtp_port);

        if let (Some(user), Some(pass)) = (&self.smtp_user, &self.smtp_password) {
            transport_builder =
                transport_builder.credentials(Credentials::new(user.clone(), pass.clone()));
        }

        let mailer = transport_builder.build();
        mailer
            .send(email)
            .await
            .map_err(|e| NotifyError::Send(e.to_string()))?;

        Ok(())
    }
}
