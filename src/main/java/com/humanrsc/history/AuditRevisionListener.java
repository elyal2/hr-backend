package com.humanrsc.history;

import io.quarkus.arc.Unremovable;
import org.hibernate.envers.RevisionListener;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Unremovable
public class AuditRevisionListener implements RevisionListener {
    @Override
    public void newRevision(Object revisionEntity) {
        // No-op: extendido solo para usar long en id de revisión
    }
}
