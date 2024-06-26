package org.syncninja.service;

import org.neo4j.ogm.model.Result;
import org.syncninja.dto.CommitFileDTO;
import org.syncninja.dto.FileStatusEnum;
import org.syncninja.dto.StatusFileDTO;
import org.syncninja.model.Branch;
import org.syncninja.model.Commit;
import org.syncninja.model.NinjaNode;
import org.syncninja.model.committree.CommitDirectory;
import org.syncninja.model.committree.CommitFile;
import org.syncninja.model.committree.CommitNode;
import org.syncninja.model.statetree.StateNode;
import org.syncninja.model.statetree.StateRoot;
import org.syncninja.repository.BranchRepository;
import org.syncninja.repository.NinjaNodeRepository;
import org.syncninja.repository.StateTreeRepository;
import org.syncninja.util.CommitContainer;
import org.syncninja.util.FileTrackingState;
import org.syncninja.util.ResourceBundleEnum;
import org.syncninja.util.StateTreeUpdate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CheckoutService {
    private final BranchRepository branchRepository;
    private final StateTreeService stateTreeService;
    private final StateTreeRepository stateTreeRepository;
    private final CommitService commitService;
    private final NinjaNodeRepository ninjaNodeRepository;
    private final PathFinderService pathFinderService;
    private final StateTreeUpdate stateTreeUpdate;
    private final StatusService statusService;


    public CheckoutService() {
        this.branchRepository = new BranchRepository();
        this.commitService = new CommitService();
        this.stateTreeService = new StateTreeService();
        this.stateTreeRepository = new StateTreeRepository();
        this.ninjaNodeRepository = new NinjaNodeRepository();
        this.pathFinderService = new PathFinderService();
        this.stateTreeUpdate = new StateTreeUpdate();
        this.statusService = new StatusService();
    }

    public void createNewBranch(String branchName, String path) throws Exception {
        if (branchRepository.findByName(branchName, path).isPresent()) {
            throw new Exception(ResourceMessagingService.getMessage(ResourceBundleEnum.BRANCH_NAME_EXISTS, new Object[]{branchName}));
        }
        FileTrackingState state = statusService.getState(path);
        List<StatusFileDTO> untracked = state.getUntracked();
        if(untracked.isEmpty()) {
            Branch newBranch = new Branch(branchName);
            newBranch.setNextCommit(commitService.createStagedCommit());
            StateRoot stateRoot = stateTreeService.getStateRoot(path);
            linkNewBranchWithNinjaNode(stateRoot, newBranch);
            updateStateRootWithNewBranch(stateRoot, newBranch);
            updateFileSystemWithTargetStagingArea(newBranch.getNextCommit().getCommitTreeRoot());
        } else {
            throw new Exception(ResourceMessagingService.getMessage(ResourceBundleEnum.CHECKOUT_FAILED_UNSTAGED_CHANGES));
        }
    }

    public void updateStateRootWithNewBranch(StateRoot stateRoot, Branch newBranch) {
        stateRoot.setCurrentBranch(newBranch);
        stateRoot.setCurrentCommit(null);
        stateTreeRepository.save(stateRoot);
    }

    public void linkNewBranchWithNinjaNode(StateRoot stateRoot, Branch newBranch) {
        NinjaNode ninjaNode = stateRoot.getCurrentNinjaNode();
        ninjaNode.getBranchList().add(newBranch);
        ninjaNodeRepository.save(ninjaNode);
    }

    public void checkout(String branchName, String path) throws Exception {
        StateRoot stateRoot = stateTreeService.getStateRoot(path);
        Optional<Branch> branchOptional = branchRepository.findByName(branchName, path);
        if (branchOptional.isPresent()) {
            FileTrackingState state = statusService.getState(path);
            List<StatusFileDTO> untracked = state.getUntracked();

            if(untracked.isEmpty()) {
                Branch branch = branchOptional.get();
                // get both sides of the path
                NinjaNode currentNode = stateRoot.getCurrentNinjaNode().getNextCommit();
                NinjaNode targetNode = branch.getLastNinjaNode().getNextCommit();
                Result result = branchRepository.getPathOfNinjaNodes(currentNode, targetNode).get();

                // get the relationships and nodes in the path
                Map<String, String>[] relationships = pathFinderService.getRelationshipsInPath(result);
                ArrayList<NinjaNode> ninjaNodesInPath = pathFinderService.getNinjaNodesInPath(result);

                //use these arrays to update the file system and the state tree and they are sorted
                CommitContainer commitContainer = new CommitContainer();
                ArrayList<NinjaNode> addedCommits = commitContainer.getCommitsToAdd();
                ArrayList<NinjaNode> removedCommits = commitContainer.getCommitsToRemove();

                // start the checkout logic
                NinjaNode ancestorNode = pathFinderService.getAncestorNode(relationships, ninjaNodesInPath);
                pathFinderService.findOutOfAncestor(addedCommits, ancestorNode, ninjaNodesInPath, relationships);
                pathFinderService.findGoingToAncestor(removedCommits, ancestorNode, ninjaNodesInPath, relationships);
                //now the arrays have the list of commits to add and remove

                //updating stateTree
                Map<String, StateNode> stateTree = stateTreeService.getStateTree(stateRoot);
                Map<StateNode, FileStatusEnum> fileStateMap = new HashMap<>();
                stateTreeUpdate.updateStateTreeByRemovingCommits(stateTree, removedCommits, fileStateMap);
                stateTreeUpdate.updateStateTreeByAddingCommits(stateTree, addedCommits, fileStateMap);
                stateTreeRepository.updateStateRoot(stateRoot, branch);
                updateFileSystemWithCurrentStagingArea(((Commit) currentNode).getCommitTreeRoot());
                stateTreeUpdate.reflectStateTreeOnFileSystem(fileStateMap);
                updateFileSystemWithTargetStagingArea(((Commit) targetNode).getCommitTreeRoot());
            } else {
                throw new Exception(ResourceMessagingService.getMessage(ResourceBundleEnum.CHECKOUT_FAILED_UNSTAGED_CHANGES));
            }
        } else {
            throw new Exception(ResourceMessagingService.getMessage(ResourceBundleEnum.BRANCH_NOT_FOUND, new Object[]{branchName}));
        }
    }


    private void updateFileSystemWithTargetStagingArea(CommitDirectory commitDirectory) throws IOException {
        if(commitDirectory == null){
            return;
        }
        for(CommitNode commitNode: commitDirectory.getCommitNodeList()) {
            if(commitNode instanceof CommitDirectory){
                updateFileSystemWithTargetStagingArea((CommitDirectory) commitNode);

                if(commitNode.getStatusEnum() == FileStatusEnum.IS_NEW) {
                    Files.createDirectories(Path.of(commitNode.getFullPath()));
                } else if(commitNode.getStatusEnum() == FileStatusEnum.IS_DELETED) {
                    Files.delete(Path.of(commitNode.getFullPath()));
                }
            } else {
                CommitFile commitFile = (CommitFile) commitNode;

                if(commitFile.getStatusEnum() == FileStatusEnum.IS_DELETED) {
                    Files.delete(Path.of(commitNode.getFullPath()));
                } else if(commitFile.getStatusEnum() == FileStatusEnum.IS_NEW) {
                    Files.write(Path.of(commitNode.getFullPath()), commitFile.getNewValuesList());
                } else {
                    List<String> lines = Files.readAllLines(Path.of(commitNode.getFullPath()));
                    for(int index = 0; index < commitFile.getLineNumberList().size(); index++) {
                        int indexValue = commitFile.getLineNumberList().get(index);
                        String value = commitFile.getNewValuesList().get(index);
                        if(indexValue - 1 < lines.size()) {
                            lines.set(indexValue - 1, value);
                        } else {
                            lines.add(value);
                        }
                    }
                    Files.write(Path.of(commitNode.getFullPath()), lines);
                }
            }
        }
    }

    private void updateFileSystemWithCurrentStagingArea(CommitDirectory commitDirectory) throws IOException {
        if(commitDirectory == null){
            return;
        }
        for(CommitNode commitNode: commitDirectory.getCommitNodeList()) {
            if(commitNode instanceof CommitDirectory){
                updateFileSystemWithCurrentStagingArea((CommitDirectory) commitNode);
                if(commitNode.getStatusEnum() == FileStatusEnum.IS_NEW) {
                    Files.delete(Path.of(commitNode.getFullPath()));
                } else if(commitNode.getStatusEnum() == FileStatusEnum.IS_DELETED) {
                    Files.createDirectories(Path.of(commitNode.getFullPath()));
                }
            } else {
                CommitFile commitFile = (CommitFile) commitNode;
                if(commitFile.getStatusEnum() == FileStatusEnum.IS_DELETED) {
                    Files.write(Path.of(commitNode.getFullPath()), commitFile.getOldValuesList());
                } else if(commitFile.getStatusEnum() == FileStatusEnum.IS_NEW) {
                    Files.delete(Path.of(commitNode.getFullPath()));
                } else {
                    List<String> lines = Files.readAllLines(Path.of(commitNode.getFullPath()));
                    for(int index = 0; index < commitFile.getLineNumberList().size(); index++) {
                        int indexValue = commitFile.getLineNumberList().get(index);
                        if(indexValue - 1 >= commitFile.getOldValuesList().size()){
                            lines.remove(indexValue - 1);
                        }
                        else{
                            String value = commitFile.getOldValuesList().get(index);
                            lines.set(indexValue - 1 , value);
                        }
                    }

                    for(int i = lines.size() - 1; i >= 0; i--) {
                        int index = commitFile.getLineNumberList().indexOf(i + 1);
                        if(index == -1){
                            break;
                        }
                        if(lines.get(i).equals("") && commitFile.getOldValuesList().get(index).equals("") && !commitFile.getNewValuesList().get(index).equals("")) {
                            lines.remove(i);
                        } else {
                            break;
                        }
                    }

                    Files.write(Path.of(commitNode.getFullPath()), lines);
                }
            }
        }
    }
}
