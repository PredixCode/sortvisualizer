package com.predixcode.sortvisualizer.algorithms;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import com.predixcode.sortvisualizer.core.StepCallback;
import com.predixcode.sortvisualizer.ui.SortElement;
import com.predixcode.sortvisualizer.ui.SortElement.ElementState;

public class TreeSort extends AbstractSortAlgorithm {

    // Inner class for BST Node
    private static class Node {
        SortElement elementData; // Store the SortElement itself
        Node left, right;
        int originalIndex; // To map back to UI element if needed for tree visualization

        Node(SortElement item, int originalIdx) {
            elementData = item;
            originalIndex = originalIdx;
            left = right = null;
        }
    }

    private Node bstRoot;
    private int currentIndexToInsert; // For building phase: index from original `elements` list
    private Deque<Node> traversalStack; // For in-order traversal phase
    private int currentIndexToPlace;  // For traversal phase: index in `elements` to place sorted item
    private boolean isSortedFlag = false;

    private enum TreeSortInternalState {
        IDLE,
        BUILDING_BST_PICK_ELEMENT,
        BUILDING_BST_TRAVERSE_AND_INSERT,
        TRAVERSAL_PREP,
        TRAVERSAL_GO_LEFT,
        TRAVERSAL_VISIT_NODE,
        TRAVERSAL_GO_RIGHT
    }
    private TreeSortInternalState currentState;
    private Node bstCurrentTraversalNode; // For traversing during insertion or in-order
    private SortElement elementToInsertVisually; // Element being inserted for visualization

    public TreeSort() {
        // Constructor
    }

    @Override
    public void initialize(List<SortElement> elements, StepCallback callback) {
        super.initialize(elements, callback);
    }

    @Override
    public void reset() {
        this.bstRoot = null;
        this.currentIndexToInsert = 0;
        this.traversalStack = new ArrayDeque<>();
        this.currentIndexToPlace = 0;
        this.isSortedFlag = (this.elements == null || this.elements.isEmpty());
        this.currentState = this.isSortedFlag ? TreeSortInternalState.IDLE : TreeSortInternalState.BUILDING_BST_PICK_ELEMENT;
        this.bstCurrentTraversalNode = null;
        this.elementToInsertVisually = null;

        if (this.elements != null && this.callback != null) {
            for (SortElement el : this.elements) el.setState(ElementState.NORMAL);
        }
    }

    @Override
    public String getName() {
        return "Tree Sort";
    }

    @Override
    public boolean nextStep() {
        if (isSortedFlag || callback.isStopRequested() || elements.isEmpty()) {
             if (!isSortedFlag && elements != null && !elements.isEmpty()) {
                for (SortElement el : elements) {
                     if (el.getState() != ElementState.SORTED) el.setState(ElementState.NORMAL);
                }
                callback.requestVisualUpdate();
            }
            isSortedFlag = true;
            return false;
        }

        switch (currentState) {
            case BUILDING_BST_PICK_ELEMENT:
                return handleBuildingBstPickElement();
            case BUILDING_BST_TRAVERSE_AND_INSERT:
                // This state would ideally be broken down further if visualizing BST traversal for insertion
                return handleBuildingBstInsert(); // Simplified for now
            case TRAVERSAL_PREP:
                bstCurrentTraversalNode = bstRoot;
                currentState = TreeSortInternalState.TRAVERSAL_GO_LEFT;
                return true;
            case TRAVERSAL_GO_LEFT:
                return handleTraversalGoLeft();
            case TRAVERSAL_VISIT_NODE:
                return handleTraversalVisitNode();
            case TRAVERSAL_GO_RIGHT:
                return handleTraversalGoRight();
            default:
                isSortedFlag = true;
                return false;
        }
    }

    @Override
    public boolean isSorted() {
        return this.isSortedFlag;
    }

