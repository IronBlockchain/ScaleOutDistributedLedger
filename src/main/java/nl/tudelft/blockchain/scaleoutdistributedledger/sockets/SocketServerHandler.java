package nl.tudelft.blockchain.scaleoutdistributedledger.sockets;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.HandshakeMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.Message;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.util.Map;
import java.util.logging.Level;

/**
 * Handler for socket server.
 */
public class SocketServerHandler extends ChannelInboundHandlerAdapter {

	private final LocalStore localStore;

	/**
	 * Constructor.
	 * @param localStore - the localstore of the node
	 */
	public SocketServerHandler(LocalStore localStore) {
		this.localStore = localStore;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof Message) {
			((Message) msg).handle(localStore);
			
			if (msg instanceof HandshakeMessage) {
				int senderId = ((HandshakeMessage) msg).getSenderId();
				Node node = localStore.getNode(senderId);

				if (node == null) {
					Log.log(Level.WARNING, "Unable to find node " + senderId);
				} else {
					Map<Node, Channel> connections = localStore.getApplication().getTransactionSender().getSocketClient().getConnections();
					if (connections.putIfAbsent(node, ctx.channel()) != null) {
						Log.log(Level.WARNING, "There are 2 sockets connecting node " + localStore.getOwnNode().getId() + " and " + node.getId());
					}
				}
			}
		} else {
			Log.log(Level.SEVERE, "Invalid message, not a message instance");
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		Log.log(Level.SEVERE, "Node " + localStore.getOwnNode().getId() + " Server: socket error", cause);
		ctx.close();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent e = (IdleStateEvent) evt;
			if (e.state() == IdleState.ALL_IDLE) {
				Log.log(Level.INFO, "Node " + localStore.getOwnNode().getId() + " Server: detected idle channel, closing connection!");
				ctx.close();
			}
		}
	}
}
