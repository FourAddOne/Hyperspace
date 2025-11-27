import { Client, Stomp } from '@stomp/stompjs';
import type { StompSubscription } from '@stomp/stompjs';
import { getAccessToken } from '@/utils/auth';

// 简单的日志级别控制
const isDevelopment = process.env.NODE_ENV === 'development';

const log = {
  debug: (...args: any[]) => {
    if (isDevelopment) {
      console.log('[DEBUG]', ...args);
    }
  },
  info: (...args: any[]) => {
    if (isDevelopment) {
      console.log('[INFO]', ...args);
    }
  },
  warn: (...args: any[]) => {
    console.warn('[WARN]', ...args);
  },
  error: (...args: any[]) => {
    console.error('[ERROR]', ...args);
  }
};

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private connected = false;
  private userStatusCallback: ((data: any) => void) | null = null;
  private messageCallback: ((data: any) => void) | null = null;
  private reconnectTimeout: any = null;
  private maxReconnectAttempts = 5;
  private reconnectAttempts = 0;

  // 连接到WebSocket服务器
  connect(
    onUserStatusChange: (data: any) => void, 
    onMessageReceived?: (data: any) => void
  ) {
    // 如果已经连接并且回调函数没有变化，则不需要重新连接
    if (this.connected && 
        this.userStatusCallback === onUserStatusChange && 
        this.messageCallback === onMessageReceived) {
      log.debug('WebSocket已经连接且回调函数未变化，无需重新连接');
      return;
    }
    
    // 如果已经连接但回调函数有变化，只需要更新回调函数和重新订阅
    if (this.connected) {
      log.debug('WebSocket已连接，更新回调函数');
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
    
    const token = getAccessToken();
    if (!token) {
      log.error('无法连接WebSocket: 没有访问令牌');
      this.scheduleReconnect();
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
        log.debug('[STOMP]', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // 连接成功回调
    this.client.onConnect = (frame) => {
      log.info('WebSocket连接成功');
      this.connected = true;
      this.reconnectAttempts = 0; // 重置重连尝试次数

      // 订阅用户状态变更消息
      this.subscribeToUserStatus();

      // 如果提供了消息回调，订阅聊天消息
      if (this.messageCallback) {
        this.subscribeToMessages();
      }
    };

    // 连接错误回调
    this.client.onStompError = (frame) => {
      log.error('WebSocket连接错误: ', frame.headers['message']);
      log.debug('详细错误信息: ', frame.body);
      this.handleConnectionError();
    };
    
    // WebSocket断开连接回调
    this.client.onDisconnect = () => {
      log.info('WebSocket断开连接');
      this.connected = false;
      this.handleConnectionError();
    };

    // WebSocket连接失败回调
    this.client.onWebSocketError = (event) => {
      log.error('WebSocket连接失败:', event);
      this.handleConnectionError();
    };

    // 启动连接
    log.debug('激活WebSocket客户端');
    this.client.activate();
  }

  // 处理连接错误
  private handleConnectionError() {
    this.connected = false;
    this.scheduleReconnect();
  }

  // 安排重新连接
  private scheduleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      log.info(`尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      
      // 清除之前的重连定时器
      if (this.reconnectTimeout) {
        clearTimeout(this.reconnectTimeout);
      }
      
      // 设置重新连接定时器
      this.reconnectTimeout = setTimeout(() => {
        log.debug('尝试重新连接WebSocket...');
        const token = getAccessToken();
        if (token) {
          this.connect(this.userStatusCallback!, this.messageCallback);
        }
      }, 3000 * this.reconnectAttempts); // 指数退避策略
    } else {
      log.error('达到最大重连尝试次数，停止重连');
    }
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
    log.debug('重新订阅WebSocket主题');
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
    
    log.debug('订阅用户状态变更消息: /topic/user-status');
    const userStatusSubscription = this.client.subscribe('/topic/user-status', (message) => {
      try {
        const data = JSON.parse(message.body);
        log.debug('收到用户状态变更消息:', data);
        if (this.userStatusCallback) {
          this.userStatusCallback(data);
        }
      } catch (error) {
        log.error('解析用户状态消息失败:', error);
      }
    });

    if (userStatusSubscription) {
      this.subscriptions.set('/topic/user-status', userStatusSubscription);
    }
  }

  // 订阅聊天消息
  private subscribeToMessages() {
    if (!this.client || !this.messageCallback) return;
    
    log.debug('订阅聊天消息: /user/queue/messages');
    const messageSubscription = this.client.subscribe('/user/queue/messages', (message) => {
      try {
        const data = JSON.parse(message.body);
        log.debug('收到聊天消息:', data);
        if (this.messageCallback) {
          this.messageCallback(data);
        }
      } catch (error) {
        log.error('解析聊天消息失败:', error);
      }
    });

    if (messageSubscription) {
      this.subscriptions.set('/user/queue/messages', messageSubscription);
    }
  }

  // 断开连接
  disconnect() {
    // 清除重连定时器
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    
    if (this.client) {
      log.info('断开WebSocket连接');
      // 取消所有订阅
      this.subscriptions.forEach((subscription, destination) => {
        subscription.unsubscribe();
      });
      this.subscriptions.clear();

      // 断开连接
      this.client.deactivate();
      this.connected = false;
      this.reconnectAttempts = 0;
    }
  }

  // 发送用户状态变更消息
  sendUserStatus(status: boolean) {
    if (this.client && this.connected) {
      const message = {
        status: status,
        timestamp: Date.now()
      };
      
      log.debug('发送用户状态消息:', message);
      this.client.publish({
        destination: '/app/user/status',
        body: JSON.stringify(message)
      });
    } else {
      log.error('WebSocket未连接，无法发送用户状态消息');
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
      log.error('WebSocket未连接，无法发送消息');
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
      log.error('WebSocket未连接，无法发送聊天消息');
    }
  }

  // 检查连接状态
  isConnected(): boolean {
    log.debug('检查WebSocket连接状态:', this.connected);
    return this.connected;
  }
}

// 创建单例实例
const webSocketService = new WebSocketService();

export default webSocketService;