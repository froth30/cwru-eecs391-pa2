package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.util.DistanceMetrics;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * This class is used as a lightweight shell for pseudo-tracking an actual game state object
 */
public class StateTracker {

    private int numPlayers;
    private int turnNumber;
    private double xExtent;
    private double yExtent;
    private Map<Integer, Point> footmen;
    private Map<Integer, Point> archers;
    private Map<Integer, Integer> unitHPs;
    private Map<Integer, Integer> unitAttackRanges;
    private Map<Integer, Point> resources;

    /**
     * This constructor initializes this state tracker using a state view
     * @param state The state view to represent by this tracker
     */
    public StateTracker(State.StateView state) {
        xExtent = state.getXExtent();
        yExtent = state.getYExtent();
        turnNumber = state.getTurnNumber();
        numPlayers = state.getPlayerNumbers().length;
        resources = initializeResources(state.getAllResourceNodes());
        footmen = initializeLocationsFromUnits(state.getUnits(0));
        archers = initializeLocationsFromUnits(state.getUnits(1));
        unitHPs = initializeUnitHPsFromState(state.getAllUnits());
        unitAttackRanges = initializeUnitAttackRangesFromState(state.getAllUnits());
    }

    /**
     * This constructor initializes this state tracker using another state tracker
     * @param stateTracker The state tracker to represent by this tracker
     */
    public StateTracker(StateTracker stateTracker) {
        xExtent = stateTracker.getXExtent();
        yExtent = stateTracker.getYExtent();
        numPlayers = stateTracker.getNumPlayers();
        turnNumber = stateTracker.getTurnNumber();
        resources = stateTracker.getResources();
        footmen = initializeLocationsFromTracker(stateTracker.getFootmen());
        archers = initializeLocationsFromTracker(stateTracker.getArchers());
        unitHPs = new HashMap<>(stateTracker.getUnitHPs());
        unitAttackRanges = stateTracker.getUnitAttackRanges();
    }

    /**
     * Initializes the HP of all units represented in this state tracker
     * @param allUnits A list of unit views
     * @return A map of each unit's HP to its respective ID
     */
    private Map<Integer, Integer> initializeUnitHPsFromState(List<Unit.UnitView> allUnits) {
        HashMap<Integer, Integer> map = new HashMap<>();
        for (Unit.UnitView unit : allUnits){
            map.put(unit.getID(), unit.getHP());
        }
        return map;
    }

    /**
     * Initializes the attack range of all units represented in this state tracker
     * @param allUnits A list of unit views
     * @return A map of each unit's attack range to its respective ID
     */
    private Map<Integer, Integer> initializeUnitAttackRangesFromState(List<Unit.UnitView> allUnits) {
        HashMap<Integer, Integer> map = new HashMap<>();
        for (Unit.UnitView unit : allUnits){
            map.put(unit.getID(), unit.getTemplateView().getRange());
        }
        return map;
    }

    /**
     * Initializes the locations of all units represented in this state tracker
     * @param units A map of each unit's point to its respective ID
     * @return A map of each unit's location to its respective ID
     */
    private Map<Integer, Point> initializeLocationsFromTracker(Map<Integer, Point> units) {
        HashMap<Integer, Point> map = new HashMap<>();
        for (Integer unitId : units.keySet()){
            map.put(unitId, new Point(units.get(unitId).x, units.get(unitId).y));
        }
        return map;
    }

    /**
     * Initializes the locations of all units represented in this state tracker
     * @param units A list of unit views
     * @return A hash map of each unit's location to its respective ID
     */
    private HashMap<Integer, Point> initializeLocationsFromUnits(java.util.List<Unit.UnitView> units){
        HashMap<Integer, Point> map = new HashMap<>();
        for (Unit.UnitView unit : units){
            map.put(unit.getID(), new Point(unit.getXPosition(), unit.getYPosition()));
        }
        return map;
    }

    /**
     * Initializes the locations of all resources represented in this state tracker
     * @param resources A list of resources
     * @return A map of each resource's location to its respective ID
     */
    private Map<Integer,Point> initializeResources(java.util.List<ResourceNode.ResourceView> resources) {
        HashMap<Integer, Point> map = new HashMap<>();
        for (ResourceNode.ResourceView resource : resources){
            map.put(resource.getID(), new Point(resource.getXPosition(), resource.getYPosition()));
        }
        return map;
    }

    public Action move(Integer unitId, Direction direction){
        if (getPlayer() == 0) {
            footmen.get(unitId).translate(direction.xComponent(), direction.yComponent());
        } else {
            archers.get(unitId).translate(direction.xComponent(), direction.yComponent());

        }
        return Action.createPrimitiveMove(unitId, direction);
    }

