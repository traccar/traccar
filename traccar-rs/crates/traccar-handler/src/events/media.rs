use async_trait::async_trait;
use traccar_model::{position_keys, Event, Position};
use crate::{EventHandler, HandlerState};

/// Generates a media event when a position carries image, video, or audio data.
pub struct MediaEventHandler;

impl MediaEventHandler {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl EventHandler for MediaEventHandler {
    fn name(&self) -> &str {
        "media"
    }

    async fn analyze_position(
        &self,
        position: &Position,
        _state: &dyn HandlerState,
    ) -> Vec<Event> {
        let has_media = position.has_attribute(position_keys::KEY_IMAGE)
            || position.has_attribute(position_keys::KEY_VIDEO)
            || position.has_attribute(position_keys::KEY_AUDIO);

        if has_media {
            let event = Event::new(Event::TYPE_MEDIA, position);
            return vec![event];
        }

        Vec::new()
    }
}
