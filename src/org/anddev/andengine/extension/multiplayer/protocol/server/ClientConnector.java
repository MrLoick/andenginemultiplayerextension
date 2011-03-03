package org.anddev.andengine.extension.multiplayer.protocol.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.anddev.andengine.extension.multiplayer.protocol.adt.message.client.IClientMessage;
import org.anddev.andengine.extension.multiplayer.protocol.adt.message.server.IServerMessage;
import org.anddev.andengine.extension.multiplayer.protocol.adt.message.server.connection.ConnectionCloseServerMessage;
import org.anddev.andengine.extension.multiplayer.protocol.server.ClientMessageReader.DefaultClientMessageReader;
import org.anddev.andengine.extension.multiplayer.protocol.server.IClientMessageHandler.DefaultClientMessageHandler;
import org.anddev.andengine.extension.multiplayer.protocol.shared.Connection;
import org.anddev.andengine.extension.multiplayer.protocol.shared.Connector;
import org.anddev.andengine.util.Debug;

/**
 * @author Nicolas Gramlich
 * @since 21:40:51 - 18.09.2009
 */
public class ClientConnector<T extends Connection> extends Connector<T> {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private final IClientMessageReader mClientMessageReader;
	private final IClientMessageHandler<T> mClientMessageHandler;

	// ===========================================================
	// Constructors
	// ===========================================================

	public ClientConnector(final T pConnection, final DefaultClientMessageHandler<T> pClientMessageHandler) throws IOException {
		this(pConnection, new DefaultClientMessageReader(), pClientMessageHandler);
	}

	public ClientConnector(final T pConnection, final IClientMessageReader pClientMessageReader, final IClientMessageHandler<T> pClientMessageHandler) throws IOException {
		super(pConnection);

		this.mClientMessageReader = pClientMessageReader;
		this.mClientMessageHandler = pClientMessageHandler;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public IClientMessageHandler<T> getClientMessageHandler() {
		return this.mClientMessageHandler;
	}

	public IClientMessageReader getClientMessageReader() {
		return this.mClientMessageReader;
	}

	public void setClientConnectorListener(final IClientConnectorListener<T> pClientConnectorListener) {
		super.setConnectorListener(pClientConnectorListener);
	}

	@SuppressWarnings("unchecked")
	@Override
	public IClientConnectorListener<T> getConnectorListener() {
		return (IClientConnectorListener<T>) super.getConnectorListener();
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void onConnected(final Connection pConnection) {

	}

	@Override
	public void onDisconnected(final Connection pConnection) {
		try {
			this.sendServerMessage(new ConnectionCloseServerMessage());
		} catch (final Throwable pThrowable) {
			Debug.e(pThrowable);
		}
	}

	@Override
	public void read(final DataInputStream pDataInputStream) throws IOException {
		final IClientMessage clientMessage = this.mClientMessageReader.readMessage(pDataInputStream);
		this.mClientMessageHandler.onHandleMessage(this, clientMessage);
		this.mClientMessageReader.recycleMessage(clientMessage);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public void registerClientMessage(final short pFlag, final Class<? extends IClientMessage> pClientMessageClass) {
		this.mClientMessageReader.registerMessage(pFlag, pClientMessageClass);
	}

	public void sendServerMessage(final IServerMessage pServerMessage) throws IOException {
		final DataOutputStream dataOutputStream = this.mConnection.getDataOutputStream();
		pServerMessage.transmit(dataOutputStream);
		dataOutputStream.flush();
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	public static interface IClientConnectorListener<T extends Connection> extends IConnectorListener<ClientConnector<T>> {
		// ===========================================================
		// Final Fields
		// ===========================================================

		// ===========================================================
		// Methods
		// ===========================================================

		// ===========================================================
		// Inner and Anonymous Classes
		// ===========================================================

		public static class DefaultClientConnectorListener<T extends Connection> implements IClientConnectorListener<T> {
			@Override
			public void onConnected(final ClientConnector<T> pConnector) {
				Debug.d("Accepted Client-Connection from: '" + pConnector.toString() + "'");
			}

			@Override
			public void onDisconnected(final ClientConnector<T> pConnector) {
				Debug.d("Closed Client-Connection from: '" + pConnector.toString() + "'");
			}
		}
	}
}
