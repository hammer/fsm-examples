package com.jeffhammerbacher.fsm_examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import com.continuent.tungsten.commons.patterns.fsm.Action;
import com.continuent.tungsten.commons.patterns.fsm.Event;
import com.continuent.tungsten.commons.patterns.fsm.Entity;
import com.continuent.tungsten.commons.patterns.fsm.Guard;
import com.continuent.tungsten.commons.patterns.fsm.State;
import com.continuent.tungsten.commons.patterns.fsm.StateMachine;
import com.continuent.tungsten.commons.patterns.fsm.StateTransitionMap;
import com.continuent.tungsten.commons.patterns.fsm.StateType;
import com.continuent.tungsten.commons.patterns.fsm.Transition;

import com.continuent.tungsten.commons.patterns.fsm.EntityAdapter;
import com.continuent.tungsten.commons.patterns.fsm.EventTypeGuard;
import com.continuent.tungsten.commons.patterns.fsm.StateChangeListener;
import com.continuent.tungsten.commons.patterns.fsm.StringEvent;

import com.continuent.tungsten.commons.patterns.fsm.FiniteStateException;
import com.continuent.tungsten.commons.patterns.fsm.TransitionRollbackException;

public class FileReaderFSM implements StateChangeListener {
  // State machine
  private StateTransitionMap stmap = null;
  private StateMachine sm = null;

  // Monitoring and management
  private static Logger logger = LoggerFactory.getLogger(FileReaderFSM.class);

  // Ctor
  public FileReaderFSM() throws Exception {
    // Define actions
    Action logAction = new LogAction();
    Action nullAction = new NullAction();

    // Define states
    stmap = new StateTransitionMap();
    State start = new State("START", StateType.START);
    State reading = new State("READING", StateType.ACTIVE);
    State end = new State("END", StateType.END);

    stmap.addState(start);
    stmap.addState(reading);
    stmap.addState(end);

    // Define guards
    Guard stopGuard = new EventTypeGuard(StopEvent.class);
    Guard stringGuard = new EventTypeGuard(StringEvent.class);

    // Define transitions
    stmap.addTransition(new Transition("START-TO-READING", stringGuard, start, logAction, reading));
    stmap.addTransition(new Transition("START-TO-END", stopGuard, start, nullAction, end));
    stmap.addTransition(new Transition("READING-TO-READING", stringGuard, reading, logAction, reading));
    stmap.addTransition(new Transition("READING-TO-END", stopGuard, reading, nullAction, end));

    // Create the state machine
    stmap.build();
    sm = new StateMachine(stmap, new EntityAdapter(this));
    sm.addListener(this);
  }

  public void readChar(char c) throws Exception {
    sm.applyEvent(new StringEvent(Character.toString(c)));
  }

  public void stop() throws Exception {
    try {
      sm.applyEvent(new StopEvent());
    } catch (Exception e) {
      logger.error("Stop operation failed", e);
      throw new Exception(e.toString());
    }
  }

  // Log state changes
  public void stateChanged(Entity entity, State oldState, State newState) {
    logger.info("State changed: " + oldState.getName() + " -> " + newState.getName());
  }

  class StopEvent extends Event
  {
    public StopEvent()
    {
      super(null);
    }
  }

  // Do nothing
  class NullAction implements Action {
    public void doAction(Event event, Entity entity, Transition transition,
                         int actionType) throws TransitionRollbackException {
    }
  }

  // Log event data
  class LogAction implements Action {
    public void doAction(Event event, Entity entity, Transition transition,
                         int actionType) throws TransitionRollbackException {
      logger.info("Event: " + event.getData());
    }
  }

  public static void main(String[] args) {
    try {
      // Build FSM
      FileReaderFSM fileReaderFSM = new FileReaderFSM();

      // Read file
      Charset encoding = Charset.defaultCharset();
      File file = new File(args[0]);
      InputStream in = new FileInputStream(file);
      Reader reader = new InputStreamReader(in, encoding);
      Reader buffer = new BufferedReader(reader);
      int r;
      while ((r = reader.read()) != -1) {
        char ch = (char) r;
        // Apply an event for each character read
        fileReaderFSM.readChar(ch);
      }

      // Terminate FSM
      fileReaderFSM.stop();
    } catch (FiniteStateException e) {
      logger.error("Unexpected state transition processing error", e);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
  }
}
