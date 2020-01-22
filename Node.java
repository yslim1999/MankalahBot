package agent;

import java.util.ArrayList;
import java.util.List;

public class Node {

    private List<Node> children;
    public final boolean isMaximising;
    public final Side side;
    public final Board board;
    private int heuristic;

    public Node(boolean isMaximising, Side side, Board board) {
        this.isMaximising = isMaximising;
        this.side = side;
        this.board = board;
        this.children = new ArrayList<>(7);
        this.heuristic = this.isMaximising() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    }

    public boolean isMaximising() {
        return this.isMaximising;
    }

    public boolean isTerminal() {
        return this.children.isEmpty();
    }

    public boolean isEndGame() {
        return this.board != null && Kalah.gameOver(this.board);
    }

    public int getHeuristic() {
        return heuristic;
    }

    public int evaluate(Heuristics heuristics) {
        this.heuristic = heuristics.evaluate(isMaximising ? side : side.opposite(), this.board);
        return this.heuristic;
    }

    public void addChild(Node node) {
        this.children.add(node);
    }

    public List<Node> getChildren() {
        return this.children;
    }

    private void stringHierarchy(StringBuilder str, int depth, int num, int maxDepth) {
        // Print ourself.
        for (int i = 0; i < depth; i++) {
            str.append(" |");
        }
        str.append("--[").append(num).append(']');
        // If we are terminal print our heuristic.
        if (isTerminal()) {
            str.append(": ").append(heuristic).append('\n');
        } else {
            // If we exceed max depth print ...
            if (depth >= maxDepth) {
                str.append(": ...\n");
            } else {
                // Otherwise print our children.
                str.append('\n');
                for (int i = 0; i < 7; i++) {
                    Node child;
                    // If child exists print it using hierarchy function.
                    if (children.size() > i && (child = children.get(i)) != null) {
                        child.stringHierarchy(str, depth + 1, i, maxDepth);
                    } else {
                        // If child does not exist print an "empty".
                        for (int e = 0; e <= depth; e++) {
                            str.append(" |");
                        }
                        str.append("--[").append(i).append("]: empty\n");
                    }
                }
            }
        }
    }

    /**
     * @param depth The maximum depth that we go through the node tree when generating the
     *              string representation.
     * @return Returns a string representation of this node tree.
     */
    public String toStringDepth(int depth) {
        StringBuilder str = new StringBuilder();
        stringHierarchy(str, 0, 0, depth);
        return str.toString();
    }
}
