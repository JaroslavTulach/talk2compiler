package org.apidesign.demo.talk2compiler.bn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Boolean network (BN) represents a basic model of N interacting entities, each state of each represented by a boolean
 * value. Each entity can asynchronously update its value (create a successor) based on the values of other entities.
 *
 * Every BN will have a 2^N associated {@link State} objects connected depending on the behaviour of update functions
 * for individual variables. Once the whole state space is populated, we should be able to dump the heap
 * and get a well defined
 * <a href="https://github.com/daemontus/heap-language">graph that we can explore using OQL</a>.
 *
 * The network is intentionally implemented very poorly in terms of memory consumption, because we want to use
 * it for testing relationships between objects in heap dumps, not to create performant code.
 *
 * @author Samuel Pasta
 */
public abstract class BooleanNetwork {
    public static BooleanNetwork generate() {
        return new TumorCellPathway();
    }

    private final int numVars;
    private final int numStates;
    private final State[] states;

    protected BooleanNetwork(int numVars) {
        if (numVars > 30) throw new IllegalArgumentException("Cannot create Boolean network with 2^"+numVars+" states.");
        this.numVars = numVars;
        this.numStates = 1 << numVars;
        this.states = initStateSpace(new HashMap<>());
    }

    public int getVariablesCount() {
        return this.numVars;
    }

    public State getState(int variable) {
        return this.states[variable];
    }

    abstract boolean getNewValueForVariable(State state, int variable);

    private State[] initStateSpace(Map<State, State> stateSpace) {
        State[] arr = new State[this.numStates];
        for (int stateLiteral = 0; stateLiteral < this.numStates; stateLiteral++) {
            State state = literalToState(stateLiteral, stateSpace);
            arr[stateLiteral] = state;
            for (int var = 0; var < numVars; var++) {
                boolean newValue = getNewValueForVariable(state, var);
                if (newValue != state.getValue(var)) {  // if the value is different, we have a new state
                    State successor = state.flipValue(var);
                    stateSpace.putIfAbsent(successor, successor);
                    successor = stateSpace.get(successor);
                    state.addSuccessor(successor);
                }
            }
        }
        return arr;
    }

    private State literalToState(int literal, Map<State, State> stateSpace) {
        boolean[] values = new boolean[numVars];
        for (int bit = 0; bit < numVars; bit++) {
            values[bit] = ((literal >> bit) & 1) == 1;
        }
        State created = new State(values);
        stateSpace.putIfAbsent(created, created);
        return stateSpace.get(created);
    }


    /**
     * A state of a boolean network holds an array of boolean values representing the values of individual
     * variables.
     *
     * It also has a list of successor states that can be updated when needed.
     *
     * Two states are equal if they share the same variable valuation.
     */
    public static final class State {
        private static final State[] EMPTY = new State[0];

        private final boolean[] values;
        private State[] successors = EMPTY;

        Object key;
        Object value;

        private State(boolean[] values) {
            this.values = values;
        }

        public boolean getValue(int variable) {
            return this.values[variable];
        }

        private State flipValue(int variable) {
            boolean[] newValues = this.values.clone();
            newValues[variable] = !newValues[variable];
            return new State(newValues);
        }

        private void addSuccessor(State state) {
            for (State s : successors) {
                if (s.equals(state)) return;
            }
            int size = this.successors.length;
            this.successors = Arrays.copyOf(this.successors, size + 1);
            this.successors[size] = state;
        }

        public State[] getSuccessors() {
            return this.successors;
        }

        @Override
        public String toString() {
            return Arrays.toString(this.values);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            State state = (State) object;
            return Arrays.equals(values, state.values);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(values);
        }

    }

    /** Type-safe accessor to store mutable information in {@link State}
     * while traversing them.
     *
     * @param <V> type of values to store and obtain from the state
     */
    public static final class StateInfoAccess<V> {
        private final Class<V> type;

        private StateInfoAccess(Class<V> type) {
            this.type = type;
        }

        public static <T> StateInfoAccess<T> create(Class<T> type) {
            return new StateInfoAccess<>(type);
        }

        public void store(State state, V value) {
            state.key = this;
            state.value = value;
        }

        public V get(State state) {
            Object key = state.key;
            Object value = state.value;

            if (key == this) {
                return this.type.cast(value);
            } else {
                return null;
            }
        }
    }
}
