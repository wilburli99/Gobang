package cn.iocoder.gobang.game;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
// 使用哈希表存储房间信息
@Component
public class RoomManager {
    private ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, String> userIdToRoomId = new ConcurrentHashMap<>();

    public void add(Room room, int userId1, int userId2) {
        rooms.put(room.getRoomId(), room);
        // 存储用户和房间的映射关系
        userIdToRoomId.put(userId1, room.getRoomId());
        userIdToRoomId.put(userId2, room.getRoomId());
    }

    public void remove(Room room, int userId1, int userId2) {
        rooms.remove(room.getRoomId());
        // 移除用户和房间的映射关系
        userIdToRoomId.remove(userId1);
        userIdToRoomId.remove(userId2);
    }

    public Room getRoomByRoomId(String roomId) {
        return rooms.get(roomId);
    }

    public Room getRoomByUserId(int userId) {
        String roomId = userIdToRoomId.get(userId);
        // 如果 userId --> roomId 不存在，则返回 null
        if (roomId == null) {
            return null;
        }
        return rooms.get(roomId);
    }
}
