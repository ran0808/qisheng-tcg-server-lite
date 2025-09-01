import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import com.game.network.codec.GameProtocolDecoder;
import com.game.network.codec.GameProtocolEncoder;
import com.game.network.protocol.GameProtocol;
import com.game.network.protocol.Opcode;
import com.game.network.util.SendMessage;
import com.game.network.util.TerminalColor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class ClientA {
    enum ClientState{
        WAITING_FOR_START_CONFIRM,//等待用户确认是否开始游戏
        WAITING_FOR_CARD_SELECTION,//等待用户选择三张卡牌
        WAITING_FOR_PLAY_CARD_SELECTION,//等待用户选择出战卡牌
        WAITING_FOR_DICE_SELECTION,//等待用户重新投掷骰子
        WAITING_FOR_ATTACK,//等待用户进行技能的选择
        WAITING_FOR_ACTION_OPTION,//让用户进行选择是重新释放技能还是结束回合
        WAITING_FOR_ANOTHER_CARD,
        WAITING_FOR_ACTION_CARD,
        WAITING_FOR_LOGIN_OR_REGISTER,
        WAITING_FOR_LOGIN_INFO,
        WAITING_FOR_REGISTER_INFO, WAITING,
    }
    //当前客户端输入状态（默认：等待开始确认）
    private static volatile ClientState currentState = ClientState.WAITING_FOR_START_CONFIRM;
    //服务器返回可用的角色卡牌列表
    private static volatile List<String> actionCards = new ArrayList<>();
    private static volatile List<String> availableCards = new ArrayList<>();
    private static volatile List<String> selectedCards = new ArrayList<>();
    private volatile static boolean firstHand = false;
    private volatile static boolean myTurn= false;
    private volatile static boolean isQuickAction = false;
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new GameProtocolDecoder())
                                    .addLast(new GameProtocolEncoder())
                                    .addLast(new IdleStateHandler(0, 60, 0))
                                    .addLast(new HeartbeatHandler())
                                    .addLast(new ClientHandler()); // 客户端消息处理器
                        }
                    });
            ChannelFuture future = bootstrap.connect("localhost", 8087).sync();
            Channel channel = future.channel();
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String msg = scanner.nextLine();
                //根据当前状态处理输入
                if (currentState==ClientState.WAITING_FOR_LOGIN_OR_REGISTER){
                    if (msg.equals("登录")){
                        System.out.println("请按格式输入您的用户名和密码，格式为：用户名：密码");
                        currentState=ClientState.WAITING_FOR_LOGIN_INFO;
                    }
                    else if (msg.equals("注册")){
                        System.out.println("请按格式输入您的要注册的用户名和密码，格式为：用户名：密码");
                        System.out.println("提示：用户名1-6位字符，密码6-10位字符");
                        currentState = ClientState.WAITING_FOR_REGISTER_INFO;
                    }
                    else {
                        System.out.println("您的输入有误，请选择登录或注册：");
                    }
                }
                else if (currentState==ClientState.WAITING_FOR_LOGIN_INFO){
                    if (msg.contains(":")||msg.contains("：")){
                        String normalizedMsg = msg.replace("：", ":");
                        if (ClientHandler.localPlayerId == null || ClientHandler.localPlayerId.isEmpty()) {
                            SendMessage.sendMessage(channel, normalizedMsg, Opcode.LOGIN_OPCODE,"0");
                        }
                        else {

                            SendMessage.sendMessage(channel, normalizedMsg, Opcode.LOGIN_OPCODE, ClientHandler.localPlayerId);
                        }
                    }
                    else {
                        System.out.println("格式错误，请使用：用户名:密码");
                    }
                }
                else if (currentState == ClientState.WAITING_FOR_REGISTER_INFO) {
                    if (msg.contains(":") || msg.contains("：")) {
                        String normalizedMsg = msg.replace("：", ":");
                        SendMessage.sendMessage(channel, normalizedMsg, Opcode.REGISTER_OPCODE, String.valueOf(0));
                    } else {
                        System.out.println("格式错误，请使用：用户名:密码");
                    }
                }
                else if (currentState ==ClientState.WAITING_FOR_START_CONFIRM) {
                //如果玩家准备好了，就发送确定，否则发送取消，其余无效
                    if (msg.equals("取消")) {
                        SendMessage.sendMessage(channel,"用户退出游戏",Opcode.LOGOUT_OPCODE,ClientHandler.localPlayerId);
                        break;
                    }
                    else if (msg.equals("确定")) {
                        SendMessage.sendMessage(channel,"玩家"+ClientHandler.localPlayerId+msg+"开始游戏",Opcode.GAME_START_OPCODE,ClientHandler.localPlayerId);
                        currentState = ClientState.WAITING;
                    }
                    else {
                        System.out.println("您的输入不合法，请输入\"确定\"或\"取消\"");
                    }
                }
                //阶段二：处理卡牌选择
                else if (currentState == ClientState.WAITING_FOR_CARD_SELECTION) {
                    //分割用户输入的卡牌
                    selectedCards = Arrays.stream(msg.split("[,，]\\s*"))
                            .map(String::trim)
                            .collect(Collectors.toList());
                    //验证输入有效性
                    if (selectedCards.size()!=3){
                        System.out.println("请选择3张卡牌，用逗号分割！");
                        continue;
                    }
                    boolean allValid = availableCards.containsAll(selectedCards);
                    if (!allValid){
                        System.out.println("输入的卡牌无效，请从提供的列表中选择！");
                        continue;
                    }
                    //发送选择的卡牌给服务器
                    GameProtocol protocol = new GameProtocol();
                    protocol.setPlayerId(Short.parseShort(ClientHandler.localPlayerId));
                    protocol.setOpcode(Opcode.SELECT_CARD_OPCODE);
                    protocol.setBody(String.join(",",selectedCards).getBytes(StandardCharsets.UTF_8));
                    channel.writeAndFlush(protocol);

                    System.out.println("卡牌选择已发送：" + selectedCards);
                    currentState = ClientState.WAITING;
                }
                //阶段三：读取玩家选择的出战卡牌
                else if (currentState == ClientState.WAITING_FOR_PLAY_CARD_SELECTION){
                    //判断所选卡牌的有效性
                    List<String> playOnCard = Arrays.stream(msg.split("[,，]\\s*"))
                            .map(String::trim)
                            .toList();
                    if (playOnCard.size()!=1){
                        System.out.println("请选择一张卡牌作为您的当前出战卡牌");
                        continue;
                    }
                    if (!selectedCards.contains(playOnCard.get(0))){
                        System.out.println("输入的卡牌无效，请重新输入您的出战卡牌");
                        continue;
                    }
                    SendMessage.sendMessage(channel,playOnCard.get(0),Opcode.SELECT_CARD_ON_OPCODE,ClientHandler.localPlayerId);
                    currentState = ClientState.WAITING_FOR_ACTION_CARD;
                }
                //阶段四：读取用户投掷骰子信息
                else if (currentState == ClientState.WAITING_FOR_DICE_SELECTION) {
                    if (msg.equals("确定")) {
                        SendMessage.sendMessage(channel, "", Opcode.DICE_RESELECT_SUBMIT, ClientHandler.localPlayerId);
                        currentState = ClientState.WAITING_FOR_ATTACK;
                    }
                    else {
                        //检查用户输入的合法性，是数字，并且数量小于8
                        List<String> diceSelected = Arrays.stream(msg.split("[,，]\\s*"))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty()) // 过滤空串
                                .toList();
                        if (diceSelected.isEmpty()) {
                            System.out.println("请输入要重掷的骰子索引（用逗号/点分隔，如1,3,5）");
                        }
                        boolean allValid = diceSelected.stream().allMatch(s -> {
                            try {
                                int num = Integer.parseInt(s);
                                return num >= 0 && num < 8;
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        });
                        if (!allValid) {
                            System.out.println("您的输入有误，核对后重新输入:");
                        }
                        else {
                            SendMessage.sendMessage(channel, String.join(",", diceSelected), Opcode.DICE_RESELECT_SUBMIT, ClientHandler.localPlayerId);
                            currentState = ClientState.WAITING_FOR_ATTACK;
                        }
                    }
                }
                //阶段五：进行攻击：第一回合
                else if (currentState==ClientState.WAITING_FOR_ATTACK) {
                    if (firstHand||myTurn) {
                        switch (msg)
                        {
                            case "1":
                                SendMessage.sendMessage(channel, "1", Opcode.ATTACK_OPCODE, ClientHandler.localPlayerId);
                                 firstHand=false;
                                 myTurn=false;
                                break;
                            case "2":
                                SendMessage.sendMessage(channel, "2", Opcode.ATTACK_OPCODE, ClientHandler.localPlayerId);
                                firstHand=false;
                                myTurn=false;
                                break;
                            case "3":
                                SendMessage.sendMessage(channel, "3", Opcode.ATTACK_OPCODE, ClientHandler.localPlayerId);
                                firstHand=false;
                                myTurn=false;
                                break;
                            default:
                                System.out.println("您输入的技能无效...");
                                break;
                        }
                    }
                    else {
                        System.out.println("不是您的回合，请等待...");
                    }
                }
            //阶段六：选择是否结束回合或继续释放技能
            else if (currentState==ClientState.WAITING_FOR_ACTION_OPTION){
                if (myTurn||firstHand){
                    String processedMsg = msg.replaceAll("\\s+", "").toLowerCase();
                    if (processedMsg.contains("重选") || processedMsg.contains("重新选")||processedMsg.contains("重新"))
                    {
                        System.out.println("请输入您重新选择的技能序号：");
                        currentState = ClientState.WAITING_FOR_ATTACK;
                        myTurn=true;
                    }
                    else if (processedMsg.contains("结束"))
                    {
                        SendMessage.sendMessage(channel,"玩家"+ClientHandler.localPlayerId+"结束本回合",Opcode.TURN_END,ClientHandler.localPlayerId);
                        currentState = ClientState.WAITING_FOR_ATTACK;
                    }
                    else if (processedMsg.contains("退出")){
                        channel.close().sync();
                        break;
                    }
                    else{
                        System.out.println("输入不明确，请输入‘重选’（重新选择技能）或‘结束’（结束回合）");
                    }
                }
                else {
                    System.out.println("不是你的回合，请等待...");
                }
            }
            //阶段七：选择另外一张作为出战卡牌
            else if (currentState == ClientState.WAITING_FOR_ANOTHER_CARD){
                    //判断所选卡牌的有效性
                    List<String> playOnCard = Arrays.stream(msg.split("[,，]\\s*"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                    if (playOnCard.size()!=1){
                        System.out.println("请选择一张卡牌作为您的当前出战卡牌");
                        continue;
                    }
                    String input = playOnCard.get(0).trim();
                    if (!selectedCards.contains(input)){
                        System.out.println("输入的卡牌无效，请重新输入您的出战卡牌");
                        continue;
                    }
                    if (isQuickAction) {
                        SendMessage.sendMessage(channel,input,Opcode.SELECT_NEW_ACTIVE_CHARACTER_QUICK_OPCODE,ClientHandler.localPlayerId);
                        currentState = ClientState.WAITING_FOR_ACTION_CARD;
                    }
                    else {
                    SendMessage.sendMessage(channel,input,Opcode.SELECT_NEW_ACTIVE_CHARACTER_OPCODE,ClientHandler.localPlayerId);
                    currentState = ClientState.WAITING_FOR_ATTACK;
                    }
            }
            //阶段八：选择行动卡
            else if (currentState==ClientState.WAITING_FOR_ACTION_CARD){
                    if (firstHand||myTurn) {
                        switch (msg) {
                            case "确定" -> {
                                System.out.println("请输入1,2或3选择技能进行攻击:");
                                currentState = ClientState.WAITING_FOR_ATTACK;
                            }
                            case "换人" -> SendMessage.sendMessage(channel,"玩家主动进行换人",Opcode.SWITCH_CHARACTER_OPCODE,ClientHandler.localPlayerId);
                            case "结束" ->
                                    SendMessage.sendMessage(channel, "玩家" + ClientHandler.localPlayerId + "结束本回合", Opcode.TURN_END, ClientHandler.localPlayerId);
                            default -> {
                                //  判断所选卡牌的有效性
                                List<String> actionCard = Arrays.stream(msg.split("[,，]\\s*"))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .toList();
                                boolean allMatch = actionCard.stream().allMatch(
                                        s -> {
                                            if (actionCards.contains(s)) {
                                                actionCards.remove(s);
                                                return true;
                                            }
                                            return false;
                                        }
                                );
                                if (!allMatch) {
                                    System.out.println("输入的卡牌无效，请重新输入");
                                } else {
                                    SendMessage.sendMessage(channel, String.join(",", actionCard), Opcode.ACTION_CARD_OPCODE, ClientHandler.localPlayerId);
                                    System.out.println("功能牌已使用，等待响应...");
                                }
                            }
                        }
                    }
                    else {
                        System.out.println("还不是你的回合，请等待...");
                    }
                }
            }


    }
        finally {
            group.shutdownGracefully();
        }
    }

    // 客户端消息处理器（打印服务器/对方发来的消息）
    private static class ClientHandler extends SimpleChannelInboundHandler<GameProtocol> {
        public static String localPlayerId;
        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, GameProtocol msg) {
            if (msg.getOpcode()==Opcode.LOGIN_OR_REGISTER_SELECTION){
                String content = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.println(TerminalColor.colorize(content,TerminalColor.BLINK));
                currentState=ClientState.WAITING_FOR_LOGIN_OR_REGISTER;
            }
            else if (msg.getOpcode()==Opcode.REGISTER_AGAIN_OPCODE){
                String content = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.println(TerminalColor.colorize(content,TerminalColor.BLINK));
                System.out.println("请重新按格式输入您的要注册的用户名和密码，格式为：用户名：密码");
                currentState=ClientState.WAITING_FOR_REGISTER_INFO;
            }
            else if (msg.getOpcode() == Opcode.ASSIGN_PLAYER_ID) {
                // 从消息体中解析玩家ID（服务器返回的字符串）
                String content = new String(msg.getBody(), StandardCharsets.UTF_8);
                Pattern pattern = Pattern.compile("玩家ID为(\\d+)");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()){
                    localPlayerId = matcher.group(1);
                    if (!content.contains("登录成功")){
                        System.out.println("成功获取玩家ID：" + localPlayerId);
                        //进行登录
                        System.out.println("请输入用户名，密码进行登录：");
                        currentState = ClientState.WAITING_FOR_LOGIN_INFO;
                    }
                }
                else {
                    System.out.println("解析玩家ID失败,请重新选择注册或登录进行操作");
                    currentState = ClientState.WAITING_FOR_LOGIN_OR_REGISTER;
                }
            }
            else if (msg.getOpcode()==Opcode.LOGIN_RESPONSE_OPCODE){
                String response = new String(msg.getBody(),StandardCharsets.UTF_8);
                System.out.println(response);
                currentState = ClientState.WAITING;
            }
            else if (msg.getOpcode() == Opcode.ALERT_OPCODE){
                String response = new String(msg.getBody(),StandardCharsets.UTF_8);
                System.out.println(response);
                if (response.contains("无效的角色选择，请重新选择")){
                    currentState = ClientState.WAITING_FOR_ANOTHER_CARD;
                }
            }
            else if (msg.getOpcode()==Opcode.MATCH_SUCCESS_OPCODE){
                String content = new String(msg.getBody(),StandardCharsets.UTF_8);
                System.out.println(content);
                currentState = ClientState.WAITING_FOR_START_CONFIRM;
            }
            else if (msg.getOpcode() == Opcode.SEND_CARD_OPCODE) {
                //处理卡牌选择信息
                String content = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.println(content);
                //解析卡牌列表
                if (content.startsWith("[")){
                    String cardStr = content.replaceAll("[\\[\\]]","");
                    availableCards =new ArrayList<> (Arrays.asList(cardStr.split(", ")));
                    //切换状态为"等待卡牌选择"，并提醒用户输入
                    currentState = ClientState.WAITING_FOR_CARD_SELECTION;
                }
            } else if (msg.getOpcode()==Opcode.SELECT_CARD_ON_OPCODE) {
                //处理用户输入出战卡牌
                String content = new String(msg.getBody(),StandardCharsets.UTF_8);
                System.out.println(content);
                currentState = ClientState.WAITING_FOR_PLAY_CARD_SELECTION;
            } else if (msg.getOpcode()==Opcode.DICE_RESELECT_PROMPT) {
                //处理骰子投递，以及重新选择问题
                String content = new String(msg.getBody(),StandardCharsets.UTF_8);
                System.out.println(content);
                System.out.println("请选择您要重新投掷的骰子：(输入骰子的位序)，或输入'确定'不重掷");
                currentState = ClientState.WAITING_FOR_DICE_SELECTION;
            }else if (msg.getOpcode()==Opcode.DICE_RESELECT_RESULT){
                //完成骰子重掷，进行下一步
                String content = new String(msg.getBody(),StandardCharsets.UTF_8);
                System.out.println(content);
            }else if (msg.getOpcode()==Opcode.SEND_ACTION_CARD_OPCODE){
                //处理卡牌选择信息，将content转化为列表进行存储
                String content = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.println("您的功能牌是："+content);
                String cardStr = content.replaceAll("[\\[\\]]", "");
                actionCards = Arrays.stream(cardStr.split(",\\s*")).collect(Collectors.toList());
                if (firstHand) {
                    System.out.println("选择你要使用的功能牌，输入“确定”发起攻击，输入“换人”进行角色切换，输入“结束”结束当前回合：");
                    currentState = ClientState.WAITING_FOR_ACTION_CARD;
                }
            }
            else if(msg.getOpcode()==Opcode.FIRST_HAND_RESULT){
                String content = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.println(content);
                if (content.contains("后手")){
                    firstHand = false;
                    System.out.println("等待对方进行行动");
                }
                else {
                    firstHand = true;
                }
            }
            else if (msg.getOpcode()==Opcode.ATTACK_CONTINUE_OPCODE){
                String content = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.println(content);
                myTurn=true;
                currentState = ClientState.WAITING_FOR_ACTION_CARD;
                System.out.println("选择你要使用的功能牌，输入“确定”发起攻击，输入“换人”进行角色切换，输入“结束”结束当前回合：");
                System.out.println("您的行动卡牌为:"+actionCards);
            }
            else if (msg.getOpcode()==Opcode.TURN_START){
                String content = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.println(content);
            }
            else if (msg.getOpcode()==Opcode.ACTION_OPTION_OPCODE){
                String content = new String(msg.getBody(),StandardCharsets.UTF_8);
                System.out.println(content);
                if (content.contains("重新")){
                    myTurn=true;
                }
                currentState = ClientState.WAITING_FOR_ACTION_OPTION;
            }
            else if (msg.getOpcode()==Opcode.SEND_REMAINING_CARD_OPCODE) {
                //解析传过来的可以使用的卡牌
                String content = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.println(content);
                int index = content.indexOf("[");
                String cardName = content.substring(index).replaceAll("[\\[\\]]","");
                // 提取每个卡牌的名称
                selectedCards = Arrays.stream(cardName.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(ArrayList::new));
                isQuickAction = content.contains("快速行动");
                currentState = ClientState.WAITING_FOR_ANOTHER_CARD;
            }
            else if (msg.getOpcode()==Opcode.BROADCAST_OPCODE){
                String content = new String(msg.getBody(),StandardCharsets.UTF_8);
                System.out.println(TerminalColor.colorize("系统消息:",TerminalColor.RED));
                System.out.println(TerminalColor.colorize(content,TerminalColor.BLUE));
            } else if (msg.getOpcode() == Opcode.USER_OFFLINE) {
                // 显示“对方已下线”提示
                String tip = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.println(tip);
            } else if (msg.getOpcode() == Opcode.AUTO_EXIT) {
                // 接收服务器的自动退出通知
                String exitMsg = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.println(exitMsg);
                // 关闭客户端
                channelHandlerContext.close();
                System.exit(0); // 退出程序
            }
            else if (msg.getOpcode() == Opcode.GAME_OVER_OPCODE){
                String result = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.println(TerminalColor.colorize("\n===== 游戏结束 =====", TerminalColor.PURPLE));
                System.out.println(TerminalColor.colorize(result, TerminalColor.YELLOW));
                System.out.println(TerminalColor.colorize("====================", TerminalColor.PURPLE));
                //重掷游戏状态
                currentState = ClientState.WAITING;
                availableCards.clear();
                selectedCards.clear();
                firstHand = true;
                myTurn = false;
                Channel channel = channelHandlerContext.channel(); // 从上下文获取当前连接
                if (channel.isActive()) {
                    channel.close().addListener(future -> {
                        if (future.isSuccess()) {
                            System.out.println("已退出房间，程序即将关闭");
                        } else {
                            System.out.println("退出房间失败");
                        }
                        System.exit(0);
                    });
                }
                else {
                    System.exit(0);
                }
            }

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("客户端处理异常: {}", cause.getMessage(), cause);
            ctx.close();

        }
    }

    private static class HeartbeatHandler extends ChannelDuplexHandler{
        private static final int MAX_RETRY = 5;
        private int retryCount = 0;

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent event){
                if (event.state() == IdleState.WRITER_IDLE){
                    //发送心跳包
                    SendMessage.sendMessage(ctx.channel(),"heartbeat",Opcode.HEARTBEAT_REQUEST,ClientHandler.localPlayerId != null ? ClientHandler.localPlayerId : "0");
                    retryCount++;
                    if (retryCount>=MAX_RETRY){
                        System.out.println("服务器无响应，已断开连接");
                        ctx.close();
                        System.exit(0);
                    }
                }
            }
            else {
                super.userEventTriggered(ctx,evt);
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof GameProtocol protocol && protocol.getOpcode() == Opcode.HEARTBEAT_RESPONSE) {
                retryCount = 0;
            }
            super.channelRead(ctx, msg);
        }
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("服务器已关闭，连接断开");
            ctx.close();
            System.exit(0);  // 退出客户端程序
        }
    }

}