package com.jeffhammerbacher.fsm_examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.continuent.tungsten.commons.patterns.fsm.Action;
import com.continuent.tungsten.commons.patterns.fsm.Event;
import com.continuent.tungsten.commons.patterns.fsm.Entity;
import com.continuent.tungsten.commons.patterns.fsm.State;
import com.continuent.tungsten.commons.patterns.fsm.StateMachine;
import com.continuent.tungsten.commons.patterns.fsm.StateTransitionMap;
import com.continuent.tungsten.commons.patterns.fsm.StateType;
import com.continuent.tungsten.commons.patterns.fsm.Transition;

import com.continuent.tungsten.commons.patterns.fsm.EntityAdapter;
import com.continuent.tungsten.commons.patterns.fsm.PositiveGuard;
import com.continuent.tungsten.commons.patterns.fsm.StateChangeListener;

import com.continuent.tungsten.commons.patterns.fsm.FiniteStateException;
import com.continuent.tungsten.commons.patterns.fsm.TransitionRollbackException;

public class SimpleFSM implements StateChangeListener {
  // State machine
  private StateTransitionMap stmap = null;
  private StateMachine sm = null;

  // Monitoring and management
  private static Logger logger = LoggerFactory.getLogger(SimpleFSM.class);

  // Ctor
  public SimpleFSM() throws Exception {
    // Define actions
    Action nullAction = new NullAction();

    // Define states
    stmap = new StateTransitionMap();
    State start = new State("START", StateType.START);
    State end = new State("END", StateType.END);

    stmap.addState(start);
    stmap.addState(end);

    // Define transitions
    stmap.addTransition(new Transition("START-TO-END", new PositiveGuard(),
                                       start, nullAction, end));

    // Create the state machine
    stmap.build();
    sm = new StateMachine(stmap, new EntityAdapter(this));
    sm.addListener(this);
  }

  public StateMachine getStateMachine() {
    return sm;
  }

  // Log state changes
  public void stateChanged(Entity entity, State oldState, State newState) {
    logger.info("State changed: " + oldState.getName() + " -> " + newState.getName());
  }


  // Do nothing 
  class NullAction implements Action {
    public void doAction(Event event, Entity entity, Transition transition,
                         int actionType) throws TransitionRollbackException {
    }
  }

  public static void main(String[] args) {
    try {
      SimpleFSM simpleFSM = new SimpleFSM();
      StateMachine sm = simpleFSM.getStateMachine();
      sm.applyEvent(new Event("end"));
    } catch (FiniteStateException e) {
      logger.error("Unexpected state transition processing error", e);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
  }
}
