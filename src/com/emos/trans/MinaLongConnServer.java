package com.emos.trans;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import com.emos.trans.log.MLog;

public class MinaLongConnServer {

	private static final int PORT = 8002;

//	public static Map<Long, MHolder> holderMap = new HashMap<Long, MHolder>();

	public void start() throws IOException {

		IoAcceptor acceptor = new NioSocketAcceptor();
		acceptor.getFilterChain().addLast("logger", new LoggingFilter());
		// acceptor.getFilterChain().addLast(
		// "codec",
		// new ProtocolCodecFilter(new TextLineCodecFactory(Charset
		// .forName("UTF-8"))));
		// acceptor.getFilterChain().addLast("String", new
		// ProtocolCodecFilter(new TextLineEncoder(), new ));
		acceptor.setHandler(new MinaLongConnServerHandler());
		acceptor.getSessionConfig().setReadBufferSize(4096 * 2);
		acceptor.getSessionConfig().setReaderIdleTime(MCommon.READER_IDEL_TIMEOUT);
		acceptor.bind(new InetSocketAddress(PORT));
		MLog.logger.info("Listeningon port " + PORT);
	}

	public static void main(String[] args) throws IOException {
		MinaLongConnServer server = new MinaLongConnServer();
		try {
			server.start();
		} catch (BindException e) {
			MLog.logger.error("Address already in use. PORT={}", PORT);
		}
	}
}
