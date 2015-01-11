package com.emos.trans;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.List;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import com.emos.trans.log.MLog;
import com.emos.trans.msg.HomeAlarmHandler;
import com.emos.trans.msg.HomeRegisterHandler;
import com.emos.trans.msg.PhoneControlHandler;
import com.emos.trans.msg.PhoneLoginHandler;
import com.emos.trans.msg.PhoneUUIDHandler;
import com.emos.trans.msgInterface.MsgDispatcher;
import com.emos.trans.pojo.UserTemp;
import com.emos.trans.test.Test;
import com.emos.trans.util.DBHelper;

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
		
		DBHelper.cleanTables();
//		Test.testDB();
		
		/*
		 * 注册消息处理模块
		 */
		//手机端注册，登录
		PhoneLoginHandler rgsHandler = new PhoneLoginHandler();
		MsgDispatcher.registerMsgHandler(MCommon.MSG_PHONE_LOGIN, rgsHandler);
		//家庭端注册
		HomeRegisterHandler homeRegisterHandler = new HomeRegisterHandler();
		MsgDispatcher.registerMsgHandler(MCommon.MSG_HOME_REG, homeRegisterHandler);
		MsgDispatcher.registerMsgHandler(MCommon.MSG_HOME_UNREG, homeRegisterHandler);
		//手机端UUID注册、注销
		PhoneUUIDHandler phoneUUIDHandler = new PhoneUUIDHandler();
		MsgDispatcher.registerMsgHandler(MCommon.MSG_PHONE_REG_UUID, phoneUUIDHandler);
		MsgDispatcher.registerMsgHandler(MCommon.MSG_PHONE_UNREG_UUID, phoneUUIDHandler);
		//手机端UUID注册、注销
		PhoneControlHandler phoneControlHandler = new PhoneControlHandler();
		MsgDispatcher.registerMsgHandler(MCommon.MSG_PHONE_CONTROL, phoneControlHandler);
		MsgDispatcher.registerMsgHandler(MCommon.MSG_PHONE_CONTROL_RE, phoneControlHandler);
		//家庭端的推送消息处理
		HomeAlarmHandler homeAlarmHandler = new HomeAlarmHandler();
		MsgDispatcher.registerMsgHandler(MCommon.MSG_HOME_ALARM, homeAlarmHandler);
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
