package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event;

import tomas.gonzalez.ConvertidorUnificadorJavaSwing.domain.model.MediaItem;
import java.util.Objects;

public final class MediaInspectedEvent implements AppEvent {
	private final MediaItem mediaItem;

	public MediaInspectedEvent(MediaItem mediaItem) {
		this.mediaItem = mediaItem;
	}

	public MediaItem mediaItem() { return mediaItem; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MediaInspectedEvent that = (MediaInspectedEvent) o;
		return Objects.equals(mediaItem, that.mediaItem);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mediaItem);
	}

	@Override
	public String toString() {
		return "MediaInspectedEvent[mediaItem=" + mediaItem + "]";
	}
}