    private boolean handleBuildingBstPickElement() {
        if (currentIndexToInsert < elements.size()) {
            elementToInsertVisually = elements.get(currentIndexToInsert); // Get the SortElement
            callback.reportElementStateChange(currentIndexToInsert, ElementState.PIVOT); // Highlight element to insert
            // For a more detailed step, next state would be TRAVERSE_TO_INSERT_POINT
            bstCurrentTraversalNode = bstRoot; // Start traversal from root for insertion
            currentState = TreeSortInternalState.BUILDING_BST_TRAVERSE_AND_INSERT;
            callback.requestVisualUpdate();
            return true;
        } else {
            // Finished building BST, prepare for in-order traversal
            // Clear visual states from build phase
            for(SortElement el : elements) el.setState(ElementState.NORMAL); 
            callback.requestVisualUpdate();

            currentState = TreeSortInternalState.TRAVERSAL_PREP;
            return true;
        }
    }
    
    // Simplified BST insertion for one step. A true step-by-step would trace the path.
    private boolean handleBuildingBstInsert() {
        if (elementToInsertVisually == null) { // Should have been picked
            currentState = TreeSortInternalState.BUILDING_BST_PICK_ELEMENT;
            return true;
        }

        // Perform the actual insertion logic (simplified here, not step-by-step for insertion path)
        // The 'originalIndex' in Node is for if you wanted to draw the tree and map nodes to original array positions
        bstRoot = insertRec(bstRoot, elementToInsertVisually, currentIndexToInsert);
        
        // After insertion, mark the original element as "processed" or NORMAL
        callback.reportElementStateChange(currentIndexToInsert, ElementState.NORMAL); // Or a "PROCESSED_INTO_TREE" state
        elementToInsertVisually = null;
        currentIndexToInsert++;
        currentState = TreeSortInternalState.BUILDING_BST_PICK_ELEMENT;
        callback.requestVisualUpdate(); // Show updated array (element state change)
        return true;
    }

    private Node insertRec(Node rootNode, SortElement elementToInsert, int originalIdx) {
        if (rootNode == null) {
            // Visually, this is where the new node is placed.
            // The SortElement itself is now part of the tree structure.
            return new Node(elementToInsert, originalIdx);
        }
        // For visualization: callback.reportCompare(elementToInsert.originalIndex, rootNode.elementData.originalIndex);
        if (elementToInsert.getValue() < rootNode.elementData.getValue()) {
            rootNode.left = insertRec(rootNode.left, elementToInsert, originalIdx);
        } else if (elementToInsert.getValue() >= rootNode.elementData.getValue()) { // Allow duplicates, place in right subtree
            rootNode.right = insertRec(rootNode.right, elementToInsert, originalIdx);
        }
        return rootNode;
    }


    private boolean handleTraversalGoLeft() {
        while (bstCurrentTraversalNode != null) {
            traversalStack.push(bstCurrentTraversalNode);
            // For visualization: highlight bstCurrentTraversalNode.elementData in its original array position
            // callback.reportElementStateChange(bstCurrentTraversalNode.originalIndex, ElementState.COMPARE);
            // callback.requestVisualUpdate();
            // Thread.sleep(callback.getAnimationDelayMs()); // If algorithm controls sleep
            bstCurrentTraversalNode = bstCurrentTraversalNode.left;
        }
        currentState = TreeSortInternalState.TRAVERSAL_VISIT_NODE;
        return true;
    }

    private boolean handleTraversalVisitNode() {
        if (traversalStack.isEmpty()) {
            // Traversal complete
            isSortedFlag = true;
            // callback.reportSortCompleted(); // Controller handles this
            return false;
        }

        Node nodeToVisit = traversalStack.pop();
        // Place the node's data into the elements list at currentIndexToPlace
        // The SortElement from the tree node is placed back into the main list.
        elements.set(currentIndexToPlace, nodeToVisit.elementData);
        callback.reportElementStateChange(currentIndexToPlace, ElementState.SORTED);
        // For visualization: also highlight nodeToVisit.elementData in tree if drawn separately
        
        currentIndexToPlace++;
        bstCurrentTraversalNode = nodeToVisit.right; // Prepare to go right
        currentState = TreeSortInternalState.TRAVERSAL_GO_RIGHT; // Or TRAVERSAL_GO_LEFT if right is null
        callback.requestVisualUpdate();
        return true;
    }
    
    private boolean handleTraversalGoRight() {
        // After visiting a node, we try to go right. Then the process repeats by going left from the right child.
        currentState = TreeSortInternalState.TRAVERSAL_GO_LEFT;
        return true; // bstCurrentTraversalNode is already set to node.right
    }
}