    public boolean positionAvailable(int x, int y){
        return isInBounds(x, y) && !isBlockedByUnit(x, y) && !isBlockedByResource(x, y);
    }

    private boolean isBlockedByResource(int x, int y) {
        return resources.containsValue(new Point(x, y));
    }

    private boolean isBlockedByUnit(int x, int y) {
        return archers.containsValue(new Point(x, y))
                || footmen.containsValue(new Point(x, y));
    }

    private boolean isInBounds(int x, int y) {
        return (x >= 0 && y >= 0 && x <= xExtent && y <= yExtent);
    }

    public int getPlayer() {
        return turnNumber % numPlayers;
    }

    public Map<Integer, Point> getArchers() {
        return archers;
    }

    public Map<Integer, Point> getFootmen() {
        return footmen;
    }

    public int getNumPlayers() {
        return numPlayers;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public double getXExtent() {
        return xExtent;
    }

    public double getYExtent() {
        return yExtent;
    }

    public Map<Integer,Point> getResources() {
        return resources;
    }

    /**
     * The main utility function representing this state tracker. This method uses several other helper methods.
     * @return A utility value assessing the score of this state
     */
    public double getUtility() {
        return getUtilityFromTargetDistance() * 100 +
                getUtilityFromTargetCornering() * 100 +
                getUtilityFromObstacles() * 1;
    }

    private int getDistance(Point p1, Point p2) {
        return DistanceMetrics.chebyshevDistance(p1.x, p1.y, p2.x, p2.y);
    }

    public int getDistance(Integer footmanId, Integer archerId){
        return !(archers.containsKey(archerId) && footmen.containsKey(footmanId)) ?
                0 : DistanceMetrics.chebyshevDistance(
                archers.get(archerId).x, archers.get(archerId).y,
                footmen.get(footmanId).x, footmen.get(footmanId).y
        );
    }

    private int getClosestArcher(int footmanId) {
        Point footmanLoc = footmen.get(footmanId);
        int closestArcher = -1;
        int minDistance = Integer.MAX_VALUE;
        for (int archerId : archers.keySet()) {
            int distance = getDistance(footmanLoc, archers.get(archerId));
            if (distance < minDistance) {
                closestArcher = archerId;
                minDistance = distance;
            }
        }
        return closestArcher;
    }

    /**
     * Gets a utility value based on the distance to the footmen's targets
     * @return
     */
    private double getUtilityFromTargetDistance() {
        double utility = 0;
        for (int footmanId : footmen.keySet()) {
            int distance = getDistance(
                    footmen.get(footmanId),
                    archers.get(getClosestArcher(footmanId))
            );
            utility += Math.pow(distance, -2);
        }
        return utility;
    }

    /**
     * Gets a utility value based on how close each archer is to a corner of the map
     * @return
     */
    private double getUtilityFromTargetCornering() {
        double utility = 0;
        for (Point archerLoc : archers.values()) {
            Point closestCorner = new Point(
                    (int)(Math.round(archerLoc.x / (xExtent - 1)) * (xExtent - 1)),
                    (int)(Math.round(archerLoc.y / (yExtent - 1)) * (yExtent - 1))
            );
            int distance = getDistance(archerLoc, closestCorner);
            utility += Math.pow(distance, -2);
        }
        return utility;
    }

    /**
     * Gets a utility value based on obstacles in the general path between each footman and its target
     * @return
     */
    private double getUtilityFromObstacles() {
        double utility = 0;
        for (int footmanId : footmen.keySet()) {
            Point footmanLoc = footmen.get(footmanId);
            Point archerLoc = archers.get(getClosestArcher(footmanId));
            int xMin = Math.min(footmanLoc.x, archerLoc.x);
            int xMax = Math.max(footmanLoc.x, archerLoc.x);
            int yMin = Math.min(footmanLoc.y, archerLoc.y);
            int yMax = Math.max(footmanLoc.y, archerLoc.y);
            int numObstacles = 0;
            for (Point resourceLoc : resources.values()) {
                if (resourceLoc.x >= xMin && resourceLoc.x <= xMax && resourceLoc.y >= yMin && resourceLoc.y <= yMax) {
                    numObstacles++;
                }
            }
            int areaChecked = (xMax - xMin + 1) * (yMax - yMin + 1);
            utility -= (double)numObstacles / areaChecked;
        }
        return utility;
    }

    public Map<Integer, Integer> getUnitHPs() {
        return unitHPs;
    }

    public Map<Integer, Integer> getUnitAttackRanges() {
        return unitAttackRanges;
    }
}
