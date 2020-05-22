package cfg.ir;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import cfg.ir.nodes.CFGNode;
import cfg.ir.nodes.CFGStartNode;
import graph.Edge;
import graph.Graph;
import graph.GraphNode;
import graph.NonexistentEdgeException;
import graph.NonexistentNodeException;
import java_cup.runtime.ComplexSymbolFactory.Location;

/**
 * Graph structure for control-flow graphs. Edges optionally have a value for
 * if nodes to indicate which branch is the true branch and which is the false
 * branch.
 * @author ayang
 *
 */
public class CFGGraph implements Graph<CFGNode, Boolean> {

    private final Map<GraphNode<CFGNode>, LinkedList<Edge<CFGNode, Boolean>>> incomingEdges;
    private final Map<GraphNode<CFGNode>, LinkedList<Edge<CFGNode, Boolean>>> outgoingEdges;

    private final GraphNode<CFGNode> startNode;

    public CFGGraph(Location n) {
        this.incomingEdges = new HashMap<>();
        this.outgoingEdges = new HashMap<>();
        this.startNode = new GraphNode<>(new CFGStartNode(n));
        this.insert(startNode);
    }

    public GraphNode<CFGNode> startNode() {
        return startNode;
    }

    @Override
    public Set<GraphNode<CFGNode>> nodes() {
        return new HashSet<>(this.incomingEdges.keySet());
    }

    @Override
    public Set<Edge<CFGNode, Boolean>> edges() {
        Set<Edge<CFGNode, Boolean>> edges = new HashSet<>();
        this.incomingEdges.values().forEach(n -> edges.addAll(n));
        this.outgoingEdges.values().forEach(n -> edges.addAll(n));
        return edges;
    }

    /**
     * Removes all nodes from the graph that are unreachable from the start
     * node.
     */
    public void clean() {
        Set<GraphNode<CFGNode>> reachable = new HashSet<>();
        Queue<GraphNode<CFGNode>> worklist = new ArrayDeque<>();
        worklist.add(startNode);
        while (!worklist.isEmpty()) {
            final var node = worklist.remove();
            reachable.add(node);
            for (Edge<CFGNode, Boolean> e: this.outgoingEdges.get(node)) {
                final var out = e.end;
                if (!reachable.contains(out)) {
                    worklist.add(out);
                }
            }
        }
        final var unreachable = nodes();
        unreachable.removeAll(reachable);
        for (GraphNode<CFGNode> node: unreachable) {
            this.remove(node);
        }
    }

    @Override
    public boolean insert(GraphNode<CFGNode> node) {
        if (this.outgoingEdges.containsKey(node)) {
            return false;
        }
        this.incomingEdges.put(node, new LinkedList<>());
        this.outgoingEdges.put(node, new LinkedList<>());
        return true;
    }

    @Override
    public GraphNode<CFGNode> remove(GraphNode<CFGNode> node)
            throws NonexistentNodeException {
        if (node.equals(this.startNode)) {
            throw new UnsupportedOperationException("Attempted to remove start node.");
        }
        if (!this.outgoingEdges.containsKey(node)) {
            throw new NonexistentNodeException(node);
        }
        if (this.containsEdge(node, node)) {
            this.unlink(node, node);
        }
        final var incoming = Set.copyOf(this.incomingEdges.get(node));
        incoming.forEach(in -> this.unlink(in));

        final var outgoing = Set.copyOf(this.outgoingEdges.get(node));
        outgoing.forEach(out -> this.unlink(out));

        this.incomingEdges.remove(node);
        this.outgoingEdges.remove(node);

        return node;
    }

    /**
     * For If node, a list is returned are returned such that the true branch
     * edges prioritizes the false branch.
     * <p>
     * For example, suppose the if node has two out nodes, then the first
     * element of the list if the true branch, and second element of the list
     * is the false branch.
     */
    @Override
    public List<GraphNode<CFGNode>> outgoingNodes(GraphNode<CFGNode> node)
            throws NonexistentNodeException {
        if (!this.outgoingEdges.containsKey(node)) {
            throw new NonexistentNodeException(node);
        }
        return this.outgoingEdges.get(node).stream()
                                           .map(e -> e.end)
                                           .collect(Collectors.toList());
    }

