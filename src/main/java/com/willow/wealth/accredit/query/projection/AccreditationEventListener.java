package com.willow.wealth.accredit.query.projection;

import com.willow.wealth.accredit.event.AccreditationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes accreditation events from Kafka and hands them to the projector.
 * The offset is committed only after the event was applied (ack-mode: record),
 * so a crash mid-way means the event is redelivered, not lost.
 */
@Component
public class AccreditationEventListener {

	private final AccreditationProjector projector;

	public AccreditationEventListener(AccreditationProjector projector) {
		this.projector = projector;
	}

	@KafkaListener(topics = "${accredit.kafka.topic.name}", groupId = "${spring.application.name}-projector")
	public void onEvent(AccreditationEvent event) {
		projector.apply(event);
	}

}
