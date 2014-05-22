
import java.net.InetSocketAddress;
import java.util.HashMap;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinaLongConnServerHandler extends IoHandlerAdapter {

	private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());

	@Override
	public void sessionOpened(IoSession session) {

		InetSocketAddress remoteAddress = (InetSocketAddress) session
				.getRemoteAddress();

		String clientIp = remoteAddress.getAddress().getHostAddress();

		System.out.println("LongConnect Server opened Session ID ="
				+ String.valueOf(session.getId()));

		System.out.println("Received from IP: " + clientIp);
	}

	@Override
	public void messageReceived(IoSession session, Object message) {

		System.out.println("-------------------------------------");
		IoBuffer mBuffer;
		mBuffer = (IoBuffer) message;
		int count = mBuffer.limit();
		byte[] bufBytes = mBuffer.array();
		short type, len;
		type = (short) bufBytes[0];
		if (type == 0x01) {
			System.out.println("heart Beat.");
			return;
		}
		short l1 = (short) bufBytes[2];
		short l0 = (short) bufBytes[3];
		l1 <<= 8;
		len = (short) (l1 | l0);

		System.out.println("len : " + len);
		String expression = message.toString();
		System.out.println("Message is:" + expression);

	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) {

		System.out.println("Disconnectingthe idle.");

		// disconnect an idle client

		session.close(true);

	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {

		// close the connection onexceptional situation

		System.out.println("in exception.");
		System.out.println(cause.getMessage());
		logger.warn(cause.getMessage(), cause);

		session.close(true);

	}

}
