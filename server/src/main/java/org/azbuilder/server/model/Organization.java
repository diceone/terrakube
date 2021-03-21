package org.azbuilder.server.model;

import com.yahoo.elide.annotation.Include;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Include(rootLevel = true, type = "organization")
@Getter
@Setter
@Entity
public class Organization {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;
    private String description;

    @OneToMany(mappedBy = "organization")
    private List<Workspace> workspaces;

    @OneToMany(mappedBy = "organization")
    private List<Module> modules;
}
