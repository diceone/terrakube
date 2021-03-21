package org.azbuilder.server.model;

import com.yahoo.elide.annotation.Include;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.UUID;


@Include(type = "secret")
@Getter
@Setter
@Entity
public class Secret {

    @Id
    @GeneratedValue
    private UUID id;

    private String key;
    private String value;

    @ManyToOne
    private Workspace workspace;
}
