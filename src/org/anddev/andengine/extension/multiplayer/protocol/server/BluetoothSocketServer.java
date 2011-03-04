package org.anddev.andengine.extension.multiplayer.protocol.server;

import java.io.IOException;
import java.util.UUID;

import org.anddev.andengine.extension.multiplayer.protocol.server.ClientConnector.IClientConnectorListener;
import org.anddev.andengine.extension.multiplayer.protocol.server.ClientConnector.IClientConnectorListener.DefaultClientConnectorListener;
import org.anddev.andengine.extension.multiplayer.protocol.shared.BluetoothSocketConnection;
import org.anddev.andengine.util.Debug;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

/**
 * @author Nicolas Gramlich
 * @since 15:41:31 - 03.03.2011
 */
public abstract class BluetoothSocketServer extends Server<BluetoothSocketConnection, ClientConnector<BluetoothSocketConnection>> {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private final String mUUID;
	private BluetoothServerSocket mBluetoothServerSocket;

	// ===========================================================
	// Constructors
	// ===========================================================

	public BluetoothSocketServer(final String pUUID) {
		this(pUUID, new DefaultClientConnectorListener<BluetoothSocketConnection>());
	}

	public BluetoothSocketServer(final String pUUID, final IClientConnectorListener<BluetoothSocketConnection> pClientConnectorListener) {
		this(pUUID, pClientConnectorListener, new IServerStateListener.DefaultServerStateListener());
	}

	public BluetoothSocketServer(final String pUUID, final IServerStateListener pServerStateListener) {
		this(pUUID, new DefaultClientConnectorListener<BluetoothSocketConnection>(), pServerStateListener);
	}

	public BluetoothSocketServer(final String pUUID, final IClientConnectorListener<BluetoothSocketConnection> pClientConnectorListener, final IServerStateListener pServerStateListener) {
		super(pClientConnectorListener, pServerStateListener);

		this.mUUID = pUUID;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	protected abstract ClientConnector<BluetoothSocketConnection> newClientConnector(final BluetoothSocketConnection pBluetoothSocketConnection) throws IOException;

	@Override
	protected void onInit() throws IOException {
		this.mBluetoothServerSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord(this.getClass().getName(), UUID.fromString(this.mUUID));
	}

	@Override
	protected ClientConnector<BluetoothSocketConnection> acceptClientConnector() throws IOException {
		/* Wait for an incoming connection. */
		final BluetoothSocket clientBluetoothSocket = this.mBluetoothServerSocket.accept();

		/* Spawn a new ClientConnector, which send and receive data to and from the client. */
		return this.newClientConnector(new BluetoothSocketConnection(clientBluetoothSocket));
	}

	@Override
	public void onClosed() {
		try {
			this.mBluetoothServerSocket.close(); // TODO Put to SocketUtils
		} catch (final IOException e) {
			Debug.e(e);
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
