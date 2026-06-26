package com.onde.core.config;

import com.onde.core.security.InputSanitizationEventListener;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InputSanitizationHibernateConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final InputSanitizationEventListener inputSanitizationEventListener;

    public InputSanitizationHibernateConfig(
            EntityManagerFactory entityManagerFactory,
            InputSanitizationEventListener inputSanitizationEventListener
    ) {
        this.entityManagerFactory = entityManagerFactory;
        this.inputSanitizationEventListener = inputSanitizationEventListener;
    }

    @PostConstruct
    public void registerInputSanitizationListeners() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory
                .getServiceRegistry()
                .getService(EventListenerRegistry.class);

        registry.getEventListenerGroup(EventType.PRE_INSERT)
                .appendListener(inputSanitizationEventListener);
        registry.getEventListenerGroup(EventType.PRE_UPDATE)
                .appendListener(inputSanitizationEventListener);
    }
}
