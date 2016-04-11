package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.*;
import edu.cwru.sepia.util.Direction;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {

    // Pseudo state tracker to increase efficiency
    private StateTracker stateTracker;

    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     * <p>
     * You may find the following state methods useful:
     * <p>
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns all of the obstacles in the map
     * state.getResourceNode(Integer resourceID): Return a ResourceView for the given ID
     * <p>
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     * <p>
     * For a given unit you will need to find the attack damage, range and max HP
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit deals
     * unitView.getTemplateView().getBaseHealth(): The maximum amount of health of this unit
     *
     * @param state Current state of the episode
     */
    public GameState(State.StateView state) {
        stateTracker = new StateTracker(state);
    }

    /**
     * This constructor is used to instantiate the pseudo state tracker of this game state
     * @param stateTracker A state tracker shell to be employed by this game state
     */
    public GameState(StateTracker stateTracker){
        this.stateTracker = new StateTracker(stateTracker);
    }

    /**
     * You will implement this function.
     * <p>
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     * <p>
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     * <p>
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {
       return stateTracker.getUtility();
    }

    /**
     * You will implement this function.
     * <p>
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     * <p>
     * You may find it useful to iterate over all the different directions in SEPIA.
     * <p>
     * for(Direction direction : Directions.values())
     * <p>
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     *
     * @return All possible actions and their associated resulting game state
     */
    public List<GameStateChild> getChildren() {
        List <GameStateChild> children = new ArrayList<>();
        Queue<Integer> units = new PriorityQueue<>(getCurrentPlayersUnits().keySet());
        while (!units.isEmpty()){
            Integer unitId = units.poll();
            for (Direction dir1 : getCardinalDirections()){
                int xDestUnitId = getCurrentPlayersUnits().get(unitId).x + dir1.xComponent();
                int yDestUnitId = getCurrentPlayersUnits().get(unitId).y + dir1.yComponent();
                for (Integer unitRemaining : units){
                    for (Direction dir2 : getCardinalDirections()){
                        int xDestRemUnitId = getCurrentPlayersUnits().get(unitId).x + dir2.xComponent();
                        int yDestRemUnitId = getCurrentPlayersUnits().get(unitId).y + dir2.yComponent();
                        HashMap<Integer, Action> actionMap = new HashMap<>();
                        GameState state = new GameState(stateTracker);
                        if (positionAvailable(xDestUnitId, yDestUnitId)) {
                            actionMap.put(unitId, state.move(unitId, dir1));
                        }
                        if (positionAvailable(xDestRemUnitId, yDestRemUnitId)){
                            actionMap.put(unitRemaining, state.move(unitRemaining, dir2));
                        }
                        children.add(new GameStateChild(actionMap, state));
                    }
                }
            }
        }
        return children;
    }

    /**
     * Gets a list of the cardinal directions
     * @return An array of directions of length 4, populated by cardinal directions
     */
    private static Direction[] getCardinalDirections() {
        Direction[] directions = new Direction[4];
        directions[0] = Direction.NORTH;
        directions[1] = Direction.EAST;
        directions[2] = Direction.WEST;
        directions[3] = Direction.SOUTH;
        return directions;
    }

    /**
     * Tracks a unit's movement
     * @param unitID The ID of the unit performing the move
     * @param direction The direction of movement
     * @return The action resulting from performing the move
     */
    private Action move(Integer unitID, Direction direction) {
        return stateTracker.move(unitID, direction);
    }

    /**
     * Checks if the specified position is available to move into
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return True if the coordinates are clear, or false if there is an obstacle
     */
    private boolean positionAvailable(int x, int y) {
        return stateTracker.positionAvailable(x, y);
    }

    /**
     * Gets the current player
     * @return The player controlling the next move in this game state
     */
    public int getPlayer() {
        return stateTracker.getPlayer();
    }

    /**
     * Gets the set of all archers
     * @return A map of each archer's point-location to its respective ID
     */
    public Map<Integer, Point> getArchers() {
        return stateTracker.getArchers();
    }

    /**
     * Gets the set of all footmen
     * @return A map of each footman's point-location to its respective ID
     */
    public Map<Integer, Point> getFootmen() {
        return stateTracker.getFootmen();
    }

    /**
     * Gets the set of all units controlled by the current player
     * @return A map of each unit's point-location to its respective ID
     */
    public Map<Integer, Point> getCurrentPlayersUnits() {
        return (getPlayer() == 0) ? getFootmen() : getArchers();
    }

    /**
     * Computes the distance between two units in this game state
     * @param unitId1 The ID of the first unit
     * @param unitId2 The ID of the second unit
     * @return The Chebyshev distance between the two units' locations
     */
    private double getDistance(int unitId1, int unitId2) {
        return stateTracker.getDistance(unitId1, unitId2);
    }


}




