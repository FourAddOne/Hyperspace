package com.lihuahua.hyperspace.server;

import com.lihuahua.hyperspace.Result.Result;
import com.lihuahua.hyperspace.models.dto.GroupDTO;

import java.util.List;

public interface GroupServer {
    //创建群聊
    public void createGroup(GroupDTO groupDTO);

    //加入群聊
    public void joinGroup(GroupDTO groupDTO);

    //退出群聊
    public void quitGroup(GroupDTO groupDTO);

    //群聊消息
    public void groupMessage(GroupDTO groupDTO);

    //群聊列表
    public List<GroupDTO> groupList(GroupDTO groupDTO);

    //群聊用户列表
    public void groupUserList(GroupDTO groupDTO);


}
