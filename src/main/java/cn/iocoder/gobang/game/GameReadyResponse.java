package cn.iocoder.gobang.game;

import lombok.Data;
// 这个类表示游戏开始的响应
@Data
public class GameReadyResponse {
    private String message;
    private boolean ok;
    private String reason;
    private String roomId;
    private int thisUserId;
    private int thatUserId;
    private int whiteUser;
    private String thisUsername; // 新增：当前玩家的用户名
    private String thatUsername; // 新增：对手玩家的用户名
}
