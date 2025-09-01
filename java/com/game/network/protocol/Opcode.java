package com.game.network.protocol;

/**
 * 协议操作码定义类
 * 按功能模块划分数值区间，便于管理和扩展：
 * - 登录注册：0x0000-0x000F
 * - 连接心跳：0x0010-0x001F
 * - 用户状态：0x0020-0x002F
 * - 游戏匹配与初始化：0x0030-0x003F
 * - 角色与卡牌：0x0040-0x005F
 * - 战斗与回合：0x0060-0x007F
 * - 骰子相关：0x0080-0x008F
 * - 广播相关：0x0090-0x009F
 */
public class Opcode {

    // ======================================
    // 登录注册相关（0x0000-0x000F）
    // ======================================
    public static final short ALERT_OPCODE                = 0x0000; // 向客户端发送提醒消息(如登录提示)
    public static final short LOGIN_OPCODE                = 0x0001; // 登录请求
    public static final short LOGIN_RESPONSE_OPCODE       = 0x0002; // 登录响应
    public static final short LOGOUT_OPCODE               = 0x0003; // 退出登录请求
    public static final short LOGOUT_RESPONSE_OPCODE      = 0x0004; // 退出登录响应
    public static final short LOGIN_OR_REGISTER_SELECTION = 0x0005; // 选择登录或注册
    public static final short REGISTER_OPCODE             = 0x0006; // 注册请求
    public static final short REGISTER_AGAIN_OPCODE       = 0x0007; // 重新注册用户信息请求

    // ======================================
    // 连接与心跳相关（0x0010-0x001F）
    // ======================================
    public static final short HEARTBEAT_REQUEST  = 0x0010; // 心跳请求
    public static final short HEARTBEAT_RESPONSE = 0x0011; // 心跳回应

    // ======================================
    // 用户状态相关（0x0020-0x002F）
    // ======================================
    public static final short ASSIGN_PLAYER_ID = 0x0020; // 服务器发放客户端ID
    public static final short USER_OFFLINE     = 0x0021; // 用户退出消息通知
    public static final short AUTO_EXIT        = 0x0022; // 用户自动退出通知

    // ======================================
    // 游戏匹配与初始化相关（0x0030-0x003F）
    // ======================================
    public static final short MATCH_SUCCESS_OPCODE = 0x0031; // 匹配成功通知
    public static final short GAME_START_OPCODE    = 0x0032; // 游戏开始通知

    // ======================================
    // 角色与卡牌相关（0x0040-0x005F）
    // ======================================
    public static final short SEND_CARD_OPCODE             = 0x0040; // 服务器向客户端发送角色卡(开始游戏)
    public static final short SELECT_CARD_OPCODE           = 0x0041; // 用户选择角色卡
    public static final short SELECT_CARD_ON_OPCODE        = 0x0042; // 选择出战角色
    public static final short SEND_REMAINING_CARD_OPCODE   = 0x0043; // 发送剩余卡牌信息
    public static final short ACTION_CARD_OPCODE           = 0x0044; // 选择行动卡牌
    public static final short SEND_ACTION_CARD_OPCODE      = 0x0045; // 发送行动卡给对方
    public static final short SWITCH_CHARACTER_OPCODE      = 0x0046; // 切换角色
    public static final short SELECT_NEW_ACTIVE_CHARACTER_OPCODE    = 0x0047; // 选择新活跃角色
    public static final short SELECT_NEW_ACTIVE_CHARACTER_QUICK_OPCODE = 0x0048; // 快速选择新活跃角色

    // ======================================
    // 战斗与回合相关（0x0060-0x007F）
    // ======================================
    public static final short FIRST_HAND_RESULT  = 0x0060; // 先手判断结果
    public static final short TURN_START         = 0x0061; // 回合开始通知
    public static final short TURN_END           = 0x0062; // 回合结束通知
    public static final short ATTACK_OPCODE      = 0x0063; // 攻击操作
    public static final short ATTACK_CONTINUE_OPCODE = 0x0064; // 继续攻击
    public static final short ACTION_OPTION_OPCODE   = 0x0065; // 行动选项(如选择攻击/防御等)
    public static final short GAME_OVER_OPCODE   = 0x0066; // 游戏结束通知

    // ======================================
    // 骰子相关（0x0080-0x008F）
    // ======================================
    public static final short DICE_RESELECT_PROMPT  = 0x0080; // 骰子投掷阶段提示
    public static final short DICE_RESELECT_SUBMIT  = 0x0081; // 提交重掷骰子索引
    public static final short DICE_RESELECT_RESULT  = 0x0082; // 确定最终骰子结果

    // ======================================
    // 广播相关（0x0090-0x009F）
    // ======================================
    public static final short BROADCAST_OPCODE = 0x0090; // 广播玩家间的操作
}