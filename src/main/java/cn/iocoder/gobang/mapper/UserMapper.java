package cn.iocoder.gobang.mapper;

import cn.iocoder.gobang.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    // 插入用户
    void insert(User user);
    // 根据用户名查询用户
    User selectByName(String username);
    // 玩家获胜：总场数+1，总胜数+1，分数+30
    void userWin(int userId);
    // 玩家失败：总场数+1，总胜数不变，分数-30
    void userLose(int userId);
}
