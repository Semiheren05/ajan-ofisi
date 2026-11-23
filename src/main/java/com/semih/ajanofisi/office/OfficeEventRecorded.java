package com.semih.ajanofisi.office;

import com.semih.ajanofisi.office.dto.OfficeEventResponse;

/** Published after an event is saved; the live stream sends it once the transaction commits. */
public record OfficeEventRecorded(OfficeEventResponse event) {
}
