/*
 * Copyright 2016 - 2025 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.component.VEvent;
import org.traccar.storage.QueryIgnore;
import org.traccar.storage.StorageName;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@StorageName("tc_calendars")
public class Calendar extends ExtendedModel {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private byte[] data;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) throws IOException, ParserException {
        CalendarBuilder builder = new CalendarBuilder();
        calendar = builder.build(new ByteArrayInputStream(data));
        this.data = data;
    }

    private net.fortuna.ical4j.model.Calendar calendar;

    @QueryIgnore
    @JsonIgnore
    public net.fortuna.ical4j.model.Calendar getCalendar() {
        return calendar;
    }

    public Set<Period<Instant>> findPeriods(Date date) {
        if (calendar != null) {
            Instant instant = date.toInstant();
            return calendar.<VEvent>getComponents(Component.VEVENT).stream()
                    .flatMap(event -> {
                        Temporal sample = event.getDateTimeStart().getDate();
                        var period = new Period<>(convertToMatchingTemporal(instant, sample), Duration.ZERO);
                        return event.calculateRecurrenceSet(period).stream();
                    })
                    .map(p -> new Period<>(temporalToInstant(p.getStart()), temporalToInstant(p.getEnd())))
                    .collect(Collectors.toUnmodifiableSet());
        } else {
            return Set.of();
        }
    }

    public boolean checkMoment(Date date) {
        return !findPeriods(date).isEmpty();
    }

    private static Temporal convertToMatchingTemporal(Instant instant, Temporal sample) {
        if (sample instanceof LocalDate) {
            return instant.atZone(ZoneOffset.UTC).toLocalDate();
        } else if (sample instanceof LocalDateTime) {
            return instant.atZone(ZoneOffset.UTC).toLocalDateTime();
        } else if (sample instanceof ZonedDateTime) {
            return instant.atZone(((ZonedDateTime) sample).getZone());
        } else if (sample instanceof OffsetDateTime) {
            return instant.atOffset(((OffsetDateTime) sample).getOffset());
        } else {
            return instant;
        }
    }

    private static Instant temporalToInstant(Temporal temporal) {
        if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).toInstant();
        } else if (temporal instanceof OffsetDateTime) {
            return ((OffsetDateTime) temporal).toInstant();
        } else if (temporal instanceof LocalDateTime) {
            return ((LocalDateTime) temporal).toInstant(ZoneOffset.UTC);
        } else if (temporal instanceof LocalDate) {
            return ((LocalDate) temporal).atStartOfDay(ZoneOffset.UTC).toInstant();
        } else if (temporal instanceof Instant) {
            return (Instant) temporal;
        } else {
            throw new IllegalArgumentException("Unsupported Temporal type");
        }
    }

}
