package org.syncninja.model;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

@NodeEntity
public abstract class NinjaNode extends SyncNode {

    @Relationship(type = "ParentOf")
    private final List<Branch> branchList = new ArrayList<>();

    @Relationship(type = "nextCommit")
    private Commit nextCommit;

    public NinjaNode() {
    }

    public List<Branch> getBranchList() {
        return branchList;
    }

    public Commit getNextCommit() {
        return nextCommit;
    }

    public void setNextCommit(Commit nextCommit) {
        this.nextCommit = nextCommit;
    }
}
