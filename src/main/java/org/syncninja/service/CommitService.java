package org.syncninja.service;

import org.syncninja.model.Commit;
import org.syncninja.model.NinjaNode;
import org.syncninja.model.commitTree.CommitDirectory;
import org.syncninja.repository.CommitRepository;
import org.syncninja.util.ResourceBundleEnum;

public class CommitService {
    private final CommitRepository commitRepository;
    private final StateTreeService stateTreeService;

    public CommitService() {
        this.commitRepository = new CommitRepository();
        this.stateTreeService = new StateTreeService();
    }

    public Commit createStagedCommit(){
        Commit commit = new Commit();
        commit.setCommitted(false);
        return commitRepository.save(commit);
    }

    public Commit save(String message, Commit commit) throws Exception {
        if(commit.getCommitTree() == null){
            throw new Exception(ResourceMessagingService.getMessage(ResourceBundleEnum.STAGE_AREA_IS_EMPTY));
        }
        commit.setCommitted(true);
        commit.setMessage(message);
        commit.setNextCommit(createStagedCommit());
        stateTreeService.updateStateRoot(stateTreeService.getStateRoot(commit.getCommitTree().getPath()), commit);
        return commitRepository.save(commit);
    }

    public void addCommitTree(CommitDirectory commitDirectory) throws Exception {
        NinjaNode currentNinjaNode = stateTreeService.getStateRoot(commitDirectory.getPath()).getCurrentCommit();
        if (currentNinjaNode == null){
            currentNinjaNode = stateTreeService.getStateRoot(commitDirectory.getPath()).getCurrentBranch();
        }
        Commit commit = currentNinjaNode.getNextCommit();
        commit.setCommitTree(commitDirectory);
        commitRepository.save(commit);
    }
}