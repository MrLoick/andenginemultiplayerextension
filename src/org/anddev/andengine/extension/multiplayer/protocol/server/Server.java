package org.anddev.andengine.extension.multiplayer.protocol.server;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

import org.anddev.andengine.extension.multiplayer.protocol.adt.message.server.IServerMessage;
import org.anddev.andengine.extension.multiplayer.protocol.server.ClientConnector.IClientConnectorListener;
import org.anddev.andengine.extension.multiplayer.protocol.shared.Connection;
import org.anddev.andengine.extension.multiplayer.protocol.util.constants.ProtocolConstants;
import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.SocketUtils;

/**
 * @author Nicolas Gramlich
 * @since 14:36:54 - 18.09.2009
 */
public abstract class Server<T extends ClientConnector<? extends Connection>> extends Thread implements ProtocolConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected final IServerStateListener mServerStateListener;

	private boolean mRunning = false;
	private boolean mTerminated = false;

	protected final ArrayList<T> mClientConnectors = new ArrayList<T>();
	protected final IClientConnectorListener<T> mClientConnectorListener;

	// ===========================================================
	// Constructors
	// ===========================================================

	public Server(final IClientConnectorListener<T> pClientConnectorListener, final IServerStateListener pServerStateListener) {
		this.mServerStateListener = pServerStateListener;
		this.mClientConnectorListener = pClientConnectorListener;

		this.initName();
	}

	private void initName() {
		this.setName(this.getClass().getName());
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public boolean isRunning() {
		return this.mRunning;
	}

	public boolean isTerminated() {
		return this.mTerminated ;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	protected abstract void prepare() throws IOException;
	protected abstract T acceptClientConnector() throws IOException;

	@Override
	public void run() {
		this.mRunning = true;
		this.mTerminated = false;
		this.mServerStateListener.onStarted();
		try {
//			Thread.currentThread().setPriority(Thread.MIN_PRIORITY); // TODO What ThreadPriority makes sense here?
			this.prepare();

			/* Endless waiting for incoming clients. */
			while (!Thread.interrupted()) {
				try {
					final T clientConnector = this.acceptClientConnector();
					this.mClientConnectors.add(clientConnector);

					/* Start the ClientConnector(-Thread) so it starts receiving commands. */
					clientConnector.start();
				} catch (final SocketException se) {
					if(!se.getMessage().equals(SocketUtils.SOCKETEXCEPTION_MESSAGE_SOCKET_CLOSED) && !se.getMessage().equals(SocketUtils.SOCKETEXCEPTION_MESSAGE_SOCKET_IS_CLOSED)) {
						this.mServerStateListener.onException(se);
					}

					break;
				} catch (final Throwable pThrowable) {
					this.mServerStateListener.onException(pThrowable);
				}
			}
			
			this.close();
		} catch (final Throwable pThrowable) {
			this.mServerStateListener.onException(pThrowable);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		this.close();
		super.finalize();
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public void close() {
		if(!this.mTerminated) {
			super.interrupt();

			this.mRunning = false;
			this.mTerminated = true;

			try {
				/* First interrupt all Clients. */
				final ArrayList<T> clientConnectors = this.mClientConnectors;
				for(int i = 0; i < clientConnectors.size(); i++) {
					clientConnectors.get(i).interrupt();
				}
				clientConnectors.clear();

				this.mServerStateListener.onTerminated();
			} catch (final Exception e) {
				this.mServerStateListener.onException(e);
			}
		}
	}

	public void sendBroadcastServerMessage(final IServerMessage pServerMessage) throws IOException {
		if(this.mRunning == true && this.mTerminated == false) {
			final ArrayList<T> clientConnectors = this.mClientConnectors;
			for(int i = 0; i < clientConnectors.size(); i++) {
				try {
					clientConnectors.get(i).sendServerMessage(pServerMessage);
				} catch (final IOException e) {
					this.mServerStateListener.onException(e);
				}
			}
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	public static interface IServerStateListener {
		public void onStarted();
		public void onTerminated();
		public void onException(final Throwable pThrowable);

		public static class DefaultServerStateListener implements IServerStateListener {
			@Override
			public void onStarted() {
				Debug.d("Server started.");
			}
			@Override
			public void onTerminated() {
				Debug.d("Server terminated.");
			}
			@Override
			public void onException(final Throwable pThrowable) {
				Debug.e(pThrowable);
			}
		}
	}
}
