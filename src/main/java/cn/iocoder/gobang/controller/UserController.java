package cn.iocoder.gobang.controller;

import cn.iocoder.gobang.mapper.UserMapper;
import cn.iocoder.gobang.model.User;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    @Resource
    private UserMapper userMapper;
    @PostMapping("/login")
    public Object login(String username, String password, HttpServletRequest request){
        User user = userMapper.selectByName(username);
        if (user == null || !user.getPassword().equals(password)){
            //登录失败
            return "用户不存在或密码错误";
        }
        //使用session保存用户信息
        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);
        //登录成功，返回用户信息
        return user;
    }
    @PostMapping("/register")
    public Object register(String username, String password){
        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            userMapper.insert(user);
            return user;
        }catch (org.springframework.dao.DuplicateKeyException e){ // 抛出数据库主键冲突异常，即用户名重复
            User user = new User();
            return user;
        }
    }
    @GetMapping("/userInfo")
    public Object getUserInfo(HttpServletRequest request){
        try {
            // 获取session, false表示不创建session,当已经登录时，获取session
            HttpSession session = request.getSession(false);
            User user = (User)session.getAttribute("user");
            return user;
        } catch (NullPointerException e){
            return new User();
        }
    }
}
