package com.game.network.handler;

import com.game.dto.PlayerDO;
import com.game.dto.PlayerStatus;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import com.game.network.protocol.GameProtocol;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.game.service.PlayerService;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static com.game.network.util.SendMessage.sendMessage;

//登录协议处理器
//负责解析登录请求，打印内容并返回登录响应
@Slf4j
@Component
@ChannelHandler.Sharable
public class LoginHandler extends ChannelInboundHandlerAdapter {
    @Autowired
    PlayerService playerService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        //提示用户要先登录
        sendMessage(channel, "请选择登录或进行注册", Opcode.LOGIN_OR_REGISTER_SELECTION, String.valueOf(0));
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //只处理登录协议（通过操作码判断）
        if (msg instanceof GameProtocol protocol) {
            if (protocol.getOpcode() == Opcode.LOGIN_OPCODE) {
                handleLoginRequest(ctx, protocol);
            }
            else if (protocol.getOpcode()==Opcode.REGISTER_OPCODE){
                handleRegisterRequest(ctx,protocol);
            }
            else {
                ctx.fireChannelRead(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    //处理登录请求
    private void handleLoginRequest (ChannelHandlerContext ctx, GameProtocol protocol) {
        //1.解析登录数据体
        String loginData = protocol.getBody() != null ? new String(protocol.getBody(), StandardCharsets.UTF_8) : "";
        String playerId = String.valueOf(protocol.getPlayerId());
        if ("null".equals(playerId) || playerId.trim().isEmpty()) {
            sendMessage(ctx.channel(), "登录失败：无效的玩家ID", Opcode.LOGIN_OR_REGISTER_SELECTION, "0");
            return; // 终止后续处理
        }
        // 2. 打印收到的登录信息（协议头+数据体）
        System.out.println("收到登录请求：");
        System.out.println("  版本号：" + protocol.getVersion());
        System.out.println("  操作码：" + protocol.getOpcode());
        System.out.println("  玩家ID：" + protocol.getPlayerId());
        System.out.println("  登录数据（用户名+密码）：" + loginData);
        System.out.println("  数据长度：" + protocol.getLength() + "字节");
        System.out.println("  校验和：" + protocol.getChecksum());
        //3.模拟登录验证
        boolean loginSuccess = verifyLogin(loginData);
        //4.执行自动匹配handler
        if (loginSuccess) {
            if (playerId.equals("0")){
                 playerId = String.valueOf(playerService.getByName(loginData.split(":", 2)[0]).getPlayerId());
                 sendMessage(ctx.channel(),"登录成功，玩家ID为"+playerId,Opcode.ASSIGN_PLAYER_ID,playerId);
            }
            //修改玩家的状态
            playerService.updateStatus(playerId, PlayerStatus.LOGIN);
            AutoMatchServerHandler matchHandler = ctx.channel().pipeline().get(AutoMatchServerHandler.class);
            if (matchHandler != null) {
                matchHandler.onLoginSuccess(playerId,ctx.channel()); // 通知匹配处理器登录成功
            }
        }
        else {
            //发送给客户端
            SendMessage.sendMessage(ctx.channel(),"登录失败：用户名或密码错误,请重新操作",Opcode.LOGIN_OR_REGISTER_SELECTION,playerId);
        }
    }
    private boolean verifyLogin ( String loginData) {
        if (loginData.contains(":")) {
            String[] parts = loginData.split(":", 2);
            if (parts.length != 2) return false;
            String username = parts[0];
            String password = parts[1];
            PlayerDO playerDO = playerService.getByName(username);
            return playerDO != null && passwordEncoder.matches(password, playerDO.getPassword());
        }
        return false;
    }
    //生成UUID
    private String createPlayerId(){
        for (int i=0 ; i<20 ; i++) {
            int randomNum = 1000 + new Random().nextInt(1000,9999);
            if (playerService.getByPlayerId(String.valueOf(randomNum))==null) {
                return String.valueOf(randomNum);
            }
        }
        return null;
    }
    //处理注册请求
    private void handleRegisterRequest(ChannelHandlerContext ctx, GameProtocol protocol) {
        //1.解析注册数据体
        String registerData = protocol.getBody() != null ? new String(protocol.getBody(), StandardCharsets.UTF_8) : "";
        //2.判断是否重名
        if (registerData.contains(":")){
            String[] parts = registerData.split(":",2);
            String userName = parts[0];
            String password = parts[1];
            //用户名校验
            if (userName==null || !userName.matches("^.{1,6}$")){
                sendMessage(ctx.channel(), "用户名必须为1-6位字符", Opcode.REGISTER_AGAIN_OPCODE, "0");
                return;
            }
            if (password == null || password.trim().isEmpty()) {
                sendMessage(ctx.channel(), "密码不能为空，请重新输入", Opcode.REGISTER_AGAIN_OPCODE, "0");
                return;
            }
            if (!password.matches("^.{6,10}$")){
                SendMessage.sendMessage(ctx.channel(),"密码必须为6-10位字符",Opcode.REGISTER_AGAIN_OPCODE,"0");
                return;
            }
            if (playerService.getByName(userName)!=null){
                sendMessage(ctx.channel(),"用户名已存在，请重新注册",Opcode.REGISTER_AGAIN_OPCODE, "0");
                return;
            }
            //创建玩家
            String encodedPassword = passwordEncoder.encode(password);
            String playerId = createPlayerId();
            playerService.createPlayer(playerId,userName,encodedPassword);
            //进行登录
            SendMessage.sendMessage(ctx.channel(),"您已完成注册！玩家ID为"+playerId,Opcode.ASSIGN_PLAYER_ID,playerId);
        }
        else {
            SendMessage.sendMessage(ctx.channel(),"格式错误，请使用：用户名:密码",Opcode.REGISTER_AGAIN_OPCODE,"0");
        }
    }
    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause){
        log.error("登录处理异常{}", cause.getMessage());
        ctx.close();
    }
    }