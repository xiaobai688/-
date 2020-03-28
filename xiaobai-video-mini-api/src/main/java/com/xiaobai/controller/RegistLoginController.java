package com.xiaobai.controller;

import com.xiaobai.pojo.Users;
import com.xiaobai.pojo.vo.UsersVO;
import com.xiaobai.service.UserService;
import com.xiaobai.utils.IMoocJSONResult;
import com.xiaobai.utils.MD5Utils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Api(value = "用户登录注册接口",tags = "用户登录注册的controller")
public class RegistLoginController extends BasicController{

    @Autowired UserService userService;


    @ApiOperation(value = "用户注册",notes="用户注册的方法")
    @PostMapping("/register")
    public IMoocJSONResult register(@RequestBody Users users) throws Exception {
        //1.判断用户名和密码不为空
        if(StringUtils.isBlank(users.getUsername())||StringUtils.isBlank(users.getPassword())){
            return IMoocJSONResult.errorMsg("用户名和密码不能为空!");
        }
        //2.判断用户名是否存在
        boolean usernameIsExist = userService.queryUsernameIsExist(users.getUsername());
        //3.保存用户,注册信息
        if(!usernameIsExist){
            users.setNickname(users.getUsername());
            users.setPassword(MD5Utils.getMD5Str(users.getPassword()));
            users.setFansCounts(0);
            users.setReceiveLikeCounts(0);
            users.setFollowCounts(0);
            userService.saveUser(users);
        }else {
            return IMoocJSONResult.errorMsg("用户名已存在,请重新输入用户名!");
        }

        users.setPassword("");

        //给用户一个标示放入缓存
      /*  String uniqueToken = UUID.randomUUID().toString();
		redis.set(USER_REDIS_SESSION + ":" + users.getId(), uniqueToken, 1000 * 60 * 30);
		UsersVO userVO = new UsersVO();
		BeanUtils.copyProperties(users, userVO);
		userVO.setUserToken(uniqueToken);*/
        UsersVO usersVO = setUserRedisSessionToken(users);
        return IMoocJSONResult.ok(usersVO);
    }

    public UsersVO setUserRedisSessionToken(Users userModel) {
        String uniqueToken = UUID.randomUUID().toString();
        redis.set(USER_REDIS_SESSION + ":" + userModel.getId(), uniqueToken, 1000 * 60 * 30);

        UsersVO userVO = new UsersVO();
        BeanUtils.copyProperties(userModel, userVO);
        userVO.setUserToken(uniqueToken);
        return userVO;
    }

    @ApiOperation(value = "用户登录",notes="用户登录的方法")
    @PostMapping("/login")
    public IMoocJSONResult login(@RequestBody Users user) throws Exception {
        String username = user.getUsername();
        String password = user.getPassword();

//		Thread.sleep(3000);

        // 1. 判断用户名和密码必须不为空
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return IMoocJSONResult.ok("用户名或密码不能为空...");
        }

        // 2. 判断用户是否存在
        Users userResult = userService.queryUserForLogin(username,
                MD5Utils.getMD5Str(user.getPassword()));

        // 3. 返回
        if (userResult != null) {
            userResult.setPassword("");
            UsersVO usersVO = setUserRedisSessionToken(userResult);
            return IMoocJSONResult.ok(usersVO);
        } else {
            return IMoocJSONResult.errorMsg("用户名或密码不正确, 请重试...");
        }
    }

    @ApiOperation(value="用户注销", notes="用户注销的接口")
    @ApiImplicitParam(name="userId", value="用户id", required=true,
            dataType="String", paramType="query")
    @PostMapping("/logout")
    public IMoocJSONResult logout(String userId) throws Exception {
        redis.del(USER_REDIS_SESSION + ":" + userId);
        return IMoocJSONResult.ok();
    }

}
