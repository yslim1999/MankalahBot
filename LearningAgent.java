package learning;

import agent.Agent;
import agent.Minimax;
import agent.Side;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The custom agent class used in GameEngine.
 * This forms a buffer between the main thread, and the thread in which the
 * agent is running, to simulate sending the Agent messages, and receiving messages.
 */
public class LearningAgent extends Agent {
    private final ReentrantLock incomingLock, outgoingLock;
    private final Condition incomingCondition, outgoingCondition;
    private String outgoing;
    private String incoming;
    private Side side;

    public LearningAgent(int holes, int seeds, Minimax minimax, Side side) {
        super(holes, seeds, minimax);
        incomingLock = new ReentrantLock();
        incomingCondition = incomingLock.newCondition();
        outgoingLock = new ReentrantLock();
        outgoingCondition = outgoingLock.newCondition();
        outgoing = null;
        incoming = null;
        this.side = side;
    }
    @Override protected String recvMsg() throws IOException {
        try {
            incomingLock.lock();
            while(incoming == null) try { incomingCondition.await(); } catch(InterruptedException e) {}
            String str = incoming;
            incoming = null;
            incomingCondition.signalAll();
            return str;
        } finally {
            incomingLock.unlock();
        }
    }
    @Override protected void sendMsg(String msg) {
        try {
            outgoingLock.lock();
            while(outgoing != null) try { outgoingCondition.await(); } catch(InterruptedException e) {}
            outgoing = msg;
            outgoingCondition.signalAll();
        } finally {
            outgoingLock.unlock();
        }
    }
    public String outsideRecvMsg() {
        try {
            outgoingLock.lock();
            while(outgoing == null) try { outgoingCondition.await(); } catch(InterruptedException e) {}
            String str = outgoing;
            outgoing = null;
            outgoingCondition.signalAll();
            return str;
        } finally {
            outgoingLock.unlock();
        }
    }
    public void outsideSendMsg(String msg) {
        try {
            incomingLock.lock();
            while(incoming != null) try { incomingCondition.await(); } catch(InterruptedException e) {}
            incoming = msg;
            incomingCondition.signalAll();
        } finally {
            incomingLock.unlock();
        }
    }

    public Side getSide() {
        return side;
    }
    public void changeSide() {
        side = side.opposite();
    }
}


