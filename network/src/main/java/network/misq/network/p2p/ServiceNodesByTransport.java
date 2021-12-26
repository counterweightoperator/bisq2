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

package network.misq.network.p2p;


import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import network.misq.common.util.CompletableFutureUtils;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.node.authorization.UnrestrictedAuthorizationService;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.broadcast.BroadcastResult;
import network.misq.network.p2p.services.confidential.ConfidentialService;
import network.misq.network.p2p.services.data.DataService;
import network.misq.network.p2p.services.data.NetworkPayload;
import network.misq.network.p2p.services.data.filter.DataFilter;
import network.misq.network.p2p.services.data.inventory.RequestInventoryResult;
import network.misq.network.p2p.services.peergroup.PeerGroupService;
import network.misq.security.KeyPairRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServiceNodesByTransport {
    private static final Logger log = LoggerFactory.getLogger(ServiceNodesByTransport.class);

    private final Map<Transport.Type, ServiceNode> map = new ConcurrentHashMap<>();

    public ServiceNodesByTransport(Transport.Config transportConfig,
                                   Set<Transport.Type> supportedTransportTypes,
                                   ServiceNode.Config serviceNodeConfig,
                                   Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport,
                                   Map<Transport.Type, List<Address>> seedAddressesByTransport,
                                   DataService.Config dataServiceConfig,
                                   KeyPairRepository keyPairRepository) {

        long socketTimeout = TimeUnit.MINUTES.toMillis(5);
        supportedTransportTypes.forEach(transportType -> {
            Node.Config nodeConfig = new Node.Config(transportType,
                    supportedTransportTypes,
                    new UnrestrictedAuthorizationService(),
                    transportConfig,
                    (int) socketTimeout);

            List<Address> seedAddresses = seedAddressesByTransport.get(transportType);
            checkNotNull(seedAddresses, "Seed nodes must be setup for %s", transportType);
            PeerGroupService.Config peerGroupServiceConfig = peerGroupServiceConfigByTransport.get(transportType);
            ServiceNode serviceNode = new ServiceNode(serviceNodeConfig,
                    nodeConfig,
                    peerGroupServiceConfig,
                    dataServiceConfig,
                    keyPairRepository,
                    seedAddresses);
            map.put(transportType, serviceNode);
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> bootstrap(int port, @Nullable BiConsumer<Boolean, Throwable> resultHandler) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        int numNodes = map.size();
        map.values().forEach(networkNode -> {
            networkNode.bootstrap(Node.DEFAULT_NODE_ID, port)
                    .whenComplete((result, throwable) -> {
                        if (result != null) {
                            int compl = completed.incrementAndGet();
                            if (compl + failed.get() == numNodes) {
                                future.complete(compl == numNodes);
                            }
                        } else {
                            log.error(throwable.toString(), throwable);
                            if (failed.incrementAndGet() + completed.get() == numNodes) {
                                future.complete(false);
                            }
                        }
                        if (resultHandler != null) {
                            resultHandler.accept(result, throwable);
                        }
                    });
        });
        return future;
    }

    // TODO we return first successful connection in case we have multiple transportTypes. Not sure if that is ok.
    public CompletableFuture<ConfidentialService.Result> confidentialSend(Message message, NetworkId networkId, KeyPair myKeyPair, String connectionId) {
        CompletableFuture<ConfidentialService.Result> future = new CompletableFuture<>();
        networkId.addressByNetworkType().forEach((transportType, address) -> {
            if (map.containsKey(transportType)) {
                map.get(transportType)
                        .confidentialSend(message, address, networkId.pubKey(), myKeyPair, connectionId)
                        .whenComplete((result, throwable) -> {
                            if (result != null) {
                                future.complete(result);
                            } else {
                                log.error(throwable.toString(), throwable);
                                future.completeExceptionally(throwable);
                            }
                        });
            } else {
                //todo
             /*   map.values().forEach(networkNode -> {
                    networkNode.relay(message, networkId, myKeyPair)
                            .whenComplete((connection, throwable) -> {
                                if (connection != null) {
                                    future.complete(connection);
                                } else {
                                    log.error(throwable.toString(), throwable);
                                    future.completeExceptionally(throwable);
                                }
                            });
                });*/
            }
        });
        return future;
    }

    public CompletableFuture<List<BroadcastResult>> addNetworkPayload(NetworkPayload networkPayload, KeyPair keyPair) {
        return CompletableFutureUtils.allOf(
                map.values().stream()
                        .map(serviceNode -> serviceNode.addNetworkPayload(networkPayload, keyPair))
        );
    }

    public void requestRemoveData(Message message, Consumer<BroadcastResult> resultHandler) {
        map.values().forEach(dataService -> {
            dataService.requestRemoveData(message)
                    .whenComplete((gossipResult, throwable) -> {
                        if (gossipResult != null) {
                            resultHandler.accept(gossipResult);
                        } else {
                            log.error(throwable.toString());
                        }
                    });
        });
    }

    public void requestInventory(DataFilter dataFilter, Consumer<RequestInventoryResult> resultHandler) {
        map.values().forEach(serviceNode -> {
            serviceNode.requestInventory(dataFilter)
                    .whenComplete((requestInventoryResult, throwable) -> {
                        if (requestInventoryResult != null) {
                            resultHandler.accept(requestInventoryResult);
                        } else {
                            log.error(throwable.toString());
                        }
                    });
        });
    }

    public Optional<Socks5Proxy> getSocksProxy() {
        return findServiceNode(Transport.Type.TOR)
                .flatMap(serviceNode -> {
                    try {
                        return serviceNode.getSocksProxy();
                    } catch (IOException e) {
                        log.warn("Could not get socks proxy", e);
                        return Optional.empty();
                    }
                });
    }

    public void addMessageListener(Node.Listener listener) {
        map.values().forEach(serviceNode -> serviceNode.addMessageListener(listener));
    }

    public void removeMessageListener(Node.Listener listener) {
        map.values().forEach(serviceNode -> serviceNode.removeMessageListener(listener));
    }

    public CompletableFuture<Void> shutdown() {
        return CompletableFutureUtils.allOf(map.values().stream().map(ServiceNode::shutdown))
                .orTimeout(1, TimeUnit.SECONDS)
                .thenApply(list -> {
                    map.clear();
                    return null;
                });
    }

    public Map<Transport.Type, Map<String, Address>> findMyAddresses() {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getAddressesByNodeId()));
    }

    public Optional<Map<String, Address>> findMyAddresses(Transport.Type transport) {
        return Optional.ofNullable(findMyAddresses().get(transport));
    }

    public Optional<Address> findMyAddresses(Transport.Type transport, String nodeId) {
        return findMyAddresses(transport).flatMap(map -> Optional.ofNullable(map.get(nodeId)));
    }

    public Optional<ServiceNode> findServiceNode(Transport.Type transport) {
        return Optional.ofNullable(map.get(transport));
    }

    public Optional<Node> findNode(Transport.Type transport, String nodeId) {
        return findServiceNode(transport)
                .flatMap(serviceNode -> serviceNode.findNode(nodeId));
    }
}
