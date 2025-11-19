import { Client, Stomp } from '@stomp/stompjs';
import type { StompSubscription } from '@stomp/stompjs';
import { getToken } from '@/utils/auth';

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private connected = false;
  private userStatusCallback: ((data: any) => void) | null = null;
  private messageCallback: ((data: any) => void) | null = null;

  // 连接到WebSocket服务器
  connect(
    onUserStatusChange: (data: any) => void, 
    onMessageReceived?: (data: any) => void
  ) {
    // 如果已经连接，先断开
    if (this.connected) {
      this.disconnect();
    }
    
    this.userStatusCallback = onUserStatusChange;
    if (onMessageReceived) {
      this.messageCallback = onMessageReceived;
    }
    
    const token = getToken();
    if (!token) {
      console.error('无法连接WebSocket: 没有访问令牌');
      return;
    }

    // 创建STOMP客户端
    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws', // 使用WebSocket URL
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      debug: function (str) {
        console.log('STOMP: ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // 连接成功回调
    this.client.onConnect = (frame) => {
      console.log('WebSocket连接成功: ' + frame);
      this.connected = true;

      // 订阅用户状态变更消息
      const userStatusSubscription = this.client?.subscribe('/topic/user-status', (message) => {
        try {
          const data = JSON.parse(message.body);
          console.log('收到用户状态变更消息:', data);
          if (this.userStatusCallback) {
            console.log('调用用户状态回调函数');
            this.userStatusCallback(data);
            console.log('用户状态回调函数执行完成');
          } else {
            console.log('用户状态回调函数未设置');
          }
        } catch (error) {
          console.error('解析用户状态消息失败:', error);
        }
      });

      if (userStatusSubscription) {
        this.subscriptions.set('/topic/user-status', userStatusSubscription);
      }
      
      // 如果提供了消息回调，订阅聊天消息
      if (this.messageCallback) {
        const messageSubscription = this.client?.subscribe('/user/queue/messages', (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log('收到聊天消息:', data);
            if (this.messageCallback) {
              this.messageCallback(data);
            }
          } catch (error) {
            console.error('解析聊天消息失败:', error);
          }
        });

        if (messageSubscription) {
          this.subscriptions.set('/user/queue/messages', messageSubscription);
        }
      }
    };

    // 连接错误回调
    this.client.onStompError = (frame) => {
      console.error('WebSocket连接错误: ', frame.headers['message']);
      console.error('详细错误信息: ', frame.body);
    };
    
    // WebSocket断开连接回调
    this.client.onDisconnect = () => {
      console.log('WebSocket连接已断开');
      this.connected = false;
    };

    // 启动连接
    this.client.activate();
  }

  // 断开连接
  disconnect() {
    if (this.client) {
      // 取消所有订阅
      this.subscriptions.forEach((subscription, destination) => {
        subscription.unsubscribe();
        console.log('已取消订阅:', destination);
      });
      this.subscriptions.clear();

      // 断开连接
      this.client.deactivate();
      this.connected = false;
      console.log('WebSocket连接已断开');
    }
  }

  // 发送用户状态变更消息
  sendUserStatus(status: boolean) {
    if (this.client && this.connected) {
      const message = {
        status: status,
        timestamp: Date.now()
      };
      
      this.client.publish({
        destination: '/app/user/status',
        body: JSON.stringify(message)
      });
      
      console.log('已发送用户状态消息:', message);
    } else {
      console.error('WebSocket未连接，无法发送消息');
    }
  }

  // 获取当前用户信息
  getUserInfo() {
    if (this.client && this.connected) {
      this.client.publish({
        destination: '/app/user/info',
        body: JSON.stringify({})
      });
      
      console.log('已请求用户信息');
    } else {
      console.error('WebSocket未连接，无法发送消息');
    }
  }
  
  // 发送聊天消息
  sendMessage(message: any) {
    if (this.client && this.connected) {
      this.client.publish({
        destination: '/app/chat',
        body: JSON.stringify(message)
      });
      
      console.log('已发送聊天消息:', message);
    } else {
      console.error('WebSocket未连接，无法发送消息');
    }
  }

  // 检查连接状态
  isConnected(): boolean {
    return this.connected;
  }
}

// 创建单例实例
const webSocketService = new WebSocketService();

export default webSocketService;