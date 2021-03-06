package com.jubotech.framework.netty.handler.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.protobuf.util.JsonFormat;
import com.jubotech.business.web.domain.AccountInfo;
import com.jubotech.business.web.domain.WeChatAccountInfo;
import com.jubotech.business.web.service.AccountService;
import com.jubotech.business.web.service.WeChatAccountService;
import com.jubotech.framework.netty.common.Constant;
import com.jubotech.framework.netty.utils.MessageUtil;
import com.jubotech.framework.netty.utils.NettyConnectionUtil;

import Jubo.JuLiao.IM.Wx.Proto.ChatRoomPushNotice.ChatRoomPushNoticeMessage;
import Jubo.JuLiao.IM.Wx.Proto.TransportMessageOuterClass.EnumErrorCode;
import Jubo.JuLiao.IM.Wx.Proto.TransportMessageOuterClass.EnumMsgType;
import Jubo.JuLiao.IM.Wx.Proto.TransportMessageOuterClass.TransportMessage;
import io.netty.channel.ChannelHandlerContext;

@Service
public class ChatroomPushNoticeHandler{
	private  final Logger log = LoggerFactory.getLogger(getClass());
	@Autowired
	private WeChatAccountService weChatAccountService;
	@Autowired
	private AccountService accountService;
	
	/**
	 * 手机端推送群聊列表
	 * @param ctx
	 * @param vo
	 */
	
    public  void handleMsg(ChannelHandlerContext ctx, TransportMessage vo) {
        try {
        	ChatRoomPushNoticeMessage req = vo.getContent().unpack(ChatRoomPushNoticeMessage.class);
        	log.info(JsonFormat.printer().print(req));
			WeChatAccountInfo account = weChatAccountService.findWeChatAccountInfoByWeChatId(req.getWeChatId());
			if (null != account && null != account.getAccountid() && 1 != account.getIslogined()) {
				AccountInfo accInfo = accountService.findAccountInfoByid(account.getAccountid());
				if (null != accInfo) {
					// 转发给pc端
					ChannelHandlerContext chx = NettyConnectionUtil.getClientChannelHandlerContextByUserId(accInfo.getAccount());
					if (null != chx) {
						new Thread(
				    			new Runnable() {
									public void run() {
										MessageUtil.sendJsonMsg(chx, EnumMsgType.ChatroomPushNotice,NettyConnectionUtil.getNettyId(chx), null, req);
									}
								}
				    	).start();
					}
				}
				// 告诉客户端消息已收到
				MessageUtil.sendMsg(ctx, EnumMsgType.MsgReceivedAck, vo.getAccessToken(), vo.getId(), null);
			} else {
				// 对方不在线
				MessageUtil.sendErrMsg(ctx, EnumErrorCode.TargetNotOnline, Constant.ERROR_MSG_NOTONLINE);
			}
			
        } catch (Exception e) {
            e.printStackTrace();
            MessageUtil.sendErrMsg(ctx, EnumErrorCode.InvalidParam, Constant.ERROR_MSG_DECODFAIL);
        }
    }
}