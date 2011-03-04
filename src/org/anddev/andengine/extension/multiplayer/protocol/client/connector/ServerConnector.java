package org.anddev.andengine.extension.multiplayer.protocol.client.connector;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.anddev.andengine.extension.multiplayer.protocol.adt.message.client.IClientMessage;
import org.anddev.andengine.extension.multiplayer.protocol.adt.message.client.connection.ConnectionCloseClientMessage;
import org.anddev.andengine.extension.multiplayer.protocol.adt.message.client.connection.ConnectionEstablishClientMessage;
import org.anddev.andengine.extension.multiplayer.protocol.adt.message.server.IServerMessage;
import org.anddev.andengine.extension.multiplayer.protocol.client.IServerMessageHandler;
import org.anddev.andengine.extension.multiplayer.protocol.client.IServerMessageReader;
import org.anddev.andengine.extension.multiplayer.protocol.client.IServerMessageReader.ServerMessageReader.DefaultServerMessageReader;
import org.anddev.andengine.extension.multiplayer.protocol.shared.Connection;
import org.anddev.andengine.extension.multiplayer.protocol.shared.Connector;
import org.anddev.andengine.extension.multiplayer.protocol.util.constants.ProtocolConstants;
import org.anddev.andengine.util.Debug;

/**
 * @author Nicolas Gramlich
 * @since 21:40:51 - 18.09.2009
 */
public class ServerConnector<C extends Connection> extends Connector<C> {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private final IServerMessageReader<C> mServerMessageReader;

	// ===========================================================
	// Constructors
	// ===========================================================

	public ServerConnector(final C pConnection, final IServerConnectorListener<C> pServerConnectorListener) throws IOException {
		this(pConnection, new DefaultServerMessageReader<C>(), pServerConnectorListener);
	}

	public ServerConnector(final C pConnection, final IServerMessageReader<C> pServerMessageReader, final IServerConnectorListener<C> pServerConnectorListener) throws IOException {
		super(pConnection);
		this.mServerMessageReader = pServerMessageReader;
		this.setServerConnectorListener(pServerConnectorListener);

		/* Initiate communication with the server,
		 * by sending a ConnectionEstablishClientMessage
		 * which contains the Protocol version. */
		this.sendClientMessage(new ConnectionEstablishClientMessage(ProtocolConstants.PROTOCOL_VERSION));
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public IServerMessageReader<C> getServerMessageReader() {
		return this.mServerMessageReader;
	}

	@SuppressWarnings("unchecked")
	@Override
	public IServerConnectorListener<C> getConnectorListener() {
		return (IServerConnectorListener<C>) super.getConnectorListener();
	}

	public void setServerConnectorListener(final IServerConnectorListener<C> pServerConnectorListener) {
		super.setConnectorListener(pServerConnectorListener);
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void onConnected(final Connection pConnection) {
		if(this.hasConnectorListener()) {
			this.getConnectorListener().onConnected(this);
		}
	}

	@Override
	public void onDisconnected(final Connection pConnection) {
		if(this.hasConnectorListener()) {
			this.getConnectorListener().onDisconnected(this);
		}
		try {
			this.sendClientMessage(new ConnectionCloseClientMessage());
		} catch (final Throwable pThrowable) {
			Debug.e(pThrowable);
		}
	}

	@Override
	public void read(final DataInputStream pDataInputStream) throws IOException {
		final IServerMessage serverMessage = this.mServerMessageReader.readMessage(pDataInputStream);
		this.mServerMessageReader.handleMessage(this, serverMessage);
		this.mServerMessageReader.recycleMessage(serverMessage);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public void registerServerMessage(final short pFlag, final Class<? extends IServerMessage> pServerMessageClass) {
		this.mServerMessageReader.registerMessage(pFlag, pServerMessageClass);
	}

	public void registerServerMessage(final short pFlag, final Class<? extends IServerMessage> pServerMessageClass, final IServerMessageHandler<C> pServerMessageHandler) {
		this.mServerMessageReader.registerMessage(pFlag, pServerMessageClass, pServerMessageHandler);
	}

	public void registerServerMessageHandler(final short pFlag, final IServerMessageHandler<C> pServerMessageHandler) {
		this.mServerMessageReader.registerMessageHandler(pFlag, pServerMessageHandler);
	}

	public void sendClientMessage(final IClientMessage pClientMessage) throws IOException {
		final DataOutputStream dataOutputStream = this.mConnection.getDataOutputStream();
		pClientMessage.transmit(dataOutputStream);
		dataOutputStream.flush();
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	public static interface IServerConnectorListener<T extends Connection> extends IConnectorListener<ServerConnector<T>> {
		// ===========================================================
		// Final Fields
		// ===========================================================

		// ===========================================================
		// Methods
		// ===========================================================
		
		@Override
		public void onConnected(final ServerConnector<T> pServerConnector);
		
		@Override
		public void onDisconnected(final ServerConnector<T> pServerConnector);
	}
}
