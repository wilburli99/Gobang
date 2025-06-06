package cn.iocoder.gobang.game;

import cn.iocoder.gobang.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;
// 匹配器-- 匹配对列
@Component
public class Matcher {
    private Queue<User> normalQueue = new LinkedList<>();
    private Queue<User> highQueue = new LinkedList<>();
    private Queue<User> veryHighQueue = new LinkedList<>();

    @Autowired
    private OnlineUserManage  onlineUserManage;

    // 添加用户到匹配队列
    public void add(User user){
        if (user.getScore() < 2000){
            normalQueue.offer(user);
            System.out.println("用户" + user.getUsername() + "进入普通队列");
        } else if (user.getScore() >= 2000 && user.getScore() < 3000) {
            highQueue.offer(user);
            System.out.println("用户" + user.getUsername() + "进入高级队列");
        } else {
            veryHighQueue.offer(user);
            System.out.println("用户" + user.getUsername() + "进入超高级队列");
        }
    }
    // 从匹配对列中移除用户
    public void remove(User user){
        if (user.getScore() < 2000){
            normalQueue.remove(user);
            System.out.println("用户" + user.getUsername() + "离开普通队列");
        } else if (user.getScore() >= 2000 && user.getScore() < 3000) {
            highQueue.remove(user);
            System.out.println("用户" + user.getUsername() + "离开高级队列");
        } else {
            veryHighQueue.remove(user);
            System.out.println();
        }
    }
}
