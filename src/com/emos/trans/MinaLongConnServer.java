package com.emos.trans;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

public class MinaLongConnServer {

	private static final int PORT = 8002;
	
	public static Map<Long, MHolder> holderMap = new HashMap<Long, MHolder>();

	public void start() throws IOException {
		IoAcceptor acceptor = new NioSocketAcceptor();
		acceptor.getFilterChain().addLast("logger", new LoggingFilter());
//		acceptor.getFilterChain().addLast(
//				"codec",
//				new ProtocolCodecFilter(new TextLineCodecFactory(Charset
//						.forName("UTF-8"))));
		acceptor.setHandler(new MinaLongConnServerHandler());
		acceptor.getSessionConfig().setReadBufferSize(4096);
		acceptor.bind(new InetSocketAddress(PORT));
		System.out.println("Listeningon port " + PORT);
	}

	public static void main(String[] args) throws IOException {
		MinaLongConnServer server = new MinaLongConnServer();
		server.start();
	}
}