    @Override
    public List<GraphNode<CFGNode>> incomingNodes(GraphNode<CFGNode> node)
            throws NonexistentNodeException {
        if (!this.incomingEdges.containsKey(node)) {
            throw new NonexistentNodeException(node);
        }
        return this.incomingEdges.get(node).stream().map(e -> e.start)
                .collect(Collectors.toList());
    }

    @Override
    public boolean join(GraphNode<CFGNode> start, GraphNode<CFGNode> end)
            throws NonexistentNodeException {
        if (!this.outgoingEdges.containsKey(start)) {
            throw new NonexistentNodeException(start);
        } else if (!this.incomingEdges.containsKey(end)) {
            throw new NonexistentNodeException(end);
        }
        final var edge = new Edge<CFGNode, Boolean>(start, end);
        this.outgoingEdges.get(start).add(edge);
        this.incomingEdges.get(end).add(edge);

        return true;
    }

    @Override
    public boolean join(Edge<CFGNode, Boolean> edge)
            throws NonexistentNodeException {
        final var start = edge.start;
        final var end = edge.end;
        if (!this.outgoingEdges.containsKey(start)) {
            throw new NonexistentNodeException(start);
        } else if (!this.incomingEdges.containsKey(end)) {
            throw new NonexistentNodeException(end);
        }

        if (edge.value.isPresent()) {
            if (edge.value.get()) {
                this.outgoingEdges.get(start).addFirst(edge);
            } else {
                this.outgoingEdges.get(start).addLast(edge);
            }
        } else {
            this.outgoingEdges.get(start).add(edge);
        }
        this.incomingEdges.get(end).add(edge);

        return true;
    }


    @Override
    public Edge<CFGNode, Boolean> unlink(GraphNode<CFGNode> start,
            GraphNode<CFGNode> end) throws NonexistentEdgeException {
        final var removedEdge = new Edge<CFGNode, Boolean>(start, end);
        final var removedEdgeTrue = new Edge<CFGNode, Boolean>(start, end, true);
        final var removedEdgeFalse = new Edge<CFGNode, Boolean>(start, end, false);
        if (!this.outgoingEdges.containsKey(start)) {
            throw new NonexistentEdgeException(removedEdge);
        } else if (!this.incomingEdges.containsKey(end)) {
            throw new NonexistentEdgeException(removedEdge);
        }

        final var removedEdgeSet = new HashSet<Edge<CFGNode, Boolean>>();
        removedEdgeSet.add(removedEdge);
        removedEdgeSet.add(removedEdgeTrue);
        removedEdgeSet.add(removedEdgeFalse);
        this.outgoingEdges.get(start).removeAll(removedEdgeSet);
        this.incomingEdges.get(end).removeAll(removedEdgeSet);

        return removedEdge;
    }

    @Override
    public Edge<CFGNode, Boolean> unlink(Edge<CFGNode, Boolean> edge)
            throws NonexistentEdgeException {
        if (!this.containsEdge(edge)) {
            throw new NonexistentEdgeException(edge);
        }
        final var start = edge.start;
        final var end = edge.end;
        if (!this.outgoingEdges.containsKey(start)) {
            throw new NonexistentEdgeException(edge);
        } else if (!this.incomingEdges.containsKey(end)) {
            throw new NonexistentEdgeException(edge);
        }
        this.outgoingEdges.get(start).remove(edge);
        this.incomingEdges.get(end).remove(edge);

        return edge;
    }

    @Override
    public boolean containsNode(GraphNode<CFGNode> node) {
        return this.outgoingEdges.containsKey(node);
    }

    @Override
    public boolean containsEdge(GraphNode<CFGNode> start,
            GraphNode<CFGNode> end) {
        if (this.outgoingEdges.containsKey(start)) {
            return this.outgoingEdges.get(start).contains(new Edge<>(start, end));
        }
        return false;
    }

    @Override
    public boolean containsEdge(Edge<CFGNode, Boolean> edge) {
        if (this.outgoingEdges.containsKey(edge.start)) {
            return this.outgoingEdges.get(edge.start).contains(edge);
        }
        return false;
    }


}