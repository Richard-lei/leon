package com.leon.service.impl;

import com.leon.common.Const;
import com.leon.common.ServerResponse;
import com.leon.common.TokenCache;
import com.leon.dao.UserMapper;
import com.leon.pojo.User;
import com.leon.service.IUserService;
import com.leon.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("iUserService")
public class UserServiceImpl implements IUserService{

    @Autowired
    private UserMapper userMapper;

    public ServerResponse<User> login(String username, String password){
        int resultMap = userMapper.checkUsername(username);
        if(resultMap == 0){
            return ServerResponse.createByErrorMessage("用户名不存在！！！");
        }
        //MD5密码校验
        String md5Password = MD5Util.MD5EncodeUtf8(password);
        User user = userMapper.checkLogin(username,md5Password);
        if (user == null){
            return ServerResponse.createByErrorMessage("密码输入错误！！！");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登录成功！！！",user);
    }

    public ServerResponse<String> register(User user){
        //注册前先校验数据库中用户名和邮箱是否已存在
        ServerResponse vaildResponse = this.checkVaild(user.getUsername(),Const.USERNAME);
        if (! vaildResponse.isSuccess()){
            return ServerResponse.createByErrorMessage("用户名已存在！");
        }
        vaildResponse = this.checkVaild(user.getEmail(),Const.EMAIL);
        if(! vaildResponse.isSuccess()){
            return ServerResponse.createByErrorMessage("该邮箱已被注册！");
        }
        //授权用户角色
         user.setRole(Const.Role.ROLE_CUSTOMER);
         //MD5加密
         user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
         int result = userMapper.insert(user);
         if(result == 0){
             return  ServerResponse.createByErrorMessage("注册失败！");
         }
         return ServerResponse.createBySuccessMessage("注册成功！");
    }

    /**
     * 用户名和邮箱的校验，校验数据库中是否已存在
     * @param str 用户名或邮箱
     * @param type str的类型，value为username或email
     * @return
     */
    public ServerResponse<String> checkVaild(String str, String type){
        if(StringUtils.isNotBlank(type)){
            if (Const.USERNAME.equals(type)){
                int result = userMapper.checkUsername(str);
                if (result > 0){
                   return ServerResponse.createByErrorMessage("用户名已存在！");
                }
            }
            if (Const.EMAIL.equals(type)){
                int result = userMapper.checkEmail(str);
                if (result > 0){
                    return ServerResponse.createByErrorMessage("邮箱已存在！");
                }
            }
        }else{
            ServerResponse.createByErrorMessage("参数错误！");
        }
        return ServerResponse.createBySuccessMessage("校验通过！");//true 名字和邮箱不存在
    }

    public ServerResponse<String> selectQuestion(String username){
       ServerResponse vaildResponse = this.checkVaild(username,Const.USERNAME);
       if (vaildResponse.isSuccess()){
           return ServerResponse.createByErrorMessage("用户不存在！");
       }
       String question = userMapper.selectQuestionByUsername(username);
       if (StringUtils.isNotBlank(question)){
           return ServerResponse.createBySuccess(question);
       }
       return ServerResponse.createByErrorMessage("找回密码问题为空！");
    }

    public ServerResponse<String> checkAnswer(String username, String question ,String answer){
        int result = userMapper.checkAnswer(username,question,answer);
        if (result > 0){//说明问题及答案正确
            String forgetToken = UUID.randomUUID().toString();
            //设置有有效期的Token
            TokenCache.setKey(TokenCache.TOKEN_PREFIX + username ,forgetToken);

            return ServerResponse.createBySuccess(forgetToken);
        }
        return ServerResponse.createByErrorMessage("问题答案错误！");
    }

    public ServerResponse<String> forgetResetPassword(String username,String newPassword,String forgetToken){
        if (StringUtils.isBlank(forgetToken)){
            return ServerResponse.createByErrorMessage("参数错误！");
        }
        ServerResponse vaildRespose = this.checkVaild(username,Const.USERNAME);
        if (vaildRespose.isSuccess()){
            return ServerResponse.createByErrorMessage("用户不存在！");
        }
        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX + username);

        if(StringUtils.isBlank(token)){
            return ServerResponse.createByErrorMessage("token无效或过期！");
        }
        //token匹配才能进行密码修改
        if (StringUtils.equals(forgetToken,token)){
            String md5PasswordNew = MD5Util.MD5EncodeUtf8(newPassword);
            int resultCountLine = userMapper.updatePasswordByUsername(username,md5PasswordNew);
            if (resultCountLine > 0){
                return ServerResponse.createBySuccessMessage("修改密码成功！");
            }
        }else {
            return ServerResponse.createByErrorMessage("token错误，请重新回答问题获取token");
        }

        return ServerResponse.createBySuccessMessage("忘记密码的重置密码成功！");
    }

    public ServerResponse<String> resetPassword(String passwordOld,String passwordNew,User user){
        //防止横向越权，校验用户的旧密码，一定要指定是该用户才可修改密码
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld),user.getId());
        if (resultCount == 0){
            return ServerResponse.createByErrorMessage("输入原密码错误！");
        }
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if (updateCount > 0){
            return ServerResponse.createBySuccessMessage("密码更新成功！");
        }
        return ServerResponse.createByErrorMessage("密码更新失败！");
    }
    public ServerResponse<User> updateUserInfo(User user){
        //用户名不能更新，email也需要校验
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(),user.getId());
        if (resultCount > 0){
            return ServerResponse.createByErrorMessage("Email已存在，请换一个邮箱！");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());

        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if (updateCount > 0){
            return  ServerResponse.createBySuccess("更新用户信息成功！",updateUser);
        }
        return ServerResponse.createByErrorMessage("更新用户信息失败！");
    }

    public ServerResponse<User> getUserInfoByUserId(Integer userId){
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null){
            return ServerResponse.createByErrorMessage("找不到当前用户！");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }
}
