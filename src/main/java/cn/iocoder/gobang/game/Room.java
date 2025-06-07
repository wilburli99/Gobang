package cn.iocoder.gobang.game;

import cn.iocoder.gobang.model.User;
import lombok.Data;

import java.util.UUID;
// 表示一个房间
@Data
public class Room {
    private String roomId;
    private User user1;
    private User user2;

    public Room(){
        // 使用UUID来生成随机的字符串作为房间ID
        roomId = UUID.randomUUID().toString();
    }
}
