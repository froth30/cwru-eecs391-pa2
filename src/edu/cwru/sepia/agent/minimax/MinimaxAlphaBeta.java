package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MinimaxAlphaBeta extends Agent {

    private final int numPlys;

    public MinimaxAlphaBeta(int playernum, String[] args)
    {
        super(playernum);

        if(args.length < 1)
        {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }

        numPlys = Integer.parseInt(args[0]);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);
        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this.
     *
     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
     * and book for more information.
     *
     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
     * code into other functions and methods)
     *
     * @param node The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     */
        public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta) {

            // Obtain ordered list of children
            List<GameStateChild> children = orderChildrenWithHeuristics(node.state.getChildren());

            // Return current node if it is a terminal node or if the remaining depth is zero
            if (depth <= 0 || children.isEmpty())
                return node;

            // Search children
            if (node.state.getPlayer() == 0) {
            // maximizing player
                for (GameStateChild child : children) {
                    node = max(node, alphaBetaSearch(child, depth - 1, alpha, beta));
                    alpha = Math.max(alpha, node.state.getUtility());
                    if (beta <= alpha) break;
                }
            } else {
            // minimizing player
                for (GameStateChild child : children) {
                    node = min(node, alphaBetaSearch(child, depth - 1, alpha, beta));
                    beta = Math.min(beta, node.state.getUtility());
                    if (beta <= alpha) break;
                }
            }

            // Return best child
            return node;

        }

    /**
     * Finds the node with the minimum utility value
     * @param node1 A node in the game state tree
     * @param node2 Another node in the game state tree
     * @return The node with the lesser utility, or node1 if their utilities are equal
     */
    private GameStateChild min(GameStateChild node1, GameStateChild node2) {
        if (node1.action == null)
            return node2;
        return (node2.state.getUtility() < node1.state.getUtility()) ? node2 : node1;
    }

    /**
     * Finds the node with the maximum utility value
     * @param node1 A node in the game state tree
     * @param node2 Another node in the game state tree
     * @return The node with the greater utility, or node1 if their utilities are equal
     */
    private GameStateChild max(GameStateChild node1, GameStateChild node2) {
        if (node1.action == null)
            return node2;
        return (node2.state.getUtility() > node1.state.getUtility()) ? node2 : node1;
    }

    /**
     * You will implement this.
     *
     * Given a list of children you will order them according to heuristics you make up.
     * See the assignment description for suggestions on heuristics to use when sorting.
     *
     * Use this function inside of your alphaBetaSearch method.
     *
     * Include a good comment about what your heuristics are and why you chose them.
     *
     * @param children
     * @return The list of children sorted by your heuristic.
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children) {
        Collections.sort(children, (a, b) -> Double.compare(a.state.getUtility(), b.state.getUtility()));
        return children;
    }

}
