package agent;

import java.io.IOException;

/*This is the agent class. It is the agent that will be used to play the Mancala.
 *  It communicates with the game engine provided in the ai and games course.
 */

public class Agent {

    //public static final int SWAP_DEPTH = 5;
    public static int MOVE_DEPTH = 6;

    // Hold instance of the agent.Minimax with the Agents Heuristic weights
    private Minimax minimax;
    // The number of holes the game has on each side.
    private int holes;
    // The side our agent is on.
    private Side ourSide;
    // The current board state of the game.
    private Board board;
    // The game.
    private Kalah kalah;


    public Agent(final int holes, final int seeds, final int[] weights) {
        this.ourSide = Side.SOUTH;
        this.holes = holes;
        this.board = new Board(holes, seeds);
        this.kalah = new Kalah(board);
        this.minimax = new Minimax(weights).start();
    }

    public Agent(final int holes, final int seeds, Minimax minimax) {
        this.ourSide = Side.SOUTH;
        this.holes = holes;
        this.board = new Board(holes, seeds);
        this.kalah = new Kalah(board);
        this.minimax = minimax;
    }

    /* This method is called when our agent needs to play.
      It describes what the agent will do depending on the messages
      it receives from the game engine.
     */

    public void play() throws InvalidMessageException, IOException {
        // Takes in the message from game engine
        String message = recvMsg();
        MsgType msgType = Protocol.getMessageType(message);

        // Determines if we have the capability to swap.
        boolean canSwap = false;

        // If we go first
        boolean firstMove = false;

        if (msgType == MsgType.END) {
            // Ending the game
            return;
        }

        if (msgType != MsgType.START) {
            throw new InvalidMessageException("Did not get the START message from game engine.");
        } else {
            // Here we are deciding whether we go first or second by interpreting the message.
            if (Protocol.interpretStartMsg(message)) {
                Main.log("We start SOUTH");
                this.ourSide = Side.SOUTH;
                firstMove = true;
            } else {
                // If we go second then we get to use the swap
                Main.log("We start NORTH");
                this.ourSide = Side.NORTH;
                canSwap = true;
            }
        }

        if (firstMove) {
            //hardcoded first move: hole 3
            sendMsg(Protocol.createMoveMsg(3));
        }

        while (true) {
            // Our move message
            String actionMessage = null;

            message = recvMsg();   // Wait for a message from the Game Engine
            Main.log(ourSide+": Received Message \n" + message);
            msgType = Protocol.getMessageType(message);
            // If get end message we just end the game
            if (msgType == MsgType.END) {
                break;
            }
            if (msgType != MsgType.STATE) {
                throw new InvalidMessageException("This should be a STATE messsage but isn't");
            }
            // So now we get to make a move based on what happens
            // We want to also interpret the state message

            // interpretStateMsg: information about the move that led to the state change and
            //     * who's turn it is next.
            Protocol.MoveTurn moveTurn = Protocol.interpretStateMsg(message, this.board);

            // This means a swapped occurred thus switching our positions
            if (moveTurn.move == -1) {
                Main.log(ourSide+": Opponent is swapping");
                this.ourSide = this.ourSide.opposite();
            }

            // If its the end of the turn or if we can't go again
            // then do nothing and wait for the next message
            if (moveTurn.end || !moveTurn.ourTurn) {
                continue;
            }

            // Otherwise, it is our turn.
            Main.log(ourSide+": Our turn");

            // If we can swap means we go second
            if (canSwap) {
                // If the move is good, swap.
                // Else the move was "fair", or bad, we don't swap
                //sends swap message if swapping, else leave the message unchanged (null)
                //and it will eventually do a move instead.
                if (moveTurn.move == 1 || moveTurn.move == 3 || moveTurn.move == 4 || moveTurn.move == 5){
                    agent.Main.log("We are swapping");
                    actionMessage = agent.Protocol.createSwapMsg();
                    ourSide = ourSide.opposite();
                }
                // Make canSwap false because we only can do it once
                canSwap = false;
            }
            /* Here we determine how we will move (minimax + heuristics)
              Predict what the best move is.
              State which hole to pick
              and then create move message.
            */

            if (actionMessage == null) {
                Node root = new Node(true, this.ourSide, this.board);
                int value = this.minimax.minimax(root, MOVE_DEPTH);
                actionMessage = Protocol.createMoveMsg(value);
            }
            Main.log(ourSide+": Our action message\n" + actionMessage);
            // This is used only after manipulation of move message [agent.Main.sendMsg(message);]
            // This lets the game engine know what our move was.
            sendMsg(actionMessage);
        }
    }

    /**
     * Stops our internal Minimax instance.
     * This should be used if the Agent class is being used on it's own, such
     * as outside the learning system.
     */
    public void stop() {
        minimax.stop();
    }

    /**
     * This must be used instead of Main.recvMsg, within the agent class.
     * This method is overridden to work properly with the learning system.
     * @return Returns the message read.
     * @throws IOException Throws an IOException if a problem happens.
     */
    protected String recvMsg() throws IOException {
        return Main.recvMsg();
    }

    /**
     * This must be used instead of Main.sendMsg, within the Agent class.
     * This method is overridden to work properly with the learning system.
     * @param msg The message to write.
     */
    protected void sendMsg(String msg) {
        Main.sendMsg(msg);
    }
}
