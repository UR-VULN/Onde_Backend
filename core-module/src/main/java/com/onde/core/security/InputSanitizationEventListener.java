package com.onde.core.security;

import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;

/**
 * MariaDB에 저장되는 모든 JPA 엔티티에 대해 insert/update 직전 XSS 필터를 적용합니다.
 * repository.save(), saveAll() 등 Hibernate를 거치는 모든 쓰기 경로에 자동 적용됩니다.
 */
public class InputSanitizationEventListener implements PreInsertEventListener, PreUpdateEventListener {

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        EntityFieldSanitizer.sanitizeEntity(
                event.getEntity(),
                event.getState(),
                event.getPersister().getPropertyNames()
        );
        return false;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        EntityFieldSanitizer.sanitizeEntity(
                event.getEntity(),
                event.getState(),
                event.getPersister().getPropertyNames()
        );
        return false;
    }
}
