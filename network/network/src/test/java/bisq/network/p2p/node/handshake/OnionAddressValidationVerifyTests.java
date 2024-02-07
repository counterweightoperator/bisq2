/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.network.p2p.node.handshake;

import bisq.network.common.Address;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.keys.TorKeyPair;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class OnionAddressValidationVerifyTests {
    private final TorKeyPair myTorKeyPair = TorKeyGeneration.generateKeyPair();
    private final TorKeyPair peerTorKeyPair = TorKeyGeneration.generateKeyPair();
    private final long signatureDate = System.currentTimeMillis();

    @Test
    void testVerifyNonOnionAddresses() {
        Address myAddress = Address.localHost(1234);
        Address peerAddress = Address.localHost(4321);
        boolean isValid = OnionAddressValidation.verify(myAddress, peerAddress, signatureDate, Optional.empty());
        assertThat(isValid).isTrue();
    }

    @Test
    void testVerifyMyNonOnionAddress() {
        Address myAddress = Address.localHost(1234);
        Address peerAddress = new Address(peerTorKeyPair.getOnionAddress());
        boolean isValid = OnionAddressValidation.verify(myAddress, peerAddress, signatureDate, Optional.empty());
        assertThat(isValid).isTrue();
    }

    @Test
    void testVerifyPeerNonOnionAddress() {
        Address myAddress = new Address(myTorKeyPair.getOnionAddress());
        Address peerAddress = Address.localHost(4321);
        boolean isValid = OnionAddressValidation.verify(myAddress, peerAddress, signatureDate, Optional.empty());
        assertThat(isValid).isTrue();
    }

    @Test
    void testVerifyWithoutPeerProof() {
        Address myAddress = new Address(myTorKeyPair.getOnionAddress());
        Address peerAddress = new Address(peerTorKeyPair.getOnionAddress());
        boolean isValid = OnionAddressValidation.verify(myAddress, peerAddress, signatureDate, Optional.empty());
        assertThat(isValid).isFalse();
    }

    @Test
    void testVerifyValidProof() {
        Address myAddress = new Address(myTorKeyPair.getOnionAddress());
        Address peerAddress = new Address(peerTorKeyPair.getOnionAddress());

        long signatureDate = System.currentTimeMillis();
        Optional<byte[]> signature = OnionAddressValidation.sign(myAddress, peerAddress, signatureDate, myTorKeyPair.getPrivateKey());

        boolean isValid = OnionAddressValidation.verify(myAddress, peerAddress, signatureDate, signature);
        assertThat(isValid).isFalse();
    }
}
