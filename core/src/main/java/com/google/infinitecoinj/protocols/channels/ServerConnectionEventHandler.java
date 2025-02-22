/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.infinitecoinj.protocols.channels;

import java.math.BigInteger;

import com.google.infinitecoinj.core.Sha256Hash;
import com.google.infinitecoinj.net.ProtobufParser;
import org.infinitecoin.paymentchannel.Protos;

import javax.annotation.Nullable;

/**
* A connection-specific event handler that handles events generated by client connections on a
 * {@link PaymentChannelServerListener}
*/
public abstract class ServerConnectionEventHandler {
    private ProtobufParser connectionChannel;
    // Called by ServerListener before channelOpen to set connectionChannel when it is ready to received application messages
    // Also called with null to clear connectionChannel after channelClosed()
    synchronized void setConnectionChannel(@Nullable ProtobufParser connectionChannel) { this.connectionChannel = connectionChannel; }

    /**
     * <p>Closes the channel with the client (will generate a
     * {@link ServerConnectionEventHandler#channelClosed(PaymentChannelCloseException.CloseReason)} event)</p>
     *
     * <p>Note that this does <i>NOT</i> actually broadcast the most recent payment transaction, which will be triggered
     * automatically when the channel times out by the {@link StoredPaymentChannelServerStates}, or manually by calling
     * {@link StoredPaymentChannelServerStates#closeChannel(StoredServerChannel)} with the channel returned by
     * {@link StoredPaymentChannelServerStates#getChannel(com.google.infinitecoinj.core.Sha256Hash)} with the id provided in
     * {@link ServerConnectionEventHandler#channelOpen(com.google.infinitecoinj.core.Sha256Hash)}</p>
     */
    protected final synchronized void closeChannel() {
        if (connectionChannel == null)
            throw new IllegalStateException("Channel is not fully initialized/has already been closed");

        connectionChannel.write(Protos.TwoWayChannelMessage.newBuilder()
                .setType(Protos.TwoWayChannelMessage.MessageType.CLOSE)
                .build());
        connectionChannel.closeConnection();
    }

    /**
     * Triggered when the channel is opened and application messages/payments can begin
     *
     * @param channelId A unique identifier which represents this channel (actually the hash of the multisig contract)
     */
    public abstract void channelOpen(Sha256Hash channelId);

    /**
     * Called when the payment in this channel was successfully incremented by the client
     *
     * @param by The increase in total payment
     * @param to The new total payment to us (not including fees which may be required to claim the payment)
     */
    public abstract void paymentIncrease(BigInteger by, BigInteger to);

    /**
     * <p>Called when the channel was closed for some reason. May be called without a call to
     * {@link ServerConnectionEventHandler#channelOpen(Sha256Hash)}.</p>
     *
     * <p>Note that the same channel can be reopened at any point before it expires if the client reconnects and
     * requests it.</p>
     */
    public abstract void channelClosed(PaymentChannelCloseException.CloseReason reason);
}
