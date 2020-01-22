package agent;

import java.util.List;

public class Minimax {

    private Heuristics heuristics;

    private BranchThread[] branches = new BranchThread[7];

    public Minimax(int... weights) {
        if (weights.length == 0) {
            heuristics = new Heuristics();
        } else {
            heuristics = new Heuristics(weights);
        }
    }

    public void setHeuristics(Heuristics heuristics) {
        this.heuristics = heuristics;
    }

    public Minimax start() {
        for (int i = 0; i < 7; i++) {
            branches[i] = new BranchThread(i+1);
            branches[i].start();
        }
        return this;
    }

    public void stop() {
        for (BranchThread branch : this.branches) {
            branch.stopThread();
        }
    }

    public int minimax(Node root, int depth) {
        this.generateChildren(root);

        for (int i = 0; i < 7; i++) {
            Node branchNode = root.getChildren().get(i);
            this.branches[i].writeInput(branchNode, depth);
        }

        int evaluation;
        int hole = 1;
        if (root.isMaximising()) {
            evaluation = Integer.MIN_VALUE;
            for (int i = 1; i <= 7; i++) {
                if (this.branches[i-1].root == null) continue;
                int branchEval = this.branches[i-1].readOutput();
                if (branchEval > evaluation) {
                    evaluation = branchEval;
                    hole = i;
                }
            }
        } else {
            evaluation = Integer.MAX_VALUE;
            for (int i = 1; i <= 7; i++) {
                if (this.branches[i-1].root == null) continue;
                int branchEval = this.branches[i-1].readOutput();
                if (branchEval < evaluation) {
                    evaluation = branchEval;
                    hole = i;
                }
            }
        }
        return hole;
    }

    private void generateChildren(Node parent) {
        for (int i = 1; i <= parent.board.getNoOfHoles(); i++) {
            Board newBoard = new Board(parent.board);
            Move move = new Move(parent.side, i);
            if (!Kalah.isLegalMove(newBoard, move)) {
                parent.addChild(null);
                continue;
            }

            Side next = Kalah.makeMove(newBoard, move);
            boolean nextMaximising = (next == parent.side) == parent.isMaximising();
            Node n = new Node(nextMaximising, next, newBoard);
            parent.addChild(n);
        }
    }

    public int[] minimaxArray(Node root, int depth) {
        this.generateChildren(root);

        for (int i = 0; i < 7; i++) {
            Node branchNode = root.getChildren().get(i);
            this.branches[i].writeInput(branchNode, depth);
        }

        int[] result = new int[8];

        for (int i = 1; i < 8; i++) {
            if (this.branches[i-1].root == null) continue;
            int branchEval = this.branches[i-1].readOutput();
            result[i] = branchEval;
        }
        return result;
    }

    public class BranchThread extends Thread {

        // Internals
        public final int branch;
        private boolean evaluating;
        private boolean request;

        // Inputs
        public Node root;
        public int depth;

        // Output
        private int heuristic;

        private final Object lock = new Object();

        private boolean shouldRun;

        public BranchThread(int branch) {
            super("BranchThread-"+branch);
            this.branch = branch;
        }

        public void writeInput(Node root, int depth) {
            synchronized(lock) {
                this.root = root;
                if (this.root == null) return;
                this.depth = depth;
                this.request = true;
                this.evaluating = true;
                this.lock.notifyAll();
            }
        }

        public int readOutput() {
            synchronized(lock) {
                while (this.evaluating) {
                    try {
                        this.lock.wait();
                    } catch (InterruptedException ignored) { }
                }
                return this.heuristic;
            }
        }

        @Override
        public synchronized void start() {
            synchronized(lock) {
                this.shouldRun = true;
            }
            super.start();
        }

        public void stopThread() {
            synchronized(lock) {
                this.shouldRun = false;
                this.lock.notifyAll();
            }
        }

        @Override
        public void run() {
            while(true) {

                synchronized(lock) {
                    if (!shouldRun) break;

                    if (!request) {
                        try {
                            lock.wait();
                        } catch(InterruptedException e) {}
                    }

                    // Catch spurious wakeups.
                    if (!request) continue;

                    request = false;
                    evaluating = true;
                }

                this.heuristic = this.minimax(this.root, this.depth, Integer.MIN_VALUE, Integer.MAX_VALUE);

                synchronized (lock) {
                    evaluating = false;
                    lock.notifyAll();
                }
            }
        }

        public int minimax(Node pos, int depth, int alpha, int beta) {
            // Base Case
            if ((depth <= 0 && pos.isMaximising()) || pos.isEndGame()) {
                return pos.evaluate(heuristics);
            }

            Minimax.this.generateChildren(pos);

            // agent.Minimax
            if (pos.isMaximising()) {
                int maxEvaluation = Integer.MIN_VALUE;
                List<Node> children = pos.getChildren();
                for (int i = 0; i < children.size(); i++) {
                    Node child = children.get(i);
                    if (child == null) {
                        continue;
                    }
                    int evaluation = minimax(child, depth - 1, alpha, beta);
                    if (evaluation > maxEvaluation) {
                        maxEvaluation = evaluation;
                    }
                    alpha = Math.max(alpha, evaluation);
                    if (beta <= alpha) {
                        break;
                    }
                }
                return maxEvaluation;
            } else {
                int minEvaluation = Integer.MAX_VALUE;
                List<Node> children = pos.getChildren();
                for (int i = 0; i < children.size(); i++) {
                    Node child = children.get(i);
                    if (child == null) {
                        continue;
                    }
                    int evaluation = minimax(child, depth - 1, alpha, beta);
                    if (evaluation < minEvaluation) {
                        minEvaluation = evaluation;
                    }
                    beta = Math.min(beta, evaluation);
                    if (beta <= alpha) {
                        break;
                    }
                }
                return minEvaluation;
            }
        }
    }
}
