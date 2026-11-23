package com.semih.ajanofisi.office;

import com.semih.ajanofisi.office.dto.SessionSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface OfficeEventRepository extends JpaRepository<OfficeEvent, Long> {

    /**
     * One row per session, newest activity first; a DTO projection, so no entities are loaded.
     * {@code max(e.id)} breaks ties between events saved in the same microsecond.
     */
    @Query(value = """
            select new com.semih.ajanofisi.office.dto.SessionSummary(
                e.sessionId,
                max(case when e.type = com.semih.ajanofisi.office.OfficeEventType.SESSION_STARTED
                         then e.detail end),
                min(e.createdAt),
                max(e.createdAt),
                count(e))
            from OfficeEvent e
            group by e.sessionId
            order by max(e.createdAt) desc, max(e.id) desc""",
            countQuery = "select count(distinct e.sessionId) from OfficeEvent e")
    Page<SessionSummary> findSessions(Pageable pageable);

    /** The newest events of a session (the caller puts them back in time order). */
    List<OfficeEvent> findTop500BySessionIdOrderByIdDesc(String sessionId);

    @Modifying
    @Query("delete from OfficeEvent e where e.createdAt < :before")
    int deleteOlderThan(Instant before);
}
