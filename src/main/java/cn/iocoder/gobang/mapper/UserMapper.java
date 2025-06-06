package cn.iocoder.gobang.mapper;

import cn.iocoder.gobang.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    // 插入用户
    void insert(User user);
    // 根据用户名查询用户
    User selectByName(String username);
}
