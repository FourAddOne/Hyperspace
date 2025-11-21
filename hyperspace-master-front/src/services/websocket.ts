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
    // 如果已经连接并且回调函数没有变化，则不需要重新连接
    if (this.connected && 
        this.userStatusCallback === onUserStatusChange && 
        this.messageCallback === onMessageReceived) {
      console.log('WebSocket已经连接且回调函数未变化，无需重新连接');
      return;
    }
    
    // 如果已经连接但回调函数有变化，只需要更新回调函数和重新订阅
    if (this.connected) {
      console.log('WebSocket已连接，更新回调函数');
      this.userStatusCallback = onUserStatusChange;
      if (onMessageReceived) {
        this.messageCallback = onMessageReceived;
      }
      // 重新订阅以使用新的回调函数
      this.resubscribe();
      return;
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
      // 启用STOMP客户端调试模式
      debug: function (str) {
        // 启用调试日志
        // console.log('[STOMP DEBUG]', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // 连接成功回调
    this.client.onConnect = (frame) => {
      // console.log('WebSocket连接成功:', frame);
      this.connected = true;

      // 订阅用户状态变更消息
      this.subscribeToUserStatus();

      // 如果提供了消息回调，订阅聊天消息
      if (this.messageCallback) {
        this.subscribeToMessages();
      }
    };

    // 连接错误回调
    this.client.onStompError = (frame) => {
      // console.error('WebSocket连接错误: ', frame.headers['message']);
      // console.error('详细错误信息: ', frame.body);
    };
    
    // WebSocket断开连接回调
    this.client.onDisconnect = () => {
      // console.log('WebSocket断开连接');
      this.connected = false;
    };

    // WebSocket连接失败回调
    this.client.onWebSocketError = (event) => {
      // console.error('WebSocket连接失败:', event);
    };

    // 启动连接
    // console.log('激活WebSocket客户端');
    this.client.activate();
  }

  // 更新用户状态回调函数
  updateUserStatusCallback(callback: (data: any) => void) {
    this.userStatusCallback = callback;
  }

  // 更新消息回调函数
  updateMessageCallback(callback: (data: any) => void) {
    this.messageCallback = callback;
    // 如果已经连接，重新订阅消息以使用新的回调函数
    if (this.connected) {
      this.resubscribe();
    }
  }

  // 重新订阅主题
  private resubscribe() {
    // console.log('重新订阅WebSocket主题');
    // 取消现有订阅
    this.subscriptions.forEach((subscription, destination) => {
      subscription.unsubscribe();
    });
    this.subscriptions.clear();

    // 重新订阅
    this.subscribeToUserStatus();
    if (this.messageCallback) {
      this.subscribeToMessages();
    }
  }

  // 订阅用户状态变更消息
  private subscribeToUserStatus() {
    if (!this.client) return;
    
    // console.log('订阅用户状态变更消息: /topic/user-status');
    const userStatusSubscription = this.client.subscribe('/topic/user-status', (message) => {
      try {
        const data = JSON.parse(message.body);
        // console.log('收到用户状态变更消息:', data);
        if (this.userStatusCallback) {
          this.userStatusCallback(data);
        }
      } catch (error) {
        // console.error('解析用户状态消息失败:', error);
      }
    });

    if (userStatusSubscription) {
      this.subscriptions.set('/topic/user-status', userStatusSubscription);
    }
  }

  // 订阅聊天消息
  private subscribeToMessages() {
    if (!this.client || !this.messageCallback) return;
    
    // console.log('订阅聊天消息: /user/queue/messages');
    const messageSubscription = this.client.subscribe('/user/queue/messages', (message) => {
      try {
        const data = JSON.parse(message.body);
        // console.log('收到聊天消息:', data);
        if (this.messageCallback) {
          this.messageCallback(data);
        }
      } catch (error) {
        // console.error('解析聊天消息失败:', error);
      }
    });

    if (messageSubscription) {
      this.subscriptions.set('/user/queue/messages', messageSubscription);
    }
  }

  // 断开连接
  disconnect() {
    if (this.client) {
      // console.log('断开WebSocket连接');
      // 取消所有订阅
      this.subscriptions.forEach((subscription, destination) => {
        subscription.unsubscribe();
      });
      this.subscriptions.clear();

      // 断开连接
      this.client.deactivate();
      this.connected = false;
    }
  }

  // 发送用户状态变更消息
  sendUserStatus(status: boolean) {
    if (this.client && this.connected) {
      const message = {
        status: status,
        timestamp: Date.now()
      };
      
      // console.log('发送用户状态消息:', message);
      this.client.publish({
        destination: '/app/user/status',
        body: JSON.stringify(message)
      });
    } else {
      // console.error('WebSocket未连接，无法发送消息');
    }
  }

  // 获取当前用户信息
  getUserInfo() {
    if (this.client && this.connected) {
      this.client.publish({
        destination: '/app/user/info',
        body: JSON.stringify({})
      });
    } else {
      // console.error('WebSocket未连接，无法发送消息');
    }
  }
  
  // 发送聊天消息
  sendMessage(message: any) {
    if (this.client && this.connected) {
      this.client.publish({
        destination: '/app/chat',
        body: JSON.stringify(message)
      });
    } else {
      // console.error('WebSocket未连接，无法发送消息');
    }
  }

  // 检查连接状态
  isConnected(): boolean {
    // console.log('检查WebSocket连接状态:', this.connected);
    return this.connected;
  }
}

// 创建单例实例
const webSocketService = new WebSocketService();

export default webSocketService;