package com.lihuahua.hyperspace.controller.message;


import com.lihuahua.hyperspace.Service.MessageService;
import com.lihuahua.hyperspace.models.dto.SendMessageDTO;
import com.lihuahua.hyperspace.models.entity.Message;
import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.utils.JwtTokenUtil;
import com.lihuahua.hyperspace.utils.MessageConvertUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

/**
 * 发送消息的接口方法
 * @param sendMessageDTO 包含消息内容的DTO对象
 * @param request HTTP请求对象，用于获取请求头中的认证信息
 * @return 返回操作结果，包含成功或失败信息
 */
    @PostMapping("/send")
    public ResVO sendMessage(@RequestBody SendMessageDTO sendMessageDTO, HttpServletRequest request) {
        try {
            // 从JWT token中获取当前用户ID
            String token = request.getHeader("Authorization");
            String currentUserId = null;
        // 检查token是否存在并以"Bearer "开头
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);  // 去掉"Bearer "前缀
                // 验证token有效性
                if (!JwtTokenUtil.validateToken(token)) {
                    return ResVO.fail("401", "无效的认证令牌");
                }
                currentUserId = JwtTokenUtil.getUserIdFromToken(token);  // 从token中提取用户ID
            } else {
                return ResVO.fail("401", "缺少认证令牌");
            }
            
            // 检查用户ID是否存在
            if (currentUserId == null || currentUserId.isEmpty()) {
                return ResVO.fail("400", "无法获取用户信息");
            }
            
            // 发送私聊消息
        // 将DTO转换为实体对象，并设置当前用户ID和目标用户ID为null（表示私聊）
            Message message = MessageConvertUtil.toEntity(sendMessageDTO, currentUserId, null);
        // 将实体对象转换为业务对象并发送消息
            messageService.sendPrivateMessage(MessageConvertUtil.toBO(message));
            
            return ResVO.success("消息发送成功");
        } catch (Exception e) {
            e.printStackTrace(); // 打印异常堆栈信息，方便调试
            return ResVO.fail("500", "消息发送失败: " + e.getMessage());
        }
    }

    @RequestMapping("/sendPrivateMessage")
    public void sendPrivateMessage(@RequestBody SendMessageDTO sendMessageDTO) {
        // 发送私聊消息
        Message message = MessageConvertUtil.toEntity(sendMessageDTO);
        messageService.sendPrivateMessage(MessageConvertUtil.toBO(message));
    }

    @RequestMapping("/sendGroupMessage")
    public void sendGroupMessage(@RequestBody SendMessageDTO sendMessageDTO) {
        // 发送群聊消息
        Message message = MessageConvertUtil.toEntity(sendMessageDTO);
        messageService.sendGroupMessage(MessageConvertUtil.toBO(message));
    }
}