package cn.iocoder.gobang.game;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
// 使用哈希表存储房间信息
@Component
public class RoomManager {
    private ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    public void add(Room room) {
        rooms.put(room.getRoomId(), room);
    }

    public void remove(Room room) {
        rooms.remove(room.getRoomId());
    }

    public Room getRoomByRoomId(String roomId) {
        return rooms.get(roomId);
    }
}
