package routing.main.command;

import routing.graph.Graph;
import routing.main.ArgParser;

/**
 * Created by pieter on 5/06/2016.
 */
public abstract class Command {
    public Command() {};
    public Command(ArgParser a) {
        initialize(a);
    }
    public boolean loadNodes() { return true; }
    protected abstract void initialize(ArgParser a);
    public abstract void execute (Graph g);
    public abstract String getName();
}
