package com.lihuahua.hyperspace.Service;

import com.lihuahua.hyperspace.models.bo.MessageBO;

public interface MessageService {

    void sendPrivateMessage(MessageBO message);

    void sendGroupMessage(MessageBO message);
}
