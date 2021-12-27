/*
 * This file is part of Misq.
 *
 * Misq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Misq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Misq. If not, see <http://www.gnu.org/licenses/>.
 */

package network.misq.network.p2p.node;


import network.misq.common.util.CompletableFutureUtils;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.peergroup.BanList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Maintains nodes per nodeId.
 * Provides delegate methods to node with given nodeId
 */
public class NodesById implements Node.Listener {
    private static final Logger log = LoggerFactory.getLogger(NodesById.class);

    private final Map<String, Node> map = new ConcurrentHashMap<>();
    private final BanList banList;
    private final Node.Config nodeConfig;
    private final Set<Node.Listener> listeners = new CopyOnWriteArraySet<>();

    public NodesById(BanList banList, Node.Config nodeConfig) {
        this.banList = banList;
        this.nodeConfig = nodeConfig;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Node getDefaultNode() {
        return getOrCreateNode(Node.DEFAULT_NODE_ID);
    }

    public CompletableFuture<Connection> send(String senderNodeId, Message message, Address address) {
        return getOrCreateNode(senderNodeId).send(message, address);
    }

    public CompletableFuture<Connection> send(String senderNodeId, Message message, Connection connection) {
        return getOrCreateNode(senderNodeId).send(message, connection);
    }

    public CompletableFuture<Connection> getConnection(String nodeId, Address address) {
        return getOrCreateNode(nodeId).getConnection(address);
    }

    public CompletableFuture<Transport.ServerSocketResult> initializeServer(String nodeId, int serverPort) {
        return getOrCreateNode(nodeId).initializeServer(serverPort);
    }

    public void addNodeListener(Node.Listener listener) {
        listeners.add(listener);
    }

    public void removeNodeListener(Node.Listener listener) {
        listeners.remove(listener);
    }

    public void addNodeListener(String nodeId, Node.Listener listener) {
        findNode(nodeId).ifPresent(node -> node.addListener(listener));
    }

    public void removeNodeListener(String nodeId, Node.Listener listener) {
        findNode(nodeId).ifPresent(node -> node.removeListener(listener));
    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFutureUtils.allOf(map.values().stream().map(Node::shutdown))
                .orTimeout(1, TimeUnit.SECONDS)
                .thenApply(list -> {
                    map.clear();
                    return null;
                });
    }

    public Optional<Address> findMyAddress(String nodeId) {
        return findNode(nodeId).flatMap(Node::findMyAddress);
    }

    public Optional<Node> findNode(String nodeId) {
        return Optional.ofNullable(map.get(nodeId));
    }

    public Map<String, Address> getAddressesByNodeId() {
        //noinspection OptionalGetWithoutIsPresent
        return map.entrySet().stream()
                .filter(e -> e.getValue().findMyAddress().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().findMyAddress().get()));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        listeners.forEach(messageListener -> messageListener.onMessage(message, connection, nodeId));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private Node getOrCreateNode(String nodeId) {
        return findNode(nodeId)
                .orElseGet(() -> {
                    Node node = new Node(banList, nodeConfig, nodeId);
                    map.put(nodeId, node);
                    node.addListener(this);
                    return node;
                });
    }
}
